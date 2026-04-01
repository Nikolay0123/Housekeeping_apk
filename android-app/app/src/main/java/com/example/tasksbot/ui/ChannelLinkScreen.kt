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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
fun ChannelLinkScreen(
    onBackToMenu: () -> Unit,
) {
    val authVm: AuthViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val link = authVm.channelLink.value
    val bnovoClient = remember { BnovoClient() }

    var bnovoId by remember { mutableStateOf(authVm.bnovoAccountId.value ?: "") }
    var bnovoKey by remember { mutableStateOf("") }
    var bnovoMsg by remember { mutableStateOf<String?>(null) }

    var bnovoDiagOpen by remember { mutableStateOf(false) }
    var bnovoDiagText by remember { mutableStateOf("") }
    var bnovoDiagLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            StandardTopBar(
                title = "Канал и Bnovo",
                subtitle = "Ссылка, учётные данные API, диагностика",
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
                subtitle = "Данные канала и тест API бронирований",
            )

            SectionGroupCard(title = "Ссылка на канал") {
                Text(
                    text = link ?: "(не задана в настройках)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            SectionGroupCard(title = "Bnovo PMS") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Укажите ID аккаунта и API-ключ из раздела Bnovo Octopus → API-доступ (как в документации к API: «id» и «password»). " +
                            "ID — обычно число с экрана доступа, без пробелов; подключение доступно владельцу аккаунта. Ключ хранится на устройстве.",
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
                        label = { Text("Bnovo: API-ключ (оставьте пустым, чтобы не менять)") },
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
                                    bnovoDiagText = "Сначала укажите ID и API-ключ (или введите ключ в поле выше)."
                                    bnovoDiagLoading = false
                                    return@launch
                                }
                                if (!NetworkStatus.hasInternet(context)) {
                                    bnovoDiagText = "Нет подключения к интернету."
                                    bnovoDiagLoading = false
                                    return@launch
                                }
                                try {
                                    bnovoDiagText = bnovoClient.runBookingsDiagnostics(idTrim, keyUse)
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
                        Text("Показать поля ответа Bnovo (/bookings)")
                    }
                    Text(
                        "Первый запрос: auth + одна страница броней (14 дней назад — 30 вперёд). " +
                            "В диалоге — ключи и фрагмент JSON; не публикуйте скриншоты с персональными данными гостей.",
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
