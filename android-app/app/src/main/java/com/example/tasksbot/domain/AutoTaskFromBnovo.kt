package com.example.tasksbot.domain

import com.example.tasksbot.db.RoomEntity
import com.example.tasksbot.network.BnovoClient
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Автозадание на «завтра» для 1-го и 4-го этажей по данным Bnovo (лимит [TaskLogic.AREA_LIMIT] м²).
 */
object AutoTaskFromBnovo {

    enum class FloorChoice {
        First,
        Fourth,
    }

    data class PlannedRoom(
        val entity: RoomEntity,
        val cleaningType: String,
        /** 1 этаж: мастер «соединены / разъединены» для classic. */
        val needsClassicBedWizard: Boolean,
        /** 401.1, 402.4, 404.1, 405.4 — соединены или разъединены (комплекты 11/12). */
        val needsFloor4LayoutWizard: Boolean,
        /** Остальные номера 4 этажа — цвет и число кроватей (ёмкость из Bnovo). */
        val needsFloor4PerBedWizard: Boolean,
        /** Верхняя граница числа кроватей в мастере per-bed. */
        val floor4MaxBeds: Int,
    )

    sealed class BnovoWizardStep {
        data class ClassicBeds(val planned: PlannedRoom) : BnovoWizardStep()
        data class Floor4Layout(val planned: PlannedRoom) : BnovoWizardStep()
        data class Floor4PerBed(val planned: PlannedRoom, val maxBeds: Int) : BnovoWizardStep()
    }

    fun buildBnovoWizardSteps(planned: List<PlannedRoom>): List<BnovoWizardStep> {
        val out = ArrayList<BnovoWizardStep>()
        for (p in planned) {
            when {
                p.needsClassicBedWizard -> out.add(BnovoWizardStep.ClassicBeds(p))
                p.needsFloor4LayoutWizard -> out.add(BnovoWizardStep.Floor4Layout(p))
                p.needsFloor4PerBedWizard -> out.add(BnovoWizardStep.Floor4PerBed(p, p.floor4MaxBeds.coerceIn(1, 20)))
            }
        }
        return out
    }

    private val FLOOR1_NUMBER_NAMES: List<String> = (101..109).map { "Номер $it" }

    private val FLOOR1_COMMON_NAMES: List<String> = listOf(
        "Кабинет администрации",
        "Кабинет директора",
        "Комната администраторов",
        "1 этаж",
        "Кухня",
    )

    private val FLOOR4_ORDER: List<String> = listOf(
        "Номер 401.1", "Номер 401.2", "Номер 401.3", "Номер 401.4",
        "Номер 402.1", "Номер 402.2", "Номер 402.3", "Номер 402.4",
        "Номер 403",
        "Номер 404.1", "Номер 404.2", "Номер 404.3", "Номер 404.4",
        "Номер 405.1", "Номер 405.2", "Номер 405.3", "Номер 405.4",
        "Блок 401", "Блок 402", "Кухня блока 401,402",
        "Блок 404", "Блок 405", "Кухня блока 404,405",
        "Холл 4 этаж", "Лестница до 5 этажа",
    )

    fun tomorrowCleaningDate(): LocalDate = LocalDate.now().plusDays(1)

    fun plannedRoomsForFloor(floor: FloorChoice): List<String> = when (floor) {
        FloorChoice.First -> FLOOR1_NUMBER_NAMES + FLOOR1_COMMON_NAMES
        FloorChoice.Fourth -> FLOOR4_ORDER
    }

    fun emptyPlanMessageForFloor(floor: FloorChoice): String = when (floor) {
        FloorChoice.First -> "По данным Bnovo на завтра нет задач по номерам 101–109. Проверьте API и названия номеров."
        FloorChoice.Fourth -> "По данным Bnovo на завтра нет задач по выбранным помещениям 4 этажа. Проверьте API и названия номеров."
    }

