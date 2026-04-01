package com.example.tasksbot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tasksbot.ui.components.ScreenWelcomeStrip
import com.example.tasksbot.ui.components.SectionGroupCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onDone: () -> Unit,
) {
    val authVm: AuthViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var pin by rememberSaveable { mutableStateOf("") }
    var pin2 by rememberSaveable { mutableStateOf("") }
    var token by rememberSaveable { mutableStateOf("") }
    var channelId by rememberSaveable { mutableStateOf("") }
    var channelLink by rememberSaveable { mutableStateOf("") }
    var maxBotToken by rememberSaveable { mutableStateOf("") }
    var maxChatId by rememberSaveable { mutableStateOf("") }
    var vkAccessToken by rememberSaveable { mutableStateOf("") }
    var vkGroupId by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val scheme = MaterialTheme.colorScheme
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Первичная настройка",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.surface,
                    titleContentColor = scheme.onSurface,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenWelcomeStrip(
                title = "Добро пожаловать",
                subtitle = "PIN, Telegram, MAX и VK — данные хранятся на устройстве",
            )

            SectionGroupCard(title = "PIN начальника") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it },
                        label = { Text("PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                    OutlinedTextField(
                        value = pin2,
                        onValueChange = { pin2 = it },
                        label = { Text("Повторите PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            }

            SectionGroupCard(title = "Telegram") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("BOT_TOKEN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                    OutlinedTextField(
                        value = channelId,
                        onValueChange = { channelId = it },
                        label = { Text("CHANNEL_ID (например -100123...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                    OutlinedTextField(
                        value = channelLink,
                        onValueChange = { channelLink = it },
                        label = { Text("CHANNEL_LINK (необязательно)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            }

            SectionGroupCard(title = "MAX") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = maxBotToken,
                        onValueChange = { maxBotToken = it },
                        label = { Text("MAX_BOT_TOKEN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                    OutlinedTextField(
                        value = maxChatId,
                        onValueChange = { maxChatId = it },
                        label = { Text("MAX_CHAT_ID (ID чата/группы)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            }

            SectionGroupCard(title = "ВКонтакте") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = vkAccessToken,
                        onValueChange = { vkAccessToken = it },
                        label = { Text("VK_ACCESS_TOKEN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                    OutlinedTextField(
                        value = vkGroupId,
                        onValueChange = { vkGroupId = it },
                        label = { Text("VK_GROUP_ID (id группы, например 12345)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            }

            error?.let {
                Text(it, color = scheme.error, style = MaterialTheme.typography.bodyMedium)
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
                    if (maxBotToken.trim().isEmpty() || maxChatId.trim().isEmpty()) {
                        error = "Заполните MAX_BOT_TOKEN и MAX_CHAT_ID."
                        return@Button
                    }
                    if (vkAccessToken.trim().isEmpty() || vkGroupId.trim().isEmpty()) {
                        error = "Заполните VK_ACCESS_TOKEN и VK_GROUP_ID."
                        return@Button
                    }

                    scope.launch {
                        authVm.setup(
                            pinRaw = pinTrim,
                            botToken = token.trim(),
                            channelId = channelId.trim(),
                            channelLink = channelLink.trim().ifEmpty { null },
                            maxBotToken = maxBotToken.trim(),
                            maxChatId = maxChatId.trim(),
                            vkAccessToken = vkAccessToken.trim(),
                            vkGroupId = vkGroupId.trim(),
                        )
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("Сохранить и продолжить", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}