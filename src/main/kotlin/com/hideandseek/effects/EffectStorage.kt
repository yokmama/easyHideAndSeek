package com.hideandseek.effects

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe storage for active effects
 *
 * Memory budget: ~2.5KB per player (target: <3KB)
 * - ConcurrentHashMap overhead: ~64 bytes per entry
 * - ActiveEffect: ~200 bytes per instance
 * - Metadata: ~200 bytes per effect (assuming 5 key-value pairs)
 * - Total per effect: ~464 bytes
 * - Max 5 effects per player: ~2.3KB + map overhead
 */
class EffectStorage {
    /**
     * Primary storage: PlayerId -> (EffectType -> ActiveEffect)
     * Nested map allows multiple effect types per player
     */
    private val effects = ConcurrentHashMap<UUID, ConcurrentHashMap<EffectType, ActiveEffect>>()

    /**
     * Store or update an active effect
     * @param effect The effect to store
     */
    fun putEffect(effect: ActiveEffect) {
        effects.computeIfAbsent(effect.playerId) { ConcurrentHashMap() }
            .put(effect.effectType, effect)
    }

    /**
     * Get a specific active effect for a player
     * @param playerId Player UUID
     * @param effectType Effect type to retrieve
     * @return ActiveEffect if exists, null otherwise
     */
    fun getEffect(playerId: UUID, effectType: EffectType): ActiveEffect? {
        return effects[playerId]?.get(effectType)
    }

    /**
     * Get all active effects for a player
     * @param playerId Player UUID
     * @return Map of effect types to active effects (empty if none)
     */
    fun getAllEffects(playerId: UUID): Map<EffectType, ActiveEffect> {
        return effects[playerId]?.toMap() ?: emptyMap()
    }

    /**
     * Remove a specific effect from a player
     * @param playerId Player UUID
     * @param effectType Effect type to remove
     * @return The removed effect, or null if not found
     */
    fun removeEffect(playerId: UUID, effectType: EffectType): ActiveEffect? {
        val playerEffects = effects[playerId] ?: return null
        val removed = playerEffects.remove(effectType)

        // Clean up empty player entry
        if (playerEffects.isEmpty()) {
            effects.remove(playerId)
        }

        return removed
    }

    /**
     * Remove all effects from a player
     * @param playerId Player UUID
     * @return List of removed effects (empty if none)
     */
    fun removeAllEffects(playerId: UUID): List<ActiveEffect> {
        val removed = effects.remove(playerId)?.values?.toList() ?: emptyList()
        return removed
    }

    /**
     * Check if a player has a specific effect
     * @param playerId Player UUID
     * @param effectType Effect type to check
     * @return true if effect exists and not expired
     */
    fun hasEffect(playerId: UUID, effectType: EffectType): Boolean {
        val effect = getEffect(playerId, effectType) ?: return false
        return !effect.isExpired()
    }

    /**
     * Get all player IDs with active effects
     * @return Set of player UUIDs
     */
    fun getAllPlayers(): Set<UUID> {
        return effects.keys.toSet()
    }

    /**
     * Clear all effects from storage
     * Used during game reset or plugin reload
     */
    fun clearAll() {
        effects.clear()
    }

    /**
     * Remove expired effects from storage
     * Should be called periodically (e.g., every 60 seconds)
     * @return Number of effects removed
     */
    fun cleanupExpired(): Int {
        var removed = 0

        effects.forEach { (playerId, playerEffects) ->
            val iterator = playerEffects.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.isExpired()) {
                    iterator.remove()
                    removed++
                }
            }

            // Clean up empty player entries
            if (playerEffects.isEmpty()) {
                effects.remove(playerId)
            }
        }

        return removed
    }

    /**
     * Estimate current memory usage in bytes
     * Used for monitoring and optimization
     * @return Estimated bytes (approximate)
     */
    fun estimateMemoryUsage(): Long {
        var total = 0L

        effects.forEach { (_, playerEffects) ->
            // Map overhead: ~64 bytes per entry
            total += 64

            playerEffects.forEach { (_, effect) ->
                // ActiveEffect: ~200 bytes
                total += 200
                // Metadata: ~40 bytes per key-value pair
                total += effect.metadata.size * 40L
                // Nested map entry: ~64 bytes
                total += 64
            }
        }

        return total
    }

    /**
     * Get storage statistics for debugging
     * @return Map of metric name to value
     */
    fun getStats(): Map<String, Any> {
        val totalEffects = effects.values.sumOf { it.size }
        val activeEffects = effects.values.sumOf { playerEffects ->
            playerEffects.values.count { !it.isExpired() }
        }

        return mapOf(
            "total_players" to effects.size,
            "total_effects" to totalEffects,
            "active_effects" to activeEffects,
            "expired_effects" to (totalEffects - activeEffects),
            "estimated_memory_kb" to (estimateMemoryUsage() / 1024)
        )
    }
}
