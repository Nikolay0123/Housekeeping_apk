package com.example.tasksbot.network

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Клиент Open API Bnovo PMS (JWT + /bookings).
 * Форматы полей в ответе могут отличаться — разбор сделан устойчивым к вложенности и именам.
 */
class BnovoClient(
    private val client: OkHttpClient = defaultClient(),
) {
    private val gson = Gson()
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    data class NormalizedBooking(
        val roomLabel: String,
        val arrival: LocalDate,
        val departure: LocalDate,
    )

    private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun fetchAccessToken(accountId: String, apiKey: String): String = withContext(Dispatchers.IO) {
        val body = gson.toJson(
            mapOf(
                "id" to accountId.trim(),
                "password" to apiKey.trim(),
            ),
        )
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/auth")
            .post(body.toRequestBody(JSON))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw BnovoApiException("Bnovo auth ${resp.code}: ${text.take(500)}")
            }
            extractAccessToken(text)
                ?: throw BnovoApiException("В ответе Bnovo нет access_token. Фрагмент: ${text.take(400)}")
        }
    }

    suspend fun fetchBookingsNormalized(
        accessToken: String,
        dateFrom: LocalDate,
        dateTo: LocalDate,
    ): List<NormalizedBooking> = withContext(Dispatchers.IO) {
        val from = dateFrom.format(dateFmt)
        val to = dateTo.format(dateFmt)
        val url = "$BASE_URL/api/v1/bookings?date_from=$from&date_to=$to"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${accessToken.trim()}")
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw BnovoApiException("Bnovo bookings ${resp.code}: ${text.take(500)}")
            }
            parseBookingsPayload(text)
        }
    }

    private fun extractAccessToken(json: String): String? {
        val root = runCatching { gson.fromJson(json, JsonObject::class.java) }.getOrNull() ?: return null
        listOf("access_token", "token", "jwt").forEach { k ->
            root.getString(k)?.takeIf { it.isNotBlank() }?.let { return it }
        }
        val data = root.getAsJsonObject("data") ?: return null
        listOf("access_token", "token").forEach { k ->
            data.getString(k)?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    private fun parseBookingsPayload(json: String): List<NormalizedBooking> {
        val root = runCatching { gson.fromJson(json, JsonElement::class.java) }.getOrNull()
            ?: return emptyList()
        val arr = extractArray(root) ?: return emptyList()
        val out = ArrayList<NormalizedBooking>()
        for (el in arr) {
            if (!el.isJsonObject) continue
            val o = el.asJsonObject
            val arrival = parseDateFromObject(o) ?: continue
            val departure = parseDepartureDate(o) ?: continue
            val roomLabels = extractRoomLabels(o)
            for (label in roomLabels) {
                out.add(NormalizedBooking(roomLabel = label, arrival = arrival, departure = departure))
            }
        }
        return out
    }

    private fun extractArray(root: JsonElement): JsonArray? {
        if (root.isJsonArray) return root.asJsonArray
        if (!root.isJsonObject) return null
        val o = root.asJsonObject
        listOf("data", "bookings", "items", "result").forEach { key ->
            val el = o[key] ?: return@forEach
            if (el.isJsonArray) return el.asJsonArray
            if (el.isJsonObject) {
                val inner = el.asJsonObject["booking"] ?: el.asJsonObject["data"]
                if (inner != null && inner.isJsonArray) return inner.asJsonArray
            }
        }
        return null
    }

    private fun parseDateFromObject(o: JsonObject): LocalDate? {
        val keys = listOf(
            "date_arrival",
            "date_arrival_hotel",
            "arrival_date",
            "arrival",
            "check_in",
            "checkin",
            "date_from",
            "dateCheckIn",
            "start_date",
        )
        for (k in keys) {
            parseDate(o.get(k))?.let { return it }
        }
        val room = o["room"]?.takeIf { it.isJsonObject }?.asJsonObject
        if (room != null) {
            for (k in keys) {
                parseDate(room.get(k))?.let { return it }
            }
        }
        return null
    }

    private fun parseDepartureDate(o: JsonObject): LocalDate? {
        val keys = listOf(
            "date_departure",
            "date_departure_hotel",
            "departure_date",
            "departure",
            "check_out",
            "checkout",
            "date_to",
            "dateCheckOut",
            "end_date",
        )
        for (k in keys) {
            parseDate(o.get(k))?.let { return it }
        }
        val room = o["room"]?.takeIf { it.isJsonObject }?.asJsonObject
        if (room != null) {
            for (k in keys) {
                parseDate(room.get(k))?.let { return it }
            }
        }
        return null
    }

    private fun parseDate(el: JsonElement?): LocalDate? {
        if (el == null || el.isJsonNull) return null
        val s = when {
            el.isJsonPrimitive && el.asJsonPrimitive.isString -> el.asString.trim()
            else -> el.toString().trim('"')
        }
        if (s.isEmpty()) return null
        val dayPart = s.take(10)
        return runCatching { LocalDate.parse(dayPart, dateFmt) }.getOrNull()
    }

    private fun extractRoomLabels(o: JsonObject): List<String> {
        val labels = LinkedHashSet<String>()
        val directKeys = listOf("room_name", "room_number", "roomNumber", "apartment", "room_title")
        for (k in directKeys) {
            o.getString(k)?.let { normalizeRoomLabel(it)?.let { l -> labels.add(l) } }
        }
        val room = o["room"]
        if (room != null && room.isJsonObject) {
            val ro = room.asJsonObject
            for (k in listOf("name", "title", "number", "room_number", "id")) {
                val raw = ro.get(k) ?: continue
                if (raw.isJsonPrimitive && raw.asJsonPrimitive.isString) {
                    normalizeRoomLabel(raw.asString)?.let { labels.add(it) }
                } else if (raw.isJsonPrimitive && raw.asJsonPrimitive.isNumber) {
                    normalizeRoomLabel(raw.asNumber.toString())?.let { labels.add(it) }
                }
            }
        }
        if (labels.isEmpty()) {
            val id = o["room_id"] ?: o["id_room"]
            if (id != null && id.isJsonPrimitive && id.asJsonPrimitive.isNumber) {
                labels.add("id:${id.asNumber}")
            }
        }
        return labels.toList()
    }

    private fun JsonObject.getString(key: String): String? {
        val el = this[key] ?: return null
        if (!el.isJsonPrimitive || !el.asJsonPrimitive.isString) return null
        return el.asString
    }

    class BnovoApiException(message: String) : Exception(message)

    companion object {
        private const val BASE_URL = "https://api.pms.bnovo.ru"
        private val JSON = "application/json; charset=utf-8".toMediaType()

        /** "101", "Номер 101" → канон для сопоставления с базой приложения. */
        fun normalizeRoomLabel(raw: String): String? {
            val t = raw.trim()
            if (t.startsWith("id:")) return t
            val digits = Regex("(\\d{3,4}(?:\\.\\d+)?)").find(t)?.groupValues?.getOrNull(1)
            if (digits != null) return "Номер $digits"
            val n = t.removePrefix("Номер").trim()
            if (n.all { it.isDigit() } && n.length in 3..4) return "Номер $n"
            return null
        }
    }
}
