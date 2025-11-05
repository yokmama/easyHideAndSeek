package com.hideandseek.respawn

import org.bukkit.Location

/**
 * Result of a respawn operation
 */
sealed class RespawnResult {
    /**
     * Respawn successful
     *
     * @property location The location where player was respawned
     */
    data class Success(val location: Location) : RespawnResult()

    /**
     * Respawn failed
     *
     * @property reason The reason for failure
     */
    data class Failure(val reason: RespawnFailureReason) : RespawnResult()
}

/**
 * Reasons why a respawn operation might fail
 */
enum class RespawnFailureReason {
    /** Player is not in an active game */
    PLAYER_NOT_IN_GAME,

    /** Could not find a safe location after maximum attempts */
    NO_SAFE_LOCATION_FOUND,

    /** Game is not active or not found */
    GAME_NOT_ACTIVE,

    /** Teleport operation failed */
    TELEPORT_FAILED
}
