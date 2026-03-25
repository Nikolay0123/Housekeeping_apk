package com.example.tasksbot.ui

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tasksbot.db.AppDatabase
import com.example.tasksbot.db.RoomEntity
import com.example.tasksbot.repository.RoomsRepository
import kotlinx.coroutines.launch

class RoomsManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val roomsRepo = RoomsRepository(db)

    enum class Mode {
        List,
        Add,
        Edit,
    }

    data class UiState(
        val isLoading: Boolean = true,
        val error: String? = null,
        val mode: Mode = Mode.List,
        val rooms: List<RoomEntity> = emptyList(),
        val editingRoom: RoomEntity? = null,
    )

    val state = mutableStateOf(UiState())

    init {
        reload()
    }

    fun reload() {
        state.value = state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                roomsRepo.ensureSeeded()
                val rooms = roomsRepo.getAllRooms()
                state.value = state.value.copy(
                    isLoading = false,
                    rooms = rooms,
                    mode = Mode.List,
                    editingRoom = null,
                )
            } catch (e: Exception) {
                state.value = state.value.copy(isLoading = false, error = e.message ?: "Ошибка загрузки")
            }
        }
    }

    fun startAdd() {
        state.value = state.value.copy(mode = Mode.Add, editingRoom = null, error = null)
    }

    fun startEdit(room: RoomEntity) {
        state.value = state.value.copy(mode = Mode.Edit, editingRoom = room, error = null)
    }

    fun cancelForm() {
        state.value = state.value.copy(mode = Mode.List, editingRoom = null, error = null)
    }

    fun toggle(roomId: Int) {
        viewModelScope.launch {
            try {
                roomsRepo.toggleActive(roomId)
                val rooms = roomsRepo.getAllRooms()
                state.value = state.value.copy(rooms = rooms, error = null)
            } catch (e: Exception) {
                state.value = state.value.copy(error = e.message ?: "Ошибка")
            }
        }
    }

    fun addRoom(name: String, area: Double) {
        viewModelScope.launch {
            try {
                roomsRepo.addRoom(name, area)
                val rooms = roomsRepo.getAllRooms()
                state.value = state.value.copy(
                    rooms = rooms,
                    mode = Mode.List,
                    editingRoom = null,
                    error = null,
                )
            } catch (e: Exception) {
                state.value = state.value.copy(error = e.message ?: "Ошибка добавления")
            }
        }
    }

    fun setArea(roomId: Int, area: Double) {
        viewModelScope.launch {
            try {
                roomsRepo.setArea(roomId, area)
                val rooms = roomsRepo.getAllRooms()
                state.value = state.value.copy(
                    rooms = rooms,
                    mode = Mode.List,
                    editingRoom = null,
                    error = null,
                )
            } catch (e: Exception) {
                state.value = state.value.copy(error = e.message ?: "Ошибка обновления")
            }
        }
    }
}

