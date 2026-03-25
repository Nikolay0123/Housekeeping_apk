# Tasks-for-housekeepers-bot (Android)

This Android app is a local-storage re-implementation of the Telegram “hotel cleaning tasks” bot:
- Create tasks: choose employee -> build queue of rooms -> cleaning type -> optional linen variant/color
- Send to Telegram channel via Bot API
- Local history of sent tasks
- Room management (add/edit/toggle active rooms)
- Access protected by PIN (stored locally)

## First run
Open in Android Studio and sync Gradle.
On first launch, the app will ask for:
- `PIN`
- `BOT_TOKEN`
- `CHANNEL_ID` (e.g. `-100123456789`)
- `CHANNEL_LINK` (optional)

## Notes
Data is stored locally in SQLite (Room) on the device.