    /** Группировка сырых броней по каноническому имени комнаты (как в приложении). */
    fun indexBookingsByRoom(bookings: List<BnovoClient.NormalizedBooking>): Map<String, List<BnovoClient.NormalizedBooking>> {
        val m = HashMap<String, MutableList<BnovoClient.NormalizedBooking>>()
        for (b in bookings) {
            val key = BnovoClient.normalizeRoomLabel(b.roomLabel) ?: continue
            if (key.startsWith("id:")) continue
            m.getOrPut(key) { ArrayList() }.add(b)
        }
        return m
    }

    fun planFirstFloor(
        activeRoomsByName: Map<String, RoomEntity>,
        bookingsByRoom: Map<String, List<BnovoClient.NormalizedBooking>>,
        cleaningDate: LocalDate,
    ): List<PlannedRoom> = planOrderedRooms(
        roomNames = FLOOR1_NUMBER_NAMES,
        commonNames = FLOOR1_COMMON_NAMES,
        activeRoomsByName = activeRoomsByName,
        bookingsByRoom = bookingsByRoom,
        cleaningDate = cleaningDate,
        floorChoice = FloorChoice.First,
    )

    fun planFourthFloor(
        activeRoomsByName: Map<String, RoomEntity>,
        bookingsByRoom: Map<String, List<BnovoClient.NormalizedBooking>>,
        cleaningDate: LocalDate,
    ): List<PlannedRoom> = planOrderedRooms(
        roomNames = FLOOR4_ORDER.filter { it.startsWith("Номер ") },
        commonNames = FLOOR4_ORDER.filter { !it.startsWith("Номер ") },
        activeRoomsByName = activeRoomsByName,
        bookingsByRoom = bookingsByRoom,
        cleaningDate = cleaningDate,
        floorChoice = FloorChoice.Fourth,
    )

    /**
     * Приоритет в очереди уборки (меньше — раньше): выезд/заезд → выезд → текущая/смена белья → текущая.
     * Прочие ключи (например [general]) — после «текущей».
     */
    private fun cleaningTypeQueueRank(cleaningType: String): Int = when (cleaningType) {
        "departure_arrival" -> 0
        "departure" -> 1
        "current_linen" -> 2
        "current" -> 3
        else -> 4
    }

    private fun planOrderedRooms(
        roomNames: List<String>,
        commonNames: List<String>,
        activeRoomsByName: Map<String, RoomEntity>,
        bookingsByRoom: Map<String, List<BnovoClient.NormalizedBooking>>,
        cleaningDate: LocalDate,
        floorChoice: FloorChoice,
    ): List<PlannedRoom> {
        val queue = ArrayList<PlannedRoom>()
        var runningArea = 0.0
        val limit = TaskLogic.AREA_LIMIT

        val numbered = ArrayList<PlannedRoom>()
        val roomIndexByName = roomNames.withIndex().associate { it.value to it.index }
        for (name in roomNames) {
            val ent = activeRoomsByName[name] ?: continue
            val key = cleaningTypeForRoom(
                bookings = bookingsByRoom[name].orEmpty(),
                cleaningDate = cleaningDate,
            ) ?: continue
            val planned = plannedRoomFor(
                floorChoice = floorChoice,
                entity = ent,
                cleaningType = key,
                bookings = bookingsByRoom[name].orEmpty(),
                cleaningDate = cleaningDate,
            )
            numbered.add(planned)
        }
        numbered.sortWith(
            compareBy({ cleaningTypeQueueRank(it.cleaningType) }, { roomIndexByName[it.entity.name] ?: 0 }),
        )
        for (p in numbered) {
            queue.add(p)
            runningArea += p.entity.area
        }

        for (name in commonNames) {
            val ent = activeRoomsByName[name] ?: continue
            if (runningArea + ent.area > limit) break
            queue.add(
                PlannedRoom(
                    entity = ent,
                    cleaningType = "current",
                    needsClassicBedWizard = false,
                    needsFloor4LayoutWizard = false,
                    needsFloor4PerBedWizard = false,
                    floor4MaxBeds = 4,
                ),
            )
            runningArea += ent.area
        }
        return queue
    }

