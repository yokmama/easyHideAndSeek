package com.hideandseek.shop

import org.bukkit.Material

sealed class ShopAction {
    data class Disguise(val blockType: Material) : ShopAction()
    object GoBack : ShopAction()
    object Close : ShopAction()
}
