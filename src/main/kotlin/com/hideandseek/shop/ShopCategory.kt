package com.hideandseek.shop

import org.bukkit.Material

data class ShopCategory(
    val id: String,
    val displayName: String,
    val icon: Material,
    val slot: Int,
    val items: List<ShopItem>
)
