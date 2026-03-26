package com.example.tasksbot.data

data class AppSettings(
    val pinHash: String? = null,
    val botToken: String? = null,
    val channelId: String? = null,
    val channelLink: String? = null,
    val maxBotToken: String? = null,
    val maxChatId: String? = null,
    val vkAccessToken: String? = null,
    val vkGroupId: String? = null,
)

