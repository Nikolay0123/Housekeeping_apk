from __future__ import annotations

import re
from collections import defaultdict
from datetime import datetime


AREA_LIMIT = 375.0

CLEANING_TYPES = {
    "current": "текущая",
    "current_linen": "текущая/смена белья",
    "departure": "выезд",
    "departure_arrival": "выезд/заезд",
    "general": "генеральная",
}

EMPLOYEE_NAMES = {
    "dina": "ДИНА",
    "lena": "ЛЕНА",
    "olya": "ОЛЯ",
    "admin": "АДМИНИСТРАТОР",
}

LINEN_COLORS = {
    "blue": "голубое",
    "gray": "серое",
    "stripe": "в полоску",
    "white": "белое",
}
LINEN_COLOR_ORDER = ("blue", "gray", "stripe", "white")

LINEN_PACKAGES = {
    1: {
        "Простыня двуспальная": 1,
        "Пододеяльник двуспальный": 1,
        "Наволочка": 2,
        "Полотенце банное с вышивкой": 2,
        "Полотенце для лица": 2,
        "Полотенце для ног": 1,
    },
    2: {
        "Простыня 1,5 спальная": 2,
        "Пододеяльник 1,5 спальный": 2,
        "Наволочка": 2,
        "Полотенце банное с вышивкой": 2,
        "Полотенце для лица": 2,
        "Полотенце для ног": 1,
    },
    3: {
        "Простыня люкс": 1,
        "Пододеяльник люкс": 1,
        "Наволочка с люкс (с вышивкой)": 4,
        "Полотенце банное с вышивкой": 2,
        "Полотенце для лица": 2,
        "Полотенце для ног": 1,
    },
    4: {
        "Простыня люкс": 1,
        "Пододеяльник двуспальный": 1,
        "Наволочка": 2,
        "Полотенце банное с вышивкой": 2,
        "Полотенце для лица": 2,
        "Полотенце для ног": 1,
    },
}

LINEN_PACKAGES_FLOOR4 = {
    1: {
        "Простыня 1,5 спальная": 2,
        "Пододеяльник 1,5 спальный": 2,
        "Наволочка": 2,
        "Полотенце банное": 2,
        "Полотенце 40х70": 2,
    },
    2: {
        "Простыня 1,5 спальная": 3,
        "Пододеяльник 1,5 спальный": 3,
        "Наволочка": 3,
        "Полотенце банное": 3,
        "Полотенце 40х70": 3,
    },
    3: {
        "Простыня 1,5 спальная": 4,
        "Пододеяльник 1,5 спальный": 4,
        "Наволочка": 4,
        "Полотенце банное": 4,
        "Полотенце 40х70": 4,
    },
}


def format_employee_name(key: str) -> str:
    return EMPLOYEE_NAMES.get((key or "").lower(), (key or "").upper() or "—")


def format_cleaning_type(key: str) -> str:
    return CLEANING_TYPES.get(key, key)


def room_linen_profile(room_name: str) -> str | None:
    if not isinstance(room_name, str) or not room_name.startswith("Номер "):
        return None
    rest = room_name.replace("Номер ", "").strip()
    if rest.isdigit():
        n = int(rest)
        if 101 <= n <= 109:
            return "classic"
        if n == 403:
            return "floor4"
        return None
    m = re.match(r"^(\d+)(?:\.(\d+))?$", rest)
    if not m:
        return None
    major = int(m.group(1))
    minor_s = m.group(2)
    if major == 403 and minor_s is None:
        return "floor4"
    if major in (401, 402, 404, 405) and minor_s is not None:
        minor = int(minor_s)
        if 1 <= minor <= 4:
            return "floor4"
    return None


def format_linen_color(key: str | None) -> str:
    if not key:
        return ""
    return LINEN_COLORS.get(key, key)


