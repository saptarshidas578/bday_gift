# SPEC: PixelGift (working name)

Fixed requirements. Change rarely, and only by a small patch plus a note in PROGRESS.md. Sections are numbered so tasks can say "read SPEC section 7".

## 1. Objective

A personalized birthday gift that is also a lasting decorative light and clock. An ESP32 drives a 9x9 WS2812B matrix and a piezo. An Android app controls it over Bluetooth Low Energy with a polished, industry-standard UI.

Success looks like:
- The app pairs securely with the device and reconnects automatically.
- Any RGB color can be chosen from a paint-style wheel and applied with OK.
- 500+ distinct effect looks (about 60 base effects x variations), all rendered on the ESP32.
- Birthday mode: one tap starts lights, scrolling message and a piezo melody that finish even if Bluetooth drops.
- Clock runs on the ESP32 after the phone syncs time. Timer works offline.
- Settings survive power loss. The device never needs the phone to keep running.

## 2. Assumptions and open questions

### Assumptions (correct me or I proceed):
- Android only (Kotlin + Jetpack Compose). iPhone is out of scope for v1.
- The AI agent builds app, firmware and helps debug; the human wires and flashes.
- ESP32 DevKit (classic ESP32, 3.3 V logic). Exact board to be confirmed in task B01.
- Data pin is GPIO 5 and buzzer pin is GPIO 18 as defaults in config.h. Confirm in B01.
- Matrix wiring may be serpentine or straight rows. Measured in B01, set in config.h.
- No RTC module in v1. Time is synced from the phone (see section 6.5).
- A timer interrupted by power loss is cancelled on boot (see section 6.5).
- One phone connected at a time. Several phones may be bonded.
- Text is printable ASCII plus a few pixel glyphs (heart, star, smile).

### Open (need the human, none block early work):
- Recipient name, birthday message text, preferred theme colors (file: docs/birthday-content.md).
- Final product name.
- Whether to add an RTC (DS3231) later for accurate time after power loss.

## 3. Hardware

| Part | Spec |
| :--- | :--- |
| MCU | ESP32 DevKit |
| Matrix | 81x WS2812B, 9 rows x 9 cols, GRB order (verify) |
| Audio | Passive piezo on one GPIO, driven with LEDC tone |
| Power | Regulated 5 V supply (5 A target), power injection at both ends |
| Protection | 330-470 ohm in series on data, ~1000 uF across LED supply, common ground |
| Logic level | ESP32 3.3 V data may work; a 74HCT125 level shifter is the safe option |
| Enclosure | Handmade cardboard with diffuser |
| Recovery | Use the DevKit BOOT button (GPIO 0): hold 5 s to clear pairings (optional) |

Power rule: worst case 81 x 60 mA = 4.86 A. Software caps current with `FastLED.setMaxPowerInVoltsAndMilliamps(5, LED_MAX_MA)`. Default `LED_MAX_MA = 2500`, default brightness 60/255. Raise only after the human validates supply, wiring and heat.

## 4. Repo structure

```
pixelgift/
  AGENTS.md  SPEC.md  PROGRESS.md
  prompts/PROMPTS.md
  protocol/        protocol.json  effects.json  PROTOCOL.md
  firmware/        platformio.ini  include/config.h  include/secrets.h(ignored)
                   src/  test/
  app/             Android Studio project (Kotlin, Compose)
  hardware/        wiring.md  pinmap.md  bom.md  photos/
  docs/            birthday-content.md  decisions.md
```

`protocol/*.json` is the single source of truth for opcodes, error codes, enums and the effect catalog. Firmware and app mirror it (generated or tested against it).

## 5. Commands

See AGENTS.md. Native logic tests (`pio test -e native`) must cover: protocol parsing, effect math, color helpers, text layout, timer logic, scene serialization.

## 6. Firmware

### 6.1 Modules (firmware/src/)
main.cpp (setup/loop only), config.h, state (single DeviceState struct), matrix (pixel mapping, brightness cap), effects/ (registry + effect files), text (5x7 font, scroller, 3x4 clock font), clock, timer, scheduler, birthday (show timeline), audio (non-blocking melody player), storage (NVS), ble (NimBLE server, pairing, command dispatch), ota.

### 6.2 Loop rules
Non-blocking. Target 60 FPS render, effect time via `millis()`. BLE callbacks only enqueue a parsed command; the main loop applies it. No heap allocation in render paths.

