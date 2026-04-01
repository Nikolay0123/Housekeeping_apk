package com.example.tasksbot.ui

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tasksbot.domain.TaskLogic
import com.example.tasksbot.db.AppDatabase
import com.example.tasksbot.domain.QueueItem
import com.example.tasksbot.repository.EmployeesRepository
import com.example.tasksbot.repository.TasksRepository
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val tasksRepo = TasksRepository(db)
    private val employeesRepo = EmployeesRepository(db)

    data class UiState(
        val isLoading: Boolean = true,
        val error: String? = null,
        val groups: List<DateGroup> = emptyList(),
        val selectedTask: TasksRepository.TaskWithRooms? = null,
        val employeeNameByKey: Map<String, String> = emptyMap(),
    )

    data class DateGroup(
        val date: LocalDate,
        val items: List<TasksRepository.TaskWithRooms>,
    )

    val state = mutableStateOf(UiState())

    init {
        reload()
    }

    fun reload() {
        state.value = state.value.copy(isLoading = true, error = null, selectedTask = null)
        viewModelScope.launch {
            try {
                employeesRepo.ensureSeeded()
                val nameMap = employeesRepo.getAll().associate {
                    it.key.lowercase(Locale.ROOT) to it.displayName
                }
                val tasks = tasksRepo.getLast50Tasks()
                val grouped: LinkedHashMap<LocalDate, MutableList<TasksRepository.TaskWithRooms>> = linkedMapOf()
                for (t in tasks) {
                    val date = Instant.ofEpochMilli(t.task.createdAtEpochMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    val list = grouped.getOrPut(date) { mutableListOf() }
                    list += t
                }
                val sortedDatesDesc = grouped.keys.sortedDescending().take(7)
                val groups = sortedDatesDesc.map { d ->
                    DateGroup(date = d, items = grouped[d].orEmpty())
                }

                state.value = state.value.copy(
                    isLoading = false,
                    groups = groups,
                    selectedTask = null,
                    employeeNameByKey = nameMap,
                )
            } catch (e: Exception) {
                state.value = state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка загрузки истории",
                )
            }
        }
    }

    fun selectTask(task: TasksRepository.TaskWithRooms) {
        state.value = state.value.copy(selectedTask = task)
    }

    fun backToList() {
        state.value = state.value.copy(selectedTask = null)
    }

    fun formatGroupLabel(date: LocalDate): String = TaskLogic.formatDateGroup(date)

    fun formatTaskTime(millis: Long): String = TaskLogic.formatTimeHHmm(millis)

    fun formatEmployeeLabel(employeeKey: String): String =
        TaskLogic.formatEmployeeName(employeeKey, state.value.employeeNameByKey)

    fun formatHistoryDetail(task: TasksRepository.TaskWithRooms): String =
        TaskLogic.formatHistoryDetailText(
            taskId = task.task.id,
            createdAtMillis = task.task.createdAtEpochMillis,
            employeeKey = task.task.employeeKey,
            rooms = task.rooms,
            totalArea = task.task.totalArea,
            comment = task.task.comment,
            employeeDisplayNames = state.value.employeeNameByKey,
        )

    fun countRooms(rooms: List<QueueItem>): Int = rooms.size
}
