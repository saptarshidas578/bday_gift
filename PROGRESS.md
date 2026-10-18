# PROGRESS

Agent: update this at the end of every task, then commit. Keep it terse. Legend: [x] done, [~] in progress, [ ] to do, [H] needs the human (wiring, flashing, phone check)

## Next step

A02: DeviceTransport, FakeDevice, protocol codec + unit tests

## Phases

### P00 Bootstrap
- [x] P00 repo, folders, .gitignore, first commit, tag v0.0-init

### Track A: App on a fake device (no hardware needed)
- [x] A01 Android scaffold, theme, navigation shell
- [ ] A02 DeviceTransport, FakeDevice, protocol codec + unit tests
- [ ] A03 Studio: paint-style color wheel + OK, HEX/RGB, recents
- [ ] A04 Brightness, favorites, palettes, scenes row, mini matrix preview
- [ ] A05 Effects browser from effects.json
- [ ] A06 Magic tab: birthday, message, draw, music (UI)
- [ ] A07 Time tab: clock styles, timer, schedules, sleep
- [ ] A08 Device tab, onboarding, permissions, pairing UI
- [ ] A09 Polish pass 1: states, motion, accessibility, icon, splash

### Track B: Firmware
- [ ] B01 [H] PlatformIO project + pixel walk + color order + pin confirm
- [ ] B02 Matrix mapping, brightness and current cap
- [ ] B03 Effect engine + first 10 effects + native tests
- [ ] B04 Remaining effects to 60 bases + effects.json complete
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
- Assumed Android only, no RTC, timer cancelled on power loss. Confirm or change in SPEC section 2.

## Hardware facts (fill in as measured)
- Board:
- Data pin / buzzer pin:
- Matrix order (serpentine?):
- Color order (GRB?):
- Supply rating:
- Passkey card written: no

## Debug notes

(only non-obvious findings, 2 lines max each)
