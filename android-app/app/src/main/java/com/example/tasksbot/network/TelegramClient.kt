package com.example.tasksbot.network

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class TelegramClient(
    private val okHttpClient: OkHttpClient = OkHttpClient(),
) {
    private val gson = Gson()

    /**
     * @return message_id отправленного сообщения
     */
    suspend fun sendMessage(
        botToken: String,
        channelId: String,
        text: String,
    ): Long = withContext(Dispatchers.IO) {
        val url = "https://api.telegram.org/bot$botToken/sendMessage"
        val payload = mapOf(
            "chat_id" to channelId,
            "text" to text,
        )
        val json = gson.toJson(payload)

        val req = Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        okHttpClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("Telegram HTTP ${resp.code}: $body")
            }
            val parsed = gson.fromJson(body, TelegramSendResponse::class.java)
            if (parsed.ok != true || parsed.result == null) {
                throw IllegalStateException("Telegram error: $body")
            }
            parsed.result!!.messageId
        }
    }

    private data class TelegramSendResponse(
        val ok: Boolean? = null,
        val result: TelegramMessageId? = null,
    )

    private data class TelegramMessageId(
        val messageId: Long,
    )
}

