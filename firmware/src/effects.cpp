#include "effects.h"
#include "matrix.h"
#include "storage.h"

static EffectEntry effectTable[75];
static size_t registeredCount = 0;

void effectsInit() {
    registeredCount = 0;

    // --- Category 1: Static / Glow ---
    effectTable[registeredCount++] = {1,  "Solid",             "Static/glow",      renderSolid};
    effectTable[registeredCount++] = {2,  "Breathing",         "Static/glow",      renderBreathing};
    effectTable[registeredCount++] = {3,  "Pulse",             "Static/glow",      renderPulse};
    effectTable[registeredCount++] = {4,  "Heartbeat",         "Static/glow",      renderHeartbeat};
    effectTable[registeredCount++] = {5,  "Candle",            "Static/glow",      renderCandle};
    effectTable[registeredCount++] = {11, "Strobe-Soft",       "Static/glow",      renderStrobeSoft};
    effectTable[registeredCount++] = {12, "Fade-Two",          "Static/glow",      renderFadeTwo};
    effectTable[registeredCount++] = {13, "Flicker",           "Static/glow",      renderFlicker};

    // --- Category 2: Rainbow / Gradient ---
    effectTable[registeredCount++] = {6,  "Rainbow Wave",      "Rainbow/gradient", renderRainbowWave};
    effectTable[registeredCount++] = {7,  "Rainbow Cycle",     "Rainbow/gradient", renderRainbowCycle};
    effectTable[registeredCount++] = {14, "Diagonal Gradient", "Rainbow/gradient", renderDiagonalGradient};
    effectTable[registeredCount++] = {15, "Radial Gradient",   "Rainbow/gradient", renderRadialGradient};
    effectTable[registeredCount++] = {16, "Aurora",            "Rainbow/gradient", renderAurora};
    effectTable[registeredCount++] = {17, "Sunset",            "Rainbow/gradient", renderSunset};
    effectTable[registeredCount++] = {18, "Palette Scroll",    "Rainbow/gradient", renderPaletteScroll};
    effectTable[registeredCount++] = {19, "Color Wheel Spin",  "Rainbow/gradient", renderColorWheelSpin};

    // --- Category 3: Motion ---
    effectTable[registeredCount++] = {8,  "Scanner",           "Motion",           renderScanner};
    effectTable[registeredCount++] = {20, "Chase",             "Motion",           renderChase};
    effectTable[registeredCount++] = {21, "Wipe",              "Motion",           renderWipe};
    effectTable[registeredCount++] = {22, "Bounce",            "Motion",           renderBounce};
    effectTable[registeredCount++] = {23, "Spiral",            "Motion",           renderSpiral};
    effectTable[registeredCount++] = {24, "Radar",             "Motion",           renderRadar};
    effectTable[registeredCount++] = {25, "Rain",              "Motion",           renderRain};
    effectTable[registeredCount++] = {26, "Waterfall",         "Motion",           renderWaterfall};
    effectTable[registeredCount++] = {27, "Snake Path",        "Motion",           renderSnakePath};
    effectTable[registeredCount++] = {28, "Comet",             "Motion",           renderComet};
    effectTable[registeredCount++] = {29, "Theater Chase",     "Motion",           renderTheaterChase};
    effectTable[registeredCount++] = {30, "Row Sweep",         "Motion",           renderRowSweep};

    // --- Category 4: Particles ---
    effectTable[registeredCount++] = {9,  "Sparkle",           "Particles",        renderSparkle};
    effectTable[registeredCount++] = {31, "Twinkle",           "Particles",        renderTwinkle};
    effectTable[registeredCount++] = {32, "Fireflies",         "Particles",        renderFireflies};
    effectTable[registeredCount++] = {33, "Fireworks",         "Particles",        renderFireworks};
    effectTable[registeredCount++] = {34, "Snow",              "Particles",        renderSnow};
    effectTable[registeredCount++] = {35, "Confetti",          "Particles",        renderConfetti};
    effectTable[registeredCount++] = {36, "Meteor",            "Particles",        renderMeteor};
    effectTable[registeredCount++] = {37, "Bubbles",           "Particles",        renderBubbles};
    effectTable[registeredCount++] = {38, "Glitter",           "Particles",        renderGlitter};
    effectTable[registeredCount++] = {39, "Stardust",          "Particles",        renderStardust};

    // --- Category 5: Nature / Fire ---
    effectTable[registeredCount++] = {10, "Fire",              "Nature/fire",      renderFire};
    effectTable[registeredCount++] = {40, "Lava",              "Nature/fire",      renderLava};
    effectTable[registeredCount++] = {41, "Ocean",             "Nature/fire",      renderOcean};
    effectTable[registeredCount++] = {42, "Lightning",         "Nature/fire",      renderLightning};
    effectTable[registeredCount++] = {43, "Northern Lights",   "Nature/fire",      renderNorthernLights};
    effectTable[registeredCount++] = {44, "Embers",            "Nature/fire",      renderEmbers};
    effectTable[registeredCount++] = {45, "Storm",             "Nature/fire",      renderStorm};
    effectTable[registeredCount++] = {46, "Wind",              "Nature/fire",      renderWind};
    effectTable[registeredCount++] = {67, "Bioluminescent Ocean", "Nature/fire",   renderBioOcean};

    // --- Category 6: Geometric ---
    effectTable[registeredCount++] = {47, "Ripple",            "Geometric",        renderRipple};
    effectTable[registeredCount++] = {48, "Expanding Squares", "Geometric",        renderExpandingSquares};
    effectTable[registeredCount++] = {49, "Plasma",            "Geometric",        renderPlasma};
    effectTable[registeredCount++] = {50, "Kaleidoscope",      "Geometric",        renderKaleidoscope};
    effectTable[registeredCount++] = {51, "Tunnel",            "Geometric",        renderTunnel};
    effectTable[registeredCount++] = {52, "Checker",           "Geometric",        renderChecker};
    effectTable[registeredCount++] = {53, "Diamonds",          "Geometric",        renderDiamonds};
    effectTable[registeredCount++] = {54, "Pulse Rings",       "Geometric",        renderPulseRings};
    effectTable[registeredCount++] = {55, "Noise Field",       "Geometric",        renderNoiseField};

    // --- Category 7: Retro ---
    effectTable[registeredCount++] = {56, "Matrix Code",       "Retro",            renderMatrixCode};
    effectTable[registeredCount++] = {57, "Game of Life",      "Retro",            renderGameOfLife};
    effectTable[registeredCount++] = {58, "Snake Game Demo",   "Retro",            renderSnakeGameDemo};
    effectTable[registeredCount++] = {59, "Pong Demo",         "Retro",            renderPongDemo};
    effectTable[registeredCount++] = {60, "Equalizer Demo",    "Retro",            renderEqualizerDemo};

    // --- Category 8: Celebration ---
    effectTable[registeredCount++] = {61, "Hearts",            "Celebration",      renderHearts};
    effectTable[registeredCount++] = {62, "Balloons",          "Celebration",      renderBalloons};
    effectTable[registeredCount++] = {63, "Candles",           "Celebration",      renderCandles};
    effectTable[registeredCount++] = {64, "Confetti Burst",    "Celebration",      renderConfettiBurst};
    effectTable[registeredCount++] = {65, "Party Strobe",      "Celebration",      renderPartyStrobe};
    effectTable[registeredCount++] = {66, "Star Burst",        "Celebration",      renderStarBurst};
}

