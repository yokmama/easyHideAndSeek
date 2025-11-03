package com.hideandseek.listeners

import com.hideandseek.disguise.DisguiseManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.Plugin

class PlayerMoveListener(
    private val disguiseManager: DisguiseManager
) : Listener {

    private var plugin: Plugin? = null

    fun setPlugin(plugin: Plugin) {
        this.plugin = plugin
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val to = event.to ?: return

        if (!disguiseManager.isDisguised(player.uniqueId)) {
            return
        }

        // Get disguise data to check the original location
        val disguiseData = disguiseManager.getActiveDisguises()[player.uniqueId] ?: return
        val disguiseLocation = disguiseData.blockLocation

        // Calculate distance from disguise location
        val dx = to.blockX - disguiseLocation.blockX
        val dy = to.blockY - disguiseLocation.blockY
        val dz = to.blockZ - disguiseLocation.blockZ
        val distance = kotlin.math.sqrt((dx * dx + dy * dy + dz * dz).toDouble())

        // Only undisguise if player moved more than 1 block away from disguise location
        if (distance > 1.0) {
            plugin?.logger?.info("[Disguise] Player ${player.name} moved ${String.format("%.2f", distance)} blocks from disguise location")
            disguiseManager.undisguise(player, "movement")
        }
    }
}
