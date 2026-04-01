package com.example.tasksbot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tasksbot.ui.components.ScreenWelcomeStrip
import com.example.tasksbot.ui.components.SectionGroupCard
import com.example.tasksbot.ui.components.StandardTopBar

@Composable
fun RoomManagementScreen(
    onBackToMenu: () -> Unit,
) {
    val vm: RoomsManagementViewModel = viewModel()
    val state = vm.state.value
    val scheme = MaterialTheme.colorScheme

    val (barTitle, barSubtitle) = when (state.mode) {
        RoomsManagementViewModel.Mode.List ->
            "Помещения" to "Площади и включение в список выбора"
        RoomsManagementViewModel.Mode.Add ->
            "Новое помещение" to null
        RoomsManagementViewModel.Mode.Edit ->
            "Редактирование" to state.editingRoom?.name
    }

    Scaffold(
        topBar = {
            StandardTopBar(
                title = barTitle,
                subtitle = barSubtitle,
                onNavigateBack = {
                    when (state.mode) {
                        RoomsManagementViewModel.Mode.List -> onBackToMenu()
                        else -> vm.cancelForm()
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                state.isLoading -> item { CircularProgressIndicator() }
                state.error != null -> item {
                    Text(
                        "Ошибка: ${state.error}",
                        color = scheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> when (state.mode) {
                RoomsManagementViewModel.Mode.List -> {
                    item {
                        ScreenWelcomeStrip(
                            title = "Справочник",
                            subtitle = "Отключённые помещения не попадают в выбор при создании задания",
                        )
                    }
                    item {
                        Button(
                            onClick = { vm.startAdd() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text("Добавить помещение", fontWeight = FontWeight.Medium)
                        }
                    }
                    item {
                        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.35f))
                    }
                    items(state.rooms, key = { it.id }) { room ->
                        val status = if (room.isActive) "В списке выбора" else "Отключено"
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = scheme.surface),
                            border = BorderStroke(
                                1.dp,
                                scheme.outlineVariant.copy(alpha = 0.45f),
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        room.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        "${String.format(java.util.Locale.US, "%.2f", room.area)} м²",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = scheme.onSurfaceVariant,
                                    )
                                    Text(
                                        status,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (room.isActive) scheme.primary else scheme.error,
                                    )
                                }
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.End,
                                ) {
                                    OutlinedButton(
                                        onClick = { vm.startEdit(room) },
                                        shape = MaterialTheme.shapes.small,
                                    ) {
                                        Text("Площадь")
                                    }
                                    OutlinedButton(
                                        onClick = { vm.toggle(room.id) },
                                        shape = MaterialTheme.shapes.small,
                                    ) {
                                        Text(if (room.isActive) "Отключить" else "Включить")
                                    }
                                }
                            }
                        }
                    }
                }

                RoomsManagementViewModel.Mode.Add -> {
                    item {
                        var name by remember { mutableStateOf("") }
                        var areaText by remember { mutableStateOf("") }
                        var error by remember { mutableStateOf<String?>(null) }

                        SectionGroupCard(title = "Новая запись") {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Название") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                )
                                OutlinedTextField(
                                    value = areaText,
                                    onValueChange = { areaText = it },
                                    label = { Text("Площадь (число, например 25.5)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                )

                                error?.let {
                                    Text(
                                        it,
                                        color = scheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }

                                Button(
                                    onClick = {
                                        error = null
                                        val area = areaText.trim().replace(',', '.').toDoubleOrNull()
                                        if (name.trim().isEmpty()) {
                                            error = "Введите название."
                                            return@Button
                                        }
                                        if (area == null || area <= 0.0) {
                                            error = "Площадь должна быть > 0"
                                            return@Button
                                        }
                                        vm.addRoom(name.trim(), area)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                ) {
                                    Text("Добавить", fontWeight = FontWeight.Medium)
                                }

                                OutlinedButton(
                                    onClick = { vm.cancelForm() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                ) {
                                    Text("Назад")
                                }
                            }
                        }
                    }
                }

                RoomsManagementViewModel.Mode.Edit -> {
                    val room = state.editingRoom
                    if (room == null) {
                        item {
                            Text("Ошибка: нет выбранного помещения.")
                        }
                    } else {
                        item {
                            var areaText by remember(room.id) { mutableStateOf(room.area.toString()) }
                            var error by remember { mutableStateOf<String?>(null) }

                            SectionGroupCard(title = room.name) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    OutlinedTextField(
                                        value = areaText,
                                        onValueChange = { areaText = it },
                                        label = { Text("Новая площадь") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                    )

                                    error?.let {
                                        Text(
                                            it,
                                            color = scheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            error = null
                                            val area = areaText.trim().replace(',', '.').toDoubleOrNull()
                                            if (area == null || area <= 0.0) {
                                                error = "Площадь должна быть > 0"
                                                return@Button
                                            }
                                            vm.setArea(room.id, area)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                    ) {
                                        Text("Сохранить", fontWeight = FontWeight.Medium)
                                    }

                                    OutlinedButton(
                                        onClick = { vm.cancelForm() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                    ) {
                                        Text("Назад")
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}
