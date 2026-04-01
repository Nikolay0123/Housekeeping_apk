package com.example.tasksbot.repository

import com.example.tasksbot.domain.QueueItem
import com.example.tasksbot.domain.TaskLogic
import com.example.tasksbot.db.AppDatabase
import com.example.tasksbot.db.TaskEntity
import com.example.tasksbot.network.TelegramClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

class TasksRepository(
    private val db: AppDatabase,
    private val telegramClient: TelegramClient = TelegramClient(),
) {
    private val gson = Gson()

    data class SavedTask(
        val taskId: Int,
        /** null если отправка только в мессенджер с устройства (Viber и т.п.) */
        val messageId: Long?,
    )

    data class TaskWithRooms(
        val task: TaskEntity,
        val rooms: List<QueueItem>,
    )

    suspend fun sendTaskAndSave(
        botToken: String,
        channelId: String,
        employeeKey: String,
        queue: List<QueueItem>,
        totalArea: Double,
        comment: String?,
        taskForDate: LocalDate? = null,
        employeeDisplayNames: Map<String, String> = emptyMap(),
    ): SavedTask {
        val createdAt = System.currentTimeMillis()
        val roomsJson = gson.toJson(queue)
        val msgId = telegramClient.sendMessage(
            botToken,
            channelId,
            text = buildMessage(
                employeeKey,
                queue,
                totalArea,
                comment,
                taskForDate,
                employeeDisplayNames,
            ),
        )
        val task = TaskEntity(
            createdAtEpochMillis = createdAt,
            employeeKey = employeeKey,
            roomsListJson = roomsJson,
            totalArea = totalArea,
            messageId = msgId,
            comment = comment,
        )
        val idLong = db.taskDao().insert(task)
        return SavedTask(taskId = idLong.toInt(), messageId = msgId)
    }

    suspend fun saveTaskLocal(
        employeeKey: String,
        queue: List<QueueItem>,
        totalArea: Double,
        comment: String?,
    ): SavedTask {
        val createdAt = System.currentTimeMillis()
        val roomsJson = gson.toJson(queue)
        val task = TaskEntity(
            createdAtEpochMillis = createdAt,
            employeeKey = employeeKey,
            roomsListJson = roomsJson,
            totalArea = totalArea,
            messageId = null,
            comment = comment,
        )
        val idLong = db.taskDao().insert(task)
        return SavedTask(taskId = idLong.toInt(), messageId = null)
    }

    fun buildChannelMessage(
        employeeKey: String,
        queue: List<QueueItem>,
        totalArea: Double,
        comment: String?,
        taskForDate: LocalDate? = null,
        employeeDisplayNames: Map<String, String> = emptyMap(),
    ): String = TaskLogic.formatChannelMessage(
        employeeKey = employeeKey,
        queue = queue,
        totalArea = totalArea,
        comment = comment,
        taskForDate = taskForDate,
        employeeDisplayNames = employeeDisplayNames,
    )

    private fun buildMessage(
        employeeKey: String,
        queue: List<QueueItem>,
        totalArea: Double,
        comment: String?,
        taskForDate: LocalDate? = null,
        employeeDisplayNames: Map<String, String> = emptyMap(),
    ): String {
        return TaskLogic.formatChannelMessage(
            employeeKey = employeeKey,
            queue = queue,
            totalArea = totalArea,
            comment = comment,
            taskForDate = taskForDate,
            employeeDisplayNames = employeeDisplayNames,
        )
    }

    suspend fun getLast50Tasks(): List<TaskWithRooms> {
        val tasks = db.taskDao().getLast50()
        return tasks.map { t ->
            val rooms: List<QueueItem> = parseRooms(t.roomsListJson)
            TaskWithRooms(task = t, rooms = rooms)
        }
    }

    suspend fun getTaskById(taskId: Int): TaskWithRooms? {
        val t = db.taskDao().getById(taskId) ?: return null
        val rooms: List<QueueItem> = parseRooms(t.roomsListJson)
        return TaskWithRooms(task = t, rooms = rooms)
    }

    private fun parseRooms(roomsListJson: String): List<QueueItem> {
        val type = object : TypeToken<List<QueueItem>>() {}.type
        return gson.fromJson(roomsListJson, type) ?: emptyList()
    }
}

