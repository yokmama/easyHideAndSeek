package com.hideandseek.items

import com.hideandseek.effects.ActiveEffect
import com.hideandseek.effects.EffectHandler
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * Effect handler for SPEED effect
 * Applies Speed II potion effect for increased movement speed
 */
class SpeedBoostHandler : EffectHandler {

    override fun apply(player: Player, effect: ActiveEffect) {
        // Apply Speed II potion effect
        val duration = effect.getTotalDuration().toInt() * 20 // Convert to ticks
        val speedEffect = PotionEffect(
            PotionEffectType.SPEED,
            duration,
            1, // Amplifier 1 (Speed II)
            false, // No ambient particles
            false, // No particles
            true   // Show icon
        )
        player.addPotionEffect(speedEffect)

        // Visual and audio feedback
        player.playSound(player.location, Sound.ENTITY_HORSE_GALLOP, 1.0f, 1.5f)
        player.spawnParticle(
            Particle.CLOUD,
            player.location.add(0.0, 0.1, 0.0),
            10,
            0.3, 0.1, 0.3,
            0.1
        )

        player.sendMessage("§a⚡ スピードブーストが適用されました（${effect.getTotalDuration()}秒間）")
    }

    override fun remove(player: Player, effect: ActiveEffect) {
        // Remove Speed potion effect
        player.removePotionEffect(PotionEffectType.SPEED)

        player.sendMessage("§7スピードブーストの効果が切れました")
    }
}
