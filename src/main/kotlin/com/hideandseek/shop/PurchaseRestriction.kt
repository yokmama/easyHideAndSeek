package com.hideandseek.shop

/**
 * Purchase restriction configuration for shop items
 *
 * Defines limits and cooldowns to prevent item spam and maintain game balance
 */
data class PurchaseRestriction(
    /**
     * Scope of the restriction (per-player, per-team, or global)
     */
    val restrictionType: RestrictionType,

    /**
     * Maximum number of purchases allowed within scope
     * null = unlimited purchases
     */
    val maxQuantity: Int?,

    /**
     * Cooldown duration in seconds between purchases/uses
     * null = no cooldown
     */
    val cooldownSeconds: Int? = null,

    /**
     * When the purchase limit or cooldown should reset
     * Default: NEVER (persists for entire game)
     */
    val resetTrigger: ResetTrigger = ResetTrigger.NEVER
) {
    /**
     * Check if this restriction has a purchase limit
     */
    fun hasLimit(): Boolean = maxQuantity != null

    /**
     * Check if this restriction has a cooldown
     */
    fun hasCooldown(): Boolean = cooldownSeconds != null

    /**
     * Get human-readable description of restriction
     */
    fun getDescription(): String {
        val parts = mutableListOf<String>()

        maxQuantity?.let {
            val scope = when (restrictionType) {
                RestrictionType.PER_PLAYER -> "per player"
                RestrictionType.PER_TEAM -> "per team"
                RestrictionType.GLOBAL -> "total"
            }
            parts.add("Limit: $it $scope")
        }

        cooldownSeconds?.let {
            parts.add("Cooldown: ${it}s")
        }

        if (resetTrigger != ResetTrigger.NEVER) {
            parts.add("Resets: ${resetTrigger.name.lowercase().replace('_', ' ')}")
        }

        return parts.joinToString(" | ")
    }

    companion object {
        /**
         * Create a per-player restriction with no cooldown
         */
        fun perPlayer(maxPurchases: Int): PurchaseRestriction {
            return PurchaseRestriction(
                restrictionType = RestrictionType.PER_PLAYER,
                maxQuantity = maxPurchases,
                cooldownSeconds = null,
                resetTrigger = ResetTrigger.NEVER
            )
        }

        /**
         * Create a per-team restriction
         */
        fun perTeam(maxPurchases: Int): PurchaseRestriction {
            return PurchaseRestriction(
                restrictionType = RestrictionType.PER_TEAM,
                maxQuantity = maxPurchases,
                cooldownSeconds = null,
                resetTrigger = ResetTrigger.NEVER
            )
        }

        /**
         * Create a cooldown-only restriction
         */
        fun withCooldown(cooldownSeconds: Int, resetOn: ResetTrigger = ResetTrigger.ON_USE): PurchaseRestriction {
            return PurchaseRestriction(
                restrictionType = RestrictionType.PER_PLAYER,
                maxQuantity = null,
                cooldownSeconds = cooldownSeconds,
                resetTrigger = resetOn
            )
        }
    }
}
