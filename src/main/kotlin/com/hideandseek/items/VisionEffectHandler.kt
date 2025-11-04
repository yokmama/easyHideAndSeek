package com.hideandseek.items

import com.hideandseek.effects.ActiveEffect
import com.hideandseek.effects.EffectHandler
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * Effect handler for VISION effect
 * Applies Night Vision potion effect + increased view distance
 */
class VisionEffectHandler : EffectHandler {

    override fun apply(player: Player, effect: ActiveEffect) {
        // Store original view distance for restoration
        val originalViewDistance = player.clientViewDistance

        // Apply Night Vision potion effect
        val duration = effect.getTotalDuration().toInt() * 20 // Convert to ticks
        val nightVision = PotionEffect(
            PotionEffectType.NIGHT_VISION,
            duration,
            0, // Amplifier 0 (Night Vision I)
            false, // No ambient particles
            false, // No particles
            true   // Show icon
        )
        player.addPotionEffect(nightVision)

        // Increase client view distance to 32 chunks
        player.sendViewDistance = 32

        // Visual feedback
        player.playSound(player.location, Sound.ENTITY_ENDER_EYE_LAUNCH, 1.0f, 1.0f)
        player.spawnParticle(
            Particle.END_ROD,
            player.location.add(0.0, 1.0, 0.0),
            5,
            0.5, 0.5, 0.5,
            0.02
        )

        // Store original view distance in metadata for restoration
        // Note: This is now handled by storing in ActiveEffect.metadata during creation
        player.sendMessage("§b👁 視界拡大が適用されました（${effect.getTotalDuration()}秒間）")
    }

    override fun remove(player: Player, effect: ActiveEffect) {
        // Remove Night Vision potion effect
        player.removePotionEffect(PotionEffectType.NIGHT_VISION)

        // Restore original view distance
        val originalDistance = effect.getMetadata<Int>("originalViewDistance")
        if (originalDistance != null) {
            player.sendViewDistance = originalDistance
        } else {
            // Fallback to server default (usually 10)
            player.sendViewDistance = 10
        }

        // Feedback message
        player.sendMessage("§7視界拡大の効果が切れました")
    }
}
