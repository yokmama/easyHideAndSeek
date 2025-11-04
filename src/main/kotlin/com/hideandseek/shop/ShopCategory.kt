package com.hideandseek.shop

import org.bukkit.Material

data class ShopCategory(
    val id: String,
    val displayName: String,
    val icon: Material,
    val slot: Int,
    val items: List<ShopItem>,
    val roleFilter: String? = null  // "SEEKER" or "HIDER" (null = visible to all)
) {
    /**
     * Check if this category is visible to a specific role
     * @param role Player's role ("SEEKER" or "HIDER")
     * @return true if category should be shown
     */
    fun isVisibleTo(role: String): Boolean {
        return roleFilter == null || roleFilter.equals(role, ignoreCase = true)
    }

    /**
     * Get items filtered for a specific role
     * @param role Player's role ("SEEKER" or "HIDER")
     * @return List of items visible to this role
     */
    fun getItemsForRole(role: String): List<ShopItem> {
        return items.filter { item ->
            item.roleFilter == null || item.roleFilter.equals(role, ignoreCase = true)
        }
    }

    /**
     * Get all items regardless of role
     * (used for admin previews or config validation)
     */
    fun getAllItems(): List<ShopItem> = items
}
