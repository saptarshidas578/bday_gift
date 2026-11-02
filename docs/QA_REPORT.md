# QA Report: Task QA-1 Full App Audit

## Summary

- **Total Issues Found**: 14
- **Blocker**: 3 (Fixed)
- **Major**: 6 (Fixed)
- **Minor / Polish**: 5 (Fixed)
- **Status**: All 14 Fixed; 75 JVM Unit Tests passing; `lintDebug` passing (0 errors); Release minification build passing.

## Issue Tracking Table

| ID | Severity | Area | Steps to Reproduce | Expected vs Actual | File:Line | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **BUG-01** | **Blocker** | B. Connection Chip | Tap top connection chip when disconnected on real hardware | Expected: Reconnects to last bonded device or prompts safely. Actual: App crashed with unhandled exception due to hardcoded dummy MAC address `"FA:KE:DE:VI:CE:00"`. | `MainViewModel.kt:42` | **Fixed** |
| **BUG-02** | **Blocker** | A. Pairing / BLE | Tap "Connect" on discovered device while BLE scanner is running | Expected: Scan stops before GATT connect. Actual: Scanner ran continuously during connection & bonding, causing Android GATT status 133 / disconnect loop. | `DeviceViewModel.kt:123`, `BleTransport.kt:145` | **Fixed** |
| **BUG-03** | **Blocker** | E. Effects / Color | Select "Rainbow Wave" or change effect colors on matrix | Expected: Matrix renders dynamic colors / modes. Actual: Firmware effects hardcoded colors and ignored `params.colorMode` and user colors; ACK was dropped when `getSubscribedCount() == 0`. | `effects_gradient.cpp:5`, `ble_server.cpp:108` | **Fixed** |
| **BUG-04** | **Major** | A. Pairing State | Tap Connect in PairScreen | Expected: `PairingUiState.Success` only after GATT ready and STATUS notifications active. Actual: Returned prematurely on physical socket connection before services/MTU were ready. | `BleTransport.kt:156` | **Fixed** |
| **BUG-05** | **Major** | A. Pairing Passkey | Pair with already-bonded device | Expected: Direct connect without re-prompting passkey. Actual: Showed passkey dialog and aborted Just Works bonding on bond key mismatch. | `PairScreen.kt:479`, `DeviceViewModel.kt:115` | **Fixed** |
| **BUG-06** | **Major** | B. State Sync | Reconnect after changing device settings externally | Expected: UI updates without overwriting device state. Actual: `StudioViewModel` and `EffectsViewModel` never subscribed to `statusSnapshot`. | `PixelGiftApp.kt:77`, `StudioViewModel.kt:115` | **Fixed** |
| **BUG-07** | **Major** | C. Color Drag | Drag color wheel continuously | Expected: Single throttled `SET_COLOR`. Actual: Sent redundant `SET_MODE(MODE_SOLID)` back-to-back, doubling BLE packet loss and hit ACK timeouts. | `StudioViewModel.kt:380` | **Fixed** |
| **BUG-08** | **Major** | E. Effect Dispatch | Select or modify effect parameters | Expected: Single `SET_EFFECT`. Actual: Sent redundant `SET_MODE(MODE_EFFECT)` back-to-back with `SET_EFFECT`. | `EffectsViewModel.kt:210` | **Fixed** |
| **BUG-09** | **Major** | H. Device Tab | Perform Factory Reset per SPEC 7.4 (magic 0xA5 0x5A) | Expected: Confirmation dialog + command dispatch. Actual: `onFactoryReset()` was missing from `DeviceViewModel` and `DeviceScreen`. | `DeviceViewModel.kt:190`, `DeviceScreen.kt:460` | **Fixed** |
| **BUG-10** | **Minor** | C. Studio HEX | Enter 3-digit shorthand HEX (e.g., `#F00`, `#FFF`) | Expected: Expands to `#FF0000`, `#FFFFFF`. Actual: `hexToRgb` returned null for 3-character hex strings. | `ColorMath.kt:78` | **Fixed** |
| **BUG-11** | **Minor** | D. Palettes | Enter very long palette name (>64 chars) | Expected: Sanitized and capped. Actual: Unbounded string length. | `StudioViewModel.kt:240` | **Fixed** |
| **BUG-12** | **Minor** | G. Timer | Timer seconds exceeding uint32 | Expected: Clamped to 0xFFFFFFFF. Actual: Potential Long overflow. | `TimeViewModel.kt:157` | **Fixed** |
| **BUG-13** | **Minor** | K. Matrix Size | Update matrix dimensions | Expected: Single constant. Actual: Dimensions hardcoded as 81, 243, and 9 in multiple locations. Consolidated to `ProtocolConstants`. | `ProtocolConstants.kt:8`, `DrawModel.kt:14` | **Fixed** |
| **BUG-14** | **Minor** | J. Release Build | Run `assembleRelease` with minification | Expected: Minified APK succeeds. Actual: Missing `proguard-rules.pro` created with rules for Nordic BLE and protocol models. | `app/proguard-rules.pro` | **Fixed** |

