package com.example.tasksbot.domain

import com.example.tasksbot.db.RoomEntity
import com.example.tasksbot.db.SeedData
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

object TaskLogic {
    const val AREA_LIMIT: Double = 375.0
    const val BOSS_NAME: String = "Екатерина"

    /** Группировка номеров на экране выбора:
     *  - 1 этаж: 101–109
     *  - 4 этаж: 401.1–401.4, 402.1–402.4, 403, 404.1–405.4, блоки, холл, лестница до 5 этажа
     *  - Помещения: прочее (арендаторы 2/3 этаж, входная группа, кабинеты и т.д.)
     */
    enum class RoomPickerTab {
        Floor1,
        Block404405,
        Other,
    }

    private val FLOOR4_TAB_BY_NAME: Set<String> = setOf(
        "Блок 401", "Блок 402", "Кухня блока 401,402",
        "Блок 404", "Блок 405", "Кухня блока 404,405",
        "Холл 4 этаж", "Лестница до 5 этажа",
    )

    fun roomPickerTab(roomName: String): RoomPickerTab {
        if (roomName in FLOOR4_TAB_BY_NAME) return RoomPickerTab.Block404405
        if (!roomName.startsWith("Номер ")) return RoomPickerTab.Other
        val rest = roomName.removePrefix("Номер ").trim()
        if (rest.all { it.isDigit() }) {
            val n = rest.toIntOrNull() ?: return RoomPickerTab.Other
            return when {
                n in 101..109 -> RoomPickerTab.Floor1
                n == 403 -> RoomPickerTab.Block404405
                else -> RoomPickerTab.Other
            }
        }
        val m = Regex("^(\\d+)\\.(\\d+)$").matchEntire(rest) ?: return RoomPickerTab.Other
        val major = m.groupValues[1].toInt()
        val minor = m.groupValues[2].toInt()
        if (minor !in 1..4) return RoomPickerTab.Other
        if (major in 401..402) return RoomPickerTab.Block404405
        if (major in 404..405) return RoomPickerTab.Block404405
        return RoomPickerTab.Other
    }

    fun sortRoomsForPicker(rooms: List<RoomEntity>): List<RoomEntity> {
        val order = SeedData.roomPickerPriority.withIndex().associate { it.value to it.index }
        return rooms.sortedWith(compareBy({ order[it.name] ?: 10_000 }, { it.name }))
    }

    /** Номера 404.1–405.4 — отдельный шаг «сколько кроватей застелить». */
    fun isFloor404to405BlockRoom(roomName: String): Boolean {
        if (!roomName.startsWith("Номер ")) return false
        val rest = roomName.removePrefix("Номер ").trim()
        val m = Regex("^(\\d+)\\.(\\d+)$").matchEntire(rest) ?: return false
        val major = m.groupValues[1].toInt()
        val minor = m.groupValues[2].toInt()
        return major in 404..405 && minor in 1..4
    }

    // Виды уборки для каждого номера
    val CLEANING_TYPES: LinkedHashMap<String, String> = linkedMapOf(
        "current" to "текущая",
        "current_linen" to "текущая/смена белья",
        "departure" to "выезд",
        "departure_arrival" to "выезд/заезд",
        "general" to "генеральная",
    )

    fun formatCleaningType(key: String): String = CLEANING_TYPES[key] ?: key

    private val LEGACY_EMPLOYEE_NAMES: Map<String, String> = mapOf(
        "dina" to "ДИНА",
        "lena" to "ЛЕНА",
        "olya" to "ОЛЯ",
        "admin" to "АДМИНИСТРАТОР",
    )

    /** [displayNamesByKey] — ключи в нижнем регистре; подставляется из справочника сотрудников в БД. */
    fun formatEmployeeName(
        employeeKey: String,
        displayNamesByKey: Map<String, String> = emptyMap(),
    ): String {
        val k = employeeKey.lowercase(Locale.ROOT)
        displayNamesByKey[k]?.let { return it }
        displayNamesByKey.entries.find { it.key.equals(employeeKey, ignoreCase = true) }?.value?.let { return it }
        LEGACY_EMPLOYEE_NAMES[k]?.let { return it }
        return employeeKey.uppercase(Locale.ROOT)
    }

    fun formatArea(value: Double): String {
        // Python: f"{value:,.2f}".replace(",", " ").replace(".", ",")
        val usSymbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat("#,##0.00", usSymbols)
        return df.format(value).replace(",", " ").replace(".", ",")
    }

