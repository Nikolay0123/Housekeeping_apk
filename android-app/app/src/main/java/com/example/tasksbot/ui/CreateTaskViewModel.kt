package com.example.tasksbot.ui

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tasksbot.domain.AutoTaskFromBnovo
import com.example.tasksbot.domain.QueueItem
import com.example.tasksbot.domain.TaskLogic
import com.example.tasksbot.db.AppDatabase
import com.example.tasksbot.db.RoomEntity
import com.example.tasksbot.network.BnovoClient
import com.example.tasksbot.network.NetworkStatus
import com.example.tasksbot.network.MaxClient
import com.example.tasksbot.network.VkClient
import com.example.tasksbot.repository.RoomsRepository
import com.example.tasksbot.repository.TasksRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

class CreateTaskViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val roomsRepo = RoomsRepository(db)
    private val tasksRepo = TasksRepository(db)
    private val maxClient = MaxClient()
    private val vkClient = VkClient()
    private val bnovoClient = BnovoClient()

    enum class Step {
        ChooseEmployee,
        ChooseRoomCleaningType,
        ChooseLinenVariant,
        ChooseLinenColor,
        ChooseVariant2Beds,
        ChooseFloor4Layout,
        ChooseFloor4Beds,
        QueueChangeCleaningType,
        BnovoChooseFloor,
        BnovoLoading,
        BnovoBedWizard,
        Rooms,
        AfterSent,
    }

    data class BnovoBedChoice(val joined: Boolean, val splitBeds: Int = 2)

    data class PerBedChoice(val color: String, val beds: Int)

    data class PendingAdd(
        var room: RoomEntity? = null,
        var linenProfile: String? = null, // "classic" | "floor4" | null
        var cleaningType: String? = null,
        var linenVariant: Int? = null,
        var linenColor: String? = null,
        /** Для 4 этажа (ручное): сколько кроватей максимум выводить в мастере. */
        var floor4MaxBeds: Int = 4,
    )

    data class UiState(
        val step: Step = Step.ChooseEmployee,
        val currentEmployeeKey: String = "dina",
        val activeRooms: List<RoomEntity> = emptyList(),
        val selectedRooms: List<QueueItem> = emptyList(),
        val comment: String? = null,

        val pendingAdd: PendingAdd? = null,
        val editingQueueIndex: Int? = null,

        val isSending: Boolean = false,
        val error: String? = null,

        val lastSentTotalArea: Double? = null,
        /** "telegram" | "max" | "vk" — для текста на экране после отправки */
        val lastSentChannel: String? = null,

        val bnovoCleaningDate: LocalDate? = null,
        val bnovoPlanned: List<AutoTaskFromBnovo.PlannedRoom>? = null,
        val bnovoWizardSteps: List<AutoTaskFromBnovo.BnovoWizardStep> = emptyList(),
        val bnovoWizardIndex: Int = 0,
        val bnovoLayoutChoices: Map<String, BnovoBedChoice> = emptyMap(),
        val bnovoPerBedChoices: Map<String, PerBedChoice> = emptyMap(),
        val bnovoPerBedColorDraft: String? = null,
        /** Дата уборки в тексте канала (автозадание на «завтра»). */
        val taskForChannelDate: LocalDate? = null,
    )

    val state = mutableStateOf(UiState())

    init {
        viewModelScope.launch {
            try {
                roomsRepo.ensureSeeded()
                val rooms = roomsRepo.getActiveRooms()
                state.value = state.value.copy(activeRooms = rooms)
            } catch (e: Exception) {
                state.value = state.value.copy(error = e.message ?: "Ошибка при инициализации")
            }
        }
    }

    fun selectEmployee(employeeKey: String) {
        state.value = state.value.copy(
            step = Step.Rooms,
            currentEmployeeKey = employeeKey,
            selectedRooms = emptyList(),
            comment = null,
            pendingAdd = null,
            editingQueueIndex = null,
            error = null,
            lastSentChannel = null,
            bnovoCleaningDate = null,
            bnovoPlanned = null,
            bnovoWizardSteps = emptyList(),
            bnovoWizardIndex = 0,
            bnovoLayoutChoices = emptyMap(),
            bnovoPerBedChoices = emptyMap(),
            bnovoPerBedColorDraft = null,
            taskForChannelDate = null,
        )
    }

    fun changeEmployee() {
        state.value = state.value.copy(
            step = Step.ChooseEmployee,
            selectedRooms = emptyList(),
            comment = null,
            pendingAdd = null,
            editingQueueIndex = null,
            error = null,
            lastSentChannel = null,
            bnovoCleaningDate = null,
            bnovoPlanned = null,
            bnovoWizardSteps = emptyList(),
            bnovoWizardIndex = 0,
            bnovoLayoutChoices = emptyMap(),
            bnovoPerBedChoices = emptyMap(),
            bnovoPerBedColorDraft = null,
            taskForChannelDate = null,
        )
    }

    fun clearQueue() {
        state.value = state.value.copy(selectedRooms = emptyList(), error = null, taskForChannelDate = null)
    }

    fun startBnovoWizard() {
        state.value = state.value.copy(
            step = Step.BnovoChooseFloor,
            error = null,
            bnovoCleaningDate = null,
            bnovoPlanned = null,
            bnovoWizardSteps = emptyList(),
            bnovoWizardIndex = 0,
            bnovoLayoutChoices = emptyMap(),
            bnovoPerBedChoices = emptyMap(),
            bnovoPerBedColorDraft = null,
        )
    }

    fun cancelBnovoWizard() {
        state.value = state.value.copy(
            step = Step.Rooms,
            isSending = false,
            bnovoCleaningDate = null,
            bnovoPlanned = null,
            bnovoWizardSteps = emptyList(),
            bnovoWizardIndex = 0,
            bnovoLayoutChoices = emptyMap(),
            bnovoPerBedChoices = emptyMap(),
            bnovoPerBedColorDraft = null,
        )
    }

    fun loadBnovoAndPlan(accountId: String, apiKey: String, floor: AutoTaskFromBnovo.FloorChoice) {
        val trimmedId = accountId.trim()
        val trimmedKey = apiKey.trim()
        if (trimmedId.isEmpty() || trimmedKey.isEmpty()) {
            state.value = state.value.copy(error = "Настройте Bnovo: меню → Ссылка на канал.")
            return
        }
        if (!NetworkStatus.hasInternet(getApplication())) {
            state.value = state.value.copy(
                error = "Нет подключения к интернету. Включите Wi‑Fi или мобильные данные.",
            )
            return
        }

        state.value = state.value.copy(step = Step.BnovoLoading, isSending = true, error = null)
        viewModelScope.launch {
            try {
                val cleaningDate = AutoTaskFromBnovo.tomorrowCleaningDate()
                val token = bnovoClient.fetchAccessToken(trimmedId, trimmedKey)
                val from = cleaningDate.minusDays(BnovoClient.BOOKINGS_DATE_PAST_DAYS.toLong())
                val to = cleaningDate.plusDays(BnovoClient.BOOKINGS_DATE_FUTURE_DAYS.toLong())
                val raw = bnovoClient.fetchBookingsNormalized(token, from, to)
                val byRoom = AutoTaskFromBnovo.indexBookingsByRoom(raw)
                val active = roomsRepo.getActiveRooms()
                val byName = active.associateBy { it.name }
                val planned = when (floor) {
                    AutoTaskFromBnovo.FloorChoice.First -> AutoTaskFromBnovo.planFirstFloor(byName, byRoom, cleaningDate)
                    AutoTaskFromBnovo.FloorChoice.Fourth -> AutoTaskFromBnovo.planFourthFloor(byName, byRoom, cleaningDate)
                }
                if (planned.isEmpty()) {
                    state.value = state.value.copy(
                        step = Step.Rooms,
                        isSending = false,
                        error = AutoTaskFromBnovo.emptyPlanMessageForFloor(floor),
                    )
                    return@launch
                }
                val wizardSteps = AutoTaskFromBnovo.buildBnovoWizardSteps(planned)
                if (wizardSteps.isEmpty()) {
                    applyBnovoPlanned(
                        planned,
                        emptyMap(),
                        emptyMap(),
                        cleaningDate,
                    )
                } else {
                    state.value = state.value.copy(
                        step = Step.BnovoBedWizard,
                        isSending = false,
                        bnovoCleaningDate = cleaningDate,
                        bnovoPlanned = planned,
                        bnovoWizardSteps = wizardSteps,
                        bnovoWizardIndex = 0,
                        bnovoLayoutChoices = emptyMap(),
                        bnovoPerBedChoices = emptyMap(),
                        bnovoPerBedColorDraft = null,
                        taskForChannelDate = cleaningDate,
                    )
                }
            } catch (e: Exception) {
                state.value = state.value.copy(
                    step = Step.Rooms,
                    isSending = false,
                    error = e.message ?: "Ошибка запроса к Bnovo",
                )
            }
        }
    }

    fun recordBnovoLayoutChoice(joined: Boolean, splitBedsForClassic: Int) {
        val s = state.value
        val steps = s.bnovoWizardSteps
        val idx = s.bnovoWizardIndex
        if (idx !in steps.indices || s.bnovoPlanned == null) return
        when (val step = steps[idx]) {
            is AutoTaskFromBnovo.BnovoWizardStep.ClassicBeds -> {
                val name = step.planned.entity.name
                val choice = BnovoBedChoice(joined = joined, splitBeds = splitBedsForClassic.coerceIn(1, 2))
                advanceAfterBnovoLayout(s.copy(bnovoLayoutChoices = s.bnovoLayoutChoices + (name to choice)))
            }
            is AutoTaskFromBnovo.BnovoWizardStep.Floor4Layout -> {
                val name = step.planned.entity.name
                val choice = BnovoBedChoice(joined = joined, splitBeds = 2)
                advanceAfterBnovoLayout(s.copy(bnovoLayoutChoices = s.bnovoLayoutChoices + (name to choice)))
            }
            else -> {}
        }
    }

    fun recordBnovoPerBedColor(colorKey: String) {
        if (colorKey !in TaskLogic.LINEN_COLOR_ORDER_FLOOR4_PER_BED) return
        state.value = state.value.copy(bnovoPerBedColorDraft = colorKey)
    }

    fun recordBnovoPerBedBedsCount(beds: Int) {
        val s = state.value
        val step = s.bnovoWizardSteps.getOrNull(s.bnovoWizardIndex)
        if (step !is AutoTaskFromBnovo.BnovoWizardStep.Floor4PerBed || s.bnovoPlanned == null) return
        val color = s.bnovoPerBedColorDraft ?: return
        val maxB = step.maxBeds.coerceIn(1, 20)
        val b = beds.coerceIn(1, maxB)
        val name = step.planned.entity.name
        advanceAfterBnovoLayout(
            s.copy(
                bnovoPerBedChoices = s.bnovoPerBedChoices + (name to PerBedChoice(color, b)),
                bnovoPerBedColorDraft = null,
            ),
        )
    }

    fun cancelBnovoPerBedColorDraft() {
        state.value = state.value.copy(bnovoPerBedColorDraft = null)
    }

    private fun advanceAfterBnovoLayout(s: UiState) {
        val planned = s.bnovoPlanned ?: return
        val nextIdx = s.bnovoWizardIndex + 1
        if (nextIdx >= s.bnovoWizardSteps.size) {
            applyBnovoPlanned(
                planned,
                s.bnovoLayoutChoices,
                s.bnovoPerBedChoices,
                s.bnovoCleaningDate ?: AutoTaskFromBnovo.tomorrowCleaningDate(),
            )
        } else {
            state.value = s.copy(bnovoWizardIndex = nextIdx, bnovoPerBedColorDraft = null)
        }
    }

    private fun applyBnovoPlanned(
        planned: List<AutoTaskFromBnovo.PlannedRoom>,
        layoutChoices: Map<String, BnovoBedChoice>,
        perBedChoices: Map<String, PerBedChoice>,
        cleaningDate: LocalDate,
    ) {
        val queue = planned.map { p ->
            val n = p.entity.name
            val lo = layoutChoices[n]
            val pb = perBedChoices[n]
            AutoTaskFromBnovo.plannedToQueueItem(
                planned = p,
                bedsJoined = lo?.joined ?: true,
                splitBeds = lo?.splitBeds ?: 2,
                floor4PerBedColor = pb?.color,
                floor4PerBedCount = pb?.beds,
            )
        }
        state.value = state.value.copy(
            step = Step.Rooms,
            selectedRooms = queue,
            isSending = false,
            error = null,
            bnovoCleaningDate = null,
            bnovoPlanned = null,
            bnovoWizardSteps = emptyList(),
            bnovoWizardIndex = 0,
            bnovoLayoutChoices = emptyMap(),
            bnovoPerBedChoices = emptyMap(),
            bnovoPerBedColorDraft = null,
            taskForChannelDate = cleaningDate,
        )
    }

    fun setComment(comment: String?) {
        state.value = state.value.copy(comment = comment, error = null)
    }

    fun addRoomStart(room: RoomEntity) {
        val linenProfile = TaskLogic.roomLinenProfile(room.name)
        state.value = state.value.copy(
            step = Step.ChooseRoomCleaningType,
            pendingAdd = PendingAdd(room = room, linenProfile = linenProfile),
            editingQueueIndex = null,
            error = null,
        )
    }

    fun cancelAddFlow() {
        state.value = state.value.copy(
            step = Step.Rooms,
            pendingAdd = null,
            error = null,
        )
    }

    fun chooseCleaningTypeForAdd(cleaningTypeKey: String) {
        val pending = state.value.pendingAdd ?: return
        val room = pending.room ?: return
        val linenProfile = pending.linenProfile

        if (linenProfile == "floor4" && cleaningTypeKey != "current") {
            when {
                TaskLogic.isFloor4LayoutBnovoRoom(room.name) -> {
                    state.value = state.value.copy(
                        step = Step.ChooseFloor4Layout,
                        pendingAdd = pending.copy(cleaningType = cleaningTypeKey),
                        error = null,
                    )
                    return
                }
                TaskLogic.isFloor4PerBedBnovoRoom(room.name) -> {
                    state.value = state.value.copy(
                        step = Step.ChooseLinenColor,
                        pendingAdd = pending.copy(
                            cleaningType = cleaningTypeKey,
                            linenVariant = TaskLogic.LINEN_VARIANT_FLOOR4_PER_BED,
                            linenColor = null,
                            floor4MaxBeds = pending.floor4MaxBeds.coerceIn(1, 4),
                        ),
                        error = null,
                    )
                    return
                }
            }
        }

        if (linenProfile != null && cleaningTypeKey != "current") {
            state.value = state.value.copy(
                step = Step.ChooseLinenVariant,
                pendingAdd = pending.copy(cleaningType = cleaningTypeKey, linenVariant = null),
                error = null,
            )
        } else {
            val added = QueueItem(
                id = room.id,
                name = room.name,
                area = room.area,
                cleaningType = cleaningTypeKey,
            )
            state.value = state.value.copy(
                step = Step.Rooms,
                selectedRooms = state.value.selectedRooms + added,
                pendingAdd = null,
                error = null,
            )
        }
    }

    fun cancelLinenVariantFlow() {
        state.value = state.value.copy(step = Step.Rooms, pendingAdd = null, error = null)
    }

    fun chooseLinenVariant(variant: Int) {
        val pending = state.value.pendingAdd ?: return
        val linenProfile = pending.linenProfile ?: return

        if (linenProfile == "floor4") {
            state.value = state.value.copy(
                step = Step.ChooseLinenColor,
                pendingAdd = pending.copy(linenVariant = variant),
                error = null,
            )
        } else if (linenProfile == "classic") {
            if (variant == 2) {
                state.value = state.value.copy(
                    step = Step.ChooseVariant2Beds,
                    pendingAdd = pending.copy(linenVariant = variant),
                    error = null,
                )
            } else {
                val room = pending.room ?: return
                val cleaningType = pending.cleaningType ?: "current"
                val added = QueueItem(
                    id = room.id,
                    name = room.name,
                    area = room.area,
                    cleaningType = cleaningType,
                    linenVariant = variant,
                )
                state.value = state.value.copy(
                    step = Step.Rooms,
                    selectedRooms = state.value.selectedRooms + added,
                    pendingAdd = null,
                    error = null,
                )
            }
        }
    }

    fun cancelToLinenVariantsFromBeds() {
        val pending = state.value.pendingAdd ?: return
        state.value = state.value.copy(
            step = Step.ChooseLinenVariant,
            pendingAdd = pending.copy(linenVariant = null),
            error = null,
        )
    }

    fun chooseLinenColor(colorKey: String) {
        val pending = state.value.pendingAdd ?: return
        val room = pending.room ?: return
        val cleaningType = pending.cleaningType ?: "current"
        val variant = pending.linenVariant ?: return

        if (pending.linenProfile == "floor4" && variant == TaskLogic.LINEN_VARIANT_FLOOR4_PER_BED) {
            state.value = state.value.copy(
                step = Step.ChooseFloor4Beds,
                pendingAdd = pending.copy(linenColor = colorKey),
                error = null,
            )
            return
        }

        val added = QueueItem(
            id = room.id,
            name = room.name,
            area = room.area,
            cleaningType = cleaningType,
            linenProfile = "floor4",
            linenVariant = variant,
            linenColor = colorKey,
        )
        state.value = state.value.copy(
            step = Step.Rooms,
            selectedRooms = state.value.selectedRooms + added,
            pendingAdd = null,
            error = null,
        )
    }

    fun setFloor4BedsCount(beds: Int) {
        val pending = state.value.pendingAdd ?: return
        val room = pending.room ?: return
        val cleaningType = pending.cleaningType ?: "current"
        val variant = pending.linenVariant ?: return
        val colorKey = pending.linenColor ?: return
        val maxB = pending.floor4MaxBeds.coerceIn(1, 20)

        val added = QueueItem(
            id = room.id,
            name = room.name,
            area = room.area,
            cleaningType = cleaningType,
            linenProfile = "floor4",
            linenVariant = variant,
            linenColor = colorKey,
            linenBeds = beds.coerceIn(1, maxB),
        )
        state.value = state.value.copy(
            step = Step.Rooms,
            selectedRooms = state.value.selectedRooms + added,
            pendingAdd = null,
            error = null,
        )
    }

    fun cancelFloor4BedsToColor() {
        val pending = state.value.pendingAdd ?: return
        state.value = state.value.copy(
            step = Step.ChooseLinenColor,
            pendingAdd = pending.copy(linenColor = null),
            error = null,
        )
    }

    fun recordManualFloor4Layout(joined: Boolean) {
        val pending = state.value.pendingAdd ?: return
        val room = pending.room ?: return
        val cleaningType = pending.cleaningType ?: return
        val v = if (joined) TaskLogic.LINEN_VARIANT_FLOOR4_JOINED else TaskLogic.LINEN_VARIANT_FLOOR4_SPLIT
        val added = QueueItem(
            id = room.id,
            name = room.name,
            area = room.area,
            cleaningType = cleaningType,
            linenProfile = "floor4",
            linenVariant = v,
        )
        state.value = state.value.copy(
            step = Step.Rooms,
            selectedRooms = state.value.selectedRooms + added,
            pendingAdd = null,
            error = null,
        )
    }

    fun cancelToLinenVariantFromColor() {
        val pending = state.value.pendingAdd ?: return
        state.value = state.value.copy(
            step = Step.ChooseLinenVariant,
            pendingAdd = pending.copy(),
            error = null,
        )
    }

    fun moveUp(index: Int) {
        val list = state.value.selectedRooms.toMutableList()
        if (index <= 0 || index >= list.size) return
        val tmp = list[index - 1]
        list[index - 1] = list[index]
        list[index] = tmp
        state.value = state.value.copy(selectedRooms = list, error = null)
    }

    fun moveDown(index: Int) {
        val list = state.value.selectedRooms.toMutableList()
        if (index < 0 || index >= list.size - 1) return
        val tmp = list[index + 1]
        list[index + 1] = list[index]
        list[index] = tmp
        state.value = state.value.copy(selectedRooms = list, error = null)
    }

    fun removeAt(index: Int) {
        val list = state.value.selectedRooms.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        state.value = state.value.copy(selectedRooms = list, error = null)
    }

    fun changeQueueItemCleaningType(index: Int) {
        if (index !in state.value.selectedRooms.indices) return
        state.value = state.value.copy(
            step = Step.QueueChangeCleaningType,
            editingQueueIndex = index,
            error = null,
        )
    }

    fun chooseCleaningTypeForQueueItem(cleaningTypeKey: String) {
        val idx = state.value.editingQueueIndex ?: return
        val list = state.value.selectedRooms.toMutableList()
        val old = list[idx]
        list[idx] = old.copy(cleaningType = cleaningTypeKey)
        state.value = state.value.copy(
            step = Step.Rooms,
            selectedRooms = list,
            editingQueueIndex = null,
            error = null,
        )
    }

    fun cancelQueueChangeCleaningType() {
        state.value = state.value.copy(
            step = Step.Rooms,
            editingQueueIndex = null,
            error = null,
        )
    }

    fun setVariant2Beds(beds: Int) {
        val pending = state.value.pendingAdd ?: return
        val room = pending.room ?: return
        val cleaningType = pending.cleaningType ?: "current"

        val added = QueueItem(
            id = room.id,
            name = room.name,
            area = room.area,
            cleaningType = cleaningType,
            linenVariant = 2,
            linenBeds = beds,
        )
        state.value = state.value.copy(
            step = Step.Rooms,
            selectedRooms = state.value.selectedRooms + added,
            pendingAdd = null,
            error = null,
        )
    }

    fun queueTotalArea(): Double = state.value.selectedRooms.sumOf { it.area }

    fun sendTask(botToken: String, channelId: String) {
        val current = state.value
        if (current.selectedRooms.isEmpty()) {
            state.value = current.copy(error = "Очередь пуста. Добавьте помещения.")
            return
        }
        if (botToken.isBlank() || channelId.isBlank()) {
            state.value = current.copy(error = "Не задан BOT_TOKEN или CHANNEL_ID.")
            return
        }
        if (!NetworkStatus.hasInternet(getApplication())) {
            state.value = current.copy(
                error = "Нет подключения к интернету. Включите Wi‑Fi или мобильные данные.",
            )
            return
        }

        state.value = current.copy(isSending = true, error = null)
        viewModelScope.launch {
            try {
                val total = queueTotalArea()
                val saved = tasksRepo.sendTaskAndSave(
                    botToken = botToken,
                    channelId = channelId,
                    employeeKey = current.currentEmployeeKey,
                    queue = current.selectedRooms,
                    totalArea = total,
                    comment = current.comment,
                    taskForDate = current.taskForChannelDate,
                )
                state.value = state.value.copy(
                    isSending = false,
                    step = Step.AfterSent,
                    lastSentTotalArea = total,
                    selectedRooms = emptyList(),
                    comment = null,
                    pendingAdd = null,
                    editingQueueIndex = null,
                    error = null,
                    lastSentChannel = "telegram",
                    taskForChannelDate = null,
                )
            } catch (e: Exception) {
                state.value = state.value.copy(
                    isSending = false,
                    error = e.message ?: "Ошибка отправки",
                )
            }
        }
    }

    fun sendTaskMax(maxBotToken: String, maxChatId: String) {
        val current = state.value
        if (current.selectedRooms.isEmpty()) {
            state.value = current.copy(error = "Очередь пуста. Добавьте помещения.")
            return
        }

        if (maxBotToken.isBlank() || maxChatId.isBlank()) {
            state.value = current.copy(error = "Не заданы MAX_BOT_TOKEN или MAX_CHAT_ID.")
            return
        }
        if (!NetworkStatus.hasInternet(getApplication())) {
            state.value = current.copy(
                error = "Нет подключения к интернету. Включите Wi‑Fi или мобильные данные.",
            )
            return
        }

        state.value = current.copy(isSending = true, error = null)
        viewModelScope.launch {
            try {
                val total = queueTotalArea()
                val text = tasksRepo.buildChannelMessage(
                    employeeKey = current.currentEmployeeKey,
                    queue = current.selectedRooms,
                    totalArea = total,
                    comment = current.comment,
                    taskForDate = current.taskForChannelDate,
                )
                maxClient.sendMessage(
                    botToken = maxBotToken,
                    chatId = maxChatId,
                    text = text,
                )
                tasksRepo.saveTaskLocal(
                    employeeKey = current.currentEmployeeKey,
                    queue = current.selectedRooms,
                    totalArea = total,
                    comment = current.comment,
                )
                state.value = state.value.copy(
                    isSending = false,
                    step = Step.AfterSent,
                    lastSentTotalArea = total,
                    selectedRooms = emptyList(),
                    comment = null,
                    pendingAdd = null,
                    editingQueueIndex = null,
                    error = null,
                    lastSentChannel = "max",
                    taskForChannelDate = null,
                )
            } catch (e: Exception) {
                state.value = state.value.copy(
                    isSending = false,
                    error = e.message ?: "Ошибка отправки в MAX",
                )
            }
        }
    }

    fun sendTaskVk(vkAccessToken: String, vkGroupId: String) {
        val current = state.value
        if (current.selectedRooms.isEmpty()) {
            state.value = current.copy(error = "Очередь пуста. Добавьте помещения.")
            return
        }

        if (vkAccessToken.isBlank() || vkGroupId.isBlank()) {
            state.value = current.copy(error = "Не заданы VK_ACCESS_TOKEN или VK_GROUP_ID.")
            return
        }
        if (!NetworkStatus.hasInternet(getApplication())) {
            state.value = current.copy(
                error = "Нет подключения к интернету. Включите Wi‑Fi или мобильные данные.",
            )
            return
        }

        state.value = current.copy(isSending = true, error = null)
        viewModelScope.launch {
            try {
                val total = queueTotalArea()
                val text = tasksRepo.buildChannelMessage(
                    employeeKey = current.currentEmployeeKey,
                    queue = current.selectedRooms,
                    totalArea = total,
                    comment = current.comment,
                    taskForDate = current.taskForChannelDate,
                )
                val ownerId = "-${vkGroupId.trim()}"
                vkClient.postWall(
                    accessToken = vkAccessToken,
                    ownerId = ownerId,
                    message = text,
                )
                tasksRepo.saveTaskLocal(
                    employeeKey = current.currentEmployeeKey,
                    queue = current.selectedRooms,
                    totalArea = total,
                    comment = current.comment,
                )
                state.value = state.value.copy(
                    isSending = false,
                    step = Step.AfterSent,
                    lastSentTotalArea = total,
                    selectedRooms = emptyList(),
                    comment = null,
                    pendingAdd = null,
                    editingQueueIndex = null,
                    error = null,
                    lastSentChannel = "vk",
                    taskForChannelDate = null,
                )
            } catch (e: Exception) {
                state.value = state.value.copy(
                    isSending = false,
                    error = e.message ?: "Ошибка отправки во ВКонтакте",
                )
            }
        }
    }

    fun startNewTask() {
        state.value = state.value.copy(
            step = Step.ChooseEmployee,
            selectedRooms = emptyList(),
            comment = null,
            pendingAdd = null,
            editingQueueIndex = null,
            error = null,
            lastSentTotalArea = null,
            isSending = false,
            lastSentChannel = null,
            bnovoCleaningDate = null,
            bnovoPlanned = null,
            bnovoWizardSteps = emptyList(),
            bnovoWizardIndex = 0,
            bnovoLayoutChoices = emptyMap(),
            bnovoPerBedChoices = emptyMap(),
            bnovoPerBedColorDraft = null,
            taskForChannelDate = null,
        )
    }
}

