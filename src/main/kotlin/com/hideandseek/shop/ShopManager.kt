package com.hideandseek.shop

import com.hideandseek.config.ConfigManager
import com.hideandseek.utils.ItemBuilder
import com.hideandseek.utils.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class ShopManager(
    private val plugin: Plugin,
    private val configManager: ConfigManager
) {
    private val categories = mutableListOf<ShopCategory>()
    private val shopItemKey: NamespacedKey = NamespacedKey(plugin, "shop_item")
    private val categoryKey: NamespacedKey = NamespacedKey(plugin, "shop_category")
    private val itemIdKey: NamespacedKey = NamespacedKey(plugin, "shop_item_id")

    fun loadCategories() {
        categories.clear()
        val shopConfig = configManager.shop.getConfigurationSection("shop")
        if (shopConfig != null) {
            val loadedCategories = com.hideandseek.config.ShopConfig(shopConfig).loadCategories()
            categories.addAll(loadedCategories)
            plugin.logger.info("Loaded ${categories.size} shop categories")
        } else {
            plugin.logger.warning("No shop configuration found")
        }
    }

    fun createShopItem(): ItemStack {
        val shopConfig = configManager.shop.getConfigurationSection("shop")
        if (shopConfig == null) {
            return ItemStack(Material.EMERALD)
        }

        val config = com.hideandseek.config.ShopConfig(shopConfig)
        val material = config.getShopItemMaterial()
        val displayName = config.getShopItemDisplayName()
        val lore = config.getShopItemLore()
        val glow = config.getShopItemGlow()

        val item = ItemBuilder(material)
            .displayName(displayName)
            .lore(lore)
            .persistentData(shopItemKey, PersistentDataType.BYTE, 1.toByte())

        if (glow) {
            item.glow(true)
        }

        return item.build()
    }

    fun isShopItem(item: ItemStack?): Boolean {
        if (item == null) return false
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(shopItemKey, PersistentDataType.BYTE)
    }

    fun openMainMenu(player: Player) {
        if (categories.isEmpty()) {
            MessageUtil.send(player, "&cShop is not available")
            return
        }

        val inventory = Bukkit.createInventory(null, 54, MessageUtil.colorize("&a&lShop Menu"))

        for (category in categories) {
            if (category.slot in 0..53) {
                val item = ItemBuilder(category.icon)
                    .displayName(category.displayName)
                    .lore(listOf(
                        "&7Click to view items",
                        "&7${category.items.size} items available"
                    ))
                    .persistentData(categoryKey, PersistentDataType.STRING, category.id)
                    .build()

                inventory.setItem(category.slot, item)
            }
        }

        val closeButton = ItemBuilder(Material.BARRIER)
            .displayName("&c&lClose")
            .lore(listOf("&7Click to close"))
            .persistentData(itemIdKey, PersistentDataType.STRING, "close")
            .build()
        inventory.setItem(49, closeButton)

        player.openInventory(inventory)
    }

    fun openCategory(player: Player, categoryId: String) {
        val category = categories.find { it.id == categoryId }
        if (category == null) {
            MessageUtil.send(player, "&cCategory not found")
            return
        }

        val inventory = Bukkit.createInventory(
            null,
            54,
            MessageUtil.colorize(category.displayName)
        )

        for (shopItem in category.items) {
            if (shopItem.slot in 0..53) {
                val item = ItemBuilder(shopItem.material)
                    .displayName(shopItem.displayName)
                    .lore(
                        shopItem.lore +
                        listOf(
                            "",
                            "&7Price: &e${shopItem.price} coins",
                            "&aClick to purchase"
                        )
                    )
                    .persistentData(itemIdKey, PersistentDataType.STRING, shopItem.id)
                    .persistentData(categoryKey, PersistentDataType.STRING, category.id)
                    .build()

                inventory.setItem(shopItem.slot, item)
            }
        }

        val backButton = ItemBuilder(Material.ARROW)
            .displayName("&e&lBack")
            .lore(listOf("&7Return to main menu"))
            .persistentData(itemIdKey, PersistentDataType.STRING, "back")
            .build()
        inventory.setItem(45, backButton)

        val closeButton = ItemBuilder(Material.BARRIER)
            .displayName("&c&lClose")
            .lore(listOf("&7Click to close"))
            .persistentData(itemIdKey, PersistentDataType.STRING, "close")
            .build()
        inventory.setItem(49, closeButton)

        player.openInventory(inventory)
    }

    fun getCategory(categoryId: String): ShopCategory? {
        return categories.find { it.id == categoryId }
    }

    fun getShopItem(categoryId: String, itemId: String): ShopItem? {
        val category = getCategory(categoryId) ?: return null
        return category.items.find { it.id == itemId }
    }

    fun getCategoryFromItem(item: ItemStack): String? {
        val meta = item.itemMeta ?: return null
        return meta.persistentDataContainer.get(categoryKey, PersistentDataType.STRING)
    }

    fun getItemIdFromItem(item: ItemStack): String? {
        val meta = item.itemMeta ?: return null
        return meta.persistentDataContainer.get(itemIdKey, PersistentDataType.STRING)
    }
}
