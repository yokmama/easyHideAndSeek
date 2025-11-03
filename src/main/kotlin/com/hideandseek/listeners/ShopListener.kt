package com.hideandseek.listeners

import com.hideandseek.game.GameManager
import com.hideandseek.shop.ShopManager
import com.hideandseek.utils.MessageUtil
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class ShopListener(
    private val shopManager: ShopManager,
    private val gameManager: GameManager
) : Listener {

    var disguiseManager: com.hideandseek.disguise.DisguiseManager? = null

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // Debug: Log ALL inventory clicks
        player.sendMessage("§8[Debug] Inventory click detected")

        val clickedItem = event.currentItem ?: return

        val view = event.view
        val title = view.title

        // Debug logging
        player.sendMessage("§7[Debug] Clicked item: ${clickedItem.type}")
        player.sendMessage("§7[Debug] Inventory title: $title")

        // Check if this is a shop inventory by checking if clicked item has shop metadata
        val itemId = shopManager.getItemIdFromItem(clickedItem)
        val categoryId = shopManager.getCategoryFromItem(clickedItem)

        player.sendMessage("§7[Debug] ItemId: $itemId, CategoryId: $categoryId")

        // If item has no shop metadata, it's not a shop item
        if (itemId == null && categoryId == null && !shopManager.isShopItem(clickedItem)) {
            player.sendMessage("§c[Debug] Not a shop item, ignoring")
            return
        }

        // Cancel all clicks in shop inventories
        event.isCancelled = true
        player.sendMessage("§a[Debug] Shop item detected!")

        // Handle special items (close, back)
        when (itemId) {
            "close" -> {
                player.closeInventory()
                return
            }
            "back" -> {
                shopManager.openMainMenu(player)
                return
            }
        }

        // Handle category selection (from main menu)
        if (categoryId != null && itemId == null) {
            shopManager.openCategory(player, categoryId)
            return
        }

        // Handle item purchase
        if (categoryId != null && itemId != null) {
            handleItemPurchase(player, categoryId, itemId)
            return
        }
    }

    private fun handleItemPurchase(player: Player, categoryId: String, itemId: String) {
        player.sendMessage("§e[Debug] handleItemPurchase called: category=$categoryId, item=$itemId")

        // Check if player is in a game
        val game = gameManager.activeGame
        if (game == null) {
            MessageUtil.send(player, "&cYou must be in a game to purchase items")
            player.closeInventory()
            return
        }

        if (!game.players.containsKey(player.uniqueId)) {
            MessageUtil.send(player, "&cYou are not in this game")
            player.closeInventory()
            return
        }

        // Get the shop item
        val shopItem = shopManager.getShopItem(categoryId, itemId)
        if (shopItem == null) {
            player.sendMessage("§c[Debug] Shop item not found!")
            MessageUtil.send(player, "&cItem not found")
            return
        }

        player.sendMessage("§a[Debug] Shop item found: ${shopItem.displayName}")

        // TODO: Implement economy check and deduction
        // For now, just execute the action

        // Execute the shop action
        when (val action = shopItem.action) {
            is com.hideandseek.shop.ShopAction.Disguise -> {
                player.sendMessage("§b[Debug] Disguise action triggered for: ${action.blockType}")
                val disguiseMgr = disguiseManager
                if (disguiseMgr != null) {
                    player.sendMessage("§b[Debug] DisguiseManager found, attempting disguise...")
                    val success = disguiseMgr.disguise(player, action.blockType)
                    player.sendMessage("§b[Debug] Disguise result: $success")
                    if (success) {
                        player.closeInventory()
                    }
                } else {
                    player.sendMessage("§c[Debug] DisguiseManager is NULL!")
                    MessageUtil.send(player, "&cDisguise system not available")
                    player.closeInventory()
                }
            }
            is com.hideandseek.shop.ShopAction.GoBack -> {
                shopManager.openMainMenu(player)
            }
            is com.hideandseek.shop.ShopAction.Close -> {
                player.closeInventory()
            }
        }
    }
}