const EffectEntry* getEffectById(uint8_t id) {
    for (size_t i = 0; i < registeredCount; i++) {
        if (effectTable[i].id == id) {
            return &effectTable[i];
        }
    }
    return nullptr;
}

const EffectEntry* getEffectByIndex(size_t index) {
    if (index < registeredCount) {
        return &effectTable[index];
    }
    return nullptr;
}

size_t getEffectCount() {
    return registeredCount;
}

void effectsRender(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    const EffectEntry* entry = getEffectById(params.base);
    if (entry && entry->render) {
        entry->render(t_ms, params, leds);
    } else {
        renderSolid(t_ms, params, leds);
    }
}

CRGB getEffectColor(const EffectParams& p, uint32_t t_ms, uint8_t index, CRGB fallback) {
    uint8_t spd = p.speed ? p.speed : 50;

    switch (p.colorMode) {
        case 1: { // Dual color blend
            CRGB col1 = (p.c1 == CRGB(0,0,0)) ? fallback : p.c1;
            CRGB col2 = (p.c2 == CRGB(0,0,0)) ? CRGB::Cyan : p.c2;
            uint8_t ratio = sin8((uint8_t)((t_ms * spd / 200) + (index * 20)));
            return blend(col1, col2, ratio);
        }
        case 2: { // Palette
            if (state.paletteCount > 0) {
                uint16_t total = (uint16_t)((t_ms * spd / 30) + (index * (256 / state.paletteCount)));
                uint8_t step = (total >> 8) % state.paletteCount;
                uint8_t nextStep = (step + 1) % state.paletteCount;
                uint8_t frac = total & 0xFF;
                return blend(state.palette[step], state.palette[nextStep], frac);
            }
            return (p.c1 == CRGB(0,0,0)) ? fallback : p.c1;
        }
        case 3: { // Rainbow
            uint8_t hue = (uint8_t)((t_ms * spd / 35) + (index * 25));
            return CHSV(hue, 240, 255);
        }
        case 0: // Single color
        default: {
            if (p.c1 != CRGB(0,0,0)) {
                return p.c1;
            }
            return fallback;
        }
    }
}
