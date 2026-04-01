package com.example.tasksbot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tasksbot.network.BnovoClient
import com.example.tasksbot.network.NetworkStatus
import com.example.tasksbot.ui.components.ScreenWelcomeStrip
import com.example.tasksbot.ui.components.SectionGroupCard
import com.example.tasksbot.ui.components.StandardTopBar
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBackToMenu: () -> Unit,
) {
    val authVm: AuthViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val bnovoClient = remember { BnovoClient() }

    var botToken by remember { mutableStateOf("") }
    var channelId by remember { mutableStateOf("") }
    var channelLink by remember { mutableStateOf("") }
    var maxBotToken by remember { mutableStateOf("") }
    var maxChatId by remember { mutableStateOf("") }
    var vkAccessToken by remember { mutableStateOf("") }
    var vkGroupId by remember { mutableStateOf("") }

    var bnovoId by remember { mutableStateOf("") }
    var bnovoKey by remember { mutableStateOf("") }
    var bnovoMsg by remember { mutableStateOf<String?>(null) }

    var messengerMsg by remember { mutableStateOf<String?>(null) }

    var bnovoDiagOpen by remember { mutableStateOf(false) }
    var bnovoDiagText by remember { mutableStateOf("") }
    var bnovoDiagLoading by remember { mutableStateOf(false) }

    LaunchedEffect(
        authVm.botToken.value,
        authVm.channelId.value,
        authVm.channelLink.value,
        authVm.maxBotToken.value,
        authVm.maxChatId.value,
        authVm.vkAccessToken.value,
        authVm.vkGroupId.value,
        authVm.bnovoAccountId.value,
    ) {
        botToken = authVm.botToken.value.orEmpty()
        channelId = authVm.channelId.value.orEmpty()
        channelLink = authVm.channelLink.value.orEmpty()
        maxBotToken = authVm.maxBotToken.value.orEmpty()
        maxChatId = authVm.maxChatId.value.orEmpty()
        vkAccessToken = authVm.vkAccessToken.value.orEmpty()
        vkGroupId = authVm.vkGroupId.value.orEmpty()
        bnovoId = authVm.bnovoAccountId.value.orEmpty()
    }

    Scaffold(
        topBar = {
            StandardTopBar(
                title = "Настройки",
                subtitle = "Токены, ID каналов и Bnovo",
                onNavigateBack = onBackToMenu,
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
                title = "Интеграции",
                subtitle = "Данные хранятся только на устройстве",
            )

            SectionGroupCard(title = "Telegram") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = botToken,
                        onValueChange = { botToken = it; messengerMsg = null },
                        label = { Text("BOT_TOKEN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                    OutlinedTextField(
                        value = channelId,
                        onValueChange = { channelId = it; messengerMsg = null },
                        label = { Text("CHANNEL_ID (например -100123…)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                    OutlinedTextField(
                        value = channelLink,
                        onValueChange = { channelLink = it; messengerMsg = null },
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
                        onValueChange = { maxBotToken = it; messengerMsg = null },
                        label = { Text("MAX_BOT_TOKEN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                    OutlinedTextField(
                        value = maxChatId,
                        onValueChange = { maxChatId = it; messengerMsg = null },
                        label = { Text("MAX_CHAT_ID") },
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
                        onValueChange = { vkAccessToken = it; messengerMsg = null },
                        label = { Text("VK_ACCESS_TOKEN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                    OutlinedTextField(
                        value = vkGroupId,
                        onValueChange = { vkGroupId = it; messengerMsg = null },
                        label = { Text("VK_GROUP_ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        messengerMsg = null
                        if (botToken.isBlank() || channelId.isBlank()) {
                            messengerMsg = "Заполните BOT_TOKEN и CHANNEL_ID."
                            return@launch
                        }
                        if (maxBotToken.isBlank() || maxChatId.isBlank()) {
                            messengerMsg = "Заполните MAX_BOT_TOKEN и MAX_CHAT_ID."
                            return@launch
                        }
                        if (vkAccessToken.isBlank() || vkGroupId.isBlank()) {
                            messengerMsg = "Заполните VK_ACCESS_TOKEN и VK_GROUP_ID."
                            return@launch
                        }
                        authVm.saveMessengerIntegration(
                            botToken = botToken.trim(),
                            channelId = channelId.trim(),
                            channelLink = channelLink.trim().ifEmpty { null },
                            maxBotToken = maxBotToken.trim(),
                            maxChatId = maxChatId.trim(),
                            vkAccessToken = vkAccessToken.trim(),
                            vkGroupId = vkGroupId.trim(),
                        )
                        messengerMsg = "Сохранено."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("Сохранить Telegram, MAX и VK", fontWeight = FontWeight.Medium)
            }
            messengerMsg?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SectionGroupCard(title = "Bnovo PMS") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "ID аккаунта и API-ключ из Bnovo Octopus → API-доступ. Ключ можно оставить пустым, чтобы не перезаписывать.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = bnovoId,
                        onValueChange = { bnovoId = it; bnovoMsg = null },
                        label = { Text("Bnovo: ID аккаунта") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                    OutlinedTextField(
                        value = bnovoKey,
                        onValueChange = { bnovoKey = it; bnovoMsg = null },
                        label = { Text("Bnovo: API-ключ (пусто = не менять)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                bnovoMsg = null
                                val idTrim = bnovoId.trim()
                                val keyTrim = bnovoKey.trim()
                                val keyToStore = keyTrim.ifEmpty { authVm.bnovoApiKey.value.orEmpty() }
                                if (idTrim.isEmpty() || keyToStore.isEmpty()) {
                                    bnovoMsg = "Заполните ID и ключ (или введите новый ключ)."
                                    return@launch
                                }
                                authVm.saveBnovoCredentials(accountId = idTrim, apiKey = keyToStore)
                                bnovoKey = ""
                                bnovoMsg = "Сохранено."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("Сохранить Bnovo", fontWeight = FontWeight.Medium)
                    }
                    bnovoMsg?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                bnovoDiagOpen = true
                                bnovoDiagText = ""
                                bnovoDiagLoading = true
                                val idTrim = bnovoId.trim()
                                val keyTrim = bnovoKey.trim()
                                val keyUse = keyTrim.ifEmpty { authVm.bnovoApiKey.value.orEmpty() }
                                if (idTrim.isEmpty() || keyUse.isEmpty()) {
                                    bnovoDiagText =
                                        "Сначала укажите ID и API-ключ (или введите ключ в поле выше)."
                                    bnovoDiagLoading = false
                                    return@launch
                                }
                                if (!NetworkStatus.hasInternet(context)) {
                                    bnovoDiagText = "Нет подключения к интернету."
                                    bnovoDiagLoading = false
                                    return@launch
                                }
                                try {
                                    bnovoDiagText =
                                        bnovoClient.runBookingsDiagnostics(idTrim, keyUse)
                                } catch (e: Exception) {
                                    bnovoDiagText = e.message ?: "Ошибка запроса"
                                }
                                bnovoDiagLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        enabled = !bnovoDiagLoading,
                    ) {
                        Text("Диагностика Bnovo (/bookings)")
                    }
                    Text(
                        "В диалоге — фрагмент JSON; не публикуйте персональные данные гостей.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (bnovoDiagOpen) {
        AlertDialog(
            onDismissRequest = { if (!bnovoDiagLoading) bnovoDiagOpen = false },
            title = { Text("Диагностика Bnovo") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (bnovoDiagLoading) {
                        Text("Загрузка…")
                    } else {
                        Text(
                            bnovoDiagText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { bnovoDiagOpen = false },
                    enabled = !bnovoDiagLoading,
                ) { Text("Закрыть") }
            },
        )
    }
}
