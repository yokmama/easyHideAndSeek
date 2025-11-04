package com.hideandseek.effects

import java.time.Instant
import java.util.UUID

/**
 * Represents an active effect applied to a player
 *
 * @property playerId UUID of the player this effect is applied to
 * @property effectType Type of effect (VISION, GLOW, SPEED, REACH)
 * @property startTime When the effect started
 * @property endTime When the effect expires
 * @property intensity Effect strength multiplier (e.g., 1.5 for REACH)
 * @property taskId BukkitScheduler task ID for cleanup (null if not scheduled)
 * @property metadata Additional effect-specific data (e.g., original view distance)
 */
data class ActiveEffect(
    val playerId: UUID,
    val effectType: EffectType,
    val startTime: Instant,
    val endTime: Instant,
    val intensity: Double = 1.0,
    val taskId: Int? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Get remaining duration in seconds
     * @return Seconds remaining until expiration (0 if expired)
     */
    fun getRemainingSeconds(): Long {
        val now = Instant.now()
        if (now.isAfter(endTime)) return 0
        return java.time.Duration.between(now, endTime).seconds
    }

    /**
     * Check if effect has expired
     * @return true if current time is after endTime
     */
    fun isExpired(): Boolean {
        return Instant.now().isAfter(endTime)
    }

    /**
     * Get total duration in seconds
     * @return Total duration from start to end
     */
    fun getTotalDuration(): Long {
        return java.time.Duration.between(startTime, endTime).seconds
    }

    /**
     * Get progress as percentage (0.0 to 1.0)
     * @return 0.0 at start, 1.0 at expiration
     */
    fun getProgress(): Double {
        val total = getTotalDuration()
        if (total == 0L) return 1.0

        val elapsed = java.time.Duration.between(startTime, Instant.now()).seconds
        return (elapsed.toDouble() / total.toDouble()).coerceIn(0.0, 1.0)
    }

    /**
     * Create a copy with updated taskId
     * Used when scheduling cleanup task after creation
     */
    fun withTaskId(newTaskId: Int): ActiveEffect {
        return copy(taskId = newTaskId)
    }

    /**
     * Get metadata value by key with type casting
     */
    inline fun <reified T> getMetadata(key: String): T? {
        return metadata[key] as? T
    }
}
