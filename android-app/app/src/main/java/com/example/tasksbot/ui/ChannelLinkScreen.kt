package com.example.tasksbot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ChannelLinkScreen(
    onBackToMenu: () -> Unit,
) {
    val authVm: AuthViewModel = viewModel()
    val link = authVm.channelLink.value

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ссылка на канал")
        Text(
            text = link ?: "(не задана в настройках)",
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Button(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) {
            Text("🔙 Назад")
        }
    }
}

