package com.example.tasksbot.ui

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tasksbot.domain.QueueItem
import com.example.tasksbot.domain.TaskLogic
import com.example.tasksbot.db.AppDatabase
import com.example.tasksbot.db.RoomEntity
import com.example.tasksbot.repository.RoomsRepository
import com.example.tasksbot.repository.TasksRepository
import kotlinx.coroutines.launch

class CreateTaskViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val roomsRepo = RoomsRepository(db)
    private val tasksRepo = TasksRepository(db)

    enum class Step {
        ChooseEmployee,
        ChooseRoomCleaningType,
        ChooseLinenVariant,
        ChooseLinenColor,
        ChooseVariant2Beds,
        QueueChangeCleaningType,
        Rooms,
        AfterSent,
    }

    data class PendingAdd(
        var room: RoomEntity? = null,
        var linenProfile: String? = null, // "classic" | "floor4" | null
        var cleaningType: String? = null,
        var linenVariant: Int? = null,
    )

    data class UiState(
        val step: Step = Step.ChooseEmployee,
        val currentEmployeeKey: String = "dina",
        val activeRooms: List<RoomEntity> = emptyList(),
        val selectedRooms: List<QueueItem> = emptyList(),
        val comment: String? = null,

        val pendingAdd: PendingAdd? = null,
        val editingQueueIndex: Int? = null,

        val isSending: Boolean = false,
        val error: String? = null,

        val lastSentTotalArea: Double? = null,
    )

    val state = mutableStateOf(UiState())

    init {
        viewModelScope.launch {
            try {
                roomsRepo.ensureSeeded()
                val rooms = roomsRepo.getActiveRooms()
                state.value = state.value.copy(activeRooms = rooms)
            } catch (e: Exception) {
                state.value = state.value.copy(error = e.message ?: "Ошибка при инициализации")
            }
        }
    }

    fun selectEmployee(employeeKey: String) {
        state.value = state.value.copy(
            step = Step.Rooms,
            currentEmployeeKey = employeeKey,
            selectedRooms = emptyList(),
            comment = null,
            pendingAdd = null,
            editingQueueIndex = null,
            error = null,
        )
    }

    fun changeEmployee() {
        state.value = state.value.copy(
            step = Step.ChooseEmployee,
            selectedRooms = emptyList(),
            comment = null,
            pendingAdd = null,
            editingQueueIndex = null,
            error = null,
        )
    }

    fun clearQueue() {
        state.value = state.value.copy(selectedRooms = emptyList(), error = null)
    }

    fun setComment(comment: String?) {
        state.value = state.value.copy(comment = comment, error = null)
    }

    fun addRoomStart(room: RoomEntity) {
        val linenProfile = TaskLogic.roomLinenProfile(room.name)
        state.value = state.value.copy(
            step = Step.ChooseRoomCleaningType,
            pendingAdd = PendingAdd(room = room, linenProfile = linenProfile),
            editingQueueIndex = null,
            error = null,
        )
    }

    fun cancelAddFlow() {
        state.value = state.value.copy(
            step = Step.Rooms,
            pendingAdd = null,
            error = null,
        )
    }

    fun chooseCleaningTypeForAdd(cleaningTypeKey: String) {
        val pending = state.value.pendingAdd ?: return
        val room = pending.room ?: return
        val linenProfile = pending.linenProfile

        // В Python: если linen_profile и cleaning_type != "current" => выбор белья
        if (linenProfile != null && cleaningTypeKey != "current") {
            state.value = state.value.copy(
                step = Step.ChooseLinenVariant,
                pendingAdd = pending.copy(cleaningType = cleaningTypeKey, linenVariant = null),
                error = null,
            )
        } else {
            val added = QueueItem(
                id = room.id,
                name = room.name,
                area = room.area,
                cleaningType = cleaningTypeKey,
            )
            state.value = state.value.copy(
                step = Step.Rooms,
                selectedRooms = state.value.selectedRooms + added,
                pendingAdd = null,
                error = null,
            )
        }
    }

    fun cancelLinenVariantFlow() {
        state.value = state.value.copy(step = Step.Rooms, pendingAdd = null, error = null)
    }

    fun chooseLinenVariant(variant: Int) {
        val pending = state.value.pendingAdd ?: return
        val linenProfile = pending.linenProfile ?: return

        if (linenProfile == "floor4") {
            state.value = state.value.copy(
                step = Step.ChooseLinenColor,
                pendingAdd = pending.copy(linenVariant = variant),
                error = null,
            )
        } else if (linenProfile == "classic") {
            if (variant == 2) {
                state.value = state.value.copy(
                    step = Step.ChooseVariant2Beds,
                    pendingAdd = pending.copy(linenVariant = variant),
                    error = null,
                )
            } else {
                val room = pending.room ?: return
                val cleaningType = pending.cleaningType ?: "current"
                val added = QueueItem(
                    id = room.id,
                    name = room.name,
                    area = room.area,
                    cleaningType = cleaningType,
                    linenVariant = variant,
                )
                state.value = state.value.copy(
                    step = Step.Rooms,
                    selectedRooms = state.value.selectedRooms + added,
                    pendingAdd = null,
                    error = null,
                )
            }
        }
    }

    fun cancelToLinenVariantsFromBeds() {
        val pending = state.value.pendingAdd ?: return
        state.value = state.value.copy(
            step = Step.ChooseLinenVariant,
            pendingAdd = pending.copy(linenVariant = null),
            error = null,
        )
    }

    fun chooseLinenColor(colorKey: String) {
        val pending = state.value.pendingAdd ?: return
        val room = pending.room ?: return
        val cleaningType = pending.cleaningType ?: "current"
        val variant = pending.linenVariant ?: return

        val added = QueueItem(
            id = room.id,
            name = room.name,
            area = room.area,
            cleaningType = cleaningType,
            linenProfile = "floor4",
            linenVariant = variant,
            linenColor = colorKey,
        )
        state.value = state.value.copy(
            step = Step.Rooms,
            selectedRooms = state.value.selectedRooms + added,
            pendingAdd = null,
            error = null,
        )
    }

    fun cancelToLinenVariantFromColor() {
        val pending = state.value.pendingAdd ?: return
        state.value = state.value.copy(
            step = Step.ChooseLinenVariant,
            pendingAdd = pending.copy(),
            error = null,
        )
    }

    fun moveUp(index: Int) {
        val list = state.value.selectedRooms.toMutableList()
        if (index <= 0 || index >= list.size) return
        val tmp = list[index - 1]
        list[index - 1] = list[index]
        list[index] = tmp
        state.value = state.value.copy(selectedRooms = list, error = null)
    }

    fun moveDown(index: Int) {
        val list = state.value.selectedRooms.toMutableList()
        if (index < 0 || index >= list.size - 1) return
        val tmp = list[index + 1]
        list[index + 1] = list[index]
        list[index] = tmp
        state.value = state.value.copy(selectedRooms = list, error = null)
    }

    fun removeAt(index: Int) {
        val list = state.value.selectedRooms.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        state.value = state.value.copy(selectedRooms = list, error = null)
    }

    fun changeQueueItemCleaningType(index: Int) {
        if (index !in state.value.selectedRooms.indices) return
        state.value = state.value.copy(
            step = Step.QueueChangeCleaningType,
            editingQueueIndex = index,
            error = null,
        )
    }

    fun chooseCleaningTypeForQueueItem(cleaningTypeKey: String) {
        val idx = state.value.editingQueueIndex ?: return
        val list = state.value.selectedRooms.toMutableList()
        val old = list[idx]
        list[idx] = old.copy(cleaningType = cleaningTypeKey)
        state.value = state.value.copy(
            step = Step.Rooms,
            selectedRooms = list,
            editingQueueIndex = null,
            error = null,
        )
    }

    fun cancelQueueChangeCleaningType() {
        state.value = state.value.copy(
            step = Step.Rooms,
            editingQueueIndex = null,
            error = null,
        )
    }

    fun setVariant2Beds(beds: Int) {
        val pending = state.value.pendingAdd ?: return
        val room = pending.room ?: return
        val cleaningType = pending.cleaningType ?: "current"

        val added = QueueItem(
            id = room.id,
            name = room.name,
            area = room.area,
            cleaningType = cleaningType,
            linenVariant = 2,
            linenBeds = beds,
        )
        state.value = state.value.copy(
            step = Step.Rooms,
            selectedRooms = state.value.selectedRooms + added,
            pendingAdd = null,
            error = null,
        )
    }

    fun queueTotalArea(): Double = state.value.selectedRooms.sumOf { it.area }

    fun sendTask(botToken: String, channelId: String) {
        val current = state.value
        if (current.selectedRooms.isEmpty()) {
            state.value = current.copy(error = "Очередь пуста. Добавьте помещения.")
            return
        }
        if (botToken.isBlank() || channelId.isBlank()) {
            state.value = current.copy(error = "Не задан BOT_TOKEN или CHANNEL_ID.")
            return
        }

        state.value = current.copy(isSending = true, error = null)
        viewModelScope.launch {
            try {
                val total = queueTotalArea()
                val saved = tasksRepo.sendTaskAndSave(
                    botToken = botToken,
                    channelId = channelId,
                    employeeKey = current.currentEmployeeKey,
                    queue = current.selectedRooms,
                    totalArea = total,
                    comment = current.comment,
                )
                state.value = state.value.copy(
                    isSending = false,
                    step = Step.AfterSent,
                    lastSentTotalArea = total,
                    selectedRooms = emptyList(),
                    comment = null,
                    pendingAdd = null,
                    editingQueueIndex = null,
                    error = null,
                )
            } catch (e: Exception) {
                state.value = state.value.copy(
                    isSending = false,
                    error = e.message ?: "Ошибка отправки",
                )
            }
        }
    }

    fun startNewTask() {
        state.value = state.value.copy(
            step = Step.ChooseEmployee,
            selectedRooms = emptyList(),
            comment = null,
            pendingAdd = null,
            editingQueueIndex = null,
            error = null,
            lastSentTotalArea = null,
            isSending = false,
        )
    }
}

