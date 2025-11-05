package com.hideandseek.listeners

import com.hideandseek.game.GameManager
import com.hideandseek.game.PlayerRole
import com.hideandseek.spectator.SpectatorManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Handles spectator-specific events
 */
class SpectatorEventListener(
    private val spectatorManager: SpectatorManager,
    private val gameManager: GameManager
) : Listener {

    /**
     * Restore spectator state when player joins
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        spectatorManager.restoreSpectatorState(player)
    }

    /**
     * Handle spectator cleanup on quit
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player

        // Remove from spectator team if in team
        val spectatorTeam = spectatorManager.getSpectatorTeam()
        if (spectatorTeam.contains(player)) {
            spectatorTeam.removePlayer(player)
        }

        // Check win condition if player was in active game
        val game = gameManager.activeGame ?: return
        val playerData = game.players[player.uniqueId] ?: return

        // If a hider quit, check if all hiders are gone
        if (playerData.role == PlayerRole.HIDER && !playerData.isCaptured) {
            game.players.remove(player.uniqueId)

            val remainingHiders = game.getHiders()
            if (remainingHiders.isEmpty()) {
                gameManager.endGame(com.hideandseek.game.GameResult.SEEKER_WIN)
            }
        }
    }
}
