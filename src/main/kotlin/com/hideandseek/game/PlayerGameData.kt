package com.hideandseek.game

import java.util.UUID

data class PlayerGameData(
    val uuid: UUID,
    var role: PlayerRole,
    var isCaptured: Boolean = false,
    var captureCount: Int = 0,
    var backup: PlayerBackup? = null,
    val originalRole: PlayerRole = role,  // Store original role at game start
    var hiderPoints: Int = 0,              // Points earned as Hider
    var seekerPoints: Int = 0              // Points earned as Seeker
) {
    fun capture() {
        require(role == PlayerRole.HIDER) { "Only hiders can be captured" }
        isCaptured = true
        role = PlayerRole.SPECTATOR
    }

    fun recordCapture() {
        require(role == PlayerRole.SEEKER) { "Only seekers can record captures" }
        captureCount++
    }
}
