package com.hideandseek.arena

import org.bukkit.Location
import org.bukkit.World

/**
 * Represents a configured game arena
 *
 * Contains all spatial information needed to run a game:
 * - Play area boundaries
 * - World reference
 *
 * Players spawn randomly within the arena boundaries
 */
data class Arena(
    val name: String,          // Unique identifier (used in commands)
    val displayName: String,   // Human-readable name
    val world: World,          // Bukkit World reference
    val boundaries: ArenaBoundaries  // Play area definition
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

    /**
     * Get circular boundary approximation for respawn system
     */
    fun getCircularBoundary(): GameBoundary {
        val center = boundaries.center
        val radius = (boundaries.size / 2).toInt()
        return GameBoundary(
            centerX = center.blockX,
            centerZ = center.blockZ,
            radius = radius
        )
    }
}
