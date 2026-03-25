package com.example.tasksbot.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private const val DATASTORE_NAME = "tasksbot_settings"

private val Context.dataStore by preferencesDataStore(DATASTORE_NAME)

class AppSettingsRepository(private val context: Context) {
    private val KEY_PIN_HASH = stringPreferencesKey("pinHash")
    private val KEY_BOT_TOKEN = stringPreferencesKey("botToken")
    private val KEY_CHANNEL_ID = stringPreferencesKey("channelId")
    private val KEY_CHANNEL_LINK = stringPreferencesKey("channelLink")
    private val KEY_SETUP_COMPLETE = booleanPreferencesKey("setupComplete")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs: Preferences ->
        AppSettings(
            pinHash = prefs[KEY_PIN_HASH],
            botToken = prefs[KEY_BOT_TOKEN],
            channelId = prefs[KEY_CHANNEL_ID],
            channelLink = prefs[KEY_CHANNEL_LINK],
        )
    }

    val isSetupCompleteFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SETUP_COMPLETE] ?: false
    }

    suspend fun setAll(
        pinRaw: String,
        botToken: String,
        channelId: String,
        channelLink: String?,
    ) {
        val pinHash = sha256Hex(pinRaw.trim())
        context.dataStore.edit { prefs ->
            prefs[KEY_PIN_HASH] = pinHash
            prefs[KEY_BOT_TOKEN] = botToken.trim()
            prefs[KEY_CHANNEL_ID] = channelId.trim()
            if (channelLink != null) {
                prefs[KEY_CHANNEL_LINK] = channelLink.trim()
            } else {
                prefs.remove(KEY_CHANNEL_LINK)
            }
            prefs[KEY_SETUP_COMPLETE] = true
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_PIN_HASH)
            prefs.remove(KEY_BOT_TOKEN)
            prefs.remove(KEY_CHANNEL_ID)
            prefs.remove(KEY_CHANNEL_LINK)
            prefs[KEY_SETUP_COMPLETE] = false
        }
    }

    private fun sha256Hex(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { b -> "%02x".format(b) }
    }
}

