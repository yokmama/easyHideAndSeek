package com.hideandseek.listeners

import com.hideandseek.game.GameManager
import com.hideandseek.respawn.RespawnFailureReason
import com.hideandseek.respawn.RespawnManager
import com.hideandseek.respawn.RespawnResult
import com.hideandseek.utils.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.plugin.Plugin

/**
 * Listener for player death events during gameplay
 *
 * Respawns dead players at random safe locations while preserving their role
 */
class PlayerDeathListener(
    private val plugin: Plugin,
    private val gameManager: GameManager,
    private val respawnManager: RespawnManager
) : Listener {

    /**
     * Handle player death events
     *
     * Schedules respawn after 1 tick delay to allow death animation
     */
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity

        // Check if player is in an active game
        val game = gameManager.activeGame ?: return

        // Check if this player is part of the game
        if (!game.players.containsKey(player.uniqueId)) {
            return
        }

        // Schedule respawn after 1 tick delay
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            handleRespawn(event)
        }, 1L)
    }

    /**
     * Handle the actual respawn logic
     *
     * Called 1 tick after death to allow death animation to complete
     */
    private fun handleRespawn(event: PlayerDeathEvent) {
        val player = event.entity

        // Attempt respawn
        when (val result = respawnManager.respawnPlayer(player)) {
            is RespawnResult.Success -> {
                MessageUtil.send(player, "&aRespawned at safe location")

                if (plugin.logger.isLoggable(java.util.logging.Level.FINE)) {
                    plugin.logger.fine(
                        "Respawned ${player.name} at ${result.location.blockX}, ${result.location.blockY}, ${result.location.blockZ}"
                    )
                }
            }

            is RespawnResult.Failure -> {
                when (result.reason) {
                    RespawnFailureReason.NO_SAFE_LOCATION_FOUND -> {
                        // Fallback to random spawn
                        val game = gameManager.activeGame
                        if (game != null) {
                            val fallbackLocation = gameManager.getRandomSpawnLocation(game.arena)
                            player.teleport(fallbackLocation)
                            MessageUtil.send(player, "&eNo safe location found, respawned at random location")
                            plugin.logger.warning("No safe spawn location found for ${player.name}, using random spawn")
                        }
                    }

                    RespawnFailureReason.PLAYER_NOT_IN_GAME -> {
                        // Player not in game, do nothing
                        plugin.logger.fine("${player.name} died but is not in active game")
                    }

                    RespawnFailureReason.GAME_NOT_ACTIVE -> {
                        // Game not active, do nothing
                        plugin.logger.fine("${player.name} died but game is not active")
                    }

                    RespawnFailureReason.TELEPORT_FAILED -> {
                        // Teleport failed, log error
                        MessageUtil.send(player, "&cRespawn failed, please contact an administrator")
                        plugin.logger.severe("Teleport failed for ${player.name} during respawn")
                    }
                }
            }
        }
    }
}
