package com.hideandseek.listeners

import com.hideandseek.effects.EffectManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.logging.Logger

/**
 * Listens for game events to clean up effects appropriately
 *
 * Responsibilities:
 * - Remove effects when players quit
 * - Clear effects when games end (integration point for future game events)
 * - Manage effects during state transitions (e.g., capture)
 */
class EffectCleanupListener(
    private val effectManager: EffectManager,
    private val logger: Logger
) : Listener {

    /**
     * Remove all effects when a player quits
     * Prevents orphaned effects in storage
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val removedCount = effectManager.removeAllEffects(player.uniqueId)

        if (removedCount > 0) {
            logger.fine("Removed $removedCount effects from ${player.name} on quit")
        }
    }

    /**
     * Handle game end event
     * NOTE: This will be connected to actual GameEndEvent in Phase 8 (Polish and Integration)
     * For now, this is a placeholder showing the integration point
     */
    // Placeholder for future integration:
    // @EventHandler(priority = EventPriority.MONITOR)
    // fun onGameEnd(event: GameEndEvent) {
    //     // Clear effects for all players in the game
    //     event.game.getAllPlayers().forEach { playerId ->
    //         effectManager.removeAllEffects(playerId)
    //     }
    //     logger.info("Cleared all effects for game: ${event.game.id}")
    // }

    /**
     * Handle player capture event
     * NOTE: This will be connected to actual PlayerCapturedEvent in Phase 8
     * For now, this is a placeholder showing the integration point
     *
     * Decision: Keep effects active when captured (effects persist in spectate mode)
     * Rationale: If a seeker is captured (in a future team mode), their effects
     * should remain visible for educational/spectator purposes
     */
    // Placeholder for future integration:
    // @EventHandler(priority = EventPriority.MONITOR)
    // fun onPlayerCaptured(event: PlayerCapturedEvent) {
    //     // Keep effects active - no cleanup needed
    //     // Effects will naturally expire or be cleaned up on game end
    //     logger.fine("Player ${event.hider.name} captured - keeping effects active")
    // }

    /**
     * Handle game phase change event
     * NOTE: This will be connected to actual GamePhaseChangeEvent in Phase 8
     * For now, this is a placeholder showing the integration point
     *
     * When transitioning from SEEK phase back to PREPARATION:
     * - Clear all effects (new round starting)
     */
    // Placeholder for future integration:
    // @EventHandler(priority = EventPriority.MONITOR)
    // fun onPhaseChange(event: GamePhaseChangeEvent) {
    //     if (event.newPhase == GamePhase.PREPARATION && event.oldPhase == GamePhase.SEEK) {
    //         // New round starting - clear all effects
    //         event.game.getAllPlayers().forEach { playerId ->
    //             effectManager.removeAllEffects(playerId)
    //         }
    //         logger.info("Cleared all effects for new round in game: ${event.game.id}")
    //     }
    // }
}
