from __future__ import annotations

import os
from datetime import datetime

from flask import Flask, jsonify, request, send_from_directory, session

from storage import (
    db_init,
    db_session,
    get_settings,
    set_settings,
    ensure_seed_rooms,
    list_rooms,
    add_room,
    toggle_room,
    set_room_area,
    save_task,
    list_tasks,
    get_task_detail,
)
from logic import build_channel_message
from senders import send_telegram, send_max, send_vk_wall_post


def create_app() -> Flask:
    app = Flask(__name__, static_folder="static", template_folder="templates")
    app.secret_key = os.environ.get("PWA_SECRET_KEY", "dev-secret-change-me")

    db_init()
    ensure_seed_rooms()

    def require_login():
        if not session.get("unlocked"):
            return jsonify({"error": "UNAUTHORIZED"}), 401
        return None

    @app.get("/")
    def index():
        return send_from_directory(app.static_folder, "index.html")

    @app.get("/manifest.webmanifest")
    def manifest():
        return send_from_directory(app.static_folder, "manifest.webmanifest")

    @app.get("/sw.js")
    def sw():
        resp = send_from_directory(app.static_folder, "sw.js")
        resp.headers["Content-Type"] = "text/javascript; charset=utf-8"
        return resp

    @app.get("/static/<path:path>")
    def static_files(path: str):
        return send_from_directory(app.static_folder, path)

    @app.get("/api/bootstrap")
    def bootstrap():
        s = get_settings()
        return jsonify(
            {
                "setupComplete": s.get("setupComplete", False),
                "channelLink": s.get("channelLink"),
            }
        )

    @app.post("/api/setup")
    def setup():
        body = request.get_json(force=True, silent=False) or {}
        pin = (body.get("pin") or "").strip()
        if len(pin) < 4:
            return jsonify({"error": "PIN_TOO_SHORT"}), 400

        telegram = body.get("telegram") or {}
        max_cfg = body.get("max") or {}
        vk = body.get("vk") or {}

        bot_token = (telegram.get("botToken") or "").strip()
        channel_id = (telegram.get("channelId") or "").strip()
        channel_link = (telegram.get("channelLink") or "").strip() or None

        max_token = (max_cfg.get("botToken") or "").strip()
        max_chat_id = (max_cfg.get("chatId") or "").strip()

        vk_access_token = (vk.get("accessToken") or "").strip()
        vk_group_id = (vk.get("groupId") or "").strip()

        if not bot_token or not channel_id:
            return jsonify({"error": "TELEGRAM_REQUIRED"}), 400
        if not max_token or not max_chat_id:
            return jsonify({"error": "MAX_REQUIRED"}), 400
        if not vk_access_token or not vk_group_id:
            return jsonify({"error": "VK_REQUIRED"}), 400

        set_settings(
            pin=pin,
            botToken=bot_token,
            channelId=channel_id,
            channelLink=channel_link,
            maxBotToken=max_token,
            maxChatId=max_chat_id,
            vkAccessToken=vk_access_token,
            vkGroupId=vk_group_id,
        )
        ensure_seed_rooms()
        session["unlocked"] = True
        return jsonify({"ok": True})

    @app.post("/api/login")
    def login():
        body = request.get_json(force=True, silent=False) or {}
        pin = (body.get("pin") or "").strip()
        s = get_settings()
        if not s.get("setupComplete"):
            return jsonify({"error": "SETUP_REQUIRED"}), 400
        if not s.get("pinHash"):
            return jsonify({"error": "SETUP_REQUIRED"}), 400
        from storage import verify_pin

        if not verify_pin(pin):
            return jsonify({"error": "INVALID_PIN"}), 401
        session["unlocked"] = True
        return jsonify({"ok": True})

    @app.post("/api/logout")
    def logout():
        session.pop("unlocked", None)
        return jsonify({"ok": True})

    @app.get("/api/rooms")
    def rooms_list():
        unauth = require_login()
        if unauth:
            return unauth
        return jsonify({"rooms": list_rooms()})

    @app.post("/api/rooms")
    def rooms_add():
        unauth = require_login()
        if unauth:
            return unauth
        body = request.get_json(force=True, silent=False) or {}
        name = (body.get("name") or "").strip()
        area = body.get("area")
        try:
            area_f = float(area)
        except Exception:
            return jsonify({"error": "AREA_INVALID"}), 400
        if not name:
            return jsonify({"error": "NAME_REQUIRED"}), 400
        if area_f <= 0:
            return jsonify({"error": "AREA_INVALID"}), 400
        r = add_room(name=name, area=area_f)
        return jsonify({"room": r})

    @app.post("/api/rooms/<int:room_id>/toggle")
    def rooms_toggle(room_id: int):
        unauth = require_login()
        if unauth:
            return unauth
        r = toggle_room(room_id)
        if not r:
            return jsonify({"error": "NOT_FOUND"}), 404
        return jsonify({"room": r})

    @app.post("/api/rooms/<int:room_id>/area")
    def rooms_area(room_id: int):
        unauth = require_login()
        if unauth:
            return unauth
        body = request.get_json(force=True, silent=False) or {}
        area = body.get("area")
        try:
            area_f = float(area)
        except Exception:
            return jsonify({"error": "AREA_INVALID"}), 400
        if area_f <= 0:
            return jsonify({"error": "AREA_INVALID"}), 400
        r = set_room_area(room_id, area_f)
        if not r:
            return jsonify({"error": "NOT_FOUND"}), 404
        return jsonify({"room": r})

    @app.get("/api/tasks")
    def tasks_list():
        unauth = require_login()
        if unauth:
            return unauth
        return jsonify({"tasks": list_tasks()})

    @app.get("/api/tasks/<int:task_id>")
    def tasks_detail(task_id: int):
        unauth = require_login()
        if unauth:
            return unauth
        t = get_task_detail(task_id)
        if not t:
            return jsonify({"error": "NOT_FOUND"}), 404
        return jsonify({"task": t})

    @app.post("/api/tasks/send")
    def tasks_send():
        unauth = require_login()
        if unauth:
            return unauth
        body = request.get_json(force=True, silent=False) or {}
        employee_key = (body.get("employeeKey") or "").strip() or "dina"
        comment = (body.get("comment") or "").strip() or None
        queue = body.get("queue") or []
        send_to = body.get("sendTo") or []

        if not isinstance(queue, list) or not queue:
            return jsonify({"error": "QUEUE_EMPTY"}), 400
        if not isinstance(send_to, list) or not send_to:
            return jsonify({"error": "SENDTO_EMPTY"}), 400

        total_area = 0.0
        for it in queue:
            try:
                total_area += float(it.get("area") or 0)
            except Exception:
                pass

        s = get_settings()
        msg = build_channel_message(
            employee_key=employee_key,
            queue=queue,
            total_area=total_area,
            comment=comment,
        )

        results = {"telegram": None, "max": None, "vk": None}
        try:
            if "telegram" in send_to:
                results["telegram"] = send_telegram(
                    bot_token=s["botToken"],
                    channel_id=s["channelId"],
                    text=msg,
                )
            if "max" in send_to:
                results["max"] = send_max(
                    bot_token=s["maxBotToken"],
                    chat_id=s["maxChatId"],
                    text=msg,
                )
            if "vk" in send_to:
                results["vk"] = send_vk_wall_post(
                    access_token=s["vkAccessToken"],
                    group_id=s["vkGroupId"],
                    message=msg,
                )
        except Exception as e:
            return jsonify({"error": "SEND_FAILED", "message": str(e)}), 500

        task_id = save_task(
            created_at=datetime.utcnow(),
            employee_key=employee_key,
            rooms_list=queue,
            total_area=total_area,
            comment=comment,
            telegram_message_id=results["telegram"],
            max_message_id=results["max"],
            vk_post_id=results["vk"],
        )

        return jsonify({"ok": True, "taskId": task_id, "results": results})

    return app


if __name__ == "__main__":
    app = create_app()
    host = os.environ.get("HOST", "0.0.0.0")
    port = int(os.environ.get("PORT", "5000"))
    app.run(host=host, port=port, debug=True)

