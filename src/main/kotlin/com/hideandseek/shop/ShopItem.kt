package com.hideandseek.shop

import org.bukkit.Material

data class ShopItem(
    val id: String,
    val material: Material,
    val displayName: String,
    val lore: List<String>,
    val slot: Int,
    val price: Int,
    val action: ShopAction
)
