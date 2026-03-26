from __future__ import annotations

import requests


def send_telegram(*, bot_token: str, channel_id: str, text: str) -> int:
    url = f"https://api.telegram.org/bot{bot_token}/sendMessage"
    resp = requests.post(url, json={"chat_id": channel_id, "text": text}, timeout=30)
    if resp.status_code >= 400:
        raise RuntimeError(f"Telegram HTTP {resp.status_code}: {resp.text}")
    data = resp.json()
    if not data.get("ok") or not data.get("result"):
        raise RuntimeError(f"Telegram error: {resp.text}")
    return int(data["result"]["message_id"])


def send_max(*, bot_token: str, chat_id: str, text: str) -> str:
    url = "https://platform-api.max.ru/messages"
    resp = requests.post(
        url,
        params={"chat_id": chat_id},
        headers={"Authorization": f"Bearer {bot_token}"},
        json={"text": text},
        timeout=30,
    )
    if resp.status_code >= 400:
        raise RuntimeError(f"MAX HTTP {resp.status_code}: {resp.text}")
    # MAX может возвращать разные структуры; нам достаточно подтверждения.
    return "ok"


def send_vk_wall_post(*, access_token: str, group_id: str, message: str) -> int:
    url = "https://api.vk.com/method/wall.post"
    owner_id = f"-{group_id.strip()}"
    resp = requests.post(
        url,
        data={
            "access_token": access_token,
            "owner_id": owner_id,
            "from_group": 1,
            "message": message,
            "v": "5.199",
        },
        timeout=30,
    )
    if resp.status_code >= 400:
        raise RuntimeError(f"VK HTTP {resp.status_code}: {resp.text}")
    data = resp.json()
    if "error" in data:
        err = data["error"]
        raise RuntimeError(f"VK error {err.get('error_code')}: {err.get('error_msg')}")
    post_id = int((data.get("response") or {}).get("post_id") or 0)
    return post_id

