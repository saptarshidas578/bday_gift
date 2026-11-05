# PROGRESS

Agent: update this at the end of every task, then commit. Keep it terse. Legend: [x] done, [~] in progress, [ ] to do, [H] needs the human (wiring, flashing, phone check)

## Next step

Hardware verification on ESP32 & phone: App installed and launched on phone (SM-N981U). Plug in ESP32 to flash updated firmware (`& "$env:USERPROFILE\.platformio\penv\Scripts\pio.exe" run -d firmware -t upload`), verify Draw Mode (10×10 canvas, 300B frame), custom text message scrolling (`SET_TEXT` / `SHOW_MESSAGE`), and Batch 3 effects (#54–#66) on live hardware.

## Phases

### P00 Bootstrap
- [x] P00 repo, folders, .gitignore, first commit, tag v0.0-init

### Track A: App on a fake device (no hardware needed)
- [x] A01 Android scaffold, theme, navigation shell
- [x] A02 DeviceTransport, FakeDevice, protocol codec + unit tests
- [x] A03 Studio: paint-style color wheel + OK, HEX/RGB, recents
- [x] A04 Brightness, favorites, palettes, scenes row, mini matrix preview
- [x] A05 Effects browser from effects.json
- [x] A06 Magic tab: birthday, message, draw, music (UI)
- [x] A07 Time tab: clock styles, timer, schedules, sleep
- [x] A08 Device tab, onboarding, permissions, pairing UI
- [x] A09 Polish pass 1: states, motion, accessibility, icon, splash

### Track B: Firmware
- [x] B01 [H] PlatformIO project + pixel walk + color order + pin confirm
- [x] B02 Matrix mapping, brightness and current cap
- [x] B03 Effect engine + first 10 effects + native tests
- [x] B04 Remaining effects to 60 bases + effects.json complete
- [x] B05 NVS persistence with debounce
- [x] B06 [H] BLE server, secure pairing, protocol dispatch
- [x] B07 [H] Birthday show (lights only, 60-120s, deferred audio)
- [ ] B07b Audio when buzzer arrives (melody player, beeps, mute)
- [x] B08 [H] Text engine, clock, timer
- [x] B09 [H] Scenes, schedules, sleep timer, draw, music levels
- [x] B10 OTA

### Track C: Integration
- [x] C01 [H] Real BLE transport in app, pairing with passkey
- [x] C02 [H] Integration checklist (SPEC section 13) and bug fixes
- [x] C03 App polish pass 2, widget, quick-settings tile
- [x] C04 [H] Enclosure test, final power validation, signed release APK, tag v1.0-gift

## Decisions

(date, decision, why)
- 2026-11-05: Upgraded App to full 10x10 matrix (100 LEDs, 300-byte frame), implemented custom text message engine (`SET_TEXT` / `SHOW_MESSAGE` scrolling text on matrix), and updated Batch 3 effects (Pulse Rings #54, Noise Field #55, Matrix Code #56, Game of Life #57, Snake #58, Pong #59, Equalizer #60, Hearts #61, Candles #63, Confetti #64, Party Strobe #65, Starburst #66) with full dynamic Single color, Dual tone, Palette, and Rainbow support. Updated `protocol/effects.json` flags. Verified firmware build (`pio run`, 0 errors), 100% Android unit tests passing, and `assembleDebug` clean.
- 2026-11-05: Completed Batch 2 effect improvements (effects 18-50). Added full dynamic color, dual tone, palette, and rainbow mode support across Palette Scroll (#18), Chase (#20), Wipe (#21), Bounce (#22), Spiral (#23), Radar (#24), Comet (#28), Theater Chase (#29), Row Sweep (#30), Twinkle (#31), Fireflies (#32), Fireworks (#33), Meteor (#36), Bubbles (#37), Stardust (#39), Lava (#40), Ocean (#41), Lightning (#42), Northern Lights (#43), Embers (#44), Storm (#45), Wind (#46), Ripple (#47), Plasma (#49), and Kaleidoscope (#50). Per user instruction, cleanly removed Rain (#25), Waterfall (#26), Snake Path (#27), Glitter (#38), and Expanding Squares (#48) from `effects.json`. Verified firmware build (`pio run`, 0 errors), 100% Android unit tests passing, and `assembleDebug` clean.
- 2026-11-05: Resolved Matrix R→L physical orientation, palette choosing, and Batch 1 (effects 1-5, 8-17) color controls. Inverted serpentine parity in `matrix.cpp`. Added smooth palette interpolation to `getEffectColor` and updated renderers in `effects_static`, `effects_motion`, `effects_particles`, `effects_nature`, and `effects_gradient` for full Single, Dual tone, Palette, and Rainbow support. Updated `protocol/effects.json` and `EffectDetailSheet.kt` to allow choosing named palettes with preview chips, selecting individual colors from active palettes for Primary/Secondary colors, and switching between all color modes. Verified firmware build (0 errors) and 75 Android unit tests passing, and deployed updated app to phone.
- 2026-11-05: Established **working checkpoint one** at commit `ffd8036` (tags `working-checkpoint-one` / `working-checkpoint-1`, branch `working-checkpoint-one`). Clean compile, 0 errors, build cache active. Whenever the human instructs to "go back to the working version", revert to this exact checkpoint. No further checkpoints to be created without explicit instruction.
- 2026-11-04: Resolved solid-white LED lock. Restored DATA_PIN to GPIO 5 (D5) and buzzer to GPIO 18. Removed WRITE_ENC and READ_ENC gates on BLE characteristics so phone writes execute reliably regardless of bonding/encryption state. Added NVS boot guards and reset stale NVS state to Rainbow Wave effect (#6, Rainbow mode). Verified live color animation over serial.
- 2026-11-02: Fixed effect color rendering and BLE stability. Added getEffectColor() across all 66 effects in effects_static, effects_motion, effects_particles, effects_geometric, effects_celebration, effects_retro, and effects_gradient so every effect responds dynamically to custom colors, dual blend, palette, and rainbow mode. Prevented accidental disconnect when tapping top connection chip. Flash verified on ESP32 (COM8) and app installed on phone.
- 2026-11-02: Completed Task QA-1. Fixed 14 bugs across Areas A-K: BLE scanner collision stopping prior to connect, real device address handling on connection chip (preventing app crash), awaiting onDeviceReady for pairing success, removing redundant back-to-back SetMode packets, adding getEffectColor support for rainbow/dual/palette effect modes, and 3-digit HEX parsing.
- 2026-11-02: Completed Task C04. Built signed release APK (PixelGift-release.apk) with external keystore outside git, published user guide (docs/user-guide.md), power validation at 2500 mA cap, and tagged v1.0-gift.
- 2026-11-02: Added home-screen widget (PixelGiftWidgetProvider) and Quick Settings tile (PixelGiftTileService), Cherry Red icon and splash pass, personal note for Madhumitha's 19th birthday in About dialog, and verified lintDebug clean with 0 errors.
- 2026-11-02: Implemented BleTransport using Nordic Android BLE library (ble-ktx 2.7.5 + scanner 1.6.0) with MTU 185, write queue + ACK timeout, exponential auto-reconnect, and state sync on connect.
- 2026-11-02: Pure BLE streaming OTA with min_spiffs dual-partition rollback and monotonic version check (Option B).
- 2026-11-02: No piezo buzzer yet. Defer all audio to B07b. Birthday show runs lights-only; timer uses visual alert. App shows audio controls disabled with 'Speaker not connected'.
- 2026-11-02: Hardware matrix changed to 100 LEDs (10x10) connected to GPIO 5 (D5).
- Assumed Android only, no RTC, timer cancelled on power loss. Confirm or change in SPEC section 2.

## Hardware facts (fill in as measured)
- Board: ESP32 DevKit V1 (esp32dev)
- Data pin / buzzer pin: GPIO 5 (D5, confirmed working) / GPIO 18 (Buzzer default)
- Matrix size: 10x10 (100 LEDs)
- Matrix order (serpentine?): Yes, serpentine (row 0 L->R, row 1 R->L, row 2 L->R...)
- Color order (GRB?): GRB (accurate on RGB color cycle test)
- Supply rating: 5V regulated (FastLED capped at 2500 mA, brightness safe 40/255)
- Passkey card written: no

## Debug notes

(only non-obvious findings, 2 lines max each)
- Sealed class companion `val` lists cause JVM circular initialization NPE; use custom getter `get() = listOf(...)`.
- Inject optional CoroutineScope into ViewModels to avoid unmocked Looper crashes in pure JVM unit tests.
- Avoid collecting infinite flows in ViewModel init when tests use runBlocking; use immediate StateFlow updates.


