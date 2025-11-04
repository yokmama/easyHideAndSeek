package com.hideandseek.items

import com.hideandseek.effects.ActiveEffect
import com.hideandseek.effects.EffectHandler
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player

/**
 * Effect handler for REACH effect
 * Extends attack range by 1.5x (4.5 blocks → 6.75 blocks)
 *
 * Note: This is tracked in the effect system, but the actual reach extension
 * is implemented in combat listeners (EntityDamageByEntityListener) which
 * check for active REACH effects and extend raycasting range accordingly.
 */
class ReachExtenderHandler : EffectHandler {

    override fun apply(player: Player, effect: ActiveEffect) {
        // Visual and audio feedback
        player.playSound(player.location, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 0.8f)
        player.spawnParticle(
            Particle.SWEEP_ATTACK,
            player.location.add(0.0, 1.0, 0.0),
            3,
            0.5, 0.5, 0.5,
            0.0
        )

        player.sendMessage("§d🗡 攻撃範囲拡大が適用されました（${effect.getTotalDuration()}秒間、1.5倍）")
    }

    override fun remove(player: Player, effect: ActiveEffect) {
        player.sendMessage("§7攻撃範囲拡大の効果が切れました")
    }
}
