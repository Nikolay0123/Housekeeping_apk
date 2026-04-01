package com.example.tasksbot.repository

import com.example.tasksbot.db.AppDatabase
import com.example.tasksbot.db.EmployeeEntity
import java.util.Locale
import java.util.UUID

class EmployeesRepository(
    private val db: AppDatabase,
) {
    suspend fun ensureSeeded() {
        val dao = db.employeeDao()
        if (dao.count() > 0) return
        dao.insert(EmployeeEntity(key = "dina", displayName = "ДИНА"))
        dao.insert(EmployeeEntity(key = "lena", displayName = "ЛЕНА"))
        dao.insert(EmployeeEntity(key = "olya", displayName = "ОЛЯ"))
    }

    suspend fun getAll(): List<EmployeeEntity> = db.employeeDao().getAll()

    suspend fun count(): Int = db.employeeDao().count()

    suspend fun add(displayNameRaw: String, keyRaw: String?): Result<EmployeeEntity> {
        val displayName = displayNameRaw.trim()
        if (displayName.isEmpty()) return Result.failure(IllegalArgumentException("Введите имя сотрудника."))

        val key = if (keyRaw.isNullOrBlank()) {
            "emp_" + UUID.randomUUID().toString().replace("-", "").take(10)
        } else {
            keyRaw.trim().lowercase(Locale.ROOT)
        }

        if (!key.matches(Regex("^[a-z0-9_]{2,40}$"))) {
            return Result.failure(
                IllegalArgumentException("Код: 2–40 символов латиницы, цифры и подчёркивание."),
            )
        }

        if (db.employeeDao().getByKey(key) != null) {
            return Result.failure(IllegalArgumentException("Такой код уже занят."))
        }

        val row = EmployeeEntity(key = key, displayName = displayName)
        val id = db.employeeDao().insert(row)
        return Result.success(row.copy(id = id.toInt()))
    }

    suspend fun deleteById(id: Int): Result<Unit> {
        if (count() <= 1) {
            return Result.failure(IllegalStateException("Нельзя удалить последнего сотрудника."))
        }
        val e = db.employeeDao().getById(id) ?: return Result.failure(IllegalArgumentException("Сотрудник не найден."))
        db.employeeDao().delete(e)
        return Result.success(Unit)
    }
}
