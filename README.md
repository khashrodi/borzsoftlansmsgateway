# BorzSoft LAN SMS Gateway

[![Build Debug APK](https://github.com/khashrodi/borzsoftlansmsgateway/actions/workflows/build.yml/badge.svg)](https://github.com/khashrodi/borzsoftlansmsgateway/actions/workflows/build.yml)

> Turn your Android phone into a professional local-network SMS gateway with a modern Web Dashboard and REST API. **Device-locked. Offline. BorzSoft branded.**

## Features

| Feature | Details |
|---------|---------|
| **Device Lock** | MAC-locked to `fc:d9:08:c3:20:60`. Unauthorized devices see error screen |
| **HTTP Server** | NanoHTTPD on port `8080`, LAN-only access |
| **REST API** | `/api/send`, `/api/status`, `/api/logs`, `/api/session` |
| **Web Dashboard** | Modern HTML panel with QR login, SMS form, real-time logs |
| **QR Login** | Scan from the app to auto-login in browser |
| **Dual SIM** | Route SMS via SIM1, SIM2, or Default |
| **Session Mgmt** | Token-based, 10-min expiry, IP binding, revocation |
| **Rate Limiting** | 10 SMS/minute per session |
| **Room Database** | SQLite: SMS logs, sessions, IP audit log |
| **Foreground Service** | Runs reliably in background, survives screen-off |
| **Auto Boot** | Optionally starts gateway on device boot |
| **Settings** | Port, timeout, rate limit, IP binding all configurable |

## Install APK

1. Go to the [Actions tab](https://github.com/khashrodi/borzsoftlansmsgateway/actions)
2. Open the latest successful **Build Debug APK** run
3. Download **BorzSoft-LAN-SMS-Gateway-DEBUG-APK**
4. Enable "Install from unknown sources" on your Android device
5. Install the `.apk` file

> **Debug APK is pre-signed** and installs directly — no extra steps.

## How to Use

### Mobile App
1. Open **BorzSoft SMS Gateway**
2. Grant SMS + Phone State permissions
3. Go to **Dashboard** tab → tap **Start Gateway**
4. Go to **Web Access** tab — see QR code + URL

### Web Dashboard
1. Connect your PC/tablet to the same WiFi
2. Open `http://[PHONE_IP]:8080` in browser
3. Scan QR code OR paste the token → Login
4. Send SMS from the browser, view real-time logs

### REST API
```bash
# Check status
curl http://192.168.1.100:8080/api/status

# Send SMS
curl -X POST http://192.168.1.100:8080/api/send \
  -H "Content-Type: application/json" \
  -d '{"phone":"+93700000000","message":"Hello","sim":"sim1"}'

# View logs
curl http://192.168.1.100:8080/api/logs

# Create session
curl -X POST http://192.168.1.100:8080/api/session

# Web send (authenticated)
curl -X POST http://192.168.1.100:8080/web/send \
  -H "Content-Type: application/json" \
  -d '{"token":"YOUR_TOKEN","phone":"+93700000000","message":"Hello","sim":"default"}'
```

## API Reference

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/status` | None | Server status, IP, SMS count |
| `POST` | `/api/send` | None | Send SMS directly |
| `GET` | `/api/logs?limit=N` | None | Recent SMS logs |
| `POST` | `/api/session` | None | Create web session token |
| `DELETE` | `/api/session` | None | Revoke session(s) |
| `GET` | `/api/sessions` | None | List active sessions |
| `POST` | `/web/send` | Token | Send SMS via web session |
| `GET` | `/api/healthz` | None | Health check |

## Security

- **Device Lock** — MAC address `fc:d9:08:c3:20:60` only
- **LAN Only** — Requests from public IPs are rejected (403)
- **Token Auth** — 10-minute session tokens for web access
- **IP Binding** — Sessions optionally bound to originating IP
- **Rate Limiting** — Max 10 SMS per minute per token
- **Session Revocation** — Revoke individual or all sessions from app

## Tech Stack

- **Language**: Kotlin 2.0
- **UI**: Material Components 3, ViewBinding, Fragments
- **HTTP Server**: NanoHTTPD
- **Database**: Room + SQLite
- **DI**: Hilt
- **QR**: ZXing Core
- **Arch**: MVVM + LiveData + ViewModels

## Screen Overview

| Screen | Description |
|--------|-------------|
| **Splash** | BorzSoft branded loading screen with device lock check |
| **Dashboard** | Gateway on/off, stats (SMS today, sent, failed, sessions) |
| **Send SMS** | Phone + message + SIM selector + char counter |
| **Web Access** | QR code, token, URL, active session list, revoke |
| **Logs** | Real-time SMS log with status badges |
| **Settings** | Port, timeout, rate limit, IP binding, auto-boot |

## License

BorzSoft proprietary. Device-locked to `fc:d9:08:c3:20:60`.
