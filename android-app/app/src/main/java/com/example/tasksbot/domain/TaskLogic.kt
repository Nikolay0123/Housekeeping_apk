package com.example.tasksbot.domain

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

    // Виды уборки для каждого номера
    val CLEANING_TYPES: LinkedHashMap<String, String> = linkedMapOf(
        "current" to "текущая",
        "current_linen" to "текущая/смена белья",
        "departure" to "выезд",
        "departure_arrival" to "выезд/заезд",
        "general" to "генеральная",
    )

    fun formatCleaningType(key: String): String = CLEANING_TYPES[key] ?: key

    fun formatEmployeeName(employeeKey: String): String {
        val names = mapOf(
            "dina" to "ДИНА",
            "lena" to "ЛЕНА",
            "olya" to "ОЛЯ",
            "admin" to "АДМИНИСТРАТОР",
        )
        return names[employeeKey.lowercase()] ?: employeeKey.uppercase()
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
    )

    // Комплекты белья для номеров 401–405 (4 этаж): варианты 1/2/3 — множитель 2/3/4 на каждую позицию
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

    fun classicLinenQuantities(item: QueueItem): LinkedHashMap<String, Int>? {
        val v = item.linenVariant
        if (v == null || v !in LINEN_PACKAGES) return null
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

    fun formatChannelMessage(
        employeeKey: String,
        queue: List<QueueItem>,
        totalArea: Double,
        comment: String?,
    ): String {
        val empName = formatEmployeeName(employeeKey)
        val limit = AREA_LIMIT
        val remainder = limit - totalArea

        val now = LocalDateTime.now()
        val dateStr = now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

        val lines = mutableListOf<String>()
        lines += listOf(
            "🧹 НОВОЕ ЗАДАНИЕ",
            "━━━━━━━━━━━━━━━━━━━━━",
            "",
            "👤 Исполнитель: $empName",
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
                val variant = r.linenVariant!!
                if (profile == "classic" && variant in LINEN_PACKAGES) {
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
                        else -> ""
                    }
                } else if (profile == "floor4") {
                    val col = formatLinenColor(r.linenColor)
                    if (col.isNotEmpty()) bedConfig = " — бельё: $col"
                }
            }

            val area0 = round(r.area).toInt()
            lines += "${numEmoji} ${r.name} — ${area0} м² — ${ct}$bedConfig"

            // Totals calculation (под бельё)
            if (r.linenVariant != null) {
                val variant = r.linenVariant!!

                if (profile == "floor4" && variant in LINEN_PACKAGES_FLOOR4) {
                    val pkg = LINEN_PACKAGES_FLOOR4[variant] ?: emptyMap()
                    for ((itemName, qty) in pkg) addLinenItem(itemName, qty)
                    val ck = r.linenColor
                    if (ck != null && ck in LINEN_COLORS) {
                        val label = LINEN_COLORS[ck]!!
                        val sumQty = pkg.values.sum()
                        linenColorTotals[label] = (linenColorTotals[label] ?: 0) + sumQty
                    }
                } else if (profile == "classic" && variant in LINEN_PACKAGES) {
                    val pkg = classicLinenQuantities(r)
                    if (pkg != null) {
                        for ((itemName, qty) in pkg) addLinenItem(itemName, qty)
                        // Python добавляет весь итог "в белое".
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
    ): String {
        val totalArea0 = round(totalArea).toInt()
        val lines = mutableListOf<String>()
        lines += "📋 Задание #$taskId"
        lines += "👤 ${formatEmployeeName(employeeKey)}"
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
                val lc = formatLinenColor(r.linenColor)
                if (lc.isNotEmpty()) {
                    extra = ", комплект ${r.linenVariant} ($lc)"
                }
            } else if (profile == "classic" && r.linenVariant == 2) {
                val bk = classicVariant2BedsLabel(r.linenBeds)
                extra = ", вар.2 — ${bk} кров."
            }

            val area0 = round(r.area).toInt()
            lines += "  ${i}. ${r.name} — ${area0} м² — ${ct}$extra"
        }

        if (!comment.isNullOrBlank()) {
            lines += ""
            lines += "💬 $comment"
        }

        return lines.joinToString("\n")
    }
}

