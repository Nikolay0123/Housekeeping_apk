package com.example.tasksbot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tasksbot.ui.components.MainMenuHero
import com.example.tasksbot.ui.components.MenuDestinationCard

@Composable
fun MainMenuScreen(
    onCreateTask: () -> Unit,
    onHistory: () -> Unit,
    onRooms: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MainMenuHero()
        Text(
            text = "Задачи горничных",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Разделы",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))

        MenuDestinationCard(
            icon = Icons.Outlined.AddTask,
            title = "Создать задание",
            subtitle = "Очередь уборки, автозадание из Bnovo",
            onClick = onCreateTask,
        )
        MenuDestinationCard(
            icon = Icons.Outlined.History,
            title = "История",
            subtitle = "Отправленные задания по датам",
            onClick = onHistory,
        )
        MenuDestinationCard(
            icon = Icons.Outlined.HomeWork,
            title = "Помещения",
            subtitle = "Площади и включение в список выбора",
            onClick = onRooms,
        )
        MenuDestinationCard(
            icon = Icons.Outlined.Settings,
            title = "Настройки",
            subtitle = "Telegram, MAX, VK, Bnovo — токены и ID",
            onClick = onOpenSettings,
        )
    }
}
