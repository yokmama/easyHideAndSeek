package com.hideandseek.listeners

import com.hideandseek.game.GameManager
import com.hideandseek.game.GamePhase
import com.hideandseek.utils.MessageUtil
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class BoundaryListener(
    private val gameManager: GameManager
) : Listener {

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val to = event.to ?: return

        val game = gameManager.activeGame ?: return

        if (game.phase != GamePhase.SEEKING && game.phase != GamePhase.PREPARATION) {
            return
        }

        if (!game.players.containsKey(player.uniqueId)) {
            return
        }

        // Skip boundary check for seekers during PREPARATION phase
        // (They are frozen in waiting area and may be outside playing area)
        if (game.phase == GamePhase.PREPARATION) {
            val playerData = game.players[player.uniqueId]
            if (playerData?.role == com.hideandseek.game.PlayerRole.SEEKER) {
                return
            }
        }

        val arena = game.arena
        val boundaries = arena.boundaries

        // Use rectangular boundary check instead of circular distance
        if (!boundaries.contains(to)) {
            // Just cancel the movement, don't teleport
            // This prevents teleport loops and is more responsive
            event.isCancelled = true

            // Only show message occasionally to avoid spam (throttle to once per second)
            val now = System.currentTimeMillis()
            val lastWarning = lastBoundaryWarning[player.uniqueId] ?: 0L
            if (now - lastWarning > 1000) {
                lastBoundaryWarning[player.uniqueId] = now
                MessageUtil.send(player, "&cゲームエリア外には出られません!")

                // Debug logging (only when message is shown)
                gameManager.plugin.logger.info("[Boundary] ${player.name} tried to leave arena:")
                gameManager.plugin.logger.info("  Attempted position: (${to.blockX}, ${to.blockZ})")
                gameManager.plugin.logger.info("  Arena bounds: X[${kotlin.math.min(boundaries.pos1.blockX, boundaries.pos2.blockX)} to ${kotlin.math.max(boundaries.pos1.blockX, boundaries.pos2.blockX)}], Z[${kotlin.math.min(boundaries.pos1.blockZ, boundaries.pos2.blockZ)} to ${kotlin.math.max(boundaries.pos1.blockZ, boundaries.pos2.blockZ)}]")
            }
        }
    }

    companion object {
        // Track last boundary warning time per player to avoid message spam
        private val lastBoundaryWarning = mutableMapOf<java.util.UUID, Long>()
    }
}
