package com.example.tasksbot.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val createdAtEpochMillis: Long,
    val employeeKey: String, // "dina" | "lena" | "olya"
    val roomsListJson: String, // JSON list of QueueItem-like objects
    val totalArea: Double,
    val messageId: Long? = null,
    val comment: String? = null,
)

