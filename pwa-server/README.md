# PWA server (Flask) for iPhone

This folder contains a Flask server that serves a PWA (web app) replicating the Android app:
- Setup (PIN + tokens)
- Login (PIN)
- Main menu
- Create task (rooms tabs, linen variants, comment)
- Rooms management
- History
- Channel link
- Send to Telegram / MAX / VK

## Quick start (Windows / PowerShell)

1) Create venv and install deps:

```powershell
cd "c:\Users\user\Documents\Android studio\My_app2\Housekeeping_apk\pwa-server"
py -m venv .venv
.\.venv\Scripts\pip install -r requirements.txt
```

2) Run server:

```powershell
.\.venv\Scripts\python app.py
```

3) Open in browser:
- On the server PC: `http://127.0.0.1:5000`
- On iPhone (same Wi‑Fi): `http://<PC_LAN_IP>:5000`

## Access “from anywhere” (not only LAN)

You need a **public URL** to your PC:
- easiest: Cloudflare Tunnel or ngrok (recommended)
- or port-forwarding on your router + a domain (DDNS) + HTTPS (more work)

PWA installability on iOS works best over **HTTPS**.

