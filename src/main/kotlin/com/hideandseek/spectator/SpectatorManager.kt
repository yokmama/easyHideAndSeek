package com.hideandseek.spectator

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

/**
 * Core spectator mode management logic
 */
class SpectatorManager(private val plugin: Plugin) {

    private val spectatorConfig: SpectatorConfig = SpectatorConfig(plugin)
    private lateinit var spectatorTeam: SpectatorTeam

    /**
     * Initialize the spectator manager
     */
    fun initialize() {
        spectatorConfig.load()

        // Get main scoreboard and create spectator team
        val mainScoreboard = Bukkit.getScoreboardManager().mainScoreboard
        spectatorTeam = SpectatorTeam(mainScoreboard)

        plugin.logger.info("SpectatorManager initialized")
    }

    /**
     * Shutdown the spectator manager
     */
    fun shutdown() {
        spectatorTeam.unregister()
        plugin.logger.info("SpectatorManager shutdown")
    }

    /**
     * Check if a player is in spectator mode
     *
     * @param uuid Player UUID
     * @return true if spectator mode is enabled
     */
    fun isSpectator(uuid: UUID): Boolean {
        return spectatorConfig.isSpectator(uuid)
    }

    /**
     * Toggle spectator mode for a player
     *
     * @param player Player to toggle
     * @param enabled Whether to enable or disable spectator mode
     */
    fun toggleSpectator(player: Player, enabled: Boolean) {
        spectatorConfig.setSpectator(player.uniqueId, enabled)

        if (enabled) {
            spectatorTeam.addPlayer(player)
        } else {
            spectatorTeam.removePlayer(player)
        }
    }

    /**
     * Apply spectator mode effects to a player
     * Sets ADVENTURE gamemode with invisibility and flight
     *
     * @param player Player to apply effects to
     */
    fun applySpectatorMode(player: Player) {
        player.gameMode = GameMode.ADVENTURE
        player.allowFlight = true
        player.isFlying = true

        // Apply invisibility effect
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.INVISIBILITY,
                Int.MAX_VALUE,
                0,
                false,
                false
            )
        )

        // Disable collision
        player.isCollidable = false

        // Set flight speed from config
        val flightSpeed = plugin.config.getDouble("spectator.flight-speed", 1.5)
        player.flySpeed = (flightSpeed / 10.0).toFloat().coerceIn(0.0f, 1.0f)
    }

    /**
     * Remove spectator mode effects from a player
     *
     * @param player Player to remove effects from
     */
    fun removeSpectatorMode(player: Player) {
        player.allowFlight = false
        player.isFlying = false
        player.removePotionEffect(PotionEffectType.INVISIBILITY)
        player.isCollidable = true
        player.flySpeed = 0.1f // Default flight speed

        // Gamemode will be set by game logic when joining
    }

    /**
     * Restore spectator state for a player (called on join)
     *
     * @param player Player who joined
     */
    fun restoreSpectatorState(player: Player) {
        if (isSpectator(player.uniqueId)) {
            spectatorTeam.addPlayer(player)
        }
    }

    /**
     * Get the spectator team
     *
     * @return SpectatorTeam instance
     */
    fun getSpectatorTeam(): SpectatorTeam {
        return spectatorTeam
    }

    /**
     * Get all spectator UUIDs
     *
     * @return Set of UUIDs with spectator mode enabled
     */
    fun getAllSpectators(): Set<UUID> {
        return spectatorConfig.getAllSpectators()
    }

    /**
     * Get spectator config
     *
     * @return SpectatorConfig instance
     */
    fun getConfig(): SpectatorConfig {
        return spectatorConfig
    }
}
