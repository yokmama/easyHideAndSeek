package com.hideandseek.config

import com.hideandseek.shop.ShopAction
import com.hideandseek.shop.ShopCategory
import com.hideandseek.shop.ShopItem
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection

class ShopConfig(private val config: ConfigurationSection) {

    fun loadCategories(): List<ShopCategory> {
        val categories = mutableListOf<ShopCategory>()
        val categoriesSection = config.getConfigurationSection("categories") ?: return categories

        for (categoryKey in categoriesSection.getKeys(false)) {
            val categorySection = categoriesSection.getConfigurationSection(categoryKey) ?: continue
            
            try {
                val category = deserializeCategory(categoryKey, categorySection)
                categories.add(category)
            } catch (e: Exception) {
                println("Failed to load shop category '$categoryKey': ${e.message}")
            }
        }

        return categories
    }

    private fun deserializeCategory(id: String, section: ConfigurationSection): ShopCategory {
        val displayName = section.getString("display-name") ?: id
        val iconName = section.getString("icon") ?: "GRASS_BLOCK"
        val icon = try {
            Material.valueOf(iconName.uppercase())
        } catch (e: IllegalArgumentException) {
            Material.GRASS_BLOCK
        }
        val slot = section.getInt("slot", 10)

        val items = mutableListOf<ShopItem>()
        val itemsSection = section.getConfigurationSection("items")
        
        if (itemsSection != null) {
            for (itemKey in itemsSection.getKeys(false)) {
                val itemSection = itemsSection.getConfigurationSection(itemKey) ?: continue
                try {
                    val item = deserializeItem(itemKey, itemSection)
                    items.add(item)
                } catch (e: Exception) {
                    println("Failed to load shop item '$itemKey': ${e.message}")
                }
            }
        }

        return ShopCategory(id, displayName, icon, slot, items)
    }

    private fun deserializeItem(id: String, section: ConfigurationSection): ShopItem {
        val materialName = section.getString("material") ?: "STONE"
        val material = try {
            Material.valueOf(materialName.uppercase())
        } catch (e: IllegalArgumentException) {
            Material.STONE
        }

        val displayName = section.getString("display-name") ?: id
        val lore = section.getStringList("lore")
        val slot = section.getInt("slot", 0)
        val price = section.getInt("price", 0)

        val action = ShopAction.Disguise(material)

        return ShopItem(id, material, displayName, lore, slot, price, action)
    }

    fun getShopItemMaterial(): Material {
        val materialName = config.getString("shop-item.material", "EMERALD") ?: "EMERALD"
        return try {
            Material.valueOf(materialName.uppercase())
        } catch (e: IllegalArgumentException) {
            Material.EMERALD
        }
    }

    fun getShopItemDisplayName(): String {
        return config.getString("shop-item.display-name", "&a&lShop") ?: "&a&lShop"
    }

    fun getShopItemLore(): List<String> {
        return config.getStringList("shop-item.lore")
    }

    fun getShopItemSlot(): Int {
        return config.getInt("shop-item.slot", 8)
    }

    fun getShopItemGlow(): Boolean {
        return config.getBoolean("shop-item.glow", true)
    }
}
