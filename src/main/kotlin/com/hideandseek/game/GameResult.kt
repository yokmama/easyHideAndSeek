package com.hideandseek.game

enum class GameResult {
    SEEKER_WIN,   // All hiders captured
    HIDER_WIN,    // Time expired with hiders remaining
    CANCELLED     // Admin force-stop
}
