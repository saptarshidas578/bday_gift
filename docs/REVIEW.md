# PixelGift Project Review & Improvement Proposals (REVIEW-1)

## Executive Summary
- **Overall Health Score: 7.8 / 10**
- **Strengths**: Solid foundation with 10x10 rendering, robust 2500mA FastLED current limiter, non-blocking 60 FPS animation architecture, complete 50+ effect implementations, and comprehensive Android Compose UI.
- **Key Vulnerabilities**: OTA opcode range check blocks updates (`packet_parser.cpp:18`), custom palettes are not persisted across power loss (`storage.cpp`), BLE pairing currently uses unauthenticated "Just Works" rather than the specified passkey, and `Serial.readStringUntil` risks 1000ms watchdog frame drops.
- **Codebase Quality**: Android test suite is 100% green, but native PlatformIO tests lack `[env:native]`. UI screen files are monolithic (>800 lines) and use `remember` for ViewModels rather than Jetpack `viewModel()` lifecycle retention.

---

## Part 1: Health Check (Problems & Findings)

| ID | Severity | Area | Finding | file:line | Fix Idea |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **H-01** | **Blocker** | Protocol / Firmware | `parseCommandPacket` rejects opcodes > 24, causing all OTA commands (25-28) to fail with `ERR_UNKNOWN_OP` | `firmware/src/packet_parser.cpp:18` | Change check to `if (opcode < 1 \|\| opcode > 28)` |
| **H-02** | **Major** | Firmware / Persistence | `state.palette` (8 RGBs) and `state.paletteCount` are never saved in `storageForceSave()` or loaded in `storageInit()`; custom palettes vanish on reboot | `firmware/src/storage.cpp:110` | Add `prefs.putBytes("pal", ...)` and `prefs.putUChar("pal_cnt", ...)` |
| **H-03** | **Major** | BLE Security | ESP32 uses `ESP_IO_CAP_NONE` and `mitm=false` ("Just Works"); static `BLE_PASSKEY` in `secrets.h` is ignored, allowing unauthorized connection | `firmware/src/ble_server.cpp:71` | Enable MITM protection, display passkey on matrix, and require auth |
| **H-04** | **Major** | Firmware Robustness | `Serial.readStringUntil('\n')` has a default 1000ms timeout; any single stray serial byte stalls `loop()` for 1s, freezing LEDs and risking watchdog trip | `firmware/src/main.cpp:42` | Use non-blocking character accumulation or `Serial.setTimeout(10)` |
| **H-05** | **Major** | Protocol / Status | `bleSendSnapshot()` never sets flags for `timeSynced`, `timerRunning`, or `showRunning`; app snapshot is permanently blind to active timer/show | `firmware/src/ble_server.cpp:129` | Query `isTimeSynced()`, `isTimerRunning()`, and `isBirthdayRunning()` for flags |
| **H-06** | **Major** | Testing | `platformio.ini` defines only `[env:esp32dev]`, missing `[env:native]`; running `pio test -e native` (per AGENTS.md) crashes with `UnknownEnvNamesError` | `firmware/platformio.ini:1` | Add `[env:native]` environment with desktop Unity test harness |
| **H-07** | **Major** | App Architecture | Root ViewModels are instantiated via `remember { ... }` instead of Jetpack `viewModel()`; rotation/config changes wipe state, and root StateFlow collection causes full-screen recompositions | `app/src/main/java/com/pixelgift/app/ui/screens/PixelGiftApp.kt:64` | Migrate to `viewModel()` and hoist state collection into sub-screens |
| **H-08** | **Minor** | Matrix Size / Clock | Stacked clock places 7-column digits at cols 1..7 (leaving 1 col on left, 2 on right), leaving row 9 unused; scrolling 5x7 "HH:MM" (`clockStyle==1`) is not implemented | `firmware/src/clock_timer.cpp:71` | Center 7-column layout and implement scrolling 5x7 mode for `clockStyle == 1` |
| **H-09** | **Minor** | Matrix Size / Effects | `renderChecker` and `renderDiamonds` use hardcoded center offset `x - 4` instead of 4.5f, creating 1-pixel asymmetry on 10x10 matrix | `firmware/src/effects_geometric.cpp:45` | Replace `x - 4` with `abs((float)x - 4.5f)` |
| **H-10** | **Minor** | Firmware / OTA | `otaHandleCommit` accepts `expectedCrc` parameter but never calculates or validates binary checksum prior to reboot | `firmware/src/ota_manager.cpp:104` | Calculate CRC32 over incoming chunks and verify before `Update.end()` |
| **H-11** | **Minor** | Spec vs Reality | `SPEC.md` still describes a 9x9 matrix (81 LEDs, 4.86A max, 243-byte frame, 9-band visualizer) despite hardware evolution to 10x10 | `SPEC.md:7` | Update `SPEC.md` sections 3, 6, 7, and 9 to 10x10 (100 LEDs, 300B frame) |
| **H-12** | **Minor** | Repo Hygiene | Project root is missing a standard `README.md` for developers and human build onboarding | Root directory | Add `README.md` with build steps, pinouts, and quickstart commands |
| **H-13** | **Polish** | Token Discipline | Four primary screen files exceed 800-1000 lines (`StudioScreen.kt`: 967L, `TimeScreen.kt`: 985L), violating the AGENTS.md `<300 lines` rule | `app/src/main/java/com/pixelgift/app/ui/screens/` | Refactor large screens into sub-tab and dialog component modules |
| **H-14** | **Polish** | Hardware Documentation | `hardware/wiring.md` and `hardware/bom.md` are 3-line placeholders lacking assembly photos, cap placement, and schematic details | `hardware/wiring.md:1` | Complete full wiring schematic, capacitor placement, and diffuser tips |

