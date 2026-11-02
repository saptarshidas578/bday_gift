# PixelGift User Guide 🎁✨

*A personalized, interactive smart LED matrix gift crafted with love for Madhumitha's 19th Birthday (January 24).*

---

## 1. Quick Start

### 1.1 Powering On
1. Plug a standard **5V USB power adapter** (5V 2A or 5V 3A recommended) into the ESP32 controller.
2. The 10×10 LED matrix will power on with a warm, steady **Cherry Red** glow at a safe low brightness.

### 1.2 Installing the Android App
1. Transfer `PixelGift-release.apk` (located in `app/build/outputs/apk/release/`) to your Android phone.
2. Open the file on your phone to install (allow "Install from unknown sources" if prompted).
3. Grant **Bluetooth** and **Nearby Devices** permissions when requested.

### 1.3 Pairing with Your Matrix
1. Open **PixelGift**.
2. Tap the **Pair** button (or navigate to **Device → Pair New Device**).
3. The app scans for nearby devices and displays `PixelGift-XXXX`.
4. Tap **Connect**. The phone will pair and bond seamlessly (**No PIN required** — pairing is secured automatically).
5. Once connected, the top status pill turns green: **Connected**.

---

## 2. Exploring the App

### 🎨 Studio Tab
- **Paint-Style Color Wheel**: Drag across the hue ring and saturation square to select any of 16.7 million shades.
- **HEX & RGB Inputs**: Fine-tune colors with precise numeric or hex values.
- **Brightness Slider**: Smoothly adjust matrix brightness from 1% to 100% (software capped at 2500 mA for safety).
- **Favorites & Palettes**: Save your favorite colors and create custom color palettes.
- **Hardware Scenes (8 Slots)**:
  - Tap any scene chip (**Scene 0–7**) to load a saved look instantly.
  - Long-press any scene chip to save the current color and effect directly to the matrix's onboard memory.

---

### ✨ Effects Browser
Choose from **60 base animation engines** generating over **500 unique visual looks**:
- **Static & Glow**: Solid, Breathing, Pulse, Heartbeat, Candle Flame, Fade-Two.
- **Rainbow & Gradients**: Rainbow Wave, Rainbow Cycle, Aurora, Sunset, Color Wheel.
- **Motion**: Scanner (Cylon), Matrix Rain, Snake Path, Theater Chase, Spiral, Comet.
- **Particles**: Fireworks, Twinkle, Fireflies, Stardust, Meteor Shower, Confetti.
- **Nature**: Campfire, Lava Flow, Ocean Waves, Northern Lights, Lightning.
- **Celebration**: Floating Hearts, Balloons, Birthday Candles, Party Strobe.

*Each effect features customizable speed, direction (Left, Right, Up, Down, In, Out), color palette, and mirror modes.*

---

### 🎂 Magic Tab
- **Birthday Show**:
  - Tap **Play Birthday Show** to launch Madhumitha's personalized celebration timeline (~98 seconds):
    1. **Golden Intro Sparkle** (0–6s)
    2. **Name Reveal Banner**: Scrolling `HAPPY BIRTHDAY MADHUMITHA ! ♥` (6–22s)
    3. **Floating Hearts & Balloons** in Cherry Red and Gold (22–36s)
    4. **Celebration Fireworks** bursting across the 10×10 grid (36–52s)
    5. **Rainbow Shimmer** (52–66s)
    6. **Custom Scrolling Greeting**: `HAPPY 19TH ♥ 24 JAN ★ WISHING YOU JOY & LOVE !` (66–94s)
    7. **Finale Glow**: Seamlessly settles into a soothing, soft Cherry Red ambient breathing glow.
  - *Offline resilience*: Even if your phone disconnects mid-show, the matrix continues running the entire show locally to completion.
- **Custom Scrolling Message**: Type any message, pick a scroll speed, and watch it glide across the LEDs.
- **Draw Canvas**: A 10×10 interactive pixel canvas — paint custom pixel art with your finger in real time.
- **Music Visualizer**: Real-time microphone audio reactive equalizer with instant attack and smooth decay.

---

### ⏰ Time Tab
- **Digital Clock**: Stacked 3×4 digits displaying hours and minutes clearly on the matrix. Auto-syncs time and timezone from your phone whenever connected.
- **Countdown Timer**: Select quick presets (1, 5, 10, 15, 30 min) or a custom duration. The matrix displays a shrinking progress ring and alerts you with a flashing visual sequence when finished.
- **Schedules**: Set up to 4 recurring automated daily actions (Turn On, Turn Off, Load Scene, or Sunrise Alarm).
- **Sleep Timer**: Gradually fades out the display over 15–60 minutes for bedtime.

---

### 📱 Device Tab & System Features
- **Auto-Reconnect**: The app automatically reconnects to your matrix whenever you open the app or bring your phone into range.
- **About PixelGift**: View Madhumitha's personal 19th birthday dedication card and full hardware specifications.
- **Home-Screen Widget**: Add the PixelGift widget to your Android home screen for instant 1-tap **Power On/Off** and **Cherry Red** quick scene access.
- **Quick-Settings Tile**: Swipe down your phone's notification shade and add the **PixelGift** tile for rapid toggle from anywhere on your phone.
- **OTA Updates**: Safely stream wireless firmware updates with dual-partition automatic rollback protection.

---

## 3. Power Safety & Thermal Guidelines

- **Power Rating**: 5V DC regulated power supply.
- **Current Protection**: The firmware enforces an active software current cap (`LED_MAX_MA = 2500` mA). Even at 100% white, current is mathematically limited to prevent power supply overload, USB port resets, or overheating.
- **Brightness Testing**:
  - **25% Brightness**: Ambient room temperature; cool to the touch.
  - **50% Brightness**: Comfortable warm glow; completely safe for continuous daily use in cardboard/acrylic enclosures.
  - **100% Brightness**: Vivid and bright; FastLED dynamically scales power consumption to remain within the 2.5 A safety envelope.

---

## 4. Hardware Recovery & Reset

- **Clear Bonds**: In the app under **Device → Clear Bonds**, or hold down the physical **BOOT button (GPIO 0)** on the ESP32 for **5 seconds** while powered on. The matrix will flash to confirm, allowing pairing with a new phone.
- **Data Persistence**: All saved scenes, schedules, brightness, color, and birthday greetings are safely stored in non-volatile flash memory (NVS) with a 2-second debounce to protect hardware lifespan.
