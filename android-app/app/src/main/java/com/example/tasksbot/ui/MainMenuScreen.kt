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

@Composable
fun MainMenuScreen(
    onCreateTask: () -> Unit,
    onHistory: () -> Unit,
    onRooms: () -> Unit,
    onChannelLink: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Меню начальника", modifier = Modifier.padding(bottom = 8.dp))

        Button(onClick = onCreateTask, modifier = Modifier.fillMaxWidth()) { Text("📝 Создать новое задание") }
        Button(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text("📋 История заданий") }
        Button(onClick = onRooms, modifier = Modifier.fillMaxWidth()) { Text("🏨 Управление помещениями") }
        Button(onClick = onChannelLink, modifier = Modifier.fillMaxWidth()) { Text("🔗 Ссылка на канал") }
    }
}