---

## Part 2: Improvement Proposals

### 1. Spec vs Reality
- **Single-pass SPEC.md Synchronization**: Align matrix dimensions (10x10, 100 LEDs), 300-byte frame protocol, and current GPIO 5 / GPIO 18 pinout across `SPEC.md`.
- **Living Capabilities Matrix**: Auto-generate a markdown capability table from `protocol.json` and `effects.json` during build.
- **Audio Status Clarity**: Formally document deferred buzzer stubs in `SPEC.md` Section 6.8 so tests do not expect sound hardware.

### 2. Matrix Size
- **Unified Center Constants**: Introduce `MATRIX_CENTER_X = 4.5f` and `MATRIX_CENTER_Y = 4.5f` in `config.h` and use consistently across radial/geometric effects.
- **10x10 Tailored Font / Clock Layout**: Redesign the stacked clock digits to utilize 4x5 or 4x4 numerals spanning all 10 columns symmetrically.
- **2x2 Center Pixel Dilation**: Update diamond, spiral, and ripple animations to radiate outward from the central 2x2 diode cluster.

### 3. Protocol
- **Packet Integrity Checksums**: Append a 2-byte CRC-16 to `DRAW_CHUNK` and `OTA_CHUNK` payloads to catch BLE transport byte corruptions.
- **Slider Command Coalescing**: Implement trailing-edge rate limiting in `BleTransport` so rapid brightness/color wheel drags send intermediate frames without ACK latency bottlenecks.
- **Dynamic MTU Negotiation Check**: Verify confirmed MTU size before dispatching chunks larger than 20 bytes to prevent silent truncation on older phones.

### 4. Firmware Robustness
- **NVS Crash / Reset Logging**: Record `esp_reset_reason()` into Preferences on startup to diagnose spontaneous reboots or power dips.
- **Non-Blocking Serial Ring Buffer**: Replace `Serial.readStringUntil` with a non-blocking character accumulator that processes commands on newline without delays.
- **NVS Write-Skip Guard**: Compare dirty state with cached memory values to avoid writing NVS sectors when values haven't actually changed.

### 5. BLE Security & Pairing
- **Visual Matrix Passkey Prompt**: Display a scrolling 6-digit random or static passkey on the matrix when an unbonded phone requests pairing.
- **Bond Manager UI**: Add a list in the app's Device tab showing bonded phone MACs with individual revocation capability.
- **Encrypted Characteristic Gates**: Re-enable `WRITE_ENC` and `READ_ENC` properties once secure passkey entry is established.

### 6. App Quality
- **Jetpack ViewModel Migration**: Replace `remember { ViewModel(...) }` with standard Compose `viewModel()` or Hilt dependency injection to survive activity recreations.
- **Screen Decomposition**: Split `StudioScreen.kt`, `TimeScreen.kt`, and `MagicScreen.kt` into dedicated component packages under 250 lines each.
- **Targeted State Collection**: Hoist state collection from `PixelGiftApp.kt` down into individual screen composables to isolate recomposition boundaries.