### 6.3 Modes
0 SOLID, 1 EFFECT, 2 CLOCK, 3 TIMER, 4 BIRTHDAY, 5 MESSAGE, 6 DRAW, 7 MUSIC, 8 OFF.
Priority: TIMER alert and BIRTHDAY can interrupt; when they end, return to the previous mode (birthday ends in a soft ambient effect).

### 6.4 Persistence (NVS via Preferences, namespace pgift)
Stored: mode (SOLID/EFFECT/CLOCK/OFF only), color, brightness, effect descriptor, palette, clock style, tz offset, custom message, birthday content, 8 scenes, schedules, sleep default.
Rules: save is debounced (2 s after last change), never per frame, never per clock tick. Birthday show and timer are never auto-resumed at boot.

### 6.5 Time
- App sends epoch (UTC) + tz offset on every connect and after a manual "sync".
- ESP32 keeps time with the system clock (`settimeofday`) while powered.
- After power loss the clock shows a "sync needed" glyph until the next connection.
- Timer policy: cancelled on power loss. Schedules need valid time; they pause until synced.

### 6.6 Clock layout on 9x9
Default: stacked 3x4 digits (two digits plus a 1-column gap = 7 columns, centered in cols 1-7). Hours on rows 0-3, minutes on rows 5-8, row 4 blinks a seconds dot.
Alternate: scrolling 5x7 "HH:MM". Verify legibility on the real matrix; tune in B08.

### 6.7 Birthday show (60-120 s)
Timeline: intro sparkle (5 s), name reveal scrolling, hearts and balloons, fireworks, rainbow, scrolling greeting, finale glow that settles into a soft ambient effect. Runs on lights-only timing until buzzer hardware arrives (B07b).
Start and stop only by command (`PLAY_BIRTHDAY` / `STOP_SHOW` or serial); never auto-resumed at boot. Runs fully on the ESP32.

### 6.8 Audio (Deferred to B07b)
Piezo buzzer deferred until hardware arrives. Audio module provides no-op stubs (`playMelody`, `playBeeps`, `setMute`). Birthday show runs lights-only; timer completion uses visual flashing alert instead of beeps. App shows audio/mute controls disabled with "Speaker not connected". Full melody and beeps implemented in task B07b.

## 7. BLE protocol (v1)

### 7.1 Pairing and security
- Library: NimBLE-Arduino. LE Secure Connections with bonding.
- IO capability: display-only with a static 6-digit passkey from `secrets.h` (the human writes it on a card). Phone prompts for it on first pairing.
- All characteristics require encryption and authentication.
- Bonds persist in NVS. Command `CLEAR_BONDS` and the BOOT-button hold clear them.
- Device name: `PixelGift-XXXX` (last two MAC bytes). Advertise whenever disconnected.
- Reconnect: app stores the bonded address and auto-reconnects. On reconnect it sends `GET_STATUS` and renders the device's actual state. It never overwrites device state with cached values.

### 7.2 GATT
Service `7a1c0001-5d3e-4b8a-9c21-8f6d2e4a1b00`
- CMD `7a1c0002-5d3e-4b8a-9c21-8f6d2e4a1b00`: write (with response) and write without response for high-rate music levels.
- STATUS `7a1c0003-5d3e-4b8a-9c21-8f6d2e4a1b00`: read and notify. Request MTU 185. Keep every packet at or below 20 bytes payload unless MTU is confirmed.

### 7.3 Packet format
Little-endian. CMD: `[opcode u8][seq u8][payload...]`. STATUS notifications:
- ACK: `[0xA0][seq][code]`
- Snapshot: `[0xA1][protoVer][mode][brightness][r][g][b][effectBase][speed][flags][fwMajor][fwMinor]`
flags bits: 0 muted, 1 timeSynced, 2 timerRunning, 3 showRunning, 4 powerCapped.
Error codes: 0 OK, 1 UNKNOWN_OP, 2 BAD_LEN, 3 BAD_VALUE, 4 BUSY, 5 UNSUPPORTED, 6 STORAGE_ERR, 7 OTA_VER_ERR, 8 OTA_SPACE_ERR, 9 OTA_WRITE_ERR. Unknown opcodes and bad lengths never crash; they return an error ACK.

### 7.4 Opcodes