    /**
     * Ёмкость номера из [room_type_name] брони, релевантной дате уборки (проживание / выезд утром).
     */
    internal fun guestCapacityFromBookings(
        bookings: List<BnovoClient.NormalizedBooking>,
        cleaningDate: LocalDate,
    ): Int? {
        val C = cleaningDate
        val bounded = bookings.filter { !it.arrival.isAfter(it.departure) && it.isActiveForOccupancy() }
        val staying = bounded.filter { it.arrival.isBefore(C) && it.departure.isAfter(C) }
        val leaving = bounded.filter { it.departure == C }
        val pool = when {
            staying.isNotEmpty() -> staying
            leaving.isNotEmpty() -> leaving
            else -> bounded
        }
        var best: Int? = null
        for (b in pool) {
            val cap = TaskLogic.guestCapacityFromRoomTypeName(b.roomTypeName) ?: continue
            best = if (best == null) cap else maxOf(best, cap)
        }
        return best
    }

    fun plannedRoomFor(
        floorChoice: FloorChoice,
        entity: RoomEntity,
        cleaningType: String,
        bookings: List<BnovoClient.NormalizedBooking>,
        cleaningDate: LocalDate,
    ): PlannedRoom {
        val name = entity.name
        val cap = guestCapacityFromBookings(bookings, cleaningDate)?.coerceIn(1, 20) ?: 4
        val classic = TaskLogic.roomLinenProfile(name) == "classic"
        val needsClassic = floorChoice == FloorChoice.First &&
            classic &&
            needsBedConfigurationFirstFloor(name, cleaningType)
        val perBed = TaskLogic.isFloor4PerBedBnovoRoom(name)
        val layout = TaskLogic.isFloor4LayoutBnovoRoom(name)
        val needsF4Layout = floorChoice == FloorChoice.Fourth && layout && cleaningType != "current"
        val needsF4PerBed = floorChoice == FloorChoice.Fourth && perBed && cleaningType != "current"
        return PlannedRoom(
            entity = entity,
            cleaningType = cleaningType,
            needsClassicBedWizard = needsClassic,
            needsFloor4LayoutWizard = needsF4Layout,
            needsFloor4PerBedWizard = needsF4PerBed,
            floor4MaxBeds = cap,
        )
    }

    fun needsBedConfigurationFirstFloor(roomName: String, cleaningType: String): Boolean {
        if (roomName !in FLOOR1_NUMBER_NAMES) return false
        if (TaskLogic.isRoom108(roomName)) return false
        if (TaskLogic.isRoom101Or107(roomName)) return false
        val linen = TaskLogic.roomLinenProfile(roomName)
        if (linen != "classic") return false
        return cleaningType != "current"
    }

