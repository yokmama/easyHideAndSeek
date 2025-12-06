package com.hideandseek.listeners

import com.hideandseek.shop.ShopManager
import com.hideandseek.utils.MessageUtil
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent

class InventoryListener(
    private val shopManager: ShopManager
) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInventoryClick(event: InventoryClickEvent) {
        val clickedItem = event.currentItem
        val cursorItem = event.cursor

        // Check if the clicked item is a shop item
        if (shopManager.isShopItem(clickedItem)) {
            // Cancel all click actions on shop items to prevent any movement
            event.isCancelled = true
            return
        }

        // Also check cursor item (item being moved)
        if (shopManager.isShopItem(cursorItem)) {
            event.isCancelled = true
            return
        }

        // Check hotbar swap (number keys 1-9)
        if (event.hotbarButton >= 0) {
            val player = event.whoClicked
            val hotbarItem = player.inventory.getItem(event.hotbarButton)
            if (shopManager.isShopItem(hotbarItem)) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryDrag(event: InventoryDragEvent) {
        // Check if the dragged item is a shop item
        val draggedItem = event.oldCursor
        if (shopManager.isShopItem(draggedItem)) {
            event.isCancelled = true
            return
        }

        // Also check the new items being placed
        for (item in event.newItems.values) {
            if (shopManager.isShopItem(item)) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryMoveItem(event: InventoryMoveItemEvent) {
        // Prevent hoppers or other blocks from moving shop items
        if (shopManager.isShopItem(event.item)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val item = event.itemDrop.itemStack

        // Prevent dropping shop items
        if (shopManager.isShopItem(item)) {
            event.isCancelled = true
            MessageUtil.send(event.player, "&cYou cannot drop this item")
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        val mainHandItem = event.mainHandItem
        val offHandItem = event.offHandItem

        // Prevent swapping shop items to off-hand
        if (shopManager.isShopItem(mainHandItem) || shopManager.isShopItem(offHandItem)) {
            event.isCancelled = true
        }
    }
}
