package com.example.tasksbot.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAtEpochMillis DESC LIMIT 50")
    suspend fun getLast50(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getById(taskId: Int): TaskEntity?

    @Insert
    suspend fun insert(task: TaskEntity): Long
}

