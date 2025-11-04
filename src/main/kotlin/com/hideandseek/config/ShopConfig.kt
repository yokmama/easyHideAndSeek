package com.hideandseek.config

import com.hideandseek.effects.EffectType
import com.hideandseek.shop.ShopAction
import com.hideandseek.shop.ShopCategory
import com.hideandseek.shop.ShopItem
import com.hideandseek.shop.UsageRestriction
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
        val roleFilter = section.getString("role-filter")  // "SEEKER" or "HIDER" or null

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

        return ShopCategory(id, displayName, icon, slot, items, roleFilter)
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

        // Parse effect metadata if present
        val effectTypeStr = section.getString("effect-type")
        val effectType = effectTypeStr?.let {
            try {
                EffectType.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                println("Invalid effect type '$it' for item '$id', ignoring")
                null
            }
        }

        val effectDuration = section.getInt("effect-duration", 0).takeIf { it > 0 }
        val effectIntensity = section.getDouble("effect-intensity", 0.0).takeIf { it > 0.0 }

        // Parse taunt metadata if present
        val tauntType = section.getString("taunt-type")
        val tauntBonus = section.getInt("taunt-bonus", 0).takeIf { it > 0 }

        // Parse purchase restrictions
        val roleFilter = section.getString("role-filter")  // "SEEKER" or "HIDER" or null
        val cooldown = section.getInt("cooldown", 0).takeIf { it > 0 }
        val maxPurchases = section.getInt("max-purchases", 0).takeIf { it > 0 }

        val usageRestrictionStr = section.getString("usage-restriction") ?: "ALWAYS"
        val usageRestriction = try {
            UsageRestriction.valueOf(usageRestrictionStr.uppercase())
        } catch (e: IllegalArgumentException) {
            println("Invalid usage restriction '$usageRestrictionStr' for item '$id', using ALWAYS")
            UsageRestriction.ALWAYS
        }

        // Determine action type
        val action = when {
            effectType != null && effectDuration != null && effectIntensity != null -> {
                ShopAction.UseEffectItem(effectType, effectDuration, effectIntensity)
            }
            tauntType != null && tauntBonus != null -> {
                ShopAction.UseTauntItem(tauntType, tauntBonus, material)
            }
            else -> {
                ShopAction.Disguise(material)
            }
        }

        val item = ShopItem(
            id = id,
            material = material,
            displayName = displayName,
            lore = lore,
            slot = slot,
            price = price,
            action = action,
            effectType = effectType,
            effectDuration = effectDuration,
            effectIntensity = effectIntensity,
            tauntType = tauntType,
            tauntBonus = tauntBonus,
            roleFilter = roleFilter,
            cooldown = cooldown,
            maxPurchases = maxPurchases,
            usageRestriction = usageRestriction
        )

        // Validate item configuration
        item.validate()

        return item
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
