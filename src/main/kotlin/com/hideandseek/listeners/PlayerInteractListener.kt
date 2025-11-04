package com.hideandseek.listeners

import com.hideandseek.game.GameManager
import com.hideandseek.shop.ShopManager
import com.hideandseek.utils.MessageUtil
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class PlayerInteractListener(
    private val shopManager: ShopManager,
    private val gameManager: GameManager
) : Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return

        // Check if player right-clicked with the shop item
        if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
            if (shopManager.isShopItem(item)) {
                event.isCancelled = true

                // Check if player is in a game
                val game = gameManager.activeGame
                if (game == null) {
                    MessageUtil.send(player, "&cYou must be in a game to use the shop")
                    return
                }

                if (!game.players.containsKey(player.uniqueId)) {
                    MessageUtil.send(player, "&cYou are not in this game")
                    return
                }

                // Get player role and open the shop menu with role filtering
                val playerData = game.players[player.uniqueId]
                val playerRole = playerData?.role?.name  // "SEEKER", "HIDER", or "SPECTATOR"
                shopManager.openMainMenu(player, playerRole)
            }
        }
    }
}
