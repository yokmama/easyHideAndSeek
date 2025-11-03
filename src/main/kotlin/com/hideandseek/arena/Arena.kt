package com.hideandseek.arena

import org.bukkit.Location
import org.bukkit.World

/**
 * Represents a configured game arena
 *
 * Contains all spatial information needed to run a game:
 * - Play area boundaries
 * - Spawn points for each role
 * - World reference
 */
data class Arena(
    val name: String,          // Unique identifier (used in commands)
    val displayName: String,   // Human-readable name
    val world: World,          // Bukkit World reference
    val boundaries: ArenaBoundaries,  // Play area definition
    val spawns: ArenaSpawns    // Player spawn locations
) {
    /**
     * Check if a location is within arena boundaries
     *
     * @param location Location to check
     * @return True if within bounds
     */
    fun isInBounds(location: Location): Boolean {
        return boundaries.contains(location)
    }
}
