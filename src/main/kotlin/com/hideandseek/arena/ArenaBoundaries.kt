package com.hideandseek.arena

import org.bukkit.Location
import kotlin.math.max
import kotlin.math.min

/**
 * Arena boundary definition
 *
 * Defines rectangular play area with two corner positions
 */
data class ArenaBoundaries(
    val pos1: Location,
    val pos2: Location
) {
    /**
     * Calculate center point of the arena
     */
    val center: Location
        get() = Location(
            pos1.world,
            (pos1.x + pos2.x) / 2,
            (pos1.y + pos2.y) / 2,
            (pos1.z + pos2.z) / 2
        )

    /**
     * Calculate diagonal distance between corners
     * Used for WorldBorder size
     */
    val size: Double
        get() = pos1.distance(pos2)

    /**
     * Check if a location is within arena boundaries
     *
     * @param location Location to check
     * @return True if within bounds (X and Z only)
     */
    fun contains(location: Location): Boolean {
        if (location.world != pos1.world) return false

        val minX = min(pos1.x, pos2.x)
        val maxX = max(pos1.x, pos2.x)
        val minZ = min(pos1.z, pos2.z)
        val maxZ = max(pos1.z, pos2.z)

        return location.x in minX..maxX && location.z in minZ..maxZ
    }
}
