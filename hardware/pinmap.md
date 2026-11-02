# Hardware Pinmap: PixelGift

Default pin assignments based on SPEC.md (to be confirmed in task B01):

| Function | ESP32 GPIO | Description | Notes |
| :--- | :--- | :--- | :--- |
| WS2812B Data | GPIO 5 (D5) | Signal line to 10x10 LED matrix (100 LEDs) | Confirmed working in B01/B02; 330-470Ω series resistor |
| Piezo Buzzer | GPIO 18 | Passive piezo buzzer driven via LEDC | Non-blocking tone generation |
| Boot Button | GPIO 0 | Built-in DevKit button | Hold 5s to clear BLE pairings/bonds |
| 5V Power In | VIN / 5V | Regulated 5V supply (~5A target) | Injected at matrix ends; ~1000µF capacitor |
| Ground | GND | Common ground | ESP32, matrix, and power supply GND tied together |
