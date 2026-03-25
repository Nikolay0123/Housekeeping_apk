package com.example.tasksbot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MainMenuScreen(
    onCreateTask: () -> Unit,
    onHistory: () -> Unit,
    onRooms: () -> Unit,
    onChannelLink: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Задачи горничных",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Выберите раздел",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(onClick = onCreateTask, modifier = Modifier.fillMaxWidth()) { Text("Создать задание") }
        FilledTonalButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text("История") }
        FilledTonalButton(onClick = onRooms, modifier = Modifier.fillMaxWidth()) { Text("Помещения") }
        FilledTonalButton(onClick = onChannelLink, modifier = Modifier.fillMaxWidth()) { Text("Ссылка на канал") }
    }
}

