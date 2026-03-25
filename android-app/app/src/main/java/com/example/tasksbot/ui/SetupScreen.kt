package com.example.tasksbot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tasksbot.ui.AuthViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun SetupScreen(
    onDone: () -> Unit,
) {
    val authVm: AuthViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var channelId by remember { mutableStateOf("") }
    var channelLink by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Первичная настройка (PIN + Telegram)", modifier = Modifier.padding(bottom = 4.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = pin2,
            onValueChange = { pin2 = it },
            label = { Text("Повторите PIN") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("BOT_TOKEN") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = channelId,
            onValueChange = { channelId = it },
            label = { Text("CHANNEL_ID (например -100123...)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = channelLink,
            onValueChange = { channelLink = it },
            label = { Text("CHANNEL_LINK (необязательно)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (error != null) {
            Text(error ?: "", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                error = null
                val pinTrim = pin.trim()
                if (pinTrim.length < 4) {
                    error = "PIN должен быть не короче 4 символов."
                    return@Button
                }
                if (pinTrim != pin2.trim()) {
                    error = "PIN не совпадают."
                    return@Button
                }
                if (token.trim().isEmpty() || channelId.trim().isEmpty()) {
                    error = "Заполните BOT_TOKEN и CHANNEL_ID."
                    return@Button
                }

                scope.launch {
                    authVm.setup(
                        pinRaw = pinTrim,
                        botToken = token.trim(),
                        channelId = channelId.trim(),
                        channelLink = channelLink.trim().ifEmpty { null },
                    )
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сохранить и продолжить")
        }
    }
}

