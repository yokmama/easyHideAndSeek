package com.hideandseek.items

import com.hideandseek.game.Game
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * T040: Handler for Capture Net effect
 *
 * Next capture grants 2x reward (double coins/points)
 * Encourages risk-taking and strategic timing
 * Duration: 10 seconds (window to capture)
 * Intensity: 2.0x reward multiplier (default)
 */
class CaptureNetHandler : ItemEffectHandler {

    companion object {
        // Track active capture nets: playerId -> multiplier
        private val activeCaptureNets = ConcurrentHashMap<UUID, Double>()

        /**
         * Check if player has active Capture Net
         */
        fun hasActiveCaptureNet(playerId: UUID): Boolean {
            return activeCaptureNets.containsKey(playerId)
        }

        /**
         * Get reward multiplier for player
         * Returns 1.0 if no active net
         */
        fun getRewardMultiplier(playerId: UUID): Double {
            return activeCaptureNets.getOrDefault(playerId, 1.0)
        }

        /**
         * Consume Capture Net after successful capture
         */
        fun consumeCaptureNet(playerId: UUID): Double {
            return activeCaptureNets.remove(playerId) ?: 1.0
        }

        /**
         * Remove expired Capture Net
         */
        fun removeCaptureNet(playerId: UUID) {
            activeCaptureNets.remove(playerId)
        }

        /**
         * Clear all capture nets
         */
        fun clearAll() {
            activeCaptureNets.clear()
        }
    }

    override fun canApply(player: Player, game: Game): Boolean {
        val playerData = game.players[player.uniqueId] ?: return false

        // Must be a Seeker
        if (playerData.role.name != "SEEKER") {
            return false
        }

        // Cannot stack capture nets
        if (hasActiveCaptureNet(player.uniqueId)) {
            player.sendMessage("§c既に捕獲ネットが有効です")
            return false
        }

        // Check if there are Hiders left to capture
        val hidersRemaining = game.players.values.count {
            it.role.name == "HIDER" && !it.isCaptured
        }

        if (hidersRemaining == 0) {
            player.sendMessage("§c捕獲可能なハイダーがいません")
            return false
        }

        return true
    }

    override fun apply(player: Player, game: Game, config: ItemConfig): Result<Unit> {
        return try {
            val multiplier = config.intensity

            // Activate capture net
            activeCaptureNets[player.uniqueId] = multiplier

            // Visual feedback
            player.sendMessage("§a✦ 捕獲ネットが発動しました")
            player.sendMessage("§e${config.duration}秒以内§7に捕獲すると報酬が§6${multiplier}倍§7になります！")
            player.playSound(player.location, org.bukkit.Sound.ENTITY_FISHING_BOBBER_THROW, 1.0f, 1.5f)

            // Particle effect
            player.world.spawnParticle(
                org.bukkit.Particle.ENCHANTED_HIT,
                player.location.clone().add(0.0, 1.0, 0.0),
                50,
                0.5, 1.0, 0.5,
                0.1
            )

            // Schedule expiration with countdown
            val plugin = player.server.pluginManager.getPlugin("EasyHideAndSeek")!!
            var secondsLeft = config.duration

            val task = object : org.bukkit.scheduler.BukkitRunnable() {
                override fun run() {
                    if (secondsLeft <= 0 || !player.isOnline || !hasActiveCaptureNet(player.uniqueId)) {
                        if (hasActiveCaptureNet(player.uniqueId)) {
                            // Net expired without being used
                            removeCaptureNet(player.uniqueId)
                            player.sendMessage("§7捕獲ネットの効果が切れました（未使用）")
                            player.playSound(player.location, org.bukkit.Sound.ENTITY_ITEM_BREAK, 0.5f, 0.8f)
                        }
                        cancel()
                        return
                    }

                    // Visual countdown
                    val color = when {
                        secondsLeft <= 3 -> "§c"
                        secondsLeft <= 5 -> "§e"
                        else -> "§a"
                    }

                    player.sendActionBar("§6捕獲ネット: ${color}残り${secondsLeft}秒 §7(報酬§6${multiplier}倍§7)")

                    // Sound warning at 3 seconds
                    if (secondsLeft == 3) {
                        player.playSound(player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f)
                    }

                    secondsLeft--
                }
            }
            task.runTaskTimer(plugin, 0L, 20L)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun onExpire(player: Player, game: Game) {
        // Expiration handled in the task above
    }

    override fun getDisplayLore(config: ItemConfig): List<String> {
        val multiplierText = if (config.intensity == 2.0) "2倍" else "${config.intensity}倍"

        return listOf(
            "§7次の捕獲で報酬が増加",
            "§7タイミングを見計らって使用",
            "",
            "§b効果: §f次の捕獲報酬が$multiplierText",
            "§b制限時間: §f${config.duration}秒",
            "",
            "§c注意: 時間内に捕獲しないと無駄になる",
            "§7ハイダーを見つけてから使うのがお勧め"
        )
    }
}
