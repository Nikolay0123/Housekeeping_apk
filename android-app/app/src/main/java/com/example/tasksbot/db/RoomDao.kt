package com.example.tasksbot.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms WHERE is_active = 1 ORDER BY name ASC")
    suspend fun getActiveRooms(): List<RoomEntity>

    @Query("SELECT * FROM rooms ORDER BY name ASC")
    suspend fun getAllRooms(): List<RoomEntity>

    @Query("SELECT COUNT(*) FROM rooms")
    suspend fun countRooms(): Int

    @Insert
    suspend fun insert(room: RoomEntity): Long

    @Query("UPDATE rooms SET area = :area WHERE id = :id")
    suspend fun setArea(id: Int, area: Double)

    @Query(
        "UPDATE rooms SET is_active = CASE WHEN is_active = 1 THEN 0 ELSE 1 END WHERE id = :id"
    )
    suspend fun toggleActive(id: Int)
}

