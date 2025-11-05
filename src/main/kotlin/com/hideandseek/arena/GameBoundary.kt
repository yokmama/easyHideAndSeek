package com.hideandseek.arena

/**
 * Game boundary data for respawn system
 *
 * Represents circular boundary with center point and radius
 */
data class GameBoundary(
    val centerX: Int,
    val centerZ: Int,
    val radius: Int
)
