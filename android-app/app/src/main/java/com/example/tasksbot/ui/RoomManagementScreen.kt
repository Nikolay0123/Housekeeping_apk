package com.example.tasksbot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.abs

@Composable
fun RoomManagementScreen(
    onBackToMenu: () -> Unit,
) {
    val vm: RoomsManagementViewModel = viewModel()
    val state = vm.state.value

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🏨 Управление помещениями")
                Spacer(modifier = Modifier.padding(2.dp))
                TextButton(onClick = { onBackToMenu() }) { Text("🔙 В меню") }
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                if (state.isLoading) {
                    CircularProgressIndicator()
                } else if (state.error != null) {
                    Text("Ошибка: ${state.error}")
                } else {
                    when (state.mode) {
                        RoomsManagementViewModel.Mode.List -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { vm.startAdd() },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("➕ Добавить помещение") }

                                Divider()

                                for (room in state.rooms) {
                                    val status = if (room.isActive) "✅" else "🔴 откл."
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${room.name} — ${String.format(java.util.Locale.US, "%.2f", room.area)} м²")
                                            Text(status)
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = { vm.startEdit(room) }) { Text("✏️") }
                                            Button(onClick = { vm.toggle(room.id) }) {
                                                Text(if (room.isActive) "🔴 Откл" else "🟢 Вкл")
                                            }
                                        }
                                    }
                                    Divider()
                                }
                            }
                        }

                        RoomsManagementViewModel.Mode.Add -> {
                            var name by remember { mutableStateOf("") }
                            var areaText by remember { mutableStateOf("") }
                            var error by remember { mutableStateOf<String?>(null) }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Text("➕ Добавить помещение")
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Название") },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = areaText,
                                    onValueChange = { areaText = it },
                                    label = { Text("Площадь (число, например 25.5)") },
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                if (error != null) Text(error ?: "")

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
                                ) { Text("✅ Добавить") }

                                Button(
                                    onClick = { vm.cancelForm() },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("🔙 Назад") }
                            }
                        }

                        RoomsManagementViewModel.Mode.Edit -> {
                            val room = state.editingRoom
                            if (room == null) {
                                Text("Ошибка: нет выбранного помещения.")
                                return@item
                            }
                            var areaText by remember { mutableStateOf(room.area.toString()) }
                            var error by remember { mutableStateOf<String?>(null) }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Text("Редактировать: ${room.name}")
                                OutlinedTextField(
                                    value = areaText,
                                    onValueChange = { areaText = it },
                                    label = { Text("Новая площадь") },
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                if (error != null) Text(error ?: "")

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
                                ) { Text("✅ Сохранить") }

                                Button(onClick = { vm.cancelForm() }, modifier = Modifier.fillMaxWidth()) {
                                    Text("🔙 Назад")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

