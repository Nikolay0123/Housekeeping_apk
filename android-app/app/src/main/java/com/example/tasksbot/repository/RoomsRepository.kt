package com.example.tasksbot.repository

import com.example.tasksbot.db.AppDatabase
import com.example.tasksbot.db.RoomEntity
import com.example.tasksbot.db.SeedData

class RoomsRepository(
    private val db: AppDatabase,
) {
    suspend fun ensureSeeded() {
        val dao = db.roomDao()
        val existing = dao.getAllRooms().associateBy { it.name }
        for ((name, area) in SeedData.initialRooms) {
            if (name !in existing) {
                dao.insert(RoomEntity(name = name, area = area, isActive = true))
            }
        }
    }

    suspend fun getActiveRooms(): List<RoomEntity> = db.roomDao().getActiveRooms()

    suspend fun getAllRooms(): List<RoomEntity> = db.roomDao().getAllRooms()

    suspend fun addRoom(name: String, area: Double) {
        db.roomDao().insert(RoomEntity(name = name, area = area, isActive = true))
    }

    suspend fun setArea(roomId: Int, area: Double) {
        db.roomDao().setArea(roomId, area)
    }

    suspend fun toggleActive(roomId: Int) {
        db.roomDao().toggleActive(roomId)
    }
}

