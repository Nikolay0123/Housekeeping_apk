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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.tasksbot.ui.components.ScreenWelcomeStrip
import com.example.tasksbot.ui.components.SectionGroupCard
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
            modifier = Modifier.padding(paddingValues).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { CreateTaskBody(state = s, authVm = authVm, createVm = createVm, onGoToHistory = onGoToHistory, onBackToMenu = onBackToMenu, onCommentOpen = { isCommentDialogOpen = true }) }
        }
    }

    if (isCommentDialogOpen) {
        val scheme = MaterialTheme.colorScheme
        AlertDialog(
            onDismissRequest = { isCommentDialogOpen = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = scheme.surface,
            titleContentColor = scheme.onSurface,
            textContentColor = scheme.onSurfaceVariant,
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
                    shape = MaterialTheme.shapes.medium,
                ) { Text("ОК", fontWeight = FontWeight.Medium) }
            },
            dismissButton = {
                TextButton(
                    onClick = { isCommentDialogOpen = false },
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Отмена") }
            },
            title = {
                Text(
                    "Комментарий к заданию",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = draftComment,
                        onValueChange = { draftComment = it },
                        label = { Text("Текст (или «-» чтобы очистить)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            },
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
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                ScreenWelcomeStrip(
                    title = "Исполнитель",
                    subtitle = "Кто будет работать по этому заданию",
                )
                SectionGroupCard(title = "Сотрудник") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Выберите имя:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { createVm.selectEmployee("dina") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        ) { Text("Дина", fontWeight = FontWeight.Medium) }
                        Button(
                            onClick = { createVm.selectEmployee("lena") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        ) { Text("Лена", fontWeight = FontWeight.Medium) }
                        Button(
                            onClick = { createVm.selectEmployee("olya") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        ) { Text("Оля", fontWeight = FontWeight.Medium) }
                    }
                }
                TextButton(
                    onClick = { onBackToMenu() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Назад в меню") }
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

            val schemeRooms = MaterialTheme.colorScheme
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = schemeRooms.primaryContainer.copy(alpha = 0.45f),
                    ),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, schemeRooms.outlineVariant.copy(alpha = 0.35f)),
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
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Сформировать на завтра (Bnovo)", fontWeight = FontWeight.Medium) }

                Text("Очередь уборки", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                if (state.selectedRooms.isEmpty()) {
                    Text(
                        "Пока пусто — добавьте помещения ниже.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.selectedRooms.forEachIndexed { idx, item ->
                            QueueRow(
                                idx = idx,
                                item = item,
                                onUp = { createVm.moveUp(idx) },
                                onDown = { createVm.moveDown(idx) },
                                onDelete = { createVm.removeAt(idx) },
                                onChangeType = { createVm.changeQueueItemCleaningType(idx) },
                            )
                        }
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
                            containerColor = schemeRooms.surfaceVariant.copy(alpha = 0.4f),
                            contentColor = schemeRooms.onSurface,
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
                                    shape = MaterialTheme.shapes.medium,
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
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    if (state.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Отправить в Telegram", fontWeight = FontWeight.Medium)
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
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Отправить в MAX (группа)", fontWeight = FontWeight.Medium) }

                FilledTonalButton(
                    onClick = {
                        val accessToken = authVm.vkAccessToken.value ?: ""
                        val groupId = authVm.vkGroupId.value ?: ""
                        createVm.sendTaskVk(accessToken, groupId)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSending,
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Отправить во ВКонтакте (стена)", fontWeight = FontWeight.Medium) }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onCommentOpen,
                        modifier = Modifier.weight(1f),
                        enabled = !state.isSending,
                        shape = MaterialTheme.shapes.medium,
                    ) { Text("Комментарий", fontWeight = FontWeight.Medium) }
                    Button(
                        onClick = { createVm.clearQueue() },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isSending,
                        shape = MaterialTheme.shapes.medium,
                    ) { Text("Очистить", fontWeight = FontWeight.Medium) }
                }
                TextButton(
                    onClick = { createVm.changeEmployee() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
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
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                ScreenWelcomeStrip(
                    title = "Вид уборки",
                    subtitle = "${room.name} · ${String.format(java.util.Locale.US, "%.2f", room.area)} м²",
                )
                SectionGroupCard(title = "Варианты") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskLogic.CLEANING_TYPES.forEach { (key, label) ->
                            Button(
                                onClick = { createVm.chooseCleaningTypeForAdd(key) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                            ) { Text(label, fontWeight = FontWeight.Medium) }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { createVm.cancelAddFlow() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Отмена") }
                state.error?.let {
                    Text(
                        "Ошибка: $it",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
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

            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                ScreenWelcomeStrip(
                    title = "Комплектация белья",
                    subtitle = room.name,
                )
                if (linenProfile == "floor4") {
                    SectionGroupCard(title = "Другой порядок шагов") {
                        Text(
                            "Для номеров 4 этажа комплект выбирается сразу после вида уборки. Если вы здесь — отмените и начните снова.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (v in 1..4) {
                            LinenVariantCard(
                                title = TaskLogic.classicLinenVariantButtonTitle(v),
                                subtitle = TaskLogic.classicLinenVariantButtonSubtitle(v),
                                onClick = { createVm.chooseLinenVariant(v) },
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { createVm.cancelLinenVariantFlow() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Отмена") }
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
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
            val variantLine =
                if (perBedFlow) "комплект на кровать" else TaskLogic.floor4LinenVariantButtonTitle(variant)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                ScreenWelcomeStrip(
                    title = "Цвет белья",
                    subtitle = "${room.name} · $variantLine",
                )
                val keys =
                    if (perBedFlow) TaskLogic.LINEN_COLOR_ORDER_FLOOR4_PER_BED
                    else listOf("blue", "gray", "stripe", "white")
                SectionGroupCard(title = "Оттенок комплекта") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (key in keys) {
                            val label = TaskLogic.LINEN_COLORS[key] ?: key
                            Button(
                                onClick = { createVm.chooseLinenColor(key) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(label, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        if (perBedFlow) createVm.cancelAddFlow()
                        else createVm.cancelToLinenVariantFromColor()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
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
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                ScreenWelcomeStrip(
                    title = "Кровати в номере",
                    subtitle = room.name,
                )
                SectionGroupCard(title = "Расположение") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "От выбора зависит комплект белья.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { createVm.recordManualFloor4Layout(joined = true) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                            ) { Text("Соединены", fontWeight = FontWeight.Medium) }
                            Button(
                                onClick = { createVm.recordManualFloor4Layout(joined = false) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                            ) { Text("Разъединены", fontWeight = FontWeight.Medium) }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { createVm.cancelAddFlow() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Отмена") }
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
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                ScreenWelcomeStrip(
                    title = "Сколько кроватей застелить?",
                    subtitle = "${room.name} · ${TaskLogic.formatLinenColor(color)} · до $maxB кров.",
                )
                SectionGroupCard(title = "Выбор") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (b in 1..maxB) {
                            Button(
                                onClick = { createVm.setFloor4BedsCount(b) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(
                                    "$b ${if (b == 1) "кровать" else "кровати"}",
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { createVm.cancelFloor4BedsToColor() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Назад к цвету") }
            }
        }

        CreateTaskViewModel.Step.ChooseVariant2Beds -> {
            val pending = state.pendingAdd
            val room = pending?.room
            if (room == null) {
                Text("Ошибка: не выбран номер.")
                return
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                ScreenWelcomeStrip(
                    title = "Вариант 2 — кровати разъединены",
                    subtitle = "${room.name}. Сколько кроватей застелить?",
                )
                SectionGroupCard(title = "Вариант") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { createVm.setVariant2Beds(1) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text("1 кровать (белья в 2 раза меньше)", fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = { createVm.setVariant2Beds(2) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text("2 кровати (полный комплект)", fontWeight = FontWeight.Medium)
                        }
                    }
                }
                OutlinedButton(
                    onClick = { createVm.cancelToLinenVariantsFromBeds() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Назад к вариантам") }
            }
        }

        CreateTaskViewModel.Step.BnovoChooseFloor -> {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                ScreenWelcomeStrip(
                    title = "Автозадание на завтра",
                    subtitle = "Список по бронированиям Bnovo на завтра",
                )
                Button(
                    onClick = {
                        val id = authVm.bnovoAccountId.value.orEmpty()
                        val key = authVm.bnovoApiKey.value.orEmpty()
                        createVm.loadBnovoAndPlan(id, key, AutoTaskFromBnovo.FloorChoice.First)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("1 этаж — загрузить брони", fontWeight = FontWeight.Medium) }
                Button(
                    onClick = {
                        val id = authVm.bnovoAccountId.value.orEmpty()
                        val key = authVm.bnovoApiKey.value.orEmpty()
                        createVm.loadBnovoAndPlan(id, key, AutoTaskFromBnovo.FloorChoice.Fourth)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("4 этаж — загрузить брони", fontWeight = FontWeight.Medium) }
                OutlinedButton(
                    onClick = { createVm.cancelBnovoWizard() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Отмена") }
                state.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        CreateTaskViewModel.Step.BnovoLoading -> {
            val scheme = MaterialTheme.colorScheme
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = scheme.surface),
                border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Запрос к Bnovo…",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.height(36.dp),
                        color = scheme.primary,
                        strokeWidth = 3.dp,
                    )
                }
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
            val titleName = when (step) {
                is AutoTaskFromBnovo.BnovoWizardStep.ClassicBeds -> step.planned.entity.name
                is AutoTaskFromBnovo.BnovoWizardStep.Floor4Layout -> step.planned.entity.name
                is AutoTaskFromBnovo.BnovoWizardStep.Floor4PerBed -> step.planned.entity.name
            }
            val stripTitle = when (step) {
                is AutoTaskFromBnovo.BnovoWizardStep.Floor4PerBed -> "Бельё 4 этажа"
                else -> "Расположение кроватей"
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                ScreenWelcomeStrip(
                    title = stripTitle,
                    subtitle = "Шаг ${idx + 1} из $n · $titleName",
                )
                when (step) {
                    is AutoTaskFromBnovo.BnovoWizardStep.ClassicBeds -> {
                        SectionGroupCard(title = "Комплект (1 этаж)") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "Выберите вариант комплекта белья.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { createVm.recordBnovoLayoutChoice(joined = true, splitBedsForClassic = 2) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                    ) { Text("Соединены", fontWeight = FontWeight.Medium) }
                                    Button(
                                        onClick = { createVm.recordBnovoLayoutChoice(joined = false, splitBedsForClassic = 2) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                    ) { Text("Разъединены — 2 кровати", fontWeight = FontWeight.Medium) }
                                    Button(
                                        onClick = { createVm.recordBnovoLayoutChoice(joined = false, splitBedsForClassic = 1) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                    ) { Text("Разъединены — 1 кровать", fontWeight = FontWeight.Medium) }
                                }
                            }
                        }
                    }
                    is AutoTaskFromBnovo.BnovoWizardStep.Floor4Layout -> {
                        SectionGroupCard(title = "Расположение") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "Соединённые — двуспальная простыня; разъединённые — два комплекта 1,5 спальни.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { createVm.recordBnovoLayoutChoice(joined = true, splitBedsForClassic = 2) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                    ) { Text("Соединены", fontWeight = FontWeight.Medium) }
                                    Button(
                                        onClick = { createVm.recordBnovoLayoutChoice(joined = false, splitBedsForClassic = 2) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                    ) { Text("Разъединены", fontWeight = FontWeight.Medium) }
                                }
                            }
                        }
                    }
                    is AutoTaskFromBnovo.BnovoWizardStep.Floor4PerBed -> {
                        val draft = state.bnovoPerBedColorDraft
                        if (draft == null) {
                            SectionGroupCard(title = "Цвет комплекта") {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        "На каждую выбранную кровать. Ёмкость по Bnovo — до ${step.maxBeds} кров.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (key in TaskLogic.LINEN_COLOR_ORDER_FLOOR4_PER_BED) {
                                            val label = TaskLogic.LINEN_COLORS[key] ?: key
                                            Button(
                                                onClick = { createVm.recordBnovoPerBedColor(key) },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = MaterialTheme.shapes.medium,
                                            ) { Text(label, fontWeight = FontWeight.Medium) }
                                        }
                                    }
                                }
                            }
                        } else {
                            SectionGroupCard(title = "Число кроватей") {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        "Цвет: ${TaskLogic.formatLinenColor(draft)}. Сколько кроватей застелить?",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (b in 1..step.maxBeds) {
                                            Button(
                                                onClick = { createVm.recordBnovoPerBedBedsCount(b) },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = MaterialTheme.shapes.medium,
                                            ) {
                                                Text(
                                                    "$b ${if (b == 1) "кровать" else "кровати"}",
                                                    fontWeight = FontWeight.Medium,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { createVm.cancelBnovoPerBedColorDraft() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                            ) { Text("Назад к цвету") }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { createVm.cancelBnovoWizard() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Отмена") }
            }
        }

        CreateTaskViewModel.Step.QueueChangeCleaningType -> {
            val idx = state.editingQueueIndex
            if (idx == null || idx !in state.selectedRooms.indices) {
                Text("Ошибка: не удалось изменить вид уборки.")
                return
            }
            val item = state.selectedRooms[idx]
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                ScreenWelcomeStrip(
                    title = "Смена вида уборки",
                    subtitle = item.name,
                )
                SectionGroupCard(title = "Варианты") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskLogic.CLEANING_TYPES.forEach { (key, label) ->
                            Button(
                                onClick = { createVm.chooseCleaningTypeForQueueItem(key) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                            ) { Text(label, fontWeight = FontWeight.Medium) }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { createVm.cancelQueueChangeCleaningType() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Назад") }
            }
        }

        CreateTaskViewModel.Step.AfterSent -> {
            val empName = TaskLogic.formatEmployeeName(state.currentEmployeeKey)
            val total0 = state.lastSentTotalArea?.let { round(it).toInt() }
            val channelLine = when (state.lastSentChannel) {
                "max" -> "Задание для $empName отправлено в MAX (группа)."
                "vk" -> "Задание для $empName отправлено во ВКонтакте (стена группы)."
                else -> "Задание для $empName отправлено в Telegram-канал."
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                ScreenWelcomeStrip(
                    title = "Готово",
                    subtitle = "Задание ушло в канал",
                )
                SectionGroupCard(title = "Итог") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            channelLine,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (total0 != null) {
                            Text(
                                "Общая площадь: $total0 м²",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Button(
                    onClick = { createVm.startNewTask() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text("Создать новое задание", fontWeight = FontWeight.Medium)
                }
                FilledTonalButton(
                    onClick = onGoToHistory,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text("История", fontWeight = FontWeight.Medium)
                }
                TextButton(
                    onClick = onBackToMenu,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("В меню") }
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
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = scheme.surface,
        ),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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

    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${idx + 1}. ${item.name}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                )
                Text(
                    "${TaskLogic.formatArea(item.area)} м² · $ct$suffix",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 3,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconButton(onClick = onUp, modifier = Modifier.height(40.dp)) {
                    Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Выше")
                }
                IconButton(onClick = onDown, modifier = Modifier.height(40.dp)) {
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Ниже")
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onChangeType) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Вид уборки")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Удалить",
                        tint = scheme.error,
                    )
                }
            }
        }
    }
}

