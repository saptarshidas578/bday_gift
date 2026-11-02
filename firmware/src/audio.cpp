#include "audio.h"

// Stubs for deferred audio hardware (Task B07b)
static bool globalMute = false;

void audioInit() {
    // Buzzer hardware deferred until component arrives (Task B07b)
}

void audioLoop() {
    // Non-blocking melody/tone step (no-op stub)
}

void playMelodyHappyBirthday() {
    // No-op stub: lights-only birthday show timing is maintained
}

void playTimerDoneBeeps() {
    // No-op stub: visual alert used instead of beeps
}

void stopMelody() {
    // No-op stub
}

bool isMelodyPlaying() {
    return false;
}

void setMute(bool mute) {
    globalMute = mute;
}

bool isMuted() {
    return globalMute;
}

