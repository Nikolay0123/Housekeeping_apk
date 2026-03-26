from __future__ import annotations

import hashlib
import json
import os
from datetime import datetime
from typing import Any

from sqlalchemy import Boolean, DateTime, Float, Integer, String, Text, create_engine, select
from sqlalchemy.orm import DeclarativeBase, Mapped, Session, mapped_column


BASE_DIR = os.path.dirname(__file__)
DB_PATH = os.path.join(BASE_DIR, "pwa.db")
ENGINE = create_engine(f"sqlite:///{DB_PATH}", echo=False, future=True)


class Base(DeclarativeBase):
    pass


class Settings(Base):
    __tablename__ = "settings"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    pin_hash: Mapped[str | None] = mapped_column(String(128), nullable=True)
    bot_token: Mapped[str | None] = mapped_column(Text, nullable=True)
    channel_id: Mapped[str | None] = mapped_column(Text, nullable=True)
    channel_link: Mapped[str | None] = mapped_column(Text, nullable=True)
    max_bot_token: Mapped[str | None] = mapped_column(Text, nullable=True)
    max_chat_id: Mapped[str | None] = mapped_column(Text, nullable=True)
    vk_access_token: Mapped[str | None] = mapped_column(Text, nullable=True)
    vk_group_id: Mapped[str | None] = mapped_column(Text, nullable=True)


class Room(Base):
    __tablename__ = "rooms"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(128), nullable=False, unique=True)
    area: Mapped[float] = mapped_column(Float, nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)


class Task(Base):
    __tablename__ = "tasks"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    employee_key: Mapped[str] = mapped_column(String(32), nullable=False)
    rooms_list_json: Mapped[str] = mapped_column(Text, nullable=False)
    total_area: Mapped[float] = mapped_column(Float, nullable=False)
    comment: Mapped[str | None] = mapped_column(Text, nullable=True)
    telegram_message_id: Mapped[int | None] = mapped_column(Integer, nullable=True)
    max_message_id: Mapped[str | None] = mapped_column(Text, nullable=True)
    vk_post_id: Mapped[int | None] = mapped_column(Integer, nullable=True)


def db_init():
    Base.metadata.create_all(ENGINE)
    with Session(ENGINE) as s:
        st = s.get(Settings, 1)
        if not st:
            s.add(Settings(id=1))
            s.commit()


def db_session() -> Session:
    return Session(ENGINE)


def _sha256_hex(raw: str) -> str:
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()


def get_settings() -> dict[str, Any]:
    with Session(ENGINE) as s:
        st = s.get(Settings, 1)
        setup_complete = bool(st and st.pin_hash and st.bot_token and st.channel_id)
        return {
            "setupComplete": setup_complete,
            "pinHash": st.pin_hash if st else None,
            "botToken": st.bot_token if st else None,
            "channelId": st.channel_id if st else None,
            "channelLink": st.channel_link if st else None,
            "maxBotToken": st.max_bot_token if st else None,
            "maxChatId": st.max_chat_id if st else None,
            "vkAccessToken": st.vk_access_token if st else None,
            "vkGroupId": st.vk_group_id if st else None,
        }


def set_settings(
    *,
    pin: str,
    botToken: str,
    channelId: str,
    channelLink: str | None,
    maxBotToken: str,
    maxChatId: str,
    vkAccessToken: str,
    vkGroupId: str,
):
    with Session(ENGINE) as s:
        st = s.get(Settings, 1)
        if not st:
            st = Settings(id=1)
            s.add(st)
        st.pin_hash = _sha256_hex(pin.strip())
        st.bot_token = botToken
        st.channel_id = channelId
        st.channel_link = channelLink
        st.max_bot_token = maxBotToken
        st.max_chat_id = maxChatId
        st.vk_access_token = vkAccessToken
        st.vk_group_id = vkGroupId
        s.commit()


def verify_pin(pin: str) -> bool:
    with Session(ENGINE) as s:
        st = s.get(Settings, 1)
        if not st or not st.pin_hash:
            return False
        return _sha256_hex(pin.strip()) == st.pin_hash


