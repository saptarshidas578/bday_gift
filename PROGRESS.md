# PROGRESS

Agent: update this at the end of every task, then commit. Keep it terse. Legend: [x] done, [~] in progress, [ ] to do, [H] needs the human (wiring, flashing, phone check)

## Next step

B06 [H] BLE server, secure pairing, protocol dispatch

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
- [ ] B06 [H] BLE server, secure pairing, protocol dispatch
- [ ] B05 NVS persistence with debounce
- [ ] B06 [H] BLE server, secure pairing, protocol dispatch
- [ ] B07 [H] Piezo audio + birthday show
- [ ] B08 Text engine, clock, timer
- [ ] B09 Scenes, schedules, sleep timer, draw, music levels
- [ ] B10 OTA

### Track C: Integration
- [ ] C01 [H] Real BLE transport in app, pairing with passkey
- [ ] C02 [H] Integration checklist (SPEC section 13) and bug fixes
- [ ] C03 App polish pass 2, widget, quick-settings tile
- [ ] C04 [H] Enclosure test, final power validation, signed release APK, tag v1.0-gift

## Decisions

(date, decision, why)
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


