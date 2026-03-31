package com.example.tasksbot.ui

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tasksbot.data.AppSettingsRepository
import com.example.tasksbot.data.AppSettings
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.security.MessageDigest

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AppSettingsRepository(application.applicationContext)

    val isSetupComplete = mutableStateOf(false)
    val isUnlocked = mutableStateOf(false)

    private var currentPinHash: String? = null
    private var currentSettings: AppSettings? = null

    val botToken = mutableStateOf<String?>(null)
    val channelId = mutableStateOf<String?>(null)
    val channelLink = mutableStateOf<String?>(null)
    val maxBotToken = mutableStateOf<String?>(null)
    val maxChatId = mutableStateOf<String?>(null)
    val vkAccessToken = mutableStateOf<String?>(null)
    val vkGroupId = mutableStateOf<String?>(null)
    val bnovoAccountId = mutableStateOf<String?>(null)
    val bnovoApiKey = mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            repo.isSetupCompleteFlow.collectLatest { complete ->
                isSetupComplete.value = complete
            }
        }
        viewModelScope.launch {
            repo.settingsFlow.collectLatest { settings ->
                currentPinHash = settings.pinHash
                currentSettings = settings
                botToken.value = settings.botToken
                channelId.value = settings.channelId
                channelLink.value = settings.channelLink
                maxBotToken.value = settings.maxBotToken
                maxChatId.value = settings.maxChatId
                vkAccessToken.value = settings.vkAccessToken
                vkGroupId.value = settings.vkGroupId
                bnovoAccountId.value = settings.bnovoAccountId
                bnovoApiKey.value = settings.bnovoApiKey
            }
        }
    }

    fun login(pinRaw: String): Boolean {
        val hash = sha256Hex(pinRaw.trim())
        val ok = currentPinHash != null && hash == currentPinHash
        isUnlocked.value = ok
        return ok
    }

    fun logout() {
        isUnlocked.value = false
    }

    suspend fun setup(
        pinRaw: String,
        botToken: String,
        channelId: String,
        channelLink: String?,
        maxBotToken: String?,
        maxChatId: String?,
        vkAccessToken: String?,
        vkGroupId: String?,
    ) {
        repo.setAll(
            pinRaw = pinRaw,
            botToken = botToken,
            channelId = channelId,
            channelLink = channelLink,
            maxBotToken = maxBotToken,
            maxChatId = maxChatId,
            vkAccessToken = vkAccessToken,
            vkGroupId = vkGroupId,
        )
        isUnlocked.value = true
    }

    suspend fun saveBnovoCredentials(accountId: String?, apiKey: String?) {
        repo.setBnovoCredentials(accountId, apiKey)
    }

    suspend fun reset() {
        repo.clearAll()
        isUnlocked.value = false
    }

    private fun sha256Hex(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { b -> "%02x".format(b) }
    }
}

