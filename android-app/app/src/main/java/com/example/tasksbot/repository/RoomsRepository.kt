package com.example.tasksbot.repository

import com.example.tasksbot.db.AppDatabase
import com.example.tasksbot.db.RoomEntity
import com.example.tasksbot.db.SeedData

class RoomsRepository(
    private val db: AppDatabase,
) {
    suspend fun ensureSeeded() {
        if (db.roomDao().countRooms() > 0) return
        val dao = db.roomDao()
        for ((name, area) in SeedData.initialRooms) {
            dao.insert(RoomEntity(name = name, area = area, isActive = true))
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

