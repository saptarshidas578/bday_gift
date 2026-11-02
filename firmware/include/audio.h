#pragma once

#include <Arduino.h>

// Audio module interface (Task B07/B07b)
// Currently no-op stubs until buzzer hardware arrives (B07b)

void audioInit();
void audioLoop();
void playMelodyHappyBirthday();
void playTimerDoneBeeps();
void stopMelody();
bool isMelodyPlaying();
void setMute(bool mute);
bool isMuted();

