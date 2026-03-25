package com.example.tasksbot.domain

/**
 * Элемент очереди уборки.
 *
 * Логика 1-в-1 соответствует тому, как Python-бот кладёт данные в "selected_rooms".
 */
data class QueueItem(
    val id: Int,
    val name: String,
    val area: Double,
    val cleaningType: String,
    val linenVariant: Int? = null,      // для classic (101-109) и floor4 (401-405/403) сценариев
    val linenProfile: String? = null,   // "floor4" (для classic профиль вычисляется по имени номера)
    val linenColor: String? = null,     // key: "blue"/"gray"/"stripe"/"white"
    val linenBeds: Int? = null,         // только для classic variant=2 (1 или 2 кровати)
)

