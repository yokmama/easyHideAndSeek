package com.hideandseek.items

import com.hideandseek.game.Game
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * T035: Handler for Second Chance effect
 *
 * Grants a one-time respawn when captured, with temporary immunity
 * Duration: 10 seconds of immunity after respawn
 * This is a passive effect that activates on capture
 */
class SecondChanceHandler : ItemEffectHandler {

    companion object {
        // Track players with active Second Chance: playerId -> expirationTime
        private val activeSecondChances = ConcurrentHashMap<UUID, Long>()

        /**
         * Check if player has Second Chance available
         */
        fun hasSecondChance(playerId: UUID): Boolean {
            return activeSecondChances.containsKey(playerId)
        }

        /**
         * Consume Second Chance and grant immunity
         * Returns true if Second Chance was available and consumed
         */
        fun consumeSecondChance(player: Player, immunitySeconds: Int): Boolean {
            if (!activeSecondChances.containsKey(player.uniqueId)) {
                return false
            }

            // Remove from active list
            activeSecondChances.remove(player.uniqueId)

            // Respawn player at their original spawn
            player.gameMode = GameMode.SURVIVAL
            player.health = player.maxHealth
            player.foodLevel = 20
            player.saturation = 20f

            // Grant temporary immunity (Resistance V + Glowing)
            val immunityEffect = PotionEffect(
                PotionEffectType.RESISTANCE,
                immunitySeconds * 20,
                4,  // Resistance V (near invulnerability)
                false,
                true,
                true
            )
            val glowEffect = PotionEffect(
                PotionEffectType.GLOWING,
                immunitySeconds * 20,
                0,
                false,
                true,
                true
            )

            player.addPotionEffect(immunityEffect)
            player.addPotionEffect(glowEffect)

            // Visual feedback
            player.sendMessage("§6✦ セカンドチャンス発動！")
            player.sendMessage("§e${immunitySeconds}秒間無敵状態です")
            player.playSound(player.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f)

            // Particle effect
            player.world.spawnParticle(
                org.bukkit.Particle.TOTEM_OF_UNDYING,
                player.location.clone().add(0.0, 1.0, 0.0),
                50,
                0.5, 1.0, 0.5,
                0.1
            )

            return true
        }

        /**
         * Remove expired Second Chances
         */
        fun cleanupExpired() {
            val now = System.currentTimeMillis()
            activeSecondChances.entries.removeIf { (_, expirationTime) ->
                now > expirationTime
            }
        }

        /**
         * Clear all Second Chances (game end)
         */
        fun clearAll() {
            activeSecondChances.clear()
        }
    }

    override fun canApply(player: Player, game: Game): Boolean {
        val playerData = game.players[player.uniqueId] ?: return false

        // Must be a Hider
        if (playerData.role.name != "HIDER") {
            return false
        }

        // Cannot use while captured
        if (playerData.isCaptured) {
            return false
        }

        // Can only have one Second Chance at a time
        if (hasSecondChance(player.uniqueId)) {
            player.sendMessage("§c既にセカンドチャンスを所持しています")
            return false
        }

        return true
    }

    override fun apply(player: Player, game: Game, config: ItemConfig): Result<Unit> {
        return try {
            // Calculate expiration time (duration in seconds)
            val expirationTime = System.currentTimeMillis() + (config.duration * 1000)

            // Register Second Chance
            activeSecondChances[player.uniqueId] = expirationTime

            // Visual feedback
            player.sendMessage("§a✦ セカンドチャンスを取得しました")
            player.sendMessage("§7次に捕獲された時、1回だけ復活できます")
            player.playSound(player.location, org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.2f)

            // Particle effect
            player.world.spawnParticle(
                org.bukkit.Particle.ENCHANT,
                player.location.clone().add(0.0, 2.0, 0.0),
                30,
                0.3, 0.5, 0.3,
                0.5
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun onExpire(player: Player, game: Game) {
        // Remove from active list if not yet consumed
        if (activeSecondChances.remove(player.uniqueId) != null) {
            player.sendMessage("§7セカンドチャンスの効果が切れました（未使用）")
            player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 0.6f)
        }
    }

    override fun getDisplayLore(config: ItemConfig): List<String> {
        return listOf(
            "§7捕獲された時に1回だけ復活",
            "§7保険として非常に有用",
            "",
            "§b効果: §f捕獲時に自動復活",
            "§b無敵時間: §f${config.duration}秒",
            "§b有効期限: §f購入から10分間",
            "",
            "§c注意: 復活後は発光して目立ちます",
            "§7無敵時間中に安全な場所へ移動しましょう"
        )
    }
}
