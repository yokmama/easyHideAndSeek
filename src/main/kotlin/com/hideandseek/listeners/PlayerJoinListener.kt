package com.hideandseek.listeners

import com.hideandseek.disguise.DisguiseManager
import com.hideandseek.effects.EffectManager
import com.hideandseek.game.WorldBorderBackup
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin

/**
 * Cleans up player state on join to prevent leftover game effects
 * from server crashes or forced shutdowns
 * Also handles auto-join to game
 */
class PlayerJoinListener(
    private val plugin: Plugin,
    private val disguiseManager: DisguiseManager,
    private val effectManager: EffectManager,
    private val gameManager: com.hideandseek.game.GameManager
) : Listener {

    private val cleanedWorlds = mutableSetOf<String>()

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Reset world border to default (only once per world per server session)
        val worldName = player.world.name
        if (!cleanedWorlds.contains(worldName)) {
            val defaultBorder = WorldBorderBackup.defaultBackup(player.world)
            defaultBorder.restore(player.world)
            cleanedWorlds.add(worldName)
        }

        // Remove any leftover disguise
        if (disguiseManager.isDisguised(player.uniqueId)) {
            disguiseManager.undisguise(player, "server_restart_cleanup")
        }

        // Clear all active effects managed by the effect system
        effectManager.removeAllEffects(player.uniqueId)

        // Remove all potion effects (in case any were left over)
        player.activePotionEffects.forEach { effect ->
            player.removePotionEffect(effect.type)
        }

        // Reset view distance to default (server default is usually 10)
        player.sendViewDistance = 10

        // Ensure player is in survival mode (unless they have permission for creative)
        if (player.gameMode != GameMode.SURVIVAL && !player.hasPermission("hideandseek.admin")) {
            player.gameMode = GameMode.SURVIVAL
        }

        // Auto-join game after a short delay (to ensure player is fully loaded)
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            autoJoinGame(player)
        }, 20L) // 1 second delay
    }

    private fun autoJoinGame(player: org.bukkit.entity.Player) {
        // Don't auto-join if player is already in a game
        if (gameManager.isInGame(player)) {
            return
        }

        // Show welcome title
        player.showTitle(
            net.kyori.adventure.title.Title.title(
                com.hideandseek.utils.MessageUtil.colorize("&e&lHide and Seek"),
                com.hideandseek.utils.MessageUtil.colorize("&7ようこそ！"),
                net.kyori.adventure.title.Title.Times.times(
                    java.time.Duration.ofMillis(500),
                    java.time.Duration.ofMillis(2000),
                    java.time.Duration.ofMillis(500)
                )
            )
        )

        // Auto-join
        gameManager.joinGame(player)
    }
}
