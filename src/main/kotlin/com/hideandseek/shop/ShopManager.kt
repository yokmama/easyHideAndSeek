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

    var messageManager: com.hideandseek.i18n.MessageManager? = null

    fun loadCategories() {
        categories.clear()
        val shopConfig = configManager.shop.getConfigurationSection("shop")
        if (shopConfig != null) {
            val config = com.hideandseek.config.ShopConfig(shopConfig)
            val loadedCategories = config.loadCategories()
            categories.addAll(loadedCategories)

            // T015: Replace disguise-blocks category items with dynamically generated tier-based items
            val disguiseCategory = categories.find { it.id == "disguise-blocks" }
            if (disguiseCategory != null) {
                val dynamicDisguiseItems = config.getDisguiseBlockItems()
                // Replace the category with new items
                val updatedCategory = disguiseCategory.copy(items = dynamicDisguiseItems)
                categories.removeIf { it.id == "disguise-blocks" }
                categories.add(updatedCategory)
                plugin.logger.info("Loaded ${dynamicDisguiseItems.size} disguise block items from tier configuration")
            }

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

    fun openMainMenu(player: Player, playerRole: String? = null) {
        if (categories.isEmpty()) {
            MessageUtil.send(player, "&cShop is not available")
            return
        }

        // Debug logging
        player.sendMessage("§e[DEBUG] openMainMenu called")
        player.sendMessage("§e[DEBUG] Player role: $playerRole")
        player.sendMessage("§e[DEBUG] Total categories: ${categories.size}")
        categories.forEach { cat ->
            player.sendMessage("§e[DEBUG] Category: ${cat.id}, roleFilter: ${cat.roleFilter}, slot: ${cat.slot}")
        }

        // T050: Use localized shop title based on player role
        val titleKey = when (playerRole?.uppercase()) {
            "SEEKER" -> "shop.title.seeker"
            "HIDER" -> "shop.title.hider"
            else -> "shop.title.hider" // Default to hider shop
        }
        val title = messageManager?.getRawMessage(player, titleKey) ?: "&a&lShop Menu"
        val inventory = Bukkit.createInventory(null, 54, MessageUtil.colorize(title))

        // T022: Filter categories by player role
        val visibleCategories = if (playerRole != null) {
            categories.filter { it.isVisibleTo(playerRole) }
        } else {
            categories
        }

        player.sendMessage("§e[DEBUG] Visible categories: ${visibleCategories.size}")
        visibleCategories.forEach { cat ->
            player.sendMessage("§e[DEBUG] Visible: ${cat.id}")
        }

        for (category in visibleCategories) {
            if (category.slot in 0..53) {
                // Count role-filtered items if role is specified
                val itemCount = if (playerRole != null) {
                    category.getItemsForRole(playerRole).size
                } else {
                    category.items.size
                }

                val item = ItemBuilder(category.icon)
                    .displayName(category.displayName)
                    .lore(listOf(
                        "&7Click to view items",
                        "&7$itemCount items available"
                    ))
                    .persistentData(categoryKey, PersistentDataType.STRING, category.id)
                    .build()

                inventory.setItem(category.slot, item)
            }
        }

        // T052: Localized Close button
        val closeButtonName = messageManager?.getRawMessage(player, "ui.button.close") ?: "&c&lClose"
        val closeButton = ItemBuilder(Material.BARRIER)
            .displayName(closeButtonName)
            .lore(listOf("&7Click to close"))
            .persistentData(itemIdKey, PersistentDataType.STRING, "close")
            .build()
        inventory.setItem(49, closeButton)

        player.openInventory(inventory)
    }

    fun openCategory(player: Player, categoryId: String, playerRole: String? = null) {
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

        // T022: Filter items by player role
        var visibleItems = if (playerRole != null) {
            category.getItemsForRole(playerRole)
        } else {
            category.items
        }

        // T057: Sort items by price within categories (progressive discovery)
        // Lower price items appear first for easier access
        visibleItems = visibleItems.sortedBy { it.getEffectivePrice() }

        // T016: Group disguise blocks by camouflage tier
        if (categoryId == "disguise-blocks") {
            buildDisguiseBlocksGUI(inventory, visibleItems, category.id)
        } else {
            // Standard item layout for non-disguise categories
            for (shopItem in visibleItems) {
                if (shopItem.slot in 0..53) {
                    // T025: Build restriction indicator lore
                    val restrictionLore = buildRestrictionLore(shopItem)

                    val effectivePrice = shopItem.getEffectivePrice()
                    val item = ItemBuilder(shopItem.material)
                        .displayName(shopItem.displayName)
                        .lore(
                            shopItem.lore +
                            restrictionLore +
                            listOf(
                                "",
                                if (effectivePrice == 0) "&aFree!" else "&7Price: &e$effectivePrice coins",
                                "&aClick to purchase"
                            )
                        )
                        .persistentData(itemIdKey, PersistentDataType.STRING, shopItem.id)
                        .persistentData(categoryKey, PersistentDataType.STRING, category.id)
                        .build()

                    inventory.setItem(shopItem.slot, item)
                }
            }
        }

        // T052: Localized Back button
        val backButtonName = messageManager?.getRawMessage(player, "ui.button.back") ?: "&e&lBack"
        val backButton = ItemBuilder(Material.ARROW)
            .displayName(backButtonName)
            .lore(listOf("&7Return to main menu"))
            .persistentData(itemIdKey, PersistentDataType.STRING, "back")
            .build()
        inventory.setItem(45, backButton)

        // T052: Localized Close button
        val closeButtonName = messageManager?.getRawMessage(player, "ui.button.close") ?: "&c&lClose"
        val closeButton = ItemBuilder(Material.BARRIER)
            .displayName(closeButtonName)
            .lore(listOf("&7Click to close"))
            .persistentData(itemIdKey, PersistentDataType.STRING, "close")
            .build()
        inventory.setItem(49, closeButton)

        player.openInventory(inventory)
    }

    /**
     * T016: Build GUI for disguise blocks category with tier grouping
     */
    private fun buildDisguiseBlocksGUI(
        inventory: org.bukkit.inventory.Inventory,
        items: List<ShopItem>,
        categoryId: String
    ) {
        // Group items by camouflage tier
        val itemsByTier = items.groupBy { it.camouflageTier }

        var currentSlot = 0
        val tiers = listOf(
            CamouflageTier.HIGH_VISIBILITY,
            CamouflageTier.MEDIUM_VISIBILITY,
            CamouflageTier.LOW_VISIBILITY
        )

        for (tier in tiers) {
            val tierItems = itemsByTier[tier] ?: continue

            // Add tier separator (colored glass pane)
            if (currentSlot > 0) {
                currentSlot = ((currentSlot / 9) + 1) * 9 // Move to next row
            }

            val separatorColor = when (tier) {
                CamouflageTier.HIGH_VISIBILITY -> Material.LIME_STAINED_GLASS_PANE
                CamouflageTier.MEDIUM_VISIBILITY -> Material.YELLOW_STAINED_GLASS_PANE
                CamouflageTier.LOW_VISIBILITY -> Material.ORANGE_STAINED_GLASS_PANE
            }

            val separator = ItemBuilder(separatorColor)
                .displayName(tier.getDisplayName())
                .lore(listOf(
                    tier.getFormattedDescription(),
                    "",
                    if (tier.basePrice == 0) "&aFree!" else "&ePrice: &6${tier.basePrice} coins"
                ))
                .build()

            inventory.setItem(currentSlot, separator)
            currentSlot++

            // Add tier items
            for (shopItem in tierItems) {
                if (currentSlot >= 45) break // Reserve bottom row for navigation

                val effectivePrice = shopItem.getEffectivePrice()
                val item = ItemBuilder(shopItem.material)
                    .displayName(shopItem.displayName)
                    .lore(
                        shopItem.lore +
                        listOf(
                            "",
                            "&7Tier: ${tier.getDisplayName()}",
                            if (effectivePrice == 0) "&aFree!" else "&7Price: &e$effectivePrice coins",
                            "&aClick to purchase"
                        )
                    )
                    .persistentData(itemIdKey, PersistentDataType.STRING, shopItem.id)
                    .persistentData(categoryKey, PersistentDataType.STRING, categoryId)
                    .build()

                inventory.setItem(currentSlot, item)
                currentSlot++
            }
        }
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

    /**
     * T025/T056: Build restriction indicator lore lines for shop items
     */
    private fun buildRestrictionLore(item: ShopItem): List<String> {
        val lore = mutableListOf<String>()

        // T056: Power Item indicator (800+ coins)
        val effectivePrice = item.getEffectivePrice()
        if (effectivePrice >= 800) {
            lore.add("")
            lore.add("&6⚡ パワーアイテム &6⚡")
        }

        // Usage restriction indicator
        when (item.usageRestriction) {
            UsageRestriction.SEEK_PHASE_ONLY -> {
                lore.add("")
                lore.add("&cRequires: Seek phase")
            }
            UsageRestriction.IN_GAME_ONLY -> {
                lore.add("")
                lore.add("&cRequires: Active game")
            }
            UsageRestriction.NOT_WHILE_CAPTURED -> {
                lore.add("")
                lore.add("&cCannot use while captured")
            }
            UsageRestriction.ONCE_PER_GAME -> {
                lore.add("")
                lore.add("&cOne use per game")
            }
            UsageRestriction.ALWAYS -> {
                // No restriction indicator
            }
        }

        // Purchase limit indicator
        if (item.maxPurchases != null) {
            lore.add("&7Limit: &e${item.maxPurchases} per game")
        }

        // Cooldown indicator
        if (item.cooldown != null) {
            lore.add("&7Cooldown: &e${item.cooldown}s")
        }

        return lore
    }
}
