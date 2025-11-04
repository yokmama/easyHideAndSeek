package com.hideandseek.shop

/**
 * Camouflage effectiveness tiers for disguise blocks
 *
 * This innovative pricing system inverts traditional rarity-based pricing:
 * - High-visibility blocks (easy to spot) = FREE
 * - Low-visibility blocks (blend into environment) = EXPENSIVE
 *
 * Creates strategic choice: pay for safety or play risky for free
 */
enum class CamouflageTier(
    val tierName: String,
    val basePrice: Int,
    val description: String
) {
    /**
     * High visibility blocks that stand out and are easy to spot
     * Examples: Melon, Pumpkin, Gold Block, Diamond Block
     * Price: FREE (0 coins)
     */
    HIGH_VISIBILITY(
        tierName = "High Visibility",
        basePrice = 0,
        description = "Easily spotted - Free to use"
    ),

    /**
     * Medium visibility blocks that are somewhat noticeable
     * Examples: Oak Log, Brick, Sandstone, Glass
     * Price: 50 coins
     */
    MEDIUM_VISIBILITY(
        tierName = "Medium Visibility",
        basePrice = 50,
        description = "Somewhat noticeable"
    ),

    /**
     * Low visibility blocks that blend into typical map environments
     * Examples: Stone, Cobblestone, Dirt, Grass Block
     * Price: 100 coins (most effective camouflage)
     */
    LOW_VISIBILITY(
        tierName = "Low Visibility",
        basePrice = 100,
        description = "Blends into environment"
    );

    /**
     * Get tier name formatted for display in GUI
     */
    fun getDisplayName(): String {
        return when (this) {
            HIGH_VISIBILITY -> "§c$tierName"  // Red
            MEDIUM_VISIBILITY -> "§e$tierName" // Yellow
            LOW_VISIBILITY -> "§a$tierName"    // Green
        }
    }

    /**
     * Get tier description with formatting
     */
    fun getFormattedDescription(): String {
        return "§7$description"
    }
}
