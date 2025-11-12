package com.hideandseek.listeners

import com.hideandseek.game.GameManager
import com.hideandseek.game.PlayerRole
import com.hideandseek.respawn.RespawnFailureReason
import com.hideandseek.respawn.RespawnManager
import com.hideandseek.respawn.RespawnResult
import com.hideandseek.utils.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.plugin.Plugin

/**
 * Listener for player death events during gameplay
 *
 * Responsibilities:
 * - Seeker vs Seeker PK combat resolution with strength comparison
 * - Respawns dead players at random safe locations while preserving their role
 */
class PlayerDeathListener(
    private val plugin: Plugin,
    private val gameManager: GameManager,
    private val respawnManager: RespawnManager
) : Listener {

    /**
     * Handle player death events (HIGHEST priority to handle seeker PK first)
     *
     * Priority flow:
     * 1. Check for seeker vs seeker PK combat (strength-based resolution)
     * 2. Otherwise, schedule respawn for normal deaths
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val victim = event.entity
        val killer = victim.killer

        // Check if player is in an active game
        val game = gameManager.activeGame ?: return

        // Check if this player is part of the game
        if (!game.players.containsKey(victim.uniqueId)) {
            return
        }

        // Keep inventory on death during game
        event.keepInventory = true
        event.keepLevel = true
        event.drops.clear()
        event.droppedExp = 0
        plugin.logger.info("[ItemProtection] Keep inventory enabled for ${victim.name} (in-game death)")

        // Handle seeker vs seeker PK combat if applicable
        if (killer != null && game.players.containsKey(killer.uniqueId)) {
            val victimData = game.players[victim.uniqueId]
            val killerData = game.players[killer.uniqueId]

            // Check if this is seeker vs seeker combat
            if (victimData != null && killerData != null &&
                victimData.role == PlayerRole.SEEKER && killerData.role == PlayerRole.SEEKER) {

                // Handle seeker vs seeker PK
                handleSeekerVsSeekerPK(killer, victim, killerData, victimData, game, event)
                return
            }
        }

        // Schedule respawn after 1 tick delay for normal deaths
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

    /**
     * Handle seeker vs seeker PK combat based on strength comparison.
     *
     * Logic:
     * - Compare killer's strength vs victim's strength
     * - If killer strength < victim strength: Killer reverts to HIDER (forced reversion as punishment)
     * - If killer strength >= victim strength: Victim becomes SPECTATOR (normal defeat)
     */
    private fun handleSeekerVsSeekerPK(
        killer: org.bukkit.entity.Player,
        victim: org.bukkit.entity.Player,
        killerData: com.hideandseek.game.PlayerGameData,
        victimData: com.hideandseek.game.PlayerGameData,
        game: com.hideandseek.game.Game,
        event: PlayerDeathEvent
    ) {
        val strengthManager = (plugin as? com.hideandseek.HideAndSeekPlugin)?.seekerStrengthManager
        if (strengthManager == null) {
            plugin.logger.warning("[SeekerPK] SeekerStrengthManager not available")
            return
        }

        val killerStrength = strengthManager.getStrength(killer.uniqueId)
        val victimStrength = strengthManager.getStrength(victim.uniqueId)
        val comparison = strengthManager.compareStrength(killer.uniqueId, victim.uniqueId)

        plugin.logger.info("[SeekerPK] ${killer.name} (strength: $killerStrength) killed ${victim.name} (strength: $victimStrength)")

        // Prevent death drops
        event.drops.clear()
        event.droppedExp = 0
        event.deathMessage = null

        if (comparison < 0) {
            // Killer is WEAKER: FORCED REVERSION to HIDER (punishment)
            plugin.logger.info("[SeekerPK] ${killer.name} is weaker - reverting to HIDER as punishment")

            // Restore victim's health (they survive the attack)
            Bukkit.getScheduler().runTask(plugin, Runnable {
                victim.spigot().respawn()
                victim.health = victim.maxHealth
                victim.fireTicks = 0
                victim.fallDistance = 0.0f
                victim.foodLevel = 20
            })

            // Notify both players
            MessageUtil.send(killer, "&a自分より強い鬼を倒し、人間に戻った！")
            MessageUtil.send(victim, "&c${killer.name} があなたを倒しましたが、あなたの方が強かったため ${killer.name} が人間に戻りました！")

            // Broadcast to all players
            game.players.values.forEach { playerData ->
                Bukkit.getPlayer(playerData.uuid)?.let { player ->
                    if (player.uniqueId != killer.uniqueId && player.uniqueId != victim.uniqueId) {
                        MessageUtil.send(player, "&e${killer.name} &7が強い鬼 &e${victim.name} &7を倒し、人間に戻った！")
                    }
                }
            }

            // Play sound effects
            killer.playSound(killer.location, org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            victim.playSound(victim.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f)

            // Revert killer to hider
            Bukkit.getScheduler().runTask(plugin, Runnable {
                gameManager.revertSeekerToHider(killer, killerData, game)
            })

        } else {
            // Killer is STRONGER or EQUAL: Victim is defeated
            plugin.logger.info("[SeekerPK] ${killer.name} is stronger/equal - ${victim.name} is defeated")

            // Respawn victim and convert to spectator
            Bukkit.getScheduler().runTask(plugin, Runnable {
                victim.spigot().respawn()
                victim.health = victim.maxHealth
                victim.fireTicks = 0
                victim.fallDistance = 0.0f
                victim.foodLevel = 20

                // Notify both players
                MessageUtil.send(killer, "&a${victim.name} を倒しました！ &7(あなたの方が強い)")
                MessageUtil.send(victim, "&c${killer.name} に倒されました... &7(相手の方が強かった)")

                // Broadcast to all players
                game.players.values.forEach { playerData ->
                    Bukkit.getPlayer(playerData.uuid)?.let { player ->
                        if (player.uniqueId != killer.uniqueId && player.uniqueId != victim.uniqueId) {
                            MessageUtil.send(player, "&c${killer.name} &7が &c${victim.name} &7を倒した！ &7(鬼同士の戦い)")
                        }
                    }
                }

                // Play sound effects
                killer.playSound(killer.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
                victim.playSound(victim.location, org.bukkit.Sound.ENTITY_VILLAGER_HURT, 1.0f, 0.8f)

                // Convert victim to spectator
                victimData.role = PlayerRole.SPECTATOR
                victimData.isCaptured = true

                // Reset victim's strength
                strengthManager.resetStrength(victim.uniqueId)

                // Update victim to spectator mode using SpectatorManager
                val spectatorManager = (plugin as? com.hideandseek.HideAndSeekPlugin)?.spectatorManager
                spectatorManager?.applySpectatorMode(victim)

                plugin.logger.info("[SeekerPK] ${victim.name} converted to SPECTATOR after being defeated by ${killer.name}")
            })
        }
    }

    /**
     * Handle player respawn events to restore scoreboard and control respawn location
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player

        // Check if player is in an active game
        val game = gameManager.activeGame ?: return

        // Check if this player is part of the game
        if (!game.players.containsKey(player.uniqueId)) {
            return
        }

        // Override respawn location to prevent default bed spawn behavior
        // Find a safe respawn location within game boundaries
        val respawnLocation = respawnManager.findSafeRandomLocation(
            world = game.arena.world,
            center = org.bukkit.Location(
                game.arena.world,
                game.arena.boundaries.center.x,
                64.0,
                game.arena.boundaries.center.z
            ),
            radius = 50,
            boundaries = game.arena.boundaries
        )

        if (respawnLocation != null) {
            // Set custom respawn location to override bed spawn
            event.respawnLocation = respawnLocation
            plugin.logger.info("[Respawn] Override respawn location for ${player.name} to prevent bed spawn at ${respawnLocation.blockX}, ${respawnLocation.blockY}, ${respawnLocation.blockZ}")
        } else {
            // Fallback to arena spawn if no safe location found
            event.respawnLocation = org.bukkit.Location(
                game.arena.world,
                game.arena.boundaries.center.x,
                64.0,
                game.arena.boundaries.center.z
            )
            plugin.logger.warning("[Respawn] No safe location found, using arena center for ${player.name}")
        }

        // Restore scoreboard after respawn (delay to ensure player is fully loaded)
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val gameScoreboard = (plugin as? com.hideandseek.HideAndSeekPlugin)?.gameScoreboard
            gameScoreboard?.addPlayer(player, game)
            plugin.logger.info("[Scoreboard] Restored scoreboard for ${player.name} after respawn")
        }, 1L)
    }
}
