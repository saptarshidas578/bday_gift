# AGENTS.md: PixelGift

Read this first. Keep it short. Rules here beat habits.

## Project in one paragraph

ESP32 + 9x9 WS2812B matrix (81 LEDs) + passive piezo, controlled by an Android app over BLE (paired and bonded). The firmware renders every effect, the birthday show, clock and timer locally. The app only sends compact commands. Full detail: SPEC.md. Live status: PROGRESS.md. Task prompts: prompts/PROMPTS.md.

## Session start (do exactly this, nothing more)
1. Read AGENTS.md and PROGRESS.md.
2. Read only the SPEC.md section named in the task. Never read the whole spec "just in case".
3. Open only files you will edit. Do not scan or print the whole repo.
4. State the plan in 3 lines max, then work.

## Work loop (one task per session)
1. Do the task. Smallest change that meets the acceptance criteria.
2. Run the verify command for that task (see Commands). Fix from the error text only.
3. Update PROGRESS.md (tick the box, set "Next step", add a decision if one was made).
4. Commit. Stop. Report in 5 lines: done, verified how, anything the human must do or check.

## Token discipline
- Edit with targeted patches. Never rewrite or re-print a whole file for a small change.
- Do not paste large logs back. Quote the 3 to 10 relevant lines.
- Prefer pure, small modules (<300 lines) so less is re-read later.
- Put logic in code that has native unit tests (pio test -e native, Gradle unit tests). Hardware and phone are slow feedback loops; use them last.
- One source of truth for shared data: protocol/protocol.json and protocol/effects.json. Generate or mirror constants from them. Never hand-copy opcodes or effect ids.
- Do not add dependencies without asking. Do not refactor unrelated code.
- If a task needs more than ~5 files, split it and tell the human.

## Git rules
- `git init` on day one. .gitignore covers: .pio/, build/, .gradle/, local.properties, *.jks, *.keystore, firmware/include/secrets.h, .idea/.
- Branch per phase: `phase/<id>-<slug>`. Merge to main only when the phase verifies. main must always build.
- Conventional commits, small and frequent: `feat(firmware): add rainbow wave`, `fix(app): reconnect after bond loss`, `docs: update progress`, `test(protocol): ....`
- Tag milestones: v0.1-leds, v0.2-ble-pairs, v0.3-app-controls, v1.0-gift.
- Never commit secrets (pairing passkey lives in secrets.h, not in git).
- Never rewrite published history. Never delete failing tests to get green.

## Hardware gates: STOP and ask the human
Stop and wait for the human to confirm before: first power-up of any new wiring, first full brightness test, flashing when the serial port or board is unknown, any step that changes power wiring, anything that could draw more current than the supply allows. Ask for exactly what you need to see (photo of wiring, serial output, "does the pixel walk in order?"). Never assume pins, board model or wiring. Read them from firmware/include/config.h. Always keep the software brightness and current cap active, including in tests.

## Debugging protocol
1. Reproduce. Capture the exact error or serial output.
2. Form one hypothesis. Add the smallest log or test that proves or kills it.
3. Fix minimally. Re-run the same check. Repeat.
4. Add a regression test if the bug was in pure logic.
5. Log only non-obvious findings in PROGRESS.md under "Debug notes" (max 2 lines each).

## Commands

### Firmware (run in firmware/):
- Build: `pio run`
- Upload: `pio run -t upload`
- Serial: `pio device monitor -b 115200`
- Logic tests (no hardware): `pio test -e native`

### App (run in app/):
- Build debug APK: `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lintDebug`
- Install on phone: `./gradlew installDebug`

## Code style
- **Firmware**: C++17, Arduino framework, PlatformIO. No `delay()` in `loop()`; use non-blocking timers (`millis()`). No heap allocation inside render loops. Constants in `config.h`. Libraries: FastLED, NimBLE-Arduino. Check library versions before pinning.
- **App**: Kotlin, Jetpack Compose, Material 3, MVVM with ViewModel + StateFlow, coroutines. Unidirectional data flow. Composables are stateless; state is hoisted. No logic in composables. Strings in `strings.xml`. Every interactive element has a content description and a >=48dp target.
- **Names**: clear, no abbreviations except rgb, ble, nvs. Comments explain why, not what.

## Boundaries
- **Always**: run the verify step, keep the brightness/current cap, validate every BLE input, handle disconnects, update PROGRESS.md, commit.
- **Ask first**: new dependencies, protocol changes, pin or wiring changes, anything touching persisted data layout, changing SPEC.md requirements.
- **Never**: commit secrets, stream full LED frames continuously from the phone, block `loop()`, write NVS on every animation frame, auto-replay the birthday show at boot, disable the power cap, claim something works without running its verify step.

## Spec changes
If a decision changes, update SPEC.md first (small patch), note it in PROGRESS.md Decisions, then implement.

## Definition of done (every task)
Acceptance criteria met, verify command passes, PROGRESS.md updated, committed, and any hardware or phone check is clearly handed to the human.
