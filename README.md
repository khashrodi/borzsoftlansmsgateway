# BorzSoft LAN SMS Gateway

> Turn your Android phone into a professional local network SMS gateway with Web Dashboard and REST API.

[![Build APK](https://github.com/khashrodi/borzsoftlansmsgateway/actions/workflows/build.yml/badge.svg)](https://github.com/khashrodi/borzsoftlansmsgateway/actions/workflows/build.yml)

## Features

- **SMS Gateway**: Send SMS via REST API from any device on your LAN
- **Web Dashboard**: Modern browser-based panel for sending SMS and viewing logs
- **QR Login**: Scan QR code from the app to instantly authenticate in the browser
- **Token Login**: Manual token entry for web access
- **Dual SIM Support**: Route SMS through SIM1, SIM2, or default
- **Device Lock**: App only runs on the authorized device (MAC-based license)
- **Real-time Logs**: Live SMS log with status (Sent / Failed / Pending)
- **Rate Limiting**: 10 messages/minute per session
- **Session Management**: Create, revoke, and monitor web sessions
- **Foreground Service**: Runs reliably in background with persistent notification
- **BorzSoft Branding**: Full brand identity throughout the app

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/status` | Gateway status and stats |
| POST | `/api/send` | Send SMS (direct API) |
| GET | `/api/logs` | Recent SMS logs |
| POST | `/api/web/session` | Create web session token |
| DELETE | `/api/web/session` | Revoke session(s) |
| POST | `/web/send` | Send SMS via web token |

### Send SMS (API)
```bash
curl -X POST http://192.168.1.100:8080/api/send \
  -H "Content-Type: application/json" \
  -d '{"phone":"+98912xxxxxxx","message":"Hello","sim":"sim1"}'
```

### Send SMS (Web Panel)
```bash
curl -X POST http://192.168.1.100:8080/web/send \
  -H "Content-Type: application/json" \
  -d '{"token":"YOUR_TOKEN","phone":"+98912xxxxxxx","message":"Hello","sim":"default"}'
```

## Security

- **Device MAC Lock**: Only runs on `fc:d9:08:c3:20:60`
- **LAN-only**: All requests restricted to private IP ranges
- **Token-based auth**: Short-lived tokens (10 min) for web sessions
- **IP binding**: Sessions tied to the originating IP
- **Rate limiting**: 10 SMS/minute per token
- **Session revocation**: Revoke individual or all sessions from the app

## Building

This project uses GitHub Actions to build the APK automatically on every push.

### Manual Build
```bash
./gradlew assembleRelease
```
APK will be at: `app/build/outputs/apk/release/app-release-unsigned.apk`

### Download APK
Go to the [Actions tab](https://github.com/khashrodi/borzsoftlansmsgateway/actions) and download the latest artifact from a successful build run.

## Requirements

- Android 8.0+ (API 26+)
- Dual SIM support (optional, for SIM2 routing)
- Connected to a WiFi/LAN network

## Stack

- **Language**: Kotlin
- **HTTP Server**: NanoHTTPD
- **Database**: Room (SQLite)
- **DI**: Hilt
- **QR Generation**: ZXing
- **Serialization**: Gson

## License

BorzSoft proprietary license. Device-locked.