| Op | Name | Payload |
| :--- | :--- | :--- |
| 0x01 | SET_COLOR | r g b |
| 0x02 | SET_BRIGHTNESS | v u8 (clamped to cap) |
| 0x03 | SET_EFFECT | base u8, speed u8, dir u8, mirror u8, colorMode u8, c1 rgb, c2 rgb |
| 0x04 | SET_PALETTE | count u8 (1-8), then count x rgb |
| 0x05 | SET_MODE | mode u8 |
| 0x06 | SYNC_TIME | epoch u32, tzMinutes i16 |
| 0x07 | SET_CLOCK_STYLE | style u8 |
| 0x08 | START_TIMER | seconds u32 |
| 0x09 | CANCEL_TIMER | none |
| 0x0A | PLAY_BIRTHDAY | none |
| 0x0B | STOP_SHOW | none |
| 0x0C | SET_TEXT | target u8 (0 message, 1 birthday name, 2 birthday greeting), idx u8, total u8, up to 14 ASCII bytes |
| 0x0D | SHOW_MESSAGE | speed u8 |
| 0x0E | SAVE_SCENE | slot u8 (0-7) |
| 0x0F | LOAD_SCENE | slot u8 |
| 0x10 | SET_SCHEDULE | slot u8, enabled u8, dayMask u8, hh u8, mm u8, action u8, arg u8 |
| 0x11 | SET_SLEEP_TIMER | minutes u16 (0 cancels) |
| 0x12 | DRAW_CHUNK | offset u16, up to 14 bytes of the 243-byte RGB frame |
| 0x13 | DRAW_COMMIT | none (shows the staged frame, mode DRAW) |
| 0x14 | MUSIC_LEVELS | 9 bytes, one per column, at most 20 Hz |
| 0x15 | SET_MUTE | u8 |
| 0x16 | GET_STATUS | none |
| 0x17 | CLEAR_BONDS | none |
| 0x18 | FACTORY_RESET | magic 0xA5 0x5A |
| 0x19 | OTA_BEGIN | totalBytes u32, fwMajor u8, fwMinor u8, fwPatch u8 |
| 0x1A | OTA_CHUNK | chunkSeq u16, up to 180 bytes binary chunk |
| 0x1B | OTA_COMMIT | crc32 u32 |
| 0x1C | OTA_ABORT | none |

Schedule actions: 0 on, 1 off, 2 load scene (arg), 3 sunrise alarm (arg minutes).
Enums for dir: 0 L, 1 R, 2 U, 3 D, 4 IN, 5 OUT.
colorMode: 0 single, 1 dual, 2 palette, 3 rainbow.
Source of truth is `protocol/protocol.json`; this table must match it.

## 8. Effects engine

Registry of base effects, each `render(t, params, leds)` that is pure and cheap. A look = base + speed + dir + mirror + colorMode + colors. About 60 bases, curated variants give 500+ looks. Catalog in `protocol/effects.json`: id, name, category, supports{dir,mirror, colors,palette}, defaults. The app builds its Effects browser from this file.

Bases (target ~60; first batch of 10 in B03, rest in B04):
- **Static/glow**: solid, breathing, pulse, heartbeat, candle, strobe-soft, fade-two, flicker
- **Rainbow/gradient**: rainbow wave, rainbow cycle, diagonal gradient, radial gradient, aurora, sunset, palette scroll, color wheel spin
- **Motion**: scanner, chase, wipe, bounce, spiral, radar, rain, waterfall, snake path, comet, theater chase, row sweep
- **Particles**: sparkle, twinkle, fireflies, fireworks, snow, confetti, meteor, bubbles, glitter, stardust
- **Nature/fire**: fire, lava, ocean, lightning, northern lights, embers, storm, wind
- **Geometric**: ripple, expanding squares, plasma, kaleidoscope, tunnel, checker, diamonds, pulse rings, noise field
- **Retro**: matrix code, game of life, snake game demo, pong demo, equalizer demo
- **Celebration**: hearts, balloons, candles, confetti burst, party strobe, star burst

Previews in the app: v1 animated gradient chips from catalog metadata. v2 (optional): small Kotlin renderers for the top 20 effects. Do not duplicate all effect code in Kotlin.

## 9. Android app

