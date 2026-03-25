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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.round

@Composable
fun HistoryScreen(
    onBackToMenu: () -> Unit,
) {
    val vm: HistoryViewModel = viewModel()
    val state = vm.state.value

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📋 История заданий")
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
                } else if (state.selectedTask != null) {
                    val task = state.selectedTask
                    val detailText = com.example.tasksbot.domain.TaskLogic.formatHistoryDetailText(
                        taskId = task.task.id,
                        createdAtMillis = task.task.createdAtEpochMillis,
                        employeeKey = task.task.employeeKey,
                        rooms = task.rooms,
                        totalArea = task.task.totalArea,
                        comment = task.task.comment,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { vm.backToList() }) { Text("🔙 Назад") }
                        Divider()
                        Text(detailText)
                    }
                } else {
                    if (state.groups.isEmpty()) {
                        Text("Нет заданий.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            for (group in state.groups) {
                                Text("${vm.formatGroupLabel(group.date)}:", modifier = Modifier.padding(top = 8.dp))
                                for (t in group.items) {
                                    val timeStr = vm.formatTaskTime(t.task.createdAtEpochMillis)
                                    val emp = com.example.tasksbot.domain.TaskLogic.formatEmployeeName(t.task.employeeKey)
                                    val total0 = round(t.task.totalArea).toInt()
                                    val count = t.rooms.size

                                    Button(
                                        onClick = { vm.selectTask(t) },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("$timeStr 👩 $emp | $total0 м² | $count номера")
                                    }
                                }
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }
}