    fun formatDateGroup(d: LocalDate): String {
        val today = LocalDate.now()
        return when (d) {
            today -> "Сегодня"
            today.minusDays(1) -> "Вчера"
            else -> d.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        }
    }

    /**
     * None — без выбора комплекта белья в сценарии бота.
     * classic — номера 101–109
     * floor4 — номера 401.x, 402.x, 403, 404.x, 405.x.
     */
    fun roomLinenProfile(roomName: String): String? {
        if (!roomName.startsWith("Номер ")) return null
        val rest = roomName.removePrefix("Номер ").trim()

        if (rest.all { it.isDigit() }) {
            val n = rest.toInt()
            return when {
                n in 101..109 -> "classic"
                n == 403 -> "floor4"
                else -> null
            }
        }

        val m = Regex("^(\\d+)(?:\\.(\\d+))?$").matchEntire(rest) ?: return null
        val major = m.groupValues[1].toInt()
        val minor = m.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }?.toInt()

        if (major == 403 && minor == null) return "floor4"
        if (major in listOf(401, 402, 404, 405) && minor != null && minor in 1..4) return "floor4"
        return null
    }

    // Цвет белья (4 этаж); «белое» — и для 4 этажа, и считается для classic-комплектов 101–109 в итоге
    val LINEN_COLORS: Map<String, String> = mapOf(
        "blue" to "голубое",
        "gray" to "серое",
        "stripe" to "в полоску",
        "white" to "белое",
    )

    val LINEN_COLOR_ORDER: List<String> = listOf("blue", "gray", "stripe", "white")

    /** Только для номеров 4 этажа с выбором комплекта «на кровать» (Bnovo / ручное). */
    val LINEN_COLOR_ORDER_FLOOR4_PER_BED: List<String> = listOf("blue", "gray", "stripe")

    const val LINEN_VARIANT_FLOOR4_PER_BED: Int = 10
    const val LINEN_VARIANT_FLOOR4_JOINED: Int = 11
    const val LINEN_VARIANT_FLOOR4_SPLIT: Int = 12

    private val FLOOR4_PER_BED_ROOM_NAMES: Set<String> = setOf(
        "Номер 401.2", "Номер 401.3", "Номер 401.4",
        "Номер 402.1", "Номер 402.2", "Номер 402.3",
        "Номер 403",
        "Номер 404.2", "Номер 404.3", "Номер 404.4",
        "Номер 405.1", "Номер 405.2", "Номер 405.3",
    )

    private val FLOOR4_LAYOUT_ROOM_NAMES: Set<String> = setOf(
        "Номер 401.1", "Номер 402.4", "Номер 404.1", "Номер 405.4",
    )

    fun isFloor4PerBedBnovoRoom(roomName: String): Boolean = roomName in FLOOR4_PER_BED_ROOM_NAMES

    fun isFloor4LayoutBnovoRoom(roomName: String): Boolean = roomName in FLOOR4_LAYOUT_ROOM_NAMES

    /**
     * Число мест из [room_type_name] Bnovo (напр. «2-местный», «трёхместный»).
     * Если не распознано — null (тогда подставляется безопасный максимум в мастере).
     */
    fun guestCapacityFromRoomTypeName(roomTypeName: String?): Int? {
        if (roomTypeName.isNullOrBlank()) return null
        val s = roomTypeName.lowercase(Locale.getDefault())
        return when {
            "4-мест" in s || "4 мест" in s || "четырёхмест" in s || "четырехмест" in s -> 4
            "3-мест" in s || "3 мест" in s || "трёхмест" in s || "трехмест" in s -> 3
            "2-мест" in s || "2 мест" in s || "двухмест" in s || "двуспальн" in s -> 2
            "1-мест" in s || "1 мест" in s || "одномест" in s -> 1
            else -> null
        }
    }

    fun formatLinenColor(key: String?): String {
        if (key.isNullOrBlank()) return ""
        return LINEN_COLORS[key] ?: key
    }

    fun resolveLinenProfile(item: QueueItem): String? {
        val p = item.linenProfile
        return if (p == "classic" || p == "floor4") p else roomLinenProfile(item.name)
    }

    private const val LINEN_FOOT_TOWEL = "Полотенце для ног"

    // Комплекты белья для номеров 101–109
    // Ключ — номер варианта, значение — словарь "Наименование" → количество
    val LINEN_PACKAGES: Map<Int, Map<String, Int>> = mapOf(
        1 to mapOf(
            "Простыня двуспальная" to 1,
            "Пододеяльник двуспальный" to 1,
            "Наволочка" to 2,
            "Полотенце банное с вышивкой" to 2,
            "Полотенце для лица" to 2,
            "Полотенце для ног" to 1,
        ),
        2 to mapOf(
            "Простыня 1,5 спальная" to 2,
            "Пододеяльник 1,5 спальный" to 2,
            "Наволочка" to 2,
            "Полотенце банное с вышивкой" to 2,
            "Полотенце для лица" to 2,
            "Полотенце для ног" to 1,
        ),
        3 to mapOf(
            "Простыня люкс" to 1,
            "Пододеяльник люкс" to 1,
            "Наволочка с люкс (с вышивкой)" to 4,
            "Полотенце банное с вышивкой" to 2,
            "Полотенце для лица" to 2,
            "Полотенце для ног" to 1,
        ),
        4 to mapOf(
            "Простыня люкс" to 1,
            "Пододеяльник двуспальный" to 1,
            "Наволочка" to 2,
            "Полотенце банное с вышивкой" to 2,
            "Полотенце для лица" to 2,
            "Полотенце для ног" to 1,
        ),
        /** Номер 109 — кровати соединены (автозадание Bnovo). */
        5 to mapOf(
            "Простыня люкс" to 1,
            "Пододеяльник двуспальный" to 1,
            "Наволочка" to 2,
            "Полотенце банное с вышивкой" to 2,
            "Полотенце для лица" to 2,
            "Полотенце для ног" to 1,
            "Халат вафельный" to 1,
        ),
        /** Номер 109 — разъединены; масштаб как у варианта 2 при linenBeds. */
        6 to mapOf(
            "Простыня 1,5 спальная" to 2,
            "Пододеяльник 1,5 спальный" to 2,
            "Наволочка" to 2,
            "Полотенце банное с вышивкой" to 2,
            "Полотенце для лица" to 2,
            "Полотенце для ног" to 1,
            "Халат вафельный" to 1,
        ),
    )

    /** Номер 108: фиксированный люкс-комплект с махровым халатом (linenVariant = 3). */
    val LINEN_PACKAGE_108: Map<String, Int> = mapOf(
        "Простыня люкс" to 1,
        "Пододеяльник люкс" to 1,
        "Наволочка с люкс (с вышивкой)" to 4,
        "Полотенце банное с вышивкой" to 2,
        "Полотенце для лица" to 2,
        "Полотенце для ног" to 1,
        "Халат махровый" to 1,
    )

    /** Одна заправляемая кровать (масштаб по [linenBeds] для варианта [LINEN_VARIANT_FLOOR4_PER_BED]). */
    val LINEN_FLOOR4_PER_BED_BASE: Map<String, Int> = mapOf(
        "Простыня 1,5 спальная" to 1,
        "Пододеяльник 1,5 спальный" to 1,
        "Наволочка" to 1,
        "Полотенце банное" to 1,
        "Полотенце 40х70" to 1,
    )

    /** 401.1 / 402.4 / 404.1 / 405.4 — кровати соединены. */
    val LINEN_FLOOR4_JOINED_LAYOUT: Map<String, Int> = mapOf(
        "Простыня двуспальная" to 1,
        "Пододеяльник 1,5 спальный" to 2,
        "Наволочка" to 2,
        "Полотенце банное" to 2,
        "Полотенце 40х70" to 2,
    )

    /** Те же номера — кровати разъединены (две 1,5-спальные связки). */
    val LINEN_FLOOR4_SPLIT_LAYOUT: Map<String, Int> = mapOf(
        "Простыня 1,5 спальная" to 2,
        "Пододеяльник 1,5 спальный" to 2,
        "Наволочка" to 2,
        "Полотенце банное" to 2,
        "Полотенце 40х70" to 2,
    )

    /** Устаревшие варианты 1–3 (на 2/3/4 гостя); для совместимости со старыми заданиями в истории. */
    val LINEN_PACKAGES_FLOOR4: Map<Int, Map<String, Int>> = mapOf(
        1 to mapOf(
            "Простыня 1,5 спальная" to 2,
            "Пододеяльник 1,5 спальный" to 2,
            "Наволочка" to 2,
            "Полотенце банное" to 2,
            "Полотенце 40х70" to 2,
        ),
        2 to mapOf(
            "Простыня 1,5 спальная" to 3,
            "Пододеяльник 1,5 спальный" to 3,
            "Наволочка" to 3,
            "Полотенце банное" to 3,
            "Полотенце 40х70" to 3,
        ),
        3 to mapOf(
            "Простыня 1,5 спальная" to 4,
            "Пододеяльник 1,5 спальный" to 4,
            "Наволочка" to 4,
            "Полотенце банное" to 4,
            "Полотенце 40х70" to 4,
        ),
    )

    fun isRoom108(roomName: String): Boolean = roomName.trim() == "Номер 108"

    fun isRoom109(roomName: String): Boolean = roomName.trim() == "Номер 109"

    fun classicLinenQuantities(item: QueueItem): LinkedHashMap<String, Int>? {
        val v = item.linenVariant ?: return null

        if (isRoom108(item.name) && v == 3) {
            return LinkedHashMap(LINEN_PACKAGE_108)
        }

        if (v == 5) {
            return LinkedHashMap(LINEN_PACKAGES[5] ?: return null)
        }

        if (v == 6) {
            val base = LINEN_PACKAGES[6] ?: return null
            val bedsRaw = item.linenBeds ?: 2
            val beds = if (bedsRaw in listOf(1, 2)) bedsRaw else 2
            if (beds == 1) {
                val scaled = LinkedHashMap<String, Int>()
                for ((k, q) in base) {
                    scaled[k] =
                        if (k == LINEN_FOOT_TOWEL && q > 0) max(1, q / 2)
                        else if (k == "Халат вафельный" && q > 0) max(1, q)
                        else max(0, q / 2)
                }
                return scaled
            }
            return LinkedHashMap(base)
        }

        if (v !in LINEN_PACKAGES) return null
        val base = LINEN_PACKAGES[v] ?: return null

        if (v == 2) {
            val bedsRaw = item.linenBeds ?: 2
            val beds = if (bedsRaw in listOf(1, 2)) bedsRaw else 2
            if (beds == 1) {
                val scaled = LinkedHashMap<String, Int>()
                for ((k, q) in base) {
                    scaled[k] =
                        if (k == LINEN_FOOT_TOWEL && q > 0) max(1, q / 2)
                        else max(0, q / 2)
                }
                return scaled
            }
        }

        return LinkedHashMap(base)
    }

    fun classicVariant2BedsLabel(beds: Int?): Int {
        return if (beds == 1) 1 else 2
    }

    fun floor4DefaultBedsForVariant(variant: Int): Int = when (variant) {
        1 -> 2
        2 -> 3
        3 -> 4
        else -> 2
    }

    /**
     * Для 401–403 и т.п. — полный комплект варианта. Для 404.1–405.4 — масштабирование от числа кроватей
     * (1…4) относительно «базы» варианта (2 / 3 / 4 места).
     */
    fun floor4LinenQuantities(item: QueueItem): LinkedHashMap<String, Int>? {
        val variant = item.linenVariant ?: return null
        if (resolveLinenProfile(item) != "floor4") return null
        when (variant) {
            LINEN_VARIANT_FLOOR4_PER_BED -> {
                val n = (item.linenBeds ?: 1).coerceIn(1, 20)
                return LinkedHashMap(LINEN_FLOOR4_PER_BED_BASE.mapValues { (_, q) -> q * n })
            }
            LINEN_VARIANT_FLOOR4_JOINED -> return LinkedHashMap(LINEN_FLOOR4_JOINED_LAYOUT)
            LINEN_VARIANT_FLOOR4_SPLIT -> return LinkedHashMap(LINEN_FLOOR4_SPLIT_LAYOUT)
            else -> {
                val base = LINEN_PACKAGES_FLOOR4[variant] ?: return null
                return LinkedHashMap(base)
            }
        }
    }

    fun formatLinenPackageLines(pkg: Map<String, Int>): List<String> =
        pkg.entries.map { (name, qty) -> "• $name — $qty шт." }

    fun classicLinenVariantButtonSubtitle(variant: Int): String {
        val pkg = LINEN_PACKAGES[variant] ?: return ""
        return formatLinenPackageLines(pkg).joinToString("\n")
    }

    fun floor4LinenVariantButtonSubtitle(variant: Int): String {
        val pkg = when (variant) {
            LINEN_VARIANT_FLOOR4_PER_BED -> LINEN_FLOOR4_PER_BED_BASE
            LINEN_VARIANT_FLOOR4_JOINED -> LINEN_FLOOR4_JOINED_LAYOUT
            LINEN_VARIANT_FLOOR4_SPLIT -> LINEN_FLOOR4_SPLIT_LAYOUT
            else -> LINEN_PACKAGES_FLOOR4[variant] ?: return ""
        }
        val lines = formatLinenPackageLines(pkg)
        val label = when (variant) {
            LINEN_VARIANT_FLOOR4_PER_BED -> "За каждую выбранную кровать — полный набор строк ниже."
            LINEN_VARIANT_FLOOR4_JOINED -> "Кровати соединены."
            LINEN_VARIANT_FLOOR4_SPLIT -> "Кровати разъединены."
            1 -> "База на 2 места (устар.)."
            2 -> "База на 3 места (устар.)."
            3 -> "База на 4 места (устар.)."
            else -> ""
        }
        return if (label.isNotEmpty()) "$label\n${lines.joinToString("\n")}" else lines.joinToString("\n")
    }

    fun classicLinenVariantButtonTitle(variant: Int): String = when (variant) {
        1 -> "Вариант 1 — двуспальная связка"
        2 -> "Вариант 2 — две 1,5-спальные"
        3 -> "Вариант 3 — люкс (4 наволочки)"
        4 -> "Вариант 4 — люкс + двуспальный пододеяльник"
        5 -> "Номер 109 — соединённые кровати"
        6 -> "Номер 109 — разъединённые кровати"
        else -> "Вариант $variant"
    }

    fun floor4LinenVariantButtonTitle(variant: Int): String = when (variant) {
        LINEN_VARIANT_FLOOR4_PER_BED -> "Бельё: на каждую заправляемую кровать"
        LINEN_VARIANT_FLOOR4_JOINED -> "Кровати соединены (двуспальная простыня)"
        LINEN_VARIANT_FLOOR4_SPLIT -> "Кровати разъединены (две 1,5-спальные)"
        1 -> "Комплект на 2 гостя (база для масштаба)"
        2 -> "Комплект на 3 гостя"
        3 -> "Комплект на 4 гостя"
        else -> "Вариант $variant"
    }

    /** Строки для вставки в текст задания под номером (полный состав белья). */
    fun formatRoomLinenDetailLines(item: QueueItem): List<String> {
        val profile = resolveLinenProfile(item) ?: return emptyList()
        val v = item.linenVariant ?: return emptyList()
        val lines = mutableListOf<String>()
        when {
            profile == "classic" && (v in LINEN_PACKAGES || (isRoom108(item.name) && v == 3)) -> {
                val pkg = classicLinenQuantities(item) ?: return emptyList()
                lines += "🧺 Бельё (${classicLinenVariantButtonTitle(v)}):"
                lines += formatLinenPackageLines(pkg)
            }
            profile == "floor4" && floor4LinenQuantities(item) != null -> {
                val pkg = floor4LinenQuantities(item) ?: return emptyList()
                val col = formatLinenColor(item.linenColor)
                if (col.isNotEmpty() && v == LINEN_VARIANT_FLOOR4_PER_BED) lines += "Цвет комплекта: $col"
                if (v == LINEN_VARIANT_FLOOR4_PER_BED && item.linenBeds != null) {
                    lines += "Заправляемых кроватей: ${item.linenBeds}"
                }
                lines += "🧺 Состав белья (${floor4LinenVariantButtonTitle(v)}):"
                lines += formatLinenPackageLines(pkg)
            }
        }
        return lines
    }

    fun formatChannelMessage(
        employeeKey: String,
        queue: List<QueueItem>,
        totalArea: Double,
        comment: String?,
        /** Если задано (например автозадание Bnovo) — дата уборки «завтра». */
        taskForDate: LocalDate? = null,
        employeeDisplayNames: Map<String, String> = emptyMap(),
    ): String {
        val empName = formatEmployeeName(employeeKey, employeeDisplayNames)
        val limit = AREA_LIMIT
        val remainder = limit - totalArea

        val now = LocalDateTime.now()
        val dateStr = now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        val taskDateStr = taskForDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

        val lines = mutableListOf<String>()
        lines += listOf(
            "🧹 НОВОЕ ЗАДАНИЕ",
            "━━━━━━━━━━━━━━━━━━━━━",
            "",
            "👤 Исполнитель: $empName",
        )
        if (!taskDateStr.isNullOrBlank()) {
            lines += ""
            lines += "📅 Дата уборки: $taskDateStr"
        }
        lines += listOf(
            "",
            "ПОРЯДОК УБОРКИ:",
        )

        val linenTotals = LinkedHashMap<String, Int>()
        val linenColorTotals = LinkedHashMap<String, Int>()
        for (k in LINEN_COLOR_ORDER) {
            val label = LINEN_COLORS[k] ?: continue
            linenColorTotals[label] = 0
        }

        fun addLinenItem(name: String, qty: Int) {
            val cur = linenTotals[name] ?: 0
            linenTotals[name] = cur + qty
        }

        for ((idx, r) in queue.withIndex()) {
            val i = idx + 1
            val numEmoji = if (i <= 10) {
                listOf("1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣", "🔟")[minOf(i - 1, 9)]
            } else {
                "${i}."
            }

            val ct = formatCleaningType(r.cleaningType)
            val profile = resolveLinenProfile(r)

            var bedConfig = ""
            if (r.linenVariant != null) {
                val variant = r.linenVariant
                if (profile == "classic" && (variant in LINEN_PACKAGES || (isRoom108(r.name) && variant == 3))) {
                    bedConfig = when (variant) {
                        1 -> " — кровати соединены"
                        2 -> {
                            val bk = classicVariant2BedsLabel(r.linenBeds)
                            if (bk == 1) {
                                " — кровати разъединены, застелить 1 кровать"
                            } else {
                                " — кровати разъединены, застелить 2 кровати"
                            }
                        }
                        5 -> " — кровати соединены (109)"
                        6 -> {
                            val bk = classicVariant2BedsLabel(r.linenBeds)
                            if (bk == 1) {
                                " — кровати разъединены (109), застелить 1 кровать"
                            } else {
                                " — кровати разъединены (109), застелить 2 кровати"
                            }
                        }
                        else -> ""
                    }
                } else if (profile == "floor4") {
                    when (r.linenVariant) {
                        LINEN_VARIANT_FLOOR4_PER_BED -> {
                            val col = formatLinenColor(r.linenColor)
                            val n = r.linenBeds ?: 1
                            bedConfig = buildString {
                                if (col.isNotEmpty()) append(" — $col")
                                append(" — кроватей: $n")
                            }
                        }
                        LINEN_VARIANT_FLOOR4_JOINED -> bedConfig = " — кровати соединены"
                        LINEN_VARIANT_FLOOR4_SPLIT -> bedConfig = " — кровати разъединены"
                        else -> {
                            val col = formatLinenColor(r.linenColor)
                            if (col.isNotEmpty()) bedConfig = " — бельё: $col"
                        }
                    }
                }
            }

            val area0 = round(r.area).toInt()
            lines += "${numEmoji} ${r.name} — ${area0} м² — ${ct}$bedConfig"

            val roomLinen = formatRoomLinenDetailLines(r)
            for (ln in roomLinen) {
                lines += "    $ln"
            }

            // Totals calculation (под бельё)
            if (r.linenVariant != null) {
                val variant = r.linenVariant

                if (profile == "floor4") {
                    val pkg = floor4LinenQuantities(r) ?: continue
                    for ((itemName, qty) in pkg) addLinenItem(itemName, qty)
                    if (variant == LINEN_VARIANT_FLOOR4_PER_BED) {
                        val ck = r.linenColor
                        // В итог «по цвету» считаем условные единицы (составляющие комплекта × кроватей).
                        if (ck != null && ck in LINEN_COLORS) {
                            val label = LINEN_COLORS[ck]!!
                            val sumQty = pkg.values.sum()
                            linenColorTotals[label] = (linenColorTotals[label] ?: 0) + sumQty
                        }
                    }
                } else if (profile == "classic" && (variant in LINEN_PACKAGES || (isRoom108(r.name) && variant == 3))) {
                    val pkg = classicLinenQuantities(r)
                    if (pkg != null) {
                        for ((itemName, qty) in pkg) addLinenItem(itemName, qty)
                        linenColorTotals["белое"] = (linenColorTotals["белое"] ?: 0) + pkg.values.sum()
                    }
                }
            }
        }

        val totalArea0 = round(totalArea).toInt()
        val limit0 = round(limit).toInt()

        val remainderInt = remainder.toInt() // trunc toward zero (как Python int())
        val limitLine = if (remainder < 0) {
            "Превышение лимита: ${abs(remainderInt)} м²"
        } else {
            "Остаток лимита: $remainderInt м²"
        }

        lines += listOf(
            "",
            "📊 ИТОГО:",
            "• Помещений: ${queue.size}",
            "• Общая площадь: ${totalArea0} / ${limit0} м²",
            "• $limitLine",
            "",
            "🕐 Смена от: $dateStr",
        )

        if (linenTotals.isNotEmpty()) {
            lines += listOf("", "🧺 БЕЛЬЁ (ИТОГО ПО ЗАДАНИЮ):", "По цвету (всего единиц):")
            for (key in LINEN_COLOR_ORDER) {
                val label = LINEN_COLORS[key] ?: continue
                lines += "• $label: ${(linenColorTotals[label] ?: 0)} шт."
            }
            lines += listOf("", "По наименованию:")
            for ((itemName, qty) in linenTotals) {
                lines += "• $itemName: $qty шт."
            }
        }

        if (!comment.isNullOrBlank()) {
            lines += listOf("", "💬 Комментарий: $comment")
        }

        lines += listOf("━━━━━━━━━━━━━━━━━━━━━", "✅ Задание действительно до конца смены")
        return lines.joinToString("\n")
    }

    fun formatDateTime(millis: Long): String {
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        return dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    }

    fun formatTimeHHmm(millis: Long): String {
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        return dt.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    fun formatHistoryDetailText(
        taskId: Int,
        createdAtMillis: Long,
        employeeKey: String,
        rooms: List<QueueItem>,
        totalArea: Double,
        comment: String?,
        employeeDisplayNames: Map<String, String> = emptyMap(),
    ): String {
        val totalArea0 = round(totalArea).toInt()
        val lines = mutableListOf<String>()
        lines += "📋 Задание #$taskId"
        lines += "👤 ${formatEmployeeName(employeeKey, employeeDisplayNames)}"
        lines += "🕐 ${formatDateTime(createdAtMillis)}"
        lines += "📊 ${totalArea0} м², помещений: ${rooms.size}"
        lines += ""
        lines += "Порядок уборки:"

        for ((idx, r) in rooms.withIndex()) {
            val i = idx + 1
            val ct = formatCleaningType(r.cleaningType)
            val profile = resolveLinenProfile(r)
            var extra = ""

            if (profile == "floor4" && r.linenVariant != null) {
                val v = r.linenVariant
                extra = when (v) {
                    LINEN_VARIANT_FLOOR4_PER_BED -> {
                        val lc = formatLinenColor(r.linenColor)
                        ", ${r.linenBeds ?: 1} кров. бельё" + (if (lc.isNotEmpty()) " ($lc)" else "")
                    }
                    LINEN_VARIANT_FLOOR4_JOINED -> ", соед. кровати"
                    LINEN_VARIANT_FLOOR4_SPLIT -> ", разъед. кровати"
                    else -> {
                        val lc = formatLinenColor(r.linenColor)
                        if (lc.isNotEmpty()) ", комплект $v ($lc)" else ", комплект $v"
                    }
                }
            } else if (profile == "classic" && r.linenVariant == 2) {
                val bk = classicVariant2BedsLabel(r.linenBeds)
                extra = ", вар.2 — ${bk} кров."
            } else if (profile == "classic" && r.linenVariant == 6) {
                val bk = classicVariant2BedsLabel(r.linenBeds)
                extra = ", 109 разд. — ${bk} кров."
            } else if (profile == "classic" && r.linenVariant == 5) {
                extra = ", 109 соед."
            }

            val area0 = round(r.area).toInt()
            lines += "  ${i}. ${r.name} — ${area0} м² — ${ct}$extra"
            for (ln in formatRoomLinenDetailLines(r)) {
                lines += "     $ln"
            }
        }

        if (!comment.isNullOrBlank()) {
            lines += ""
            lines += "💬 $comment"
        }

        return lines.joinToString("\n")
    }
}

