package com.example.tasksbot.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "employees",
    indices = [Index(value = ["employee_key"], unique = true)],
)
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "employee_key") val key: String,
    @ColumnInfo(name = "display_name") val displayName: String,
)
