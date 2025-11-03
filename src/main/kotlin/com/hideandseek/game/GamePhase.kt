package com.hideandseek.game

enum class GamePhase {
    WAITING,      // Players joining
    PREPARATION,  // Hiders hiding (30s), seekers blind
    SEEKING,      // Main game phase
    ENDED         // Cleanup and results
}
