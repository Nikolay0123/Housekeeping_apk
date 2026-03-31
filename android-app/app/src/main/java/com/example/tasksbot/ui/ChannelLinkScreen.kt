package com.example.tasksbot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun ChannelLinkScreen(
    onBackToMenu: () -> Unit,
) {
    val authVm: AuthViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val link = authVm.channelLink.value

    var bnovoId by remember { mutableStateOf(authVm.bnovoAccountId.value ?: "") }
    var bnovoKey by remember { mutableStateOf("") }
    var bnovoMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ссылка на канал")
        Text(
            text = link ?: "(не задана в настройках)",
            modifier = Modifier.padding(bottom = 4.dp),
        )

        Text("Bnovo PMS (автозадания)")
        Text(
            "Укажите ID аккаунта и API-ключ из раздела Bnovo Octopus → API-доступ (как в документации к API: «id» и «password»). " +
                "ID — обычно число с экрана доступа, без пробелов; подключение доступно владельцу аккаунта. Ключ хранится на устройстве.",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = bnovoId,
            onValueChange = { bnovoId = it; bnovoMsg = null },
            label = { Text("Bnovo: ID аккаунта") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = bnovoKey,
            onValueChange = { bnovoKey = it; bnovoMsg = null },
            label = { Text("Bnovo: API-ключ (оставьте пустым, чтобы не менять)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
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
        ) { Text("Сохранить Bnovo") }
        bnovoMsg?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.primary) }

        Button(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) {
            Text("🔙 Назад")
        }
    }
}