### 9.1 Stack
Kotlin, Jetpack Compose, Material 3, MVVM, coroutines/Flow, Navigation Compose, DataStore. BLE: Nordic Android BLE library (ble-ktx) unless a blocker appears (ask first). minSdk 26. Target the current stable SDK. `BLUETOOTH_SCAN` (`neverForLocation`) and `BLUETOOTH_CONNECT` for Android 12+, location permission only for Android 11 and below. `DeviceTransport` interface with `FakeDevice` (for building the whole UI before hardware) and `BleTransport` (real). Same ViewModels use both.

### 9.2 Screens
Splash -> Onboarding (value, permissions) -> Pair (scan, pick device, enter passkey, success) -> Home with bottom navigation:
- **Studio**: paint-style color wheel + OK, brightness, favorites, palettes, scenes row, live preview matrix
- **Effects**: categories, search, favorites, "surprise me", speed/direction/mirror/color controls
- **Magic**: birthday mode, custom message, draw (9x9 pixel canvas), music reactive
- **Time**: clock + styles, timer presets (1/5/10/15/30 min, custom), schedules, sleep timer
- **Device**: connection status, auto-reconnect, mute, scenes manager, OTA, forget pairings, about

### 9.3 Color picker (paint style)
- Hue ring plus saturation/value area inside (or HSV triangle). Drag anywhere to choose.
- Magnifier loupe above the finger. Live preview on the app's mini matrix and (throttled to max 10 sends per second) on the device while dragging; OK commits and persists.
- HEX and RGB inputs, recent colors, favorites, named palettes (unlimited), eyedropper from a photo, "Cancel" reverts to the previous color.
- Output: any 24-bit RGB value.

### 9.4 Design system (industry standard)
- Material 3, dark-first, with a light theme. Custom brand color scheme, not default purple.
- Consistent 4/8 dp spacing grid, type scale, elevation and shape tokens in one theme file.
- Edge-to-edge, system bars handled, predictive back, splash screen API, adaptive icon.
- Motion: shared-element feel between cards and detail, spring animations on controls, haptics on commit.
- States designed for every screen: loading, empty, error, disconnected, reconnecting.
- Accessibility: content descriptions, 48 dp targets, contrast AA, font scaling safe, TalkBack tested.
- Connection chip always visible: Connected, Reconnecting, Offline, with one-tap retry.
- Phone, tablet and landscape layouts must not break.

### 9.5 App-side storage (DataStore)
Favorite colors, named palettes, favorite effects, last paired address, UI settings. Export and import as one JSON file so reinstalling the app or changing phones is easy.

## 10. Feature checklist (tracked in PROGRESS.md)
Color wheel + OK, brightness, 500+ effects, favorites and palettes, scenes (8, on device), birthday mode with melody, custom scrolling message, clock mode with phone sync, timer with alert, schedules, sleep timer, draw mode, music reactive, home widget and quick-settings tile, auto-reconnect, OTA, power safety cap, factory reset, onboarding, About screen.

## 11. Storage split
Device (NVS): live state, scenes, schedules, message, birthday content, bonds.
App (DataStore): favorites, palettes, effect favorites, UI prefs.
Reconnect always trusts the device snapshot.

## 12. Safety
- Brightness and current cap always on. `SET_BRIGHTNESS` is clamped in firmware.
- Validate every packet. Rate-limit commands that trigger heavy work.
- Watchdog friendly: no long blocking calls.

## 13. Testing strategy
- Native tests (firmware): protocol, effects, text, timer, scenes.
- App unit tests: codec, ViewModels with FakeDevice, color math.
- Compose UI tests for the color picker and pairing flow where practical.
- Hardware checklist (human): pixel walk, color order, power at 25/50/100% brightness with a thermal check, BLE drop mid-effect, power cycle persistence, birthday show finishes offline, clock after sync, timer alert.

## 14. Boundaries
See AGENTS.md (Always / Ask first / Never).

## 15. Success criteria
1. Fresh phone pairs with the passkey, bonds, and auto-reconnects after the phone restarts.
2. Picking any shade on the wheel and pressing OK changes the matrix to that color within 300 ms.
3. At least 500 selectable effect looks, each runs without blocking BLE.
4. Disconnecting BLE does not interrupt an effect, the show, the clock or a timer.
5. Power cycle restores color, brightness, effect and scenes; the show does not replay.
6. Birthday show runs 60-120 s with synchronized melody and finishes on ambient light.
7. Clock shows correct time after sync; timer counts down and beeps.
8. Debug APK installs, release build is signed, repo history is clean and tagged.
