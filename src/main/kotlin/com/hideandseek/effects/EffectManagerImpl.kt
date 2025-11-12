package com.hideandseek.effects

import com.hideandseek.utils.EffectScheduler
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger

/**
 * Implementation of EffectManager
 *
 * Manages active effects with:
 * - Thread-safe storage via EffectStorage
 * - Automatic expiration via EffectScheduler
 * - Overlap policy handling (REPLACE/EXTEND/REJECT/STACK)
 * - Effect-specific handlers for application/removal
 *
 * Performance characteristics:
 * - Effect application: O(1) - hashmap lookup + scheduler
 * - Effect removal: O(1) - hashmap removal + task cancel
 * - Memory: ~2.5KB per player (target: <3KB)
 */
class EffectManagerImpl(
    private val plugin: Plugin,
    private val storage: EffectStorage,
    private val scheduler: EffectScheduler,
    private val logger: Logger = plugin.logger
) : EffectManager {

    /**
     * Effect-specific handlers for applying visual/mechanical effects
     * Will be populated with specific handlers (VisionEffectHandler, etc.) in later tasks
     */
    private val effectHandlers = mutableMapOf<EffectType, EffectHandler>()

    /**
     * Register an effect handler for a specific type
     * Called during initialization in later phases
     */
    fun registerHandler(effectType: EffectType, handler: EffectHandler) {
        effectHandlers[effectType] = handler
    }

    override fun applyEffect(
        player: Player,
        effectType: EffectType,
        durationSeconds: Int,
        intensity: Double,
        overlapPolicy: OverlapPolicy,
        metadata: Map<String, Any>
    ): Boolean {
        try {
            // Validate parameters
            EffectValidator.validateEffectApplication(player, effectType, durationSeconds, intensity)

            // Check for existing effect
            val existingEffect = storage.getEffect(player.uniqueId, effectType)
            if (existingEffect != null && !existingEffect.isExpired()) {
                return handleOverlap(player, effectType, durationSeconds, intensity, overlapPolicy, metadata, existingEffect)
            }

            // Create and store new effect
            val startTime = Instant.now()
            val endTime = startTime.plusSeconds(durationSeconds.toLong())

            // T027: gameId should be passed from caller, for now use "unknown" as placeholder
            // TODO: Update callers to pass actual gameId
            var effect = ActiveEffect(
                playerId = player.uniqueId,
                effectType = effectType,
                gameId = metadata["gameId"] as? String ?: "unknown",
                startTime = startTime,
                endTime = endTime,
                intensity = intensity,
                taskId = null,
                metadata = metadata
            )

            // Schedule automatic expiration
            val taskId = scheduler.scheduleDelayed(durationSeconds) {
                removeEffect(player.uniqueId, effectType)
            }

            // Update effect with task ID
            effect = effect.withTaskId(taskId)

            // Apply visual/mechanical effect via handler
            // Pass effect with taskId so handlers can access it
            val handler = effectHandlers[effectType]
            player.sendMessage("§c[DEBUG] EffectManagerImpl: Applying effect $effectType")
            player.sendMessage("§c[DEBUG] Handler found: ${handler != null}")
            if (handler != null) {
                player.sendMessage("§c[DEBUG] Handler class: ${handler.javaClass.simpleName}")
                player.sendMessage("§c[DEBUG] Calling handler.apply()...")
                handler.apply(player, effect)
                player.sendMessage("§c[DEBUG] handler.apply() completed")
            } else {
                player.sendMessage("§c[DEBUG] No handler registered for $effectType!")
            }

            // Store effect (after handler might have modified metadata)
            storage.putEffect(effect)

            logger.fine("Applied $effectType to ${player.name} for ${durationSeconds}s (intensity: $intensity)")
            return true

        } catch (e: IllegalArgumentException) {
            return false
        }
    }

    /**
     * Handle overlap scenarios based on policy
     */
    private fun handleOverlap(
        player: Player,
        effectType: EffectType,
        durationSeconds: Int,
        intensity: Double,
        overlapPolicy: OverlapPolicy,
        metadata: Map<String, Any>,
        existingEffect: ActiveEffect
    ): Boolean {
        return when (overlapPolicy) {
            OverlapPolicy.REPLACE -> {
                // Cancel existing effect and apply new one
                removeEffect(player.uniqueId, effectType)
                applyEffect(player, effectType, durationSeconds, intensity, overlapPolicy, metadata)
            }

            OverlapPolicy.EXTEND -> {
                // Extend existing effect's duration
                val remainingSeconds = existingEffect.getRemainingSeconds()
                val newDuration = (remainingSeconds + durationSeconds).toInt()
                    .coerceAtMost(EffectValidator.getDurationRange().second)

                // Cancel and reapply with extended duration
                removeEffect(player.uniqueId, effectType)
                applyEffect(player, effectType, newDuration, intensity, OverlapPolicy.REPLACE, metadata)
            }

            OverlapPolicy.REJECT -> {
                // Keep existing effect, reject new one
                logger.fine("Rejected $effectType for ${player.name} (existing effect active)")
                false
            }

            OverlapPolicy.STACK -> {
                // Stack effects (not currently supported - treat as REPLACE)
                removeEffect(player.uniqueId, effectType)
                applyEffect(player, effectType, durationSeconds, intensity, OverlapPolicy.REPLACE, metadata)
            }
        }
    }

    override fun removeEffect(playerId: UUID, effectType: EffectType): Boolean {
        val effect = storage.removeEffect(playerId, effectType) ?: return false

        // Cancel scheduled task
        effect.taskId?.let { scheduler.cancelTask(it) }

        // Remove visual/mechanical effect via handler
        val handler = effectHandlers[effectType]
        if (handler != null) {
            plugin.server.getPlayer(playerId)?.let { player ->
                handler.remove(player, effect)
            }
        }

        logger.fine("Removed $effectType from player $playerId")
        return true
    }

    override fun removeAllEffects(playerId: UUID): Int {
        val effects = storage.removeAllEffects(playerId)

        effects.forEach { effect ->
            // Cancel scheduled tasks
            effect.taskId?.let { scheduler.cancelTask(it) }

            // Remove visual/mechanical effects via handlers
            val handler = effectHandlers[effect.effectType]
            if (handler != null) {
                plugin.server.getPlayer(playerId)?.let { player ->
                    handler.remove(player, effect)
                }
            }
        }

        logger.fine("Removed ${effects.size} effects from player $playerId")
        return effects.size
    }

    override fun hasEffect(playerId: UUID, effectType: EffectType): Boolean {
        return storage.hasEffect(playerId, effectType)
    }

    override fun getActiveEffect(playerId: UUID, effectType: EffectType): ActiveEffect? {
        return storage.getEffect(playerId, effectType)?.takeIf { !it.isExpired() }
    }

    override fun getRemainingTime(playerId: UUID, effectType: EffectType): Long {
        return storage.getEffect(playerId, effectType)?.getRemainingSeconds() ?: 0
    }

    override fun getActiveEffectTypes(playerId: UUID): Set<EffectType> {
        return storage.getAllEffects(playerId)
            .filterValues { !it.isExpired() }
            .keys
    }

    override fun getActiveEffects(playerId: UUID): List<ActiveEffect> {
        return storage.getAllEffects(playerId)
            .values
            .filter { !it.isExpired() }
            .toList()
    }

    override fun cleanupExpired(): Int {
        val cleaned = storage.cleanupExpired()
        if (cleaned > 0) {
            logger.fine("Cleaned up $cleaned expired effects")
        }
        return cleaned
    }

    override fun clearAll() {
        // Remove all effects and cancel scheduled tasks
        storage.getAllPlayers().forEach { playerId ->
            removeAllEffects(playerId)
        }
        storage.clearAll()
    }

    /**
     * Get storage statistics for debugging
     */
    fun getStats(): Map<String, Any> {
        val storageStats = storage.getStats()
        val schedulerStats = mapOf(
            "scheduled_tasks" to scheduler.getActiveTaskCount()
        )
        return storageStats + schedulerStats
    }
}

/**
 * Interface for effect-specific handlers
 * Each effect type (VISION, GLOW, SPEED, REACH) will have its own implementation
 */
interface EffectHandler {
    /**
     * Apply the effect to a player
     * @param player The player to apply the effect to
     * @param effect The effect data (includes intensity, metadata)
     */
    fun apply(player: Player, effect: ActiveEffect)

    /**
     * Remove the effect from a player
     * @param player The player to remove the effect from
     * @param effect The effect data (for cleanup context)
     */
    fun remove(player: Player, effect: ActiveEffect)
}
