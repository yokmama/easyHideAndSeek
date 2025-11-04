package com.hideandseek.shop

import com.hideandseek.effects.EffectType
import org.bukkit.Material

sealed class ShopAction {
    data class Disguise(val blockType: Material) : ShopAction()
    data class UseEffectItem(
        val effectType: EffectType,
        val duration: Int,
        val intensity: Double
    ) : ShopAction()
    data class UseTauntItem(
        val tauntType: String,   // "SNOWBALL" or "FIREWORK"
        val bonusPoints: Int,
        val material: Material   // Material to give to player
    ) : ShopAction()
    object GoBack : ShopAction()
    object Close : ShopAction()
}