def classic_linen_quantities(item: dict) -> dict[str, int] | None:
    v = item.get("linenVariant")
    if not isinstance(v, int) or v not in LINEN_PACKAGES:
        return None
    base = LINEN_PACKAGES[v]
    if v == 2:
        beds = item.get("linenBeds", 2)
        if beds not in (1, 2):
            beds = 2
        if beds == 1:
            out: dict[str, int] = {}
            for k, q in base.items():
                out[k] = max(0, q // 2)
            # towel for feet – keep at least 1 if present
            if "Полотенце для ног" in base and base["Полотенце для ног"] > 0:
                out["Полотенце для ног"] = max(1, out.get("Полотенце для ног", 0))
            return out
    return dict(base)


def format_room_linen_detail_lines(item: dict) -> list[str]:
    profile = room_linen_profile(item.get("name") or "")
    v = item.get("linenVariant")
    if not isinstance(v, int):
        return []
    lines: list[str] = []
    if profile == "classic" and v in LINEN_PACKAGES:
        pkg = classic_linen_quantities(item) or {}
        lines.append(f"🧺 Бельё (вариант {v}):")
        for k, q in pkg.items():
            lines.append(f"• {k} — {q} шт.")
    elif profile == "floor4" and v in LINEN_PACKAGES_FLOOR4:
        col = format_linen_color(item.get("linenColor"))
        if col:
            lines.append(f"Цвет комплекта: {col}")
        pkg = LINEN_PACKAGES_FLOOR4[v]
        lines.append(f"🧺 Состав белья (комплект {v}):")
        for k, q in pkg.items():
            lines.append(f"• {k} — {q} шт.")
    return lines


def build_channel_message(
    *,
    employee_key: str,
    queue: list[dict],
    total_area: float,
    comment: str | None,
) -> str:
    emp_name = format_employee_name(employee_key)
    remainder = AREA_LIMIT - total_area
    now = datetime.now().strftime("%d.%m.%Y %H:%M")

    lines: list[str] = []
    lines += [
        "🧹 НОВОЕ ЗАДАНИЕ",
        "━━━━━━━━━━━━━━━━━━━━━",
        "",
        f"👤 Исполнитель: {emp_name}",
        "",
        "ПОРЯДОК УБОРКИ:",
    ]

    linen_totals: dict[str, int] = defaultdict(int)
    linen_color_totals: dict[str, int] = {LINEN_COLORS[k]: 0 for k in LINEN_COLOR_ORDER}

    emojis = ["1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣", "🔟"]

    for idx, r in enumerate(queue, 1):
        num = emojis[idx - 1] if idx <= 10 else f"{idx}."
        ct = format_cleaning_type(r.get("cleaningType") or "current")
        area0 = int(round(float(r.get("area") or 0)))
        profile = room_linen_profile(r.get("name") or "")

        bed_config = ""
        v = r.get("linenVariant")
        if isinstance(v, int):
            if profile == "classic" and v in LINEN_PACKAGES:
                if v == 1:
                    bed_config = " — кровати соединены"
                elif v == 2:
                    beds = r.get("linenBeds", 2)
                    bed_config = (
                        " — кровати разъединены, застелить 1 кровать"
                        if beds == 1
                        else " — кровати разъединены, застелить 2 кровати"
                    )
            elif profile == "floor4":
                col = format_linen_color(r.get("linenColor"))
                if col:
                    bed_config = f" — бельё: {col}"

        lines.append(f"{num} {r.get('name')} — {area0} м² — {ct}{bed_config}")
        for ln in format_room_linen_detail_lines(r):
            lines.append(f"    {ln}")

        if isinstance(v, int):
            if profile == "floor4" and v in LINEN_PACKAGES_FLOOR4:
                pkg = LINEN_PACKAGES_FLOOR4[v]
                for item_name, qty in pkg.items():
                    linen_totals[item_name] += qty
                ck = r.get("linenColor")
                if ck in LINEN_COLORS:
                    linen_color_totals[LINEN_COLORS[ck]] += sum(pkg.values())
            elif profile == "classic" and v in LINEN_PACKAGES:
                pkg = classic_linen_quantities(r) or {}
                for item_name, qty in pkg.items():
                    linen_totals[item_name] += qty
                linen_color_totals["белое"] += sum(pkg.values())

    remainder_int = int(remainder)
    limit_line = (
        f"Превышение лимита: {abs(remainder_int)} м²"
        if remainder < 0
        else f"Остаток лимита: {remainder_int} м²"
    )

    lines += [
        "",
        "📊 ИТОГО:",
        f"• Помещений: {len(queue)}",
        f"• Общая площадь: {int(round(total_area))} / {int(round(AREA_LIMIT))} м²",
        f"• {limit_line}",
        "",
        f"🕐 Смена от: {now}",
    ]

    if linen_totals:
        lines += ["", "🧺 БЕЛЬЁ (ИТОГО ПО ЗАДАНИЮ):", "По цвету (всего единиц):"]
        for key in LINEN_COLOR_ORDER:
            label = LINEN_COLORS[key]
            lines.append(f"• {label}: {linen_color_totals.get(label, 0)} шт.")
        lines += ["", "По наименованию:"]
        for item_name, qty in linen_totals.items():
            lines.append(f"• {item_name}: {qty} шт.")

    if comment:
        lines += ["", f"💬 Комментарий: {comment}"]

    lines += ["━━━━━━━━━━━━━━━━━━━━━", "✅ Задание действительно до конца смены"]
    return "\n".join(lines)

