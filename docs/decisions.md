# Architecture & Project Decisions

Record of major decisions, date, and rationale.

| Date | Decision | Rationale |
| :--- | :--- | :--- |
| 2026-10-18 | Android only (Kotlin + Jetpack Compose) for v1 | Scope control for v1 milestone. iOS deferred. |
| 2026-10-18 | No hardware RTC module in v1; sync via phone BLE | Reduces BOM complexity. Phone syncs epoch + tz offset on connect. |
| 2026-10-18 | Timer cancelled on power loss | Unsafe or inaccurate to resume timers without battery-backed RTC. |
| 2026-10-18 | Current cap set to 2500mA with 60/255 default brightness | Safety limit prevents power supply overload before thermal validation. |
| 2026-10-18 | Protocol & effect catalog defined in JSON (`protocol/*.json`) | Single source of truth for both ESP32 firmware and Android app. |
| 2026-10-18 | NimBLE-Arduino with static passkey & LE Secure Connections | Polished pairing UX and reliable reconnect with bonded devices. |