### 7. Power & Safety
- **Soft Startup Brightness Ramp**: Ramp LED brightness over 300ms at boot to prevent sudden capacitive current surges on the 5V power supply.
- **Software Power Throttling Indicator**: Expose real-time FastLED power cap status as a subtle warning badge in the app when current limiting engages.
- **Thermal Rest Schedule**: Automatically reduce brightness by 20% after 2 continuous hours of high-intensity white/solid effects.

### 8. Testing
- **Native PlatformIO Test Harness**: Configure `[env:native]` in `platformio.ini` with mocked Arduino/FastLED headers for instant logic verification.
- **Compose UI Tests**: Add UI tests for `PaintColorWheel`, `DrawCanvas`, and `PairScreen` using `compose-ui-test`.
- **Fuzz Testing Parser**: Implement a host-side fuzz test feeding randomized byte streams into `parseCommandPacket` to guarantee zero crashes.

### 9. Repo Hygiene
- **Root README.md**: Add an engaging project overview, hardware pinout diagram, build prerequisites, and flashing commands.
- **Gitignore Hardening**: Add rules for `.apk`, `.aab`, OS metadata (`Thumbs.db`, `.DS_Store`), and Python build artifacts (`*.pyc`, `__pycache__`).
- **Reproducible Version Locks**: Pin exact PlatformIO Espressif32 platform revision and Gradle dependency lockfiles.

### 10. Token & Workflow Efficiency
- **Reusable Color Swatch / Palette Component**: Consolidate duplicated palette and color chip composables across Studio, Effects, and Magic tabs.
- **Standardized Verification Script**: Create a single `scripts/verify.ps1` script that runs both Android unit tests and PlatformIO compilation.
- **Targeted Diff Verification**: Enforce sub-300-line modularity to minimize file re-reads during future coding turns.

---

## Part 3: New Feature Proposals

| # | Group | Feature Name | Description | Extra Hardware | Effort | Impact (1-5) | Risk |
| :--- | :--- | :--- | :--- | :--- | :---: | :---: | :---: |
| 1 | Lighting | **CIE 1931 Perceptual Gamma** | Smooth non-linear brightness curve for ultra-gentle night breathing | None | S | 4 | Low |
| 2 | Lighting | **Sound-Reactive Aurora** | 10-column frequency visualizer with reactive color wave transitions | None (Phone Mic) | S | 5 | Low |
| 3 | Lighting | **Bioluminescent Ocean** | Wave swell effect where brightness and speed undulate organically | None | S | 4 | Low |
| 4 | Birthday | **Daily Secret Gift Notes** | 7-day countdown messages unlocked daily leading up to birthday | None | M | 5 | Low |
| 5 | Birthday | **Animated Memories Reel** | Sequence of multi-frame 10x10 pixel drawings with text captions | None | M | 5 | Low |
| 6 | Birthday | **Midnight Birthday Beacon** | Automatic trigger of birthday show when clock strikes midnight on Jan 24 | None (Synced Time) | S | 5 | Low |
| 7 | Clock | **Word Clock 10x10 Mode** | Illuminates matrix letters forming "IT IS HALF PAST TEN" with stencil | None (Stencil opt) | M | 5 | Medium |
| 8 | Clock | **Sunrise Awakening Alarm** | 20-min gradual amber dawn simulation with gentle alert tone | Piezo Buzzer | M | 5 | Low |
| 9 | Clock | **Pomodoro Focus Ring** | Outer perimeter countdown ring (25 min focus green / 5 min break cyan) | None | S | 4 | Low |
| 10 | Games | **Interactive Tilt Snake** | Play Snake on 10x10 matrix steering by tilting phone via BLE accelerometer | None | M | 5 | Low |
| 11 | Games | **Two-Player BLE Pong** | Control paddles via app touch buttons for miniature retro table tennis | None | S | 4 | Low |
| 12 | Games | **Interactive Life Sandbox** | Draw live seed cells on app canvas and watch Conway's Life evolve on matrix | None | S | 4 | Low |
| 13 | Smart | **Bedside Auto-Dimmer** | Automatic ambient dimming to <5% brightness between 11 PM and 7 AM | None | S | 5 | Low |
| 14 | Smart | **Weather Forecast Glow** | Morning weather mood light (Golden=Sunny, Cyan=Rain, Purple=Storm) | None (Phone Sync) | M | 4 | Low |
| 15 | Smart | **Loved-One Notification Pulse** | Subtle corner diode pulse when receiving messages from designated contact | None (Android Service)| M | 5 | Medium |
| 16 | Social | **"Thinking of You" Tap** | Single tap in app sends a glowing heartbeat pulse directly to her matrix | None (BLE / WiFi opt)| M | 5 | Low |
| 17 | Social | **Pixel Art QR Exchange** | Export/import saved drawings and messages as shareable text/QR strings | None | S | 4 | Low |
| 18 | Social | **Curated Mood Bundles** | One-tap presets ("Cozy Reading", "Romantic Dinner", "Midnight Focus") | None | S | 4 | Low |
| 19 | Care | **Matrix Self-Diagnostic** | Hardware check cycling R/G/B/W, reporting bad diodes, current, and RSSI | None | S | 4 | Low |
| 20 | Care | **Soft Disconnect Beacon** | Gentle pulsing corner amber pixel when BLE drops, instead of harsh resets | None | S | 4 | Low |

