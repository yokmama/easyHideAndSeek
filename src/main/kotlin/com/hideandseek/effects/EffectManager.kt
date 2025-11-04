package com.hideandseek.effects

import org.bukkit.entity.Player
import java.util.UUID

/**
 * Interface for managing player effects in the seeker shop system
 *
 * Responsibilities:
 * - Apply time-limited effects to players
 * - Handle effect overlap policies (REPLACE/EXTEND/REJECT/STACK)
 * - Schedule automatic effect expiration
 * - Clean up expired effects
 * - Query active effects
 *
 * Performance targets:
 * - Effect application: <500ms
 * - Effect removal: <200ms
 * - Memory: <3KB per player
 */
interface EffectManager {
    /**
     * Apply an effect to a player
     *
     * @param player The player to apply the effect to
     * @param effectType Type of effect to apply
     * @param durationSeconds Duration in seconds (5-120)
     * @param intensity Effect strength multiplier (type-specific ranges)
     * @param overlapPolicy How to handle existing effects of the same type
     * @param metadata Additional effect-specific data
     * @return true if applied successfully, false if rejected
     * @throws IllegalArgumentException if duration/intensity out of range
     */
    fun applyEffect(
        player: Player,
        effectType: EffectType,
        durationSeconds: Int,
        intensity: Double = 1.0,
        overlapPolicy: OverlapPolicy = OverlapPolicy.REPLACE,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean

    /**
     * Remove a specific effect from a player
     *
     * @param playerId Player UUID
     * @param effectType Type of effect to remove
     * @return true if effect was removed, false if not found
     */
    fun removeEffect(playerId: UUID, effectType: EffectType): Boolean

    /**
     * Remove all effects from a player
     *
     * @param playerId Player UUID
     * @return Number of effects removed
     */
    fun removeAllEffects(playerId: UUID): Int

    /**
     * Check if a player has an active effect
     *
     * @param playerId Player UUID
     * @param effectType Type of effect to check
     * @return true if effect exists and not expired
     */
    fun hasEffect(playerId: UUID, effectType: EffectType): Boolean

    /**
     * Get an active effect for a player
     *
     * @param playerId Player UUID
     * @param effectType Type of effect to retrieve
     * @return ActiveEffect if exists, null otherwise
     */
    fun getActiveEffect(playerId: UUID, effectType: EffectType): ActiveEffect?

    /**
     * Get remaining time for an effect
     *
     * @param playerId Player UUID
     * @param effectType Type of effect
     * @return Remaining seconds, or 0 if not found/expired
     */
    fun getRemainingTime(playerId: UUID, effectType: EffectType): Long

    /**
     * Get all active effect types for a player
     *
     * @param playerId Player UUID
     * @return Set of effect types currently active (non-expired)
     */
    fun getActiveEffectTypes(playerId: UUID): Set<EffectType>

    /**
     * T028: Get all active effects for a player
     *
     * @param playerId Player UUID
     * @return List of ActiveEffect objects (non-expired only)
     */
    fun getActiveEffects(playerId: UUID): List<ActiveEffect>

    /**
     * Clean up expired effects from storage
     * Should be called periodically (e.g., every 60 seconds)
     *
     * @return Number of effects cleaned up
     */
    fun cleanupExpired(): Int

    /**
     * Clear all effects from all players
     * Used during game reset or plugin reload
     */
    fun clearAll()
}
