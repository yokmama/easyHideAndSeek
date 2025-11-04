package com.hideandseek.items

import com.hideandseek.game.Game
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * T033: Handler for Shadow Sprint effect (Hider speed boost)
 *
 * Provides temporary speed boost to Hiders for escape/repositioning
 * Duration: 20 seconds (default)
 * Intensity: 1.5x speed (Speed I effect)
 */
class ShadowSprintHandler : ItemEffectHandler {

    override fun canApply(player: Player, game: Game): Boolean {
        // Can be used by Hiders at any time during the game
        val playerData = game.players[player.uniqueId] ?: return false

        // Must be a Hider
        if (playerData.role.name != "HIDER") {
            return false
        }

        // Cannot use while captured
        if (playerData.isCaptured) {
            return false
        }

        return true
    }

    override fun apply(player: Player, game: Game, config: ItemConfig): Result<Unit> {
        return try {
            // Apply Speed effect based on intensity
            val amplifier = when {
                config.intensity >= 2.0 -> 2  // Speed III (3.0x)
                config.intensity >= 1.5 -> 1  // Speed II (2.0x)
                else -> 0                      // Speed I (1.2x)
            }

            val effect = PotionEffect(
                PotionEffectType.SPEED,
                config.duration * 20,  // Convert seconds to ticks
                amplifier,
                false,  // ambient
                true,   // particles
                true    // icon
            )

            player.addPotionEffect(effect)

            // Visual feedback
            player.sendMessage("§a✦ 影の疾走が発動！ §e${config.duration}秒間§aスピードアップ")
            player.playSound(player.location, org.bukkit.Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun onExpire(player: Player, game: Game) {
        // Potion effect removes automatically, just send notification
        player.sendMessage("§7影の疾走の効果が切れました")
        player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 0.8f)
    }

    override fun getDisplayLore(config: ItemConfig): List<String> {
        val speedLevel = when {
            config.intensity >= 2.0 -> "III"
            config.intensity >= 1.5 -> "II"
            else -> "I"
        }

        return listOf(
            "§7一時的にスピードアップして逃走",
            "§7シーカーから逃げるのに最適",
            "",
            "§b効果: §fスピード $speedLevel",
            "§b持続時間: §f${config.duration}秒",
            "",
            "§7使用後は素早く隠れ場所を変更しましょう"
        )
    }
}
