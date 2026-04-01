package com.example.tasksbot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tasksbot.ui.components.StandardTopBar
import com.example.tasksbot.domain.AutoTaskFromBnovo
import com.example.tasksbot.domain.QueueItem
import com.example.tasksbot.domain.TaskLogic
import com.example.tasksbot.db.RoomEntity
import java.time.format.DateTimeFormatter
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

    LaunchedEffect(isCommentDialogOpen) {
        if (isCommentDialogOpen) {
            draftComment = s.comment ?: ""
        }
    }

    Scaffold(
        topBar = {
            StandardTopBar(
                title = "Новое задание",
                subtitle = "Очередь и отправка в канал",
                onNavigateBack = onBackToMenu,
            )
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
                Text(
                    "Кто выполняет задание?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text("Выберите сотрудника:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { createVm.selectEmployee("dina") }, modifier = Modifier.fillMaxWidth()) { Text("Дина") }
                Button(onClick = { createVm.selectEmployee("lena") }, modifier = Modifier.fillMaxWidth()) { Text("Лена") }
                Button(onClick = { createVm.selectEmployee("olya") }, modifier = Modifier.fillMaxWidth()) { Text("Оля") }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { onBackToMenu() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Назад в меню") }
            }
        }

        CreateTaskViewModel.Step.Rooms -> {
            val totalArea = state.selectedRooms.sumOf { it.area }
            val selectedIds = state.selectedRooms.map { it.id }.toSet()
            val taskDateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            var roomTab by remember { mutableStateOf(TaskLogic.RoomPickerTab.Floor1) }
            val tabRows = TaskLogic.RoomPickerTab.entries
            val tabLabel: (TaskLogic.RoomPickerTab) -> String = {
                when (it) {
                    TaskLogic.RoomPickerTab.Floor1 -> "1 этаж\n101–109"
                    TaskLogic.RoomPickerTab.Block404405 -> "4 этаж\nномера, блоки, холл"
                    TaskLogic.RoomPickerTab.Other -> "Помещения"
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    ),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            TaskLogic.formatEmployeeName(state.currentEmployeeKey),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Площадь: ${TaskLogic.formatArea(totalArea)} / ${TaskLogic.formatArea(TaskLogic.AREA_LIMIT)} м²",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        state.taskForChannelDate?.let { d ->
                            Text(
                                "Дата уборки (из Bnovo): ${d.format(taskDateFmt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = { createVm.startBnovoWizard() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSending,
                ) { Text("Сформировать на завтра (Bnovo)") }

                Text("Очередь уборки", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                if (state.selectedRooms.isEmpty()) {
                    Text("Пока пусто — добавьте помещения ниже.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
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

                Spacer(Modifier.height(4.dp))
                Text("Добавить в очередь", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    ),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ScrollableTabRow(
                            selectedTabIndex = tabRows.indexOf(roomTab).coerceIn(0, tabRows.lastIndex),
                            edgePadding = 0.dp,
                        ) {
                            tabRows.forEach { tab ->
                                Tab(
                                    selected = roomTab == tab,
                                    onClick = { roomTab = tab },
                                    text = {
                                        Text(
                                            tabLabel(tab),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 2,
                                        )
                                    },
                                )
                            }
                        }
                        val filtered = TaskLogic.sortRoomsForPicker(
                            state.activeRooms.filter { TaskLogic.roomPickerTab(it.name) == roomTab },
                        )
                        if (filtered.isEmpty()) {
                            Text(
                                "Нет активных помещений в этой группе.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            filtered.forEach { room ->
                                val inSelected = selectedIds.contains(room.id)
                                val areaText = String.format(java.util.Locale.US, "%.2f", room.area)
                                val suffix = if (inSelected) " ✓ в очереди" else ""
                                FilledTonalButton(
                                    onClick = { createVm.addRoomStart(room) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        "${room.name} ($areaText м²)$suffix",
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        val token = authVm.botToken.value ?: ""
                        val channelId = authVm.channelId.value ?: ""
                        createVm.sendTask(token, channelId)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    if (state.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Отправить в Telegram")
                    }
                }
                FilledTonalButton(
                    onClick = {
                        val token = authVm.maxBotToken.value ?: ""
                        val chatId = authVm.maxChatId.value ?: ""
                        createVm.sendTaskMax(token, chatId)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSending,
                ) { Text("Отправить в MAX (группа)") }

                FilledTonalButton(
                    onClick = {
                        val accessToken = authVm.vkAccessToken.value ?: ""
                        val groupId = authVm.vkGroupId.value ?: ""
                        createVm.sendTaskVk(accessToken, groupId)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSending,
                ) { Text("Отправить во ВКонтакте (стена)") }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onCommentOpen,
                        modifier = Modifier.weight(1f),
                        enabled = !state.isSending,
                    ) { Text("Комментарий") }
                    Button(
                        onClick = { createVm.clearQueue() },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isSending,
                    ) { Text("Очистить") }
                }
                TextButton(
                    onClick = { createVm.changeEmployee() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Другой сотрудник") }

                state.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
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

            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Комплектация белья",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    room.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (linenProfile == "floor4") {
                    Text(
                        "Для номеров 4 этажа комплект выбирается сразу после вида уборки. Если вы здесь — отмените и начните снова.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { createVm.cancelLinenVariantFlow() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Отмена")
                    }
                } else {
                    for (v in 1..4) {
                        LinenVariantCard(
                            title = TaskLogic.classicLinenVariantButtonTitle(v),
                            subtitle = TaskLogic.classicLinenVariantButtonSubtitle(v),
                            onClick = { createVm.chooseLinenVariant(v) },
                        )
                    }
                }

                TextButton(onClick = { createVm.cancelLinenVariantFlow() }, modifier = Modifier.fillMaxWidth()) { Text("Отмена") }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
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
            val perBedFlow = pending.linenProfile == "floor4" && variant == TaskLogic.LINEN_VARIANT_FLOOR4_PER_BED
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Цвет белья",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "${room.name} · ${if (perBedFlow) "комплект на кровать" else TaskLogic.floor4LinenVariantButtonTitle(variant)}",
                )
                Divider()
                val keys = if (perBedFlow) TaskLogic.LINEN_COLOR_ORDER_FLOOR4_PER_BED else listOf("blue", "gray", "stripe", "white")
                for (key in keys) {
                    val label = TaskLogic.LINEN_COLORS[key] ?: key
                    Button(onClick = { createVm.chooseLinenColor(key) }, modifier = Modifier.fillMaxWidth()) { Text(label) }
                }
                TextButton(
                    onClick = {
                        if (perBedFlow) createVm.cancelAddFlow()
                        else createVm.cancelToLinenVariantFromColor()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (perBedFlow) "Отмена" else "Назад к вариантам")
                }
            }
        }

        CreateTaskViewModel.Step.ChooseFloor4Layout -> {
            val pending = state.pendingAdd
            val room = pending?.room
            if (room == null) {
                Text("Ошибка: не выбран номер.")
                return
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Кровати в ${room.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "Выберите расположение — от этого зависит комплект белья.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { createVm.recordManualFloor4Layout(joined = true) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Соединены") }
                Button(
                    onClick = { createVm.recordManualFloor4Layout(joined = false) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Разъединены") }
                TextButton(onClick = { createVm.cancelAddFlow() }, modifier = Modifier.fillMaxWidth()) { Text("Отмена") }
            }
        }

        CreateTaskViewModel.Step.ChooseFloor4Beds -> {
            val pending = state.pendingAdd
            val room = pending?.room
            val color = pending?.linenColor
            if (room == null || color == null || pending.linenVariant != TaskLogic.LINEN_VARIANT_FLOOR4_PER_BED) {
                Text("Ошибка: не выбран номер или цвет.")
                return
            }
            val maxB = pending.floor4MaxBeds.coerceIn(1, 20)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Сколько кроватей застелить?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "${room.name} · бельё: ${TaskLogic.formatLinenColor(color)} · до $maxB кров.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                for (b in 1..maxB) {
                    Button(
                        onClick = { createVm.setFloor4BedsCount(b) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("$b ${if (b == 1) "кровать" else "кровати"}") }
                }
                TextButton(onClick = { createVm.cancelFloor4BedsToColor() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Назад к цвету")
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

        CreateTaskViewModel.Step.BnovoChooseFloor -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Автозадание на завтра",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "Список формируется по бронированиям Bnovo на завтра в порядке помещений этажа.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        val id = authVm.bnovoAccountId.value.orEmpty()
                        val key = authVm.bnovoApiKey.value.orEmpty()
                        createVm.loadBnovoAndPlan(id, key, AutoTaskFromBnovo.FloorChoice.First)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("1 этаж — загрузить брони") }
                Button(
                    onClick = {
                        val id = authVm.bnovoAccountId.value.orEmpty()
                        val key = authVm.bnovoApiKey.value.orEmpty()
                        createVm.loadBnovoAndPlan(id, key, AutoTaskFromBnovo.FloorChoice.Fourth)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("4 этаж — загрузить брони") }
                TextButton(onClick = { createVm.cancelBnovoWizard() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Отмена")
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }

        CreateTaskViewModel.Step.BnovoLoading -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Запрос к Bnovo…", style = MaterialTheme.typography.titleMedium)
                CircularProgressIndicator(modifier = Modifier.height(36.dp))
            }
        }

        CreateTaskViewModel.Step.BnovoBedWizard -> {
            val steps = state.bnovoWizardSteps
            val idx = state.bnovoWizardIndex
            val step = steps.getOrNull(idx)
            if (step == null) {
                Text("Нет данных по шагам мастера.")
                return
            }
            val n = steps.size
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    when (step) {
                        is AutoTaskFromBnovo.BnovoWizardStep.Floor4PerBed -> "Бельё 4 этажа (цвет и кровати)"
                        else -> "Расположение кроватей"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                val titleName = when (step) {
                    is AutoTaskFromBnovo.BnovoWizardStep.ClassicBeds -> step.planned.entity.name
                    is AutoTaskFromBnovo.BnovoWizardStep.Floor4Layout -> step.planned.entity.name
                    is AutoTaskFromBnovo.BnovoWizardStep.Floor4PerBed -> step.planned.entity.name
                }
                Text(
                    "Шаг ${idx + 1} из $n: $titleName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                when (step) {
                    is AutoTaskFromBnovo.BnovoWizardStep.ClassicBeds -> {
                        Text(
                            "Выберите вариант для комплекта белья (1 этаж).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { createVm.recordBnovoLayoutChoice(joined = true, splitBedsForClassic = 2) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Соединены") }
                        Button(
                            onClick = { createVm.recordBnovoLayoutChoice(joined = false, splitBedsForClassic = 2) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Разъединены — 2 кровати") }
                        Button(
                            onClick = { createVm.recordBnovoLayoutChoice(joined = false, splitBedsForClassic = 1) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Разъединены — 1 кровать") }
                    }
                    is AutoTaskFromBnovo.BnovoWizardStep.Floor4Layout -> {
                        Text(
                            "Соединённые — двуспальная простыня; разъединённые — два комплекта 1,5 спальни.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { createVm.recordBnovoLayoutChoice(joined = true, splitBedsForClassic = 2) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Соединены") }
                        Button(
                            onClick = { createVm.recordBnovoLayoutChoice(joined = false, splitBedsForClassic = 2) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Разъединены") }
                    }
                    is AutoTaskFromBnovo.BnovoWizardStep.Floor4PerBed -> {
                        val draft = state.bnovoPerBedColorDraft
                        if (draft == null) {
                            Text(
                                "Цвет комплекта (на каждую выбранную кровать). Ёмкость номера по Bnovo — до ${step.maxBeds} кров.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            for (key in TaskLogic.LINEN_COLOR_ORDER_FLOOR4_PER_BED) {
                                val label = TaskLogic.LINEN_COLORS[key] ?: key
                                Button(
                                    onClick = { createVm.recordBnovoPerBedColor(key) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text(label) }
                            }
                        } else {
                            Text(
                                "Цвет: ${TaskLogic.formatLinenColor(draft)}. Сколько кроватей застелить?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            for (b in 1..step.maxBeds) {
                                Button(
                                    onClick = { createVm.recordBnovoPerBedBedsCount(b) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("$b ${if (b == 1) "кровать" else "кровати"}") }
                            }
                            TextButton(
                                onClick = { createVm.cancelBnovoPerBedColorDraft() },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Назад к цвету") }
                        }
                    }
                }
                TextButton(onClick = { createVm.cancelBnovoWizard() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Отмена")
                }
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
                Text("Готово", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                when (state.lastSentChannel) {
                    "max" -> Text("Задание для $empName отправлено в MAX (группа).")
                    "vk" -> Text("Задание для $empName отправлено во ВКонтакте (стена группы).")
                    else -> Text("Задание для $empName отправлено в Telegram-канал.")
                }
                if (total0 != null) {
                    Text("Общая площадь: $total0 м²")
                }

                Button(onClick = { createVm.startNewTask() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Создать новое задание")
                }
                FilledTonalButton(onClick = onGoToHistory, modifier = Modifier.fillMaxWidth()) {
                    Text("История")
                }
                TextButton(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) { Text("В меню") }
            }
        }
    }
}

@Composable
private fun LinenVariantCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
            )
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
        profile == "classic" && item.linenVariant == 5 -> " (109 соед.)"
        profile == "classic" && item.linenVariant == 6 -> {
            val bk = TaskLogic.classicVariant2BedsLabel(item.linenBeds)
            " (109 разд., $bk кров.)"
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