---

## Part 4: Recommended Roadmap (Top 10 Steps)

1. **Fix OTA Opcode Gate (H-01)** [Can be done now]: Update `packet_parser.cpp` opcode boundary check to allow opcodes 25-28, unblocking OTA updates.
2. **Persist Custom Palettes in NVS (H-02)** [Can be done now]: Save and load `state.palette` and `state.paletteCount` in `storage.cpp` so custom colors survive power cycles.
3. **Complete Status Snapshot Flags (H-05)** [Can be done now]: Populate `FLAG_TIME_SYNCED`, `FLAG_TIMER_RUNNING`, and `FLAG_SHOW_RUNNING` in `bleSendSnapshot()`.
4. **Non-blocking Serial Parser (H-04)** [Can be done now]: Eliminate 1000ms `readStringUntil` blocking timeout in `main.cpp`.
5. **Decide BLE Security & Passkey Strategy (H-03)** [Decision needed by human]: Choose between enforced 6-digit gift-card passkey vs frictionless "Just Works" pairing.
6. **Migrate App ViewModels to Jetpack Architecture (H-07)** [Can be done now]: Scope ViewModels to Compose navigation lifecycle and hoist state collection out of `PixelGiftApp.kt`.
7. **Refactor Monolithic Screen Files (H-13)** [Can be done now]: Decompose `StudioScreen.kt`, `TimeScreen.kt`, `MagicScreen.kt`, and `DeviceScreen.kt` into sub-300-line components.
8. **Configure Native PlatformIO Unit Testing (H-06)** [Can be done now]: Add `[env:native]` target to `platformio.ini` to restore fast host-side testing.
9. **Synchronize Specification & Documentation (H-10, H-11, H-14)** [Can be done now]: Create `README.md`, sync `SPEC.md` to 10x10 hardware, and complete `hardware/wiring.md`.
10. **Hardware Validation & Piezo Melody Integration (B07b)** [Needs hardware]: Flash firmware to ESP32, wire buzzer to GPIO 18, and complete audio player.

---

## Questions for You (Decisions Needed)

1. **BLE Pairing Security**: Do you prefer an **enforced 6-digit passkey** that she must enter from the gift card (gift-card mystery, prevents nearby interference), or **frictionless "Just Works" pairing** (no PIN popup, instant one-tap connect)?
2. **Clock Alignment on 10x10**: Should we **center the current 7-column stacked clock** (leaving 1 column padding on left and 2 on right), or **redesign digits to a 4x5 font** that utilizes the full 10x10 grid symmetrically?
3. **Piezo Buzzer Status**: Do you currently have the **physical piezo buzzer on hand** to wire and test audio melodies now, or should audio remain **visual-alert only** until a later date?
4. **OTA Checksum Enforcement**: Should the firmware calculate and strictly verify an **explicit CRC32 checksum** before committing OTA firmware, or is the **ESP32 partition table validation** sufficient?
5. **Music Visualizer Architecture**: Should the equalizer remain **phone-mic driven over BLE** (streaming 10 levels at 20 Hz), or do you want to plan for a **direct hardware microphone module** (e.g. INMP441 / MAX9814) wired to the ESP32?
6. **Remote Internet Glow ("Thinking of You")**: Should PixelGift remain **strictly offline BLE-only** (zero network setup, long-term privacy), or would you like **optional home WiFi connectivity** to send remote heart glows over the internet?
7. **Screen File Refactoring**: May I proceed in the next session with **decoupling the 1000-line screen files** into modular sub-250-line files to improve token efficiency and maintenance?
8. **App Onboarding Passkey Card**: In the pairing screen, should we show an illustration of the **handwritten gift card with the passkey**, or a direct automatic device list?

