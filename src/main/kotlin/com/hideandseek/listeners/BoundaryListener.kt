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

        val center = boundaries.center
        val distance = to.distance(center)
        val maxDistance = boundaries.size / 2.0

        if (distance > maxDistance) {
            event.isCancelled = true

            val direction = center.toVector().subtract(to.toVector()).normalize()
            val safeLocation = center.clone().add(direction.multiply(-maxDistance * 0.9))
            safeLocation.y = to.y

            player.teleport(safeLocation)
            gameManager.plugin.logger.info("[Boundary] ${player.name} tried to leave arena: distance ${distance.toInt()} > max ${maxDistance.toInt()}")
            MessageUtil.send(player, "&cYou cannot leave the game area!")
        } else if (distance > maxDistance * 0.85) {
            MessageUtil.send(player, "&eWarning: Approaching boundary!")
        }
    }
}
