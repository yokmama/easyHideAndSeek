package com.hideandseek.spectator

import java.util.UUID

/**
 * Represents a player's spectator mode state
 *
 * @property uuid Player unique identifier
 * @property enabled Whether spectator mode is currently enabled
 * @property lastUpdated Timestamp of last state change (milliseconds since epoch)
 */
data class SpectatorState(
    val uuid: UUID,
    val enabled: Boolean,
    val lastUpdated: Long = System.currentTimeMillis()
)