SEED_ROOMS: list[tuple[str, float]] = [
    ("Кабинет директора", 12.54),
    ("Кабинет администрации", 12.54),
    ("1 этаж", 109.84),
    ("Комната администраторов", 23.97),
    ("Лестница до 5 этажа", 76.62),
    ("2 этаж", 96.3),
    ("3 этаж", 96.0),
    ("Номер 101", 17.03),
    ("Номер 102", 22.04),
    ("Номер 103", 25.54),
    ("Номер 104", 23.18),
    ("Номер 105", 26.1),
    ("Номер 106", 22.04),
    ("Номер 107", 17.91),
    ("Номер 108", 44.44),
    ("Номер 109", 22.44),
    ("Номер 404.1", 13.0),
    ("Номер 404.2", 11.7),
    ("Номер 404.3", 10.9),
    ("Номер 404.4", 12.0),
    ("Номер 405.1", 11.7),
    ("Номер 405.2", 12.7),
    ("Номер 405.3", 12.2),
    ("Номер 405.4", 12.3),
    ("Номер 403", 22.8),
    ("Номер 401.1", 12.9),
    ("Номер 401.2", 11.8),
    ("Номер 401.3", 11.7),
    ("Номер 401.4", 11.8),
    ("Номер 402.1", 12.0),
    ("Номер 402.2", 11.9),
    ("Номер 402.3", 12.0),
    ("Номер 402.4", 13.1),
    ("Холл 4 этаж", 31.8),
    ("Блок 401, 402", 109.6),
    ("Блок 404,405", 109.2),
    ("Кухня", 51.01),
]


def ensure_seed_rooms():
    with Session(ENGINE) as s:
        any_room = s.execute(select(Room.id).limit(1)).first()
        if any_room:
            return
        for name, area in SEED_ROOMS:
            s.add(Room(name=name, area=area, is_active=True))
        s.commit()


def list_rooms() -> list[dict[str, Any]]:
    with Session(ENGINE) as s:
        rooms = s.execute(select(Room).order_by(Room.name)).scalars().all()
        return [
            {"id": r.id, "name": r.name, "area": r.area, "isActive": r.is_active}
            for r in rooms
        ]


def add_room(*, name: str, area: float) -> dict[str, Any]:
    with Session(ENGINE) as s:
        r = Room(name=name, area=area, is_active=True)
        s.add(r)
        s.commit()
        s.refresh(r)
        return {"id": r.id, "name": r.name, "area": r.area, "isActive": r.is_active}


def toggle_room(room_id: int) -> dict[str, Any] | None:
    with Session(ENGINE) as s:
        r = s.get(Room, room_id)
        if not r:
            return None
        r.is_active = not r.is_active
        s.commit()
        return {"id": r.id, "name": r.name, "area": r.area, "isActive": r.is_active}


def set_room_area(room_id: int, area: float) -> dict[str, Any] | None:
    with Session(ENGINE) as s:
        r = s.get(Room, room_id)
        if not r:
            return None
        r.area = area
        s.commit()
        return {"id": r.id, "name": r.name, "area": r.area, "isActive": r.is_active}


def save_task(
    *,
    created_at: datetime,
    employee_key: str,
    rooms_list: list[dict[str, Any]],
    total_area: float,
    comment: str | None,
    telegram_message_id: int | None,
    max_message_id: str | None,
    vk_post_id: int | None,
) -> int:
    with Session(ENGINE) as s:
        t = Task(
            created_at=created_at,
            employee_key=employee_key,
            rooms_list_json=json.dumps(rooms_list, ensure_ascii=False),
            total_area=total_area,
            comment=comment,
            telegram_message_id=telegram_message_id,
            max_message_id=max_message_id,
            vk_post_id=vk_post_id,
        )
        s.add(t)
        s.commit()
        s.refresh(t)
        return t.id


def list_tasks() -> list[dict[str, Any]]:
    with Session(ENGINE) as s:
        tasks = s.execute(select(Task).order_by(Task.created_at.desc()).limit(50)).scalars().all()
        out: list[dict[str, Any]] = []
        for t in tasks:
            out.append(
                {
                    "id": t.id,
                    "createdAt": t.created_at.isoformat(),
                    "employeeKey": t.employee_key,
                    "totalArea": t.total_area,
                    "comment": t.comment,
                }
            )
        return out


def get_task_detail(task_id: int) -> dict[str, Any] | None:
    with Session(ENGINE) as s:
        t = s.get(Task, task_id)
        if not t:
            return None
        return {
            "id": t.id,
            "createdAt": t.created_at.isoformat(),
            "employeeKey": t.employee_key,
            "rooms": json.loads(t.rooms_list_json),
            "totalArea": t.total_area,
            "comment": t.comment,
            "telegramMessageId": t.telegram_message_id,
            "maxMessageId": t.max_message_id,
            "vkPostId": t.vk_post_id,
        }

