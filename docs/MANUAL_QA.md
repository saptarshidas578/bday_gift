# Manual QA Checklist: PixelGift Physical & Phone Verification

Only a human with the physical ESP32 hardware and Android phone can verify these items.

## 1. Bluetooth & Pairing Stability
- [ ] **First-Time Pairing**: Power on matrix. In App, go to `Device` -> `Pair Device…`. Tap "Connect" on `PixelGift-5AF8`. Verify pairing prompt finishes cleanly and connects without disconnecting.
- [ ] **Already-Bonded Direct Connect**: Force close the app. Reopen app. Tap the top connection chip (or "Connect" in Device tab). Verify it connects automatically to `PixelGift-5AF8` without crashing or showing passkey prompts.
- [ ] **Bluetooth Toggle Mid-Use**: While connected and displaying an effect, turn off phone Bluetooth from quick settings. Verify app shows "Offline" without crashing. Turn Bluetooth back on; verify auto-reconnect restores "Online" status within 5 seconds.

## 2. Effects & Color Rendering
- [ ] **Rainbow Wave**: In Effects tab, tap "Rainbow Wave". Verify smooth rainbow wave travels across the matrix.
- [ ] **Rainbow Color Mode on Other Effects**: Open "Scanner" or "Sparkle", tap the "Rainbow" color mode chip. Verify the effect animation dynamically shifts through rainbow colors.
- [ ] **Single & Dual Color Changes**: Open "Breathing" or "Solid". Pick primary color (Red) and secondary color (Cyan). Verify the matrix reflects the exact colors selected.

## 3. UI, Accessibility & System Edge Cases
- [ ] **Screen Rotation**: Rotate phone between portrait and landscape in Studio, Effects, and Magic tabs. Verify no dialog resets or UI clipping.
- [ ] **TalkBack / Accessibility**: Enable Android TalkBack. Navigate through the bottom navigation tabs and verify every interactive element has an informative description and meets the 48dp touch target standard.
- [ ] **Thermal & Power Stability**: Leave an effect running at 50% brightness for 10 minutes. Feel the ESP32 chip and LED matrix wiring to verify normal operating temperature (<45°C).
