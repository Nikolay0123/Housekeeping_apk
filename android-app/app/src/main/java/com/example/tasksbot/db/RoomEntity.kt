package com.example.tasksbot.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rooms",
    indices = [Index(value = ["name"], unique = true)]
)
data class RoomEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val area: Double,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
)

