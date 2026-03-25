package com.example.tasksbot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tasksbot.domain.QueueItem
import com.example.tasksbot.domain.TaskLogic
import com.example.tasksbot.db.RoomEntity
import kotlin.math.round

@Composable
fun CreateTaskScreen(
    onBackToMenu: () -> Unit,
    onGoToHistory: () -> Unit,
) {
    val createVm: CreateTaskViewModel = viewModel()
    val authVm: AuthViewModel = viewModel()
    val s = createVm.state.value

    var isCommentDialogOpen by remember { mutableStateOf(false) }
    var draftComment by remember { mutableStateOf(s.comment ?: "") }

    // Синхронизируем локальный черновик, когда открываем диалог
    if (isCommentDialogOpen && draftComment != (s.comment ?: "")) {
        draftComment = s.comment ?: ""
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Создать новое задание", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { onBackToMenu() }) { Text("🔙 В меню") }
                }
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { CreateTaskBody(state = s, authVm = authVm, createVm = createVm, onGoToHistory = onGoToHistory, onBackToMenu = onBackToMenu, onCommentOpen = { isCommentDialogOpen = true }) }
        }
    }

    if (isCommentDialogOpen) {
        AlertDialog(
            onDismissRequest = { isCommentDialogOpen = false },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = draftComment.trim()
                        val comment = when {
                            cleaned.isEmpty() -> null
                            cleaned == "-" -> null
                            else -> cleaned
                        }
                        createVm.setComment(comment)
                        isCommentDialogOpen = false
                    },
                ) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { isCommentDialogOpen = false }) { Text("Отмена") }
            },
            title = { Text("💬 Комментарий") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = draftComment,
                        onValueChange = { draftComment = it },
                        label = { Text("Введите комментарий (или '-')") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        )
    }
}

