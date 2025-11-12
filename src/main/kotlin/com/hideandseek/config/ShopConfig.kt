package com.hideandseek.config

import com.hideandseek.effects.EffectType
import com.hideandseek.shop.*
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection

class ShopConfig(private val config: ConfigurationSection) {

    // NEW: T008 - Store disguise blocks mapping
    private val disguiseBlocksMap: Map<CamouflageTier, List<Material>> by lazy {
        loadDisguiseBlocks()
    }

    /**
     * T009: Load disguise blocks configuration from disguise-blocks section
     * Returns mapping of CamouflageTier to Material list
     */
    private fun loadDisguiseBlocks(): Map<CamouflageTier, List<Material>> {
        val result = mutableMapOf<CamouflageTier, List<Material>>()
        val disguiseSection = config.getConfigurationSection("disguise-blocks") ?: return emptyMap()

        // Load high-visibility tier
        disguiseSection.getConfigurationSection("high-visibility")?.let { section ->
            val blocks = section.getStringList("blocks").mapNotNull { blockName ->
                try {
                    Material.valueOf(blockName.uppercase())
                } catch (e: IllegalArgumentException) {
                    println("Invalid material '$blockName' in high-visibility tier")
                    null
                }
            }
            result[CamouflageTier.HIGH_VISIBILITY] = blocks
        }

        // Load medium-visibility tier
        disguiseSection.getConfigurationSection("medium-visibility")?.let { section ->
            val blocks = section.getStringList("blocks").mapNotNull { blockName ->
                try {
                    Material.valueOf(blockName.uppercase())
                } catch (e: IllegalArgumentException) {
                    println("Invalid material '$blockName' in medium-visibility tier")
                    null
                }
            }
            result[CamouflageTier.MEDIUM_VISIBILITY] = blocks
        }

        // Load low-visibility tier
        disguiseSection.getConfigurationSection("low-visibility")?.let { section ->
            val blocks = section.getStringList("blocks").mapNotNull { blockName ->
                try {
                    Material.valueOf(blockName.uppercase())
                } catch (e: IllegalArgumentException) {
                    println("Invalid material '$blockName' in low-visibility tier")
                    null
                }
            }
            result[CamouflageTier.LOW_VISIBILITY] = blocks
        }

        return result
    }

    /**
     * T009: Get all disguise blocks as ShopItem list
     * Generates ShopItems for each configured disguise block
     */
    fun getDisguiseBlockItems(): List<ShopItem> {
        val items = mutableListOf<ShopItem>()
        var slotCounter = 0

        for ((tier, materials) in disguiseBlocksMap) {
            for (material in materials) {
                items.add(createDisguiseBlockItem(material, tier, slotCounter++))
            }
        }

        return items
    }

    /**
     * T013/T023: Helper method to create ShopItem for a disguise block
     */
    private fun createDisguiseBlockItem(
        material: Material,
        tier: CamouflageTier,
        slot: Int
    ): ShopItem {
        val materialName = material.name.lowercase().replace('_', ' ')
            .split(' ').joinToString(" ") { it.capitalize() }

        return ShopItem(
            id = "disguise-${material.name.lowercase()}",
            material = material,
            displayName = "§e$materialName",
            lore = listOf(
                "§7Click to disguise as this block",
                "",
                "§7Tier: ${tier.getDisplayName()}",
                tier.getFormattedDescription(),
                "",
                if (tier.basePrice == 0) "§aFree!" else "§eCost: §6${tier.basePrice} coins",
                "",
                "§cRequires: Active game"
            ),
            slot = slot,
            price = tier.basePrice,
            action = ShopAction.Disguise(material),
            roleFilter = "HIDER",
            camouflageTier = tier,
            usageRestriction = UsageRestriction.IN_GAME_ONLY  // T023: Disguise only during game
        )
    }

    /**
     * Get disguise blocks by tier
     */
    fun getDisguiseBlocksByTier(tier: CamouflageTier): List<Material> {
        return disguiseBlocksMap[tier] ?: emptyList()
    }

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

        // Parse item-type for GiveItem action
        val itemType = section.getString("item-type")
        val itemMaterial = section.getString("item-material")
        val itemAmount = section.getInt("item-amount", 1)

        println("[ShopConfig] Loading item '$id': itemType=$itemType, itemMaterial=$itemMaterial, itemAmount=$itemAmount")

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
        // IMPORTANT: Check item-type first before legacy taunt-type
        val action = when {
            (itemType == "TAUNT_ITEM" || itemType == "EQUIPMENT") && itemMaterial != null -> {
                // Give items to inventory (taunt items or equipment)
                val giveMaterial = try {
                    Material.valueOf(itemMaterial.uppercase())
                } catch (e: IllegalArgumentException) {
                    material // Fallback to display material
                }
                println("[ShopConfig] Item '$id' -> GiveItem($giveMaterial, $itemAmount) [itemType=$itemType, itemMaterial=$itemMaterial]")
                ShopAction.GiveItem(giveMaterial, itemAmount)
            }
            effectType != null && effectDuration != null && effectIntensity != null -> {
                println("[ShopConfig] Item '$id' -> UseEffectItem")
                ShopAction.UseEffectItem(effectType, effectDuration, effectIntensity)
            }
            tauntType != null && tauntBonus != null -> {
                println("[ShopConfig] Item '$id' -> UseTauntItem (legacy - DEPRECATED)")
                ShopAction.UseTauntItem(tauntType, tauntBonus, material)
            }
            else -> {
                println("[ShopConfig] Item '$id' -> Disguise (default)")
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
