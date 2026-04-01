package com.example.tasksbot.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY display_name COLLATE NOCASE ASC")
    suspend fun getAll(): List<EmployeeEntity>

    @Query("SELECT COUNT(*) FROM employees")
    suspend fun count(): Int

    @Query("SELECT * FROM employees WHERE employee_key = :key LIMIT 1")
    suspend fun getByKey(key: String): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): EmployeeEntity?

    @Insert
    suspend fun insert(employee: EmployeeEntity): Long

    @Delete
    suspend fun delete(employee: EmployeeEntity)
}
