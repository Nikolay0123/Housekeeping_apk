package com.example.tasksbot.domain

import com.example.tasksbot.db.RoomEntity
import com.example.tasksbot.network.BnovoClient
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Автозадание на «завтра» для 1-го этажа: номера 101–109 и общие помещения (лимит [TaskLogic.AREA_LIMIT] м²).
 */
object AutoTaskFromBnovo {

    enum class FloorChoice {
        /** Зона из ТЗ: 101–109 + кабинеты + 1 этаж + кухня. */
        First,
    }

    data class PlannedRoom(
        val entity: RoomEntity,
        val cleaningType: String,
        /** Нужен выбор «соединены / разъединены» (101–107, 109 при смене белья). */
        val needsBedChoice: Boolean,
    )

    private val FLOOR1_NUMBER_NAMES: List<String> = (101..109).map { "Номер $it" }

    private val FLOOR1_COMMON_NAMES: List<String> = listOf(
        "Кабинет администрации",
        "Кабинет директора",
        "Комната администраторов",
        "1 этаж",
        "Кухня",
    )

    fun tomorrowCleaningDate(): LocalDate = LocalDate.now().plusDays(1)

    fun plannedRoomsForFloor(floor: FloorChoice): List<String> = when (floor) {
        FloorChoice.First -> FLOOR1_NUMBER_NAMES + FLOOR1_COMMON_NAMES
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
    ): List<PlannedRoom> {
        val queue = ArrayList<PlannedRoom>()
        var runningArea = 0.0
        val limit = TaskLogic.AREA_LIMIT

        for (name in FLOOR1_NUMBER_NAMES) {
            val ent = activeRoomsByName[name] ?: continue
            val key = cleaningTypeForRoom(
                roomName = name,
                bookings = bookingsByRoom[name].orEmpty(),
                cleaningDate = cleaningDate,
            ) ?: continue
            val needsBeds = needsBedConfiguration(name, key)
            queue.add(PlannedRoom(entity = ent, cleaningType = key, needsBedChoice = needsBeds))
            runningArea += ent.area
        }

        for (name in FLOOR1_COMMON_NAMES) {
            val ent = activeRoomsByName[name] ?: continue
            if (runningArea + ent.area > limit) break
            queue.add(
                PlannedRoom(
                    entity = ent,
                    cleaningType = "current",
                    needsBedChoice = false,
                ),
            )
            runningArea += ent.area
        }
        return queue
    }

    fun needsBedConfiguration(roomName: String, cleaningType: String): Boolean {
        if (roomName !in FLOOR1_NUMBER_NAMES) return false
        if (TaskLogic.isRoom108(roomName)) return false
        val linen = TaskLogic.roomLinenProfile(roomName)
        if (linen != "classic") return false
        return cleaningType != "current"
    }

    /**
     * @param bedsJoined `true` — соединены; `false` — разъединены; `null` по умолчанию как соединены.
     * @param splitBeds при разъединённых: 1 или 2 кровати (для 101–107 и 109).
     */
    fun plannedToQueueItem(
        planned: PlannedRoom,
        bedsJoined: Boolean?,
        splitBeds: Int = 2,
    ): QueueItem {
        val room = planned.entity
        val ct = planned.cleaningType
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
     */
    internal fun cleaningTypeForRoom(
        roomName: String,
        bookings: List<BnovoClient.NormalizedBooking>,
        cleaningDate: LocalDate,
    ): String? {
        val C = cleaningDate
        // Включая «нулевые» ночи, если заезд = выезд (бронь на день — у API бывает arrival == departure).
        val bounded = bookings.filter { !it.arrival.isAfter(it.departure) }

        // Выезд «утром C»: в PMS часто date_departure = C, реже последняя ночь = C−1.
        val leaving = bounded.filter { it.departure == C || it.departure == C.minusDays(1) }
        val arriving = bounded.filter { it.arrival == C }
        val staying = bounded.filter { it.arrival.isBefore(C) && it.departure.isAfter(C) }

        if (leaving.isNotEmpty()) {
            return if (arriving.isNotEmpty()) "departure_arrival" else "departure"
        }
        if (staying.isNotEmpty()) {
            val b = staying.minByOrNull { it.arrival } ?: return null
            val dayNum = dayIndexFromArrival(b.arrival, C)
            if (dayNum >= 2 && dayNum % 3 == 0) return "current_linen"
            if (dayNum >= 2) return "current"
            return null
        }
        if (arriving.isNotEmpty() && staying.isEmpty() && leaving.isEmpty()) {
            return null
        }
        return null
    }

    /** День заезда = 1; следующий календарный день = 2 и т.д. */
    private fun dayIndexFromArrival(arrival: LocalDate, cleaningDate: LocalDate): Int {
        val d = ChronoUnit.DAYS.between(arrival, cleaningDate).toInt()
        return d + 1
    }
}
