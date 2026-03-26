package com.example.tasksbot.network

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class VkClient(
    private val okHttpClient: OkHttpClient = OkHttpClient(),
) {
    private val gson = Gson()

    data class VkError(
        val error_code: Int? = null,
        val error_msg: String? = null,
    )

    data class WallPostResponse(
        val post_id: Long? = null,
    )

    suspend fun postWall(
        accessToken: String,
        ownerId: String,
        message: String,
    ): Long = withContext(Dispatchers.IO) {
        val url = "https://api.vk.com/method/wall.post"
        val form = FormBody.Builder()
            .add("access_token", accessToken)
            .add("owner_id", ownerId)
            .add("from_group", "1")
            .add("message", message)
            .add("v", "5.199")
            .build()

        val req = Request.Builder()
            .url(url)
            .post(form)
            .build()

        okHttpClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("VK HTTP ${resp.code}: $body")
            }

            val parsed = gson.fromJson(body, VkResponseWallPost::class.java)
            val err = parsed.error
            if (err != null) throw IllegalStateException("VK error ${err.error_code}: ${err.error_msg}")

            parsed.response?.post_id ?: 0L
        }
    }

    private data class VkResponseWallPost(
        val response: WallPostResponse? = null,
        val error: VkError? = null,
    )
}

