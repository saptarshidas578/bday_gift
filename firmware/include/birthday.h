#pragma once

#include <Arduino.h>
#include <FastLED.h>

void birthdayInit();
void birthdayStart();
void birthdayStop();
bool isBirthdayRunning();
void birthdayRender(uint32_t now, CRGB* leds);

