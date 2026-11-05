#include "effects.h"
#include "storage.h"
#include "matrix.h"

// 56. Matrix Code
void renderMatrixCode(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(180);

    static uint32_t lastRain = 0;
    if (t_ms - lastRain >= (600 / spd)) {
        lastRain = t_ms;
        // Shift columns down
        for (int y = MATRIX_HEIGHT - 1; y > 0; y--) {
            for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
                CRGB above = matrixGetPixel(x, y - 1);
                if (above.r > 15 || above.g > 15 || above.b > 15) {
                    above.nscale8(200);
                    matrixSetPixel(x, y, above);
                }
            }
        }
        // Spawn lead drops with chosen effect color / palette / rainbow
        uint8_t rx = random8(MATRIX_WIDTH);
        CRGB leadCol = getEffectColor(params, t_ms, rx * 25, CRGB::Green);
        matrixSetPixel(rx, 0, leadCol);
    }
}

// 57. Game of Life
void renderGameOfLife(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    static uint8_t grid[MATRIX_WIDTH][MATRIX_HEIGHT] = {0};
    static uint32_t lastLifeStep = 0;
    static uint32_t lastReseed = 0;

    // Reseed every 12 seconds
    if (t_ms - lastReseed >= 12000 || lastReseed == 0) {
        lastReseed = t_ms;
        for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
            for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
                grid[x][y] = (random8() < 90) ? 1 : 0;
            }
        }
    }

    uint8_t spd = params.speed ? params.speed : 40;
    if (t_ms - lastLifeStep >= (8000 / spd)) {
        lastLifeStep = t_ms;
        uint8_t next[MATRIX_WIDTH][MATRIX_HEIGHT] = {0};

        for (int y = 0; y < MATRIX_HEIGHT; y++) {
            for (int x = 0; x < MATRIX_WIDTH; x++) {
                uint8_t neighbors = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        int nx = (x + dx + MATRIX_WIDTH) % MATRIX_WIDTH;
                        int ny = (y + dy + MATRIX_HEIGHT) % MATRIX_HEIGHT;
                        if (grid[nx][ny]) neighbors++;
                    }
                }
                if (grid[x][y] && (neighbors == 2 || neighbors == 3)) {
                    next[x][y] = 1;
                } else if (!grid[x][y] && neighbors == 3) {
                    next[x][y] = 1;
                }
            }
        }
        memcpy(grid, next, sizeof(grid));
    }

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            if (grid[x][y]) {
                CRGB lifeCol = getEffectColor(params, t_ms, x * 15 + y * 15, CRGB::LimeGreen);
                matrixSetPixel(x, y, lifeCol);
            } else {
                CRGB deadCol = CRGB::Black;
                if (params.colorMode == 1 && params.c2 != CRGB(0,0,0)) {
                    deadCol = params.c2;
                    deadCol.nscale8(40);
                }
                matrixSetPixel(x, y, deadCol);
            }
        }
    }
}

// 58. Snake Game Demo
void renderSnakeGameDemo(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    static int8_t snakeX[8] = {4, 3, 2, 1, 0, 0, 0, 0};
    static int8_t snakeY[8] = {4, 4, 4, 4, 4, 4, 4, 4};
    static int8_t foodX = 7, foodY = 7;
    static uint32_t lastSnakeMove = 0;

    uint8_t spd = params.speed ? params.speed : 45;
    if (t_ms - lastSnakeMove >= (5000 / spd)) {
        lastSnakeMove = t_ms;

        // Shift body
        for (int i = 7; i > 0; i--) {
            snakeX[i] = snakeX[i - 1];
            snakeY[i] = snakeY[i - 1];
        }

        // Move head toward food
        if (snakeX[0] < foodX) snakeX[0]++;
        else if (snakeX[0] > foodX) snakeX[0]--;
        else if (snakeY[0] < foodY) snakeY[0]++;
        else if (snakeY[0] > foodY) snakeY[0]--;

        // Check food hit
        if (snakeX[0] == foodX && snakeY[0] == foodY) {
            foodX = random8(MATRIX_WIDTH);
            foodY = random8(MATRIX_HEIGHT);
        }
    }

    matrixClear();
    CRGB foodCol = (params.colorMode == 1 && params.c2 != CRGB(0,0,0)) ? params.c2 : CRGB::Red;
    matrixSetPixel(foodX, foodY, foodCol);
    for (uint8_t i = 0; i < 8; i++) {
        CRGB snakeCol = (i == 0)
            ? getEffectColor(params, t_ms, 0, CRGB::Yellow)
            : getEffectColor(params, t_ms, i * 28, CRGB::Green);
        matrixSetPixel(snakeX[i], snakeY[i], snakeCol);
    }
}

// 59. Pong Demo
void renderPongDemo(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    static float bx = 4.5f, by = 4.5f;
    static float vx = 0.18f, vy = 0.12f;
    static int8_t p1Y = 4, p2Y = 4;
    static uint32_t lastPongStep = 0;

    if (t_ms - lastPongStep >= 30) {
        lastPongStep = t_ms;
        bx += vx;
        by += vy;

        if (by <= 0 || by >= MATRIX_HEIGHT - 1) vy = -vy;

        // Paddle 1 (x=0)
        p1Y = constrain((int8_t)by, 1, MATRIX_HEIGHT - 2);
        if (bx <= 1.0f && abs((int8_t)by - p1Y) <= 1) vx = fabsf(vx);

        // Paddle 2 (x=9)
        p2Y = constrain((int8_t)by, 1, MATRIX_HEIGHT - 2);
        if (bx >= MATRIX_WIDTH - 2 && abs((int8_t)by - p2Y) <= 1) vx = -fabsf(vx);

        // Reset if out of bounds
        if (bx < 0 || bx >= MATRIX_WIDTH) {
            bx = 4.5f; by = 4.5f;
        }
    }

    matrixClear();
    CRGB p1Col = getEffectColor(params, t_ms, 0, CRGB::Blue);
    CRGB p2Col = (params.colorMode == 1 && params.c2 != CRGB(0,0,0)) ? params.c2 : getEffectColor(params, t_ms, 128, CRGB::Red);
    CRGB ballCol = CRGB::White;
    // Draw paddles
    for (int dy = -1; dy <= 1; dy++) {
        matrixSetPixel(0, p1Y + dy, p1Col);
        matrixSetPixel(MATRIX_WIDTH - 1, p2Y + dy, p2Col);
    }
    // Draw ball
    matrixSetPixel((uint8_t)bx, (uint8_t)by, ballCol);
}

// 60. Equalizer Demo
void renderEqualizerDemo(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    matrixClear();

    for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
        uint8_t h = inoise8(x * 60, (t_ms * spd / 20));
        uint8_t barH = map(h, 40, 220, 1, MATRIX_HEIGHT);
        barH = constrain(barH, 1, MATRIX_HEIGHT);

        for (uint8_t y = 0; y < barH; y++) {
            uint8_t actualY = MATRIX_HEIGHT - 1 - y;
            CRGB fallbackCol = (y < 4) ? CRGB::Green : (y < 7 ? CRGB::Yellow : CRGB::Red);
            CRGB col = getEffectColor(params, t_ms, x * 20 + y * 15, fallbackCol);
            matrixSetPixel(x, actualY, col);
        }
    }
}
