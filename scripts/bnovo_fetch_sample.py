#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Сырой JSON от Bnovo Open API: POST /api/v1/auth → GET /api/v1/bookings (первая страница).

Запуск в PyCharm:
  1. pip install -r requirements.txt  (или: pip install requests python-dotenv)
  2. Скопируйте scripts/.env.example → scripts/.env и укажите BNOVO_ACCOUNT_ID и BNOVO_API_KEY.
     Допустим также корневой .env репозитория (Housekeeping_apk/.env) с теми же именами.
  3. Запустите скрипт. Результат: bnovo_bookings_page.json рядом со скриптом + краткий вывод в консоль.
     Если .env нет — можно задать переменные в Run Configuration или ввести в консоли.

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
    date_from = today - timedelta(days=14)
    date_to = today + timedelta(days=30)

    print(
        f"Запрос GET /bookings: {date_from} … {date_to}, limit={BOOKINGS_LIMIT}, offset=0",
        flush=True,
    )
    code, raw = fetch_bookings_page(token, date_from, date_to)

    out_path = Path(__file__).resolve().parent / "bnovo_bookings_page.json"
    out_path.write_text(raw, encoding="utf-8")
    print(f"HTTP {code}, тело сохранено: {out_path}", flush=True)

    try:
        data = json.loads(raw)
        pretty = json.dumps(data, ensure_ascii=False, indent=2)
        (out_path.with_suffix(".pretty.json")).write_text(pretty, encoding="utf-8")
        print(f"Отформатировано: {out_path.with_suffix('.pretty.json')}", flush=True)
        if isinstance(data, dict):
            print("Ключи корня:", sorted(data.keys()), flush=True)
    except json.JSONDecodeError:
        print("Ответ не JSON, см. сырой файл.", flush=True)

    if code != 200:
        print(raw[:1200], file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
