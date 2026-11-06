# PixelGift

> An ESP32 + 10×10 WS2812B LED matrix smart birthday gift controlled by an Android app over Bluetooth Low Energy (BLE).

PixelGift renders custom lighting effects, real-time pixel art, a digital clock, a countdown timer, interactive games, and a full personalized birthday celebration show directly on hardware. An accompanying modern Android app allows pairing, control, live canvas drawing, and device management.

---

## 📸 Screenshots

| Birthday Celebration | Lighting Studio & Palettes | 10×10 Live Draw Canvas |
| :---: | :---: | :---: |
| *(docs/screenshots/birthday_show.png)* | *(docs/screenshots/studio_palettes.png)* | *(docs/screenshots/draw_canvas.png)* |

| Time & Pomodoro Timer | Interactive Game of Life | Device & Diagnostics |
| :---: | :---: | :---: |
| *(docs/screenshots/time_timer.png)* | *(docs/screenshots/game_of_life.png)* | *(docs/screenshots/device_diag.png)* |

---

## ✨ Features

### Built & Verified
- **Lighting Studio**: 67 native effects across 8 categories (Static, Motion, Gradient, Nature, Celebration, Particles, Geometric, Retro) with custom speed, direction, brightness, and color palette controls.
- **CIE 1931 Perceptual Gamma**: Smooth non-linear curve for gentle, natural brightness rendering.
- **Sound-Reactive Visualizer**: 10-column real-time audio visualizer driven by phone microphone.
- **Bioluminescent Ocean**: Natural undulating wave effect (#67).
- **Birthday Celebration Show**: ~98-second standalone choreographed light show with golden sparkles, name reveal banner, hearts & balloons, fireworks, and soft ambient finale.
- **Daily Secret Gift Notes**: 7-day countdown messages leading up to the birthday date (Jan 24).
- **"Thinking of You" Heartbeat Tap**: Instant glowing heartbeat pulse sent from app to matrix.
- **Animated Memories Reel**: Slideshow previewing animated heart, star, smile, and gift sequences.
- **Midnight Birthday Beacon**: Automatic celebration trigger when the clock strikes midnight on Jan 24.
- **Digital Clock & Timer**: Stacked 3×4 digits, 4×5 clock, timezone auto-sync, 1–30 min countdown timer with perimeter progress ring, and 25m/5m Pomodoro focus mode.
- **Bedside Auto-Dimmer**: Ambient dimming window to <5% brightness between 11 PM and 7 AM.
- **Interactive Life Sandbox**: Conway's Game of Life evolving live on the 10×10 matrix from custom seeds drawn on phone.
- **Curated Mood Bundles**: One-tap scene presets (*Cozy Reading*, *Romantic Dinner*, *Midnight Focus*).
- **Matrix Self-Diagnostic**: Cycling primary colors and white to inspect individual diodes, current cap, and signal strength.
- **Soft Disconnect Beacon**: Gentle pulsing amber corner pixel when BLE disconnects instead of abrupt resets.
- **Android Integration**: Material 3 Jetpack Compose UI, home-screen widget, Quick Settings tile, and robust BLE auto-reconnect.
- **OTA Updates**: Wireless BLE firmware update engine with dual-partition automatic rollback protection.

### Planned / Deferred
- **Piezo Buzzer Melodies**: Piezo buzzer chimes and alert beeps (hardware pin mapped to GPIO 18; buzzer melodies currently deferred until hardware is attached).

---

## 🛠️ Hardware & Pin Map

| Component | Specification | Connection |
| :--- | :--- | :--- |
| **Microcontroller** | ESP32 DevKit v1 (Xtensa dual-core @ 240 MHz) | Micro-USB / 5V |
| **LED Matrix** | 10×10 WS2812B Addressable RGB Matrix (100 LEDs) | Data: **GPIO 5** |
| **Buzzer** | Passive Piezo Buzzer (Deferred) | PWM: **GPIO 18** |
| **Boot Button** | Built-in DevKit BOOT button | **GPIO 0** (Hold 5s to clear BLE bonds) |

### Wiring & Power Notes
- **Power Supply**: 5 V DC regulated power supply rated for **$\ge$ 5 A** (100 WS2812B LEDs draw up to ~6 A at full white 100% brightness).
- **Power Caps**: Firmware enforces a strict software current limiter capped at **1500 mA** and maximum default brightness of **128/255** to prevent brownouts or thermal stress.
- **Common Ground**: ESP32 GND and power supply 0 V rail **must be tied together**.
- **Protection**: A 330–470 $\Omega$ resistor on the data line (GPIO 5 $\to$ DIN) and a 1000 $\mu$F 16 V capacitor across +5 V and GND are strongly recommended.

---

## 📁 Repository Structure

```
├── app/                  # Android application (Kotlin, Jetpack Compose, Nordic BLE)
│   ├── src/main/java/    # UI screens, ViewModels, BLE transport, and protocol codec
│   └── build.gradle.kts  # Android build configuration
├── firmware/             # ESP32 PlatformIO firmware (C++17, FastLED, NimBLE)
│   ├── src/              # Effects engine, matrix driver, BLE server, clock, OTA
│   ├── include/          # Header definitions and config.h
│   └── platformio.ini    # PlatformIO environment and dependency specs
├── hardware/             # Pin maps, wiring diagrams, and bill of materials
├── protocol/             # Single-source-of-truth protocol.json and effects.json
├── docs/                 # User guide, QA reports, architecture decisions
├── scripts/              # Verification and utility scripts
├── AGENTS.md             # Development guidelines and agent rules
├── PROGRESS.md           # Implementation tracker and task log
└── SPEC.md               # Complete technical specification
```

---

## 🚀 Building & Flashing

### Firmware (ESP32)

Prerequisite: [PlatformIO Core](https://platformio.org/) installed.

```bash
cd firmware

# Set up local secrets from template
cp include/secrets.h.example include/secrets.h

# Compile firmware
pio run

# Flash to connected ESP32
pio run -t upload

# Open serial monitor (115200 baud)
pio device monitor -b 115200
```

### Android App

Prerequisite: Android Studio / JDK 17+ and Android SDK 34+.

```bash
cd app

# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Install on connected device via ADB
./gradlew installDebug
```

---

## 🔒 Pairing & Security

- **BLE Service UUID**: `0000FFF0-0000-1000-8000-00805F9B34FB`
- **Pairing Mode**: Supports friction-free Just Works bonding and LE Secure Connections.
- **Resetting Bonds**: Hold the physical **BOOT button** on the ESP32 for **5 seconds** to clear all stored BLE encryption keys and bonds.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
