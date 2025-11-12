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
     * Used for WorldBorder size (diameter that covers entire rectangular area)
     */
    val size: Double
        get() {
            // WorldBorder is circular, so we need the diagonal distance
            // to ensure the entire rectangular area fits inside
            val dx = kotlin.math.abs(pos1.x - pos2.x)
            val dz = kotlin.math.abs(pos1.z - pos2.z)
            return kotlin.math.sqrt(dx * dx + dz * dz)
        }

    /**
     * Get the width (X-axis) of the rectangular area
     */
    val width: Double
        get() = kotlin.math.abs(pos1.x - pos2.x)

    /**
     * Get the depth (Z-axis) of the rectangular area
     */
    val depth: Double
        get() = kotlin.math.abs(pos1.z - pos2.z)

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

        // Add 1.0 margin to allow players to stand on boundary blocks
        // Player position is at center of their hitbox, so without margin
        // they would be pushed back before reaching the edge block
        val margin = 1.0

        return location.x >= minX - margin && location.x <= maxX + margin &&
               location.z >= minZ - margin && location.z <= maxZ + margin
    }
}