@Composable
private fun CreateTaskBody(
    state: CreateTaskViewModel.UiState,
    authVm: AuthViewModel,
    createVm: CreateTaskViewModel,
    onGoToHistory: () -> Unit,
    onBackToMenu: () -> Unit,
    onCommentOpen: () -> Unit,
) {
    when (state.step) {
        CreateTaskViewModel.Step.ChooseEmployee -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("👤 Для кого это задание?\n\nДина, Лена или Оля — кнопки ниже.")
                Button(onClick = { createVm.selectEmployee("dina") }, modifier = Modifier.fillMaxWidth()) { Text("👩 ДИНА") }
                Button(onClick = { createVm.selectEmployee("lena") }, modifier = Modifier.fillMaxWidth()) { Text("👩 ЛЕНА") }
                Button(onClick = { createVm.selectEmployee("olya") }, modifier = Modifier.fillMaxWidth()) { Text("👩 ОЛЯ") }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { onBackToMenu() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("🔙 Назад в меню") }
            }
        }

        CreateTaskViewModel.Step.Rooms -> {
            val totalArea = state.selectedRooms.sumOf { it.area }
            val selectedIds = state.selectedRooms.map { it.id }.toSet()

            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text("👤 Задание для: ${TaskLogic.formatEmployeeName(state.currentEmployeeKey)}")
                Text("🏠 Лимит: ${TaskLogic.formatArea(totalArea)} / ${TaskLogic.formatArea(TaskLogic.AREA_LIMIT)} м²")
                Divider()

                Text("📋 ОЧЕРЕДЬ УБОРКИ:")
                if (state.selectedRooms.isEmpty()) {
                    Text("(пока пусто)")
                } else {
                    state.selectedRooms.forEachIndexed { idx, item ->
                        QueueRow(
                            idx = idx,
                            item = item,
                            onUp = { createVm.moveUp(idx) },
                            onDown = { createVm.moveDown(idx) },
                            onDelete = { createVm.removeAt(idx) },
                            onChangeType = { createVm.changeQueueItemCleaningType(idx) },
                        )
                        Divider()
                    }
                }

                Divider()
                Text("🏠 ДОСТУПНЫЕ ПОМЕЩЕНИЯ:")

                // Список всех активных помещений (как в build_rooms_screen у бота)
                state.activeRooms.forEach { room ->
                    val inSelected = selectedIds.contains(room.id)
                    val areaText = String.format(java.util.Locale.US, "%.2f", room.area)
                    val suffix = if (inSelected) " ✓" else ""
                    Button(
                        onClick = { createVm.addRoomStart(room) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("${room.name} ($areaText м²)$suffix", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Divider()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val token = authVm.botToken.value ?: ""
                            val channelId = authVm.channelId.value ?: ""
                            createVm.sendTask(token, channelId)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isSending,
                    ) {
                        if (state.isSending) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp))
                        } else {
                            Text("✅ ОТПРАВИТЬ ЗАДАНИЕ")
                        }
                    }
                    Button(
                        onClick = onCommentOpen,
                        modifier = Modifier.weight(1f),
                    ) { Text("💬 Комментарий") }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { createVm.clearQueue() },
                        modifier = Modifier.weight(1f),
                    ) { Text("❌ ОЧИСТИТЬ ВСЁ") }

                    Button(
                        onClick = { createVm.changeEmployee() },
                        modifier = Modifier.weight(1f),
                    ) { Text("🔙 Другой сотрудник") }
                }

                state.error?.let { Text("Ошибка: $it") }
            }
        }

        CreateTaskViewModel.Step.ChooseRoomCleaningType -> {
            val pending = state.pendingAdd
            val room = pending?.room
            if (room == null) {
                Text("Ошибка: не выбран номер.")
                return
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text("👤 Выберите вид уборки для: ${room.name} (${String.format(java.util.Locale.US, "%.2f", room.area)} м²)")
                Divider()
                TaskLogic.CLEANING_TYPES.forEach { (key, label) ->
                    Button(
                        onClick = { createVm.chooseCleaningTypeForAdd(key) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(label) }
                }
                Button(
                    onClick = { createVm.cancelAddFlow() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("🔙 Отмена") }
                state.error?.let { Text("Ошибка: $it") }
            }
        }

        CreateTaskViewModel.Step.ChooseLinenVariant -> {
            val pending = state.pendingAdd
            val room = pending?.room
            val linenProfile = pending?.linenProfile
            if (room == null || linenProfile == null) {
                Text("Ошибка: не хватает данных для выбора белья.")
                return
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (linenProfile == "floor4") {
                    Text(
                        text = "Выберите вариант комплектации белья для номера: ${room.name}\n\nКаждый вариант: простыня 1,5, пододеяльник 1,5, полотенце банное, полотенце 40×70.",
                    )
                    Button(onClick = { createVm.chooseLinenVariant(1) }, modifier = Modifier.fillMaxWidth()) { Text("Вариант 1 (по 2 шт.)") }
                    Button(onClick = { createVm.chooseLinenVariant(2) }, modifier = Modifier.fillMaxWidth()) { Text("Вариант 2 (по 3 шт.)") }
                    Button(onClick = { createVm.chooseLinenVariant(3) }, modifier = Modifier.fillMaxWidth()) { Text("Вариант 3 (по 4 шт.)") }
                } else {
                    Text("Выберите вариант комплектации белья для номера: ${room.name}")
                    Button(onClick = { createVm.chooseLinenVariant(1) }, modifier = Modifier.fillMaxWidth()) { Text("Вариант 1") }
                    Button(onClick = { createVm.chooseLinenVariant(2) }, modifier = Modifier.fillMaxWidth()) { Text("Вариант 2") }
                    Button(onClick = { createVm.chooseLinenVariant(3) }, modifier = Modifier.fillMaxWidth()) { Text("Вариант 3") }
                    Button(onClick = { createVm.chooseLinenVariant(4) }, modifier = Modifier.fillMaxWidth()) { Text("Вариант 4") }
                }

                Button(onClick = { createVm.cancelLinenVariantFlow() }, modifier = Modifier.fillMaxWidth()) { Text("🔙 Отмена") }
                state.error?.let { Text("Ошибка: $it") }
            }
        }

        CreateTaskViewModel.Step.ChooseLinenColor -> {
            val pending = state.pendingAdd
            val room = pending?.room
            val variant = pending?.linenVariant
            if (room == null || variant == null) {
                Text("Ошибка: не выбран комплект.")
                return
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Цвет белья для ${room.name} (комплект вариант $variant):")
                Divider()
                val keys = listOf("blue", "gray", "stripe", "white")
                for (key in keys) {
                    val label = TaskLogic.LINEN_COLORS[key] ?: key
                    Button(onClick = { createVm.chooseLinenColor(key) }, modifier = Modifier.fillMaxWidth()) { Text(label) }
                }
                Button(onClick = { createVm.cancelToLinenVariantFromColor() }, modifier = Modifier.fillMaxWidth()) {
                    Text("🔙 Назад к вариантам")
                }
            }
        }

        CreateTaskViewModel.Step.ChooseVariant2Beds -> {
            val pending = state.pendingAdd
            val room = pending?.room
            if (room == null) {
                Text("Ошибка: не выбран номер.")
                return
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Вариант 2 — кровати разъединены.\nНомер ${room.name}\n\nСколько кроватей застелить?")
                Button(onClick = { createVm.setVariant2Beds(1) }, modifier = Modifier.fillMaxWidth()) {
                    Text("1 кровать (белья в 2 раза меньше)")
                }
                Button(onClick = { createVm.setVariant2Beds(2) }, modifier = Modifier.fillMaxWidth()) {
                    Text("2 кровати (полный комплект)")
                }
                Button(
                    onClick = { createVm.cancelToLinenVariantsFromBeds() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("🔙 Назад к вариантам") }
            }
        }

        CreateTaskViewModel.Step.QueueChangeCleaningType -> {
            val idx = state.editingQueueIndex
            if (idx == null || idx !in state.selectedRooms.indices) {
                Text("Ошибка: не удалось изменить вид уборки.")
                return
            }
            val item = state.selectedRooms[idx]
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Вид уборки для: ${item.name}")
                Divider()
                TaskLogic.CLEANING_TYPES.forEach { (key, label) ->
                    Button(
                        onClick = { createVm.chooseCleaningTypeForQueueItem(key) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(label) }
                }
                Button(
                    onClick = { createVm.cancelQueueChangeCleaningType() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("🔙 Назад") }
            }
        }

        CreateTaskViewModel.Step.AfterSent -> {
            val empName = TaskLogic.formatEmployeeName(state.currentEmployeeKey)
            val total0 = state.lastSentTotalArea?.let { round(it).toInt() }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("✅ ГОТОВО!")
                Text("Задание для $empName отправлено в канал.")
                if (total0 != null) {
                    Text("Общая площадь: $total0 м²")
                }

                Button(onClick = { createVm.startNewTask() }, modifier = Modifier.fillMaxWidth()) {
                    Text("📝 СОЗДАТЬ НОВОЕ ЗАДАНИЕ")
                }
                Button(onClick = onGoToHistory, modifier = Modifier.fillMaxWidth()) {
                    Text("📋 ИСТОРИЯ")
                }
                Button(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) { Text("🔙 В меню") }
            }
        }
    }
}

@Composable
private fun QueueRow(
    idx: Int,
    item: QueueItem,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDelete: () -> Unit,
    onChangeType: () -> Unit,
) {
    val profile = TaskLogic.resolveLinenProfile(item)
    val ct = TaskLogic.formatCleaningType(item.cleaningType)

    val suffix = when {
        profile == "floor4" && item.linenVariant != null -> {
            val lc = TaskLogic.formatLinenColor(item.linenColor)
            if (lc.isNotEmpty()) " (комплект ${item.linenVariant}, $lc)" else ""
        }
        profile == "classic" && item.linenVariant == 2 -> {
            val bk = TaskLogic.classicVariant2BedsLabel(item.linenBeds)
            " (вар.2, $bk кров.)"
        }
        else -> ""
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${idx + 1}. ${item.name} — ${TaskLogic.formatArea(item.area)} м² ($ct)$suffix", maxLines = 2)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = onUp) { Text("⬆️") }
            TextButton(onClick = onDown) { Text("⬇️") }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = onChangeType) { Text("🔄") }
            TextButton(onClick = onDelete) { Text("❌") }
        }
    }
}

