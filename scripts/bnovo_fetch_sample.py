#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Сырой JSON от Bnovo Open API: POST /api/v1/auth → GET /api/v1/bookings (все страницы, limit=50).

Bnovo отдаёт не больше 50 записей за запрос; сортировка может отдавать «не те» 50 первыми
(например, только заезды с 21.03, а 17.03 окажутся на следующей странице — без пагинации их не видно).

Запуск в PyCharm:
  1. pip install -r requirements.txt  (или: pip install requests python-dotenv)
  2. Скопируйте scripts/.env.example → scripts/.env и укажите BNOVO_ACCOUNT_ID и BNOVO_API_KEY.
     Допустим также корневой .env репозитория (Housekeeping_apk/.env) с теми же именами.
  3. Результат: bnovo_bookings_full.json + .pretty.json рядом со скриптом.

При отправке ответа кому-либо — удалите или замените персональные данные гостей.
"""

from __future__ import annotations

import json
import os
import sys
from datetime import date, timedelta
from pathlib import Path

try:
    import requests
except ImportError:
    print("Установите: pip install requests", file=sys.stderr)
    sys.exit(1)

SCRIPT_DIR = Path(__file__).resolve().parent

BASE_URL = "https://api.pms.bnovo.ru"
BOOKINGS_LIMIT = 50
MAX_BOOKINGS_PAGES = 500
# Синхронно с BnovoClient.BOOKINGS_DATE_PAST_DAYS; при необходимости увеличьте (см. KDoc в приложении).
BOOKINGS_DATE_PAST_DAYS = 120
BOOKINGS_DATE_FUTURE_DAYS = 60


def _extract_token(payload: dict) -> str | None:
    if not isinstance(payload, dict):
        return None
    for key in ("access_token", "token", "jwt"):
        v = payload.get(key)
        if v and isinstance(v, str):
            return v
    data = payload.get("data")
    if isinstance(data, dict):
        for key in ("access_token", "token"):
            v = data.get(key)
            if v and isinstance(v, str):
                return v
    return None


def fetch_token(account_id: str, api_key: str) -> str:
    account_id = account_id.strip()
    api_key = api_key.strip()
    if not account_id or not api_key:
        raise SystemExit("Нужны ID аккаунта и API-ключ.")

    headers = {"Accept": "application/json", "Content-Type": "application/json"}
    session = requests.Session()

    body_id_pass = {
        "id": int(account_id) if account_id.isdigit() else account_id,
        "password": api_key,
    }
    last_text = ""
    last_code = 0

    for body in (body_id_pass, {"username": account_id, "password": api_key}):
        r = session.post(f"{BASE_URL}/api/v1/auth", json=body, headers=headers, timeout=60)
        last_code = r.status_code
        last_text = r.text
        if r.status_code == 200:
            try:
                data = r.json()
            except json.JSONDecodeError:
                break
            token = _extract_token(data)
            if token:
                return token
            raise RuntimeError(f"В ответе auth нет access_token: {r.text[:500]}")
        if r.status_code != 404:
            raise RuntimeError(f"Bnovo auth {r.status_code}: {r.text[:800]}")

    raise RuntimeError(f"Bnovo auth {last_code}: {last_text[:800]}")


def fetch_bookings_page(
    token: str,
    date_from: date,
    date_to: date,
    limit: int = BOOKINGS_LIMIT,
    offset: int = 0,
) -> tuple[int, str]:
    params = {
        "date_from": date_from.isoformat(),
        "date_to": date_to.isoformat(),
        "limit": str(limit),
        "offset": str(offset),
    }
    r = requests.get(
        f"{BASE_URL}/api/v1/bookings",
        params=params,
        headers={"Accept": "application/json", "Authorization": f"Bearer {token.strip()}"},
        timeout=120,
    )
    return r.status_code, r.text


def _extract_bookings_list(payload: object) -> list:
    if isinstance(payload, list):
        return payload
    if not isinstance(payload, dict):
        return []
    data = payload.get("data")
    if isinstance(data, dict):
        inner = data.get("bookings") or data.get("booking")
        if isinstance(inner, list):
            return inner
        if isinstance(inner, dict) and isinstance(inner.get("data"), list):
            return inner["data"]
    for key in ("bookings", "items", "result"):
        v = payload.get(key)
        if isinstance(v, list):
            return v
    return []


def fetch_all_bookings_pages(
    token: str,
    date_from: date,
    date_to: date,
) -> tuple[int, list, int]:
    """Возвращает (код последнего HTTP, объединённый список броней, число страниц)."""
    merged: list = []
    offset = 0
    pages = 0
    last_code = 200
    while pages < MAX_BOOKINGS_PAGES:
        pages += 1
        code, raw = fetch_bookings_page(token, date_from, date_to, BOOKINGS_LIMIT, offset)
        last_code = code
        if code != 200:
            return code, merged, pages
        try:
            parsed = json.loads(raw)
        except json.JSONDecodeError:
            return code, merged, pages
        chunk = _extract_bookings_list(parsed)
        merged.extend(chunk)
        if len(chunk) < BOOKINGS_LIMIT:
            break
        offset += BOOKINGS_LIMIT
    return last_code, merged, pages


def _load_dotenv() -> None:
    try:
        from dotenv import load_dotenv
    except ImportError:
        print(
            "Подсказка: без python-dotenv файл .env не читается. pip install python-dotenv",
            file=sys.stderr,
        )
        return
    load_dotenv(SCRIPT_DIR / ".env")
    load_dotenv(SCRIPT_DIR.parent / ".env")


def main() -> None:
    _load_dotenv()

    account_id = (os.environ.get("BNOVO_ACCOUNT_ID") or "").strip()
    api_key = (os.environ.get("BNOVO_API_KEY") or "").strip()

    if not account_id:
        account_id = input("ID аккаунта Bnovo (из Octopus → API-доступ): ").strip()
    if not api_key:
        import getpass

        api_key = getpass.getpass("API-ключ Bnovo: ").strip()

    print("Запрос токена…", flush=True)
    token = fetch_token(account_id, api_key)
    print("Токен получен.", flush=True)

    today = date.today()
    date_from = today - timedelta(days=BOOKINGS_DATE_PAST_DAYS)
    date_to = today + timedelta(days=BOOKINGS_DATE_FUTURE_DAYS)

    print(
        f"GET /bookings: {date_from} … {date_to}, limit={BOOKINGS_LIMIT}, все страницы (до {MAX_BOOKINGS_PAGES})",
        flush=True,
    )
    code, bookings, n_pages = fetch_all_bookings_pages(token, date_from, date_to)
    print(f"Страниц: {n_pages}, броней в объединении: {len(bookings)}", flush=True)

    if code != 200:
        print(
            "Не удалось выгрузить (посмотрите первую ошибочную страницу отдельно).",
            file=sys.stderr,
        )
        sys.exit(1)

    base_dir = Path(__file__).resolve().parent
    merged_payload = {"data": {"bookings": bookings}}
    compact_path = base_dir / "bnovo_bookings_full.json"
    pretty_path = base_dir / "bnovo_bookings_full.pretty.json"
    compact_path.write_text(
        json.dumps(merged_payload, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )
    pretty_path.write_text(
        json.dumps(merged_payload, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"Сохранено: {compact_path}", flush=True)
    print(f"Сохранено: {pretty_path}", flush=True)


if __name__ == "__main__":
    main()