    fun plannedToQueueItem(
        planned: PlannedRoom,
        bedsJoined: Boolean?,
        splitBeds: Int,
        floor4PerBedColor: String?,
        floor4PerBedCount: Int?,
    ): QueueItem {
        val room = planned.entity
        val ct = planned.cleaningType
        val name = room.name

        if (TaskLogic.isFloor4LayoutBnovoRoom(name) && ct != "current") {
            val joined = bedsJoined ?: true
            return QueueItem(
                id = room.id,
                name = room.name,
                area = room.area,
                cleaningType = ct,
                linenProfile = "floor4",
                linenVariant = if (joined) TaskLogic.LINEN_VARIANT_FLOOR4_JOINED else TaskLogic.LINEN_VARIANT_FLOOR4_SPLIT,
            )
        }

        if (TaskLogic.isFloor4PerBedBnovoRoom(name) && ct != "current") {
            val color = floor4PerBedColor?.takeIf { it in TaskLogic.LINEN_COLOR_ORDER_FLOOR4_PER_BED } ?: "blue"
            val maxB = planned.floor4MaxBeds.coerceIn(1, 20)
            val beds = (floor4PerBedCount ?: 1).coerceIn(1, maxB)
            return QueueItem(
                id = room.id,
                name = room.name,
                area = room.area,
                cleaningType = ct,
                linenProfile = "floor4",
                linenVariant = TaskLogic.LINEN_VARIANT_FLOOR4_PER_BED,
                linenColor = color,
                linenBeds = beds,
            )
        }

        val classic = TaskLogic.roomLinenProfile(room.name)
        val needsLinen = classic != null && ct != "current"

        if (!needsLinen) {
            return QueueItem(
                id = room.id,
                name = room.name,
                area = room.area,
                cleaningType = ct,
            )
        }

        if (TaskLogic.isRoom108(room.name)) {
            return QueueItem(
                id = room.id,
                name = room.name,
                area = room.area,
                cleaningType = ct,
                linenVariant = 3,
            )
        }

        if (TaskLogic.isRoom101Or107(room.name)) {
            return QueueItem(
                id = room.id,
                name = room.name,
                area = room.area,
                cleaningType = ct,
                linenVariant = TaskLogic.LINEN_VARIANT_CLASSIC_101_107,
            )
        }

        val joined = bedsJoined ?: true
        val beds = splitBeds.coerceIn(1, 2)

        if (TaskLogic.isRoom109(room.name)) {
            return if (joined) {
                QueueItem(
                    id = room.id,
                    name = room.name,
                    area = room.area,
                    cleaningType = ct,
                    linenVariant = 5,
                )
            } else {
                QueueItem(
                    id = room.id,
                    name = room.name,
                    area = room.area,
                    cleaningType = ct,
                    linenVariant = 6,
                    linenBeds = beds,
                )
            }
        }

        return if (joined) {
            QueueItem(
                id = room.id,
                name = room.name,
                area = room.area,
                cleaningType = ct,
                linenVariant = 1,
            )
        } else {
            QueueItem(
                id = room.id,
                name = room.name,
                area = room.area,
                cleaningType = ct,
                linenVariant = 2,
                linenBeds = beds,
            )
        }
    }

    /**
     * Логика вида уборки на дату [cleaningDate] (обычно «завтра»).
     * [departure] — дата выезда из брони; день выезда трактуется как день отъезда гостя (уборка «выезд» в этот день).
     * Сегменты «Выехал» / «Отменен» и с `cancel_date` не учитываются, чтобы старые брони не перекрывали текущую.
     * Выезд только при `departure == C` (не `C−1`: иначе вчерашний выезд ошибочно давал «выезд» на завтра).
     */
    internal fun cleaningTypeForRoom(
        bookings: List<BnovoClient.NormalizedBooking>,
        cleaningDate: LocalDate,
    ): String? {
        val C = cleaningDate
        val bounded = bookings.filter {
            !it.arrival.isAfter(it.departure) && it.isActiveForOccupancy()
        }

        val leaving = bounded.filter { it.departure == C }
        val arriving = bounded.filter { it.arrival == C }
        val staying = bounded.filter { it.arrival.isBefore(C) && it.departure.isAfter(C) }

        if (leaving.isNotEmpty()) {
            return if (arriving.isNotEmpty()) "departure_arrival" else "departure"
        }
        if (staying.isNotEmpty()) {
            val b = staying.minByOrNull { it.arrival } ?: return null
            val dayNum = dayIndexFromArrival(b.arrival, C)
            // Смена белья: каждый 3-й день пребывания, со сдвигом на день (4-й, 7-й, 10-й…; день заезда = 1).
            if (dayNum >= 2 && (dayNum - 1) % 3 == 0) return "current_linen"
            if (dayNum >= 2) return "current"
            return null
        }
        if (arriving.isNotEmpty() && staying.isEmpty() && leaving.isEmpty()) {
            return null
        }
        return null
    }

    private fun dayIndexFromArrival(arrival: LocalDate, cleaningDate: LocalDate): Int {
        val d = ChronoUnit.DAYS.between(arrival, cleaningDate).toInt()
        return d + 1
    }
}
