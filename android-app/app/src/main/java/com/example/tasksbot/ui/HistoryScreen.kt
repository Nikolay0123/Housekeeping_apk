package com.example.tasksbot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.round
import com.example.tasksbot.ui.components.ScreenWelcomeStrip
import com.example.tasksbot.ui.components.StandardTopBar

@Composable
fun HistoryScreen(
    onBackToMenu: () -> Unit,
) {
    val vm: HistoryViewModel = viewModel()
    val state = vm.state.value
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            StandardTopBar(
                title = "История заданий",
                subtitle = "Отправленные задания по датам",
                onNavigateBack = onBackToMenu,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ScreenWelcomeStrip(
                    title = "Журнал",
                    subtitle = "Выберите задание, чтобы открыть детали",
                )
            }
            item {
                if (state.isLoading) {
                    CircularProgressIndicator()
                } else if (state.error != null) {
                    Text(
                        "Ошибка: ${state.error}",
                        color = scheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else if (state.selectedTask != null) {
                    val task = state.selectedTask
                    val detailText = vm.formatHistoryDetail(task)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextButton(onClick = { vm.backToList() }) {
                            Text("← К списку", fontWeight = FontWeight.Medium)
                        }
                        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = scheme.surface),
                            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Text(
                                detailText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurface,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                } else {
                    if (state.groups.isEmpty()) {
                        Text(
                            "Нет заданий.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant,
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            for (group in state.groups) {
                                Text(
                                    vm.formatGroupLabel(group.date),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.primary,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                for (t in group.items) {
                                    val timeStr = vm.formatTaskTime(t.task.createdAtEpochMillis)
                                    val emp = vm.formatEmployeeLabel(t.task.employeeKey)
                                    val total0 = round(t.task.totalArea).toInt()
                                    val count = t.rooms.size

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { vm.selectTask(t) },
                                        shape = MaterialTheme.shapes.medium,
                                        colors = CardDefaults.cardColors(
                                            containerColor = scheme.surface,
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            scheme.outlineVariant.copy(alpha = 0.45f),
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    ) {
                                        Text(
                                            "$timeStr · $emp · $total0 м² · $count помещ.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(
                                                horizontal = 18.dp,
                                                vertical = 16.dp,
                                            ),
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    color = scheme.outlineVariant.copy(alpha = 0.35f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
