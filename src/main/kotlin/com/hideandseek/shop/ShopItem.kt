package com.hideandseek.shop

import com.hideandseek.effects.EffectType
import org.bukkit.Material

data class ShopItem(
    val id: String,
    val material: Material,
    val displayName: String,
    val lore: List<String>,
    val slot: Int,
    val price: Int,
    val action: ShopAction,
    // Effect-related fields (for seeker items)
    val effectType: EffectType? = null,
    val effectDuration: Int? = null,
    val effectIntensity: Double? = null,
    // Taunt-related fields (for hider items)
    val tauntType: String? = null,        // "SNOWBALL" or "FIREWORK" (null = not a taunt item)
    val tauntBonus: Int? = null,          // Bonus points for using taunt (null = no bonus)
    // Purchase restrictions
    val roleFilter: String? = null,       // "SEEKER" or "HIDER" (null = any role)
    val cooldown: Int? = null,            // Cooldown in seconds (null = no cooldown)
    val maxPurchases: Int? = null,        // Max purchases per game (null = unlimited)
    val usageRestriction: UsageRestriction = UsageRestriction.ALWAYS
) {
    /**
     * Validate that effect fields are consistent with action type
     * @throws IllegalStateException if configuration is invalid
     */
    fun validate() {
        when (action) {
            is ShopAction.UseEffectItem -> {
                require(effectType != null) {
                    "Shop item '$id' with UseEffectItem action must have effectType"
                }
                require(effectDuration != null && effectDuration > 0) {
                    "Shop item '$id' with UseEffectItem action must have valid effectDuration"
                }
                require(effectIntensity != null && effectIntensity > 0) {
                    "Shop item '$id' with UseEffectItem action must have valid effectIntensity"
                }
            }
            else -> {
                // Non-effect items should not have effect fields set
                if (effectType != null || effectDuration != null || effectIntensity != null) {
                    throw IllegalStateException(
                        "Shop item '$id' has effect fields but action is not UseEffectItem"
                    )
                }
            }
        }
    }

    /**
     * Check if this item is an effect item
     */
    fun isEffectItem(): Boolean = action is ShopAction.UseEffectItem

    /**
     * Check if this item is restricted to a specific role
     */
    fun isRoleRestricted(): Boolean = roleFilter != null

    /**
     * Check if this item has purchase limits
     */
    fun hasPurchaseLimit(): Boolean = maxPurchases != null

    /**
     * Check if this item has cooldown
     */
    fun hasCooldown(): Boolean = cooldown != null

    /**
     * Check if this item is a taunt item
     */
    fun isTauntItem(): Boolean = tauntType != null && tauntBonus != null
}
