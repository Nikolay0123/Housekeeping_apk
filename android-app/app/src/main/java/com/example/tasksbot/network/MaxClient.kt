package com.example.tasksbot.network

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MaxClient(
    private val okHttpClient: OkHttpClient = OkHttpClient(),
) {
    private val gson = Gson()

    data class MaxSendResponse(
        val ok: Boolean? = null,
        val result: Any? = null,
    )

    suspend fun sendMessage(
        botToken: String,
        chatId: String,
        text: String,
    ) = withContext(Dispatchers.IO) {
        // MAX Bot API (https://dev.max.ru/..., endpoints могут меняться)
        val url = "https://platform-api.max.ru/messages?chat_id=$chatId"
        val jsonBody = gson.toJson(
            mapOf(
                "text" to text,
            ),
        )

        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $botToken")
            .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        okHttpClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("MAX HTTP ${resp.code}: $body")
            }
            // Обычно MAX возвращает JSON, но структура может отличаться.
            // Нам важна успешность HTTP-кода.
            gson.fromJson(body, MaxSendResponse::class.java)
            true
        }
    }
}

