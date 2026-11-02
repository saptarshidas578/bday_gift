#include "effects.h"
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
                if (above.g > 15) {
                    above.g = qsub8(above.g, 25);
                    matrixSetPixel(x, y, above);
                }
            }
        }
        // Spawn green lead drops
        uint8_t rx = random8(MATRIX_WIDTH);
        matrixSetPixel(rx, 0, CRGB(180, 255, 180));
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
            matrixSetPixel(x, y, grid[x][y] ? getEffectColor(params, t_ms, 0, CRGB::LimeGreen) : CRGB::Black);
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
    matrixSetPixel(foodX, foodY, CRGB::Red);
    for (uint8_t i = 0; i < 8; i++) {
        matrixSetPixel(snakeX[i], snakeY[i], (i == 0) ? CRGB::Yellow : CRGB::Green);
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
    // Draw paddles
    for (int dy = -1; dy <= 1; dy++) {
        matrixSetPixel(0, p1Y + dy, CRGB::Blue);
        matrixSetPixel(MATRIX_WIDTH - 1, p2Y + dy, CRGB::Red);
    }
    // Draw ball
    matrixSetPixel((uint8_t)bx, (uint8_t)by, CRGB::White);
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
            CRGB col = (y < 4) ? CRGB::Green : (y < 7 ? CRGB::Yellow : CRGB::Red);
            matrixSetPixel(x, actualY, col);
        }
    }
}
