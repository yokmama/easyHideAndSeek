package com.hideandseek.arena

import org.bukkit.Location

/**
 * Arena spawn points for different roles
 *
 * Stores spawn locations with rotation (yaw/pitch)
 */
data class ArenaSpawns(
    val seeker: Location,  // Includes yaw/pitch for view direction
    val hider: Location    // Includes yaw/pitch for view direction
)
