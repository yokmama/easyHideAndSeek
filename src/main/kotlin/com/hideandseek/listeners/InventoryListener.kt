package com.hideandseek.listeners

import com.hideandseek.shop.ShopManager
import com.hideandseek.utils.MessageUtil
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent

class InventoryListener(
    private val shopManager: ShopManager
) : Listener {

    @EventHandler
    fun onPlayerInventoryClick(event: InventoryClickEvent) {
        val clickedItem = event.currentItem ?: return
        
        // Check if the clicked item is a shop item
        if (shopManager.isShopItem(clickedItem)) {
            // Allow clicking the shop item to open the menu
            // but prevent moving it around in inventory
            if (event.clickedInventory == event.whoClicked.inventory) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val item = event.itemDrop.itemStack
        
        // Prevent dropping shop items
        if (shopManager.isShopItem(item)) {
            event.isCancelled = true
            MessageUtil.send(event.player, "&cYou cannot drop this item")
        }
    }

    @EventHandler
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        val mainHandItem = event.mainHandItem
        val offHandItem = event.offHandItem
        
        // Prevent swapping shop items to off-hand
        if (shopManager.isShopItem(mainHandItem) || shopManager.isShopItem(offHandItem)) {
            event.isCancelled = true
        }
    }
}
