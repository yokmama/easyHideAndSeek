package com.hideandseek.items

import com.hideandseek.game.Game
import org.bukkit.entity.Player

/**
 * T039: Handler for Tracker Insight effect
 *
 * Shows how long each Hider has been stationary (not moved)
 * Helps identify which Hiders are likely disguised vs actively moving
 * This is an instant effect with a display duration
 * Duration: Display duration in seconds (default: 10)
 */
class TrackerInsightHandler : ItemEffectHandler {

    companion object {
        // Track last movement time for each player: playerId -> timestamp
        private val lastMovementTimes = mutableMapOf<java.util.UUID, Long>()

        /**
         * Update movement time for a player
         */
        fun updateMovementTime(playerId: java.util.UUID) {
            lastMovementTimes[playerId] = System.currentTimeMillis()
        }

        /**
         * Get seconds since last movement
         */
        fun getSecondsSinceMovement(playerId: java.util.UUID): Long {
            val lastTime = lastMovementTimes[playerId] ?: return 0L
            return (System.currentTimeMillis() - lastTime) / 1000
        }

        /**
         * Clear all tracking data
         */
        fun clearAll() {
            lastMovementTimes.clear()
        }
    }

    override fun canApply(player: Player, game: Game): Boolean {
        val playerData = game.players[player.uniqueId] ?: return false

        // Must be a Seeker
        if (playerData.role.name != "SEEKER") {
            return false
        }

        return true
    }

    override fun apply(player: Player, game: Game, config: ItemConfig): Result<Unit> {
        return try {
            // Collect movement data for all Hiders
            val hiderInsights = mutableListOf<Triple<String, Long, String>>()

            for ((playerId, playerData) in game.players) {
                if (playerData.role.name == "HIDER" && !playerData.isCaptured) {
                    val hider = org.bukkit.Bukkit.getPlayer(playerId) ?: continue

                    val secondsSinceMovement = getSecondsSinceMovement(playerId)
                    val status = when {
                        secondsSinceMovement < 5 -> "§a移動中"
                        secondsSinceMovement < 15 -> "§e静止"
                        else -> "§c長時間静止"
                    }

                    hiderInsights.add(Triple(hider.name, secondsSinceMovement, status))
                }
            }

            // Sort by time stationary (longest first)
            hiderInsights.sortByDescending { it.second }

            // Display results
            player.sendMessage("§6✦ 追跡の洞察")
            player.sendMessage("§7ハイダーの移動状況:")
            player.sendMessage("")

            if (hiderInsights.isEmpty()) {
                player.sendMessage("  §7ハイダーが見つかりません")
            } else {
                for ((name, seconds, status) in hiderInsights) {
                    val timeStr = formatTime(seconds)
                    player.sendMessage("  §7$name: $status §7($timeStr)")
                }

                player.sendMessage("")
                player.sendMessage("§7§o長時間静止しているハイダーは偽装している可能性が高い")
            }

            // Play sound
            player.playSound(player.location, org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f)

            // Display in action bar for duration
            val plugin = player.server.pluginManager.getPlugin("EasyHideAndSeek")!!
            var secondsLeft = config.duration

            val task = object : org.bukkit.scheduler.BukkitRunnable() {
                override fun run() {
                    if (secondsLeft <= 0 || !player.isOnline) {
                        cancel()
                        return
                    }

                    val stationaryCount = hiderInsights.count { it.second >= 15 }
                    player.sendActionBar("§6洞察: §e${stationaryCount}人§7が長時間静止 §7(${secondsLeft}秒)")
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
        // Instant effect, no cleanup needed
    }

    override fun getDisplayLore(config: ItemConfig): List<String> {
        return listOf(
            "§7各ハイダーの静止時間を表示",
            "§7偽装中のハイダーを推測できる",
            "",
            "§b効果: §fハイダーの移動状況を表示",
            "§b表示時間: §f${config.duration}秒",
            "",
            "§7長時間静止 = 偽装している可能性大",
            "§7移動中 = 偽装していない可能性大"
        )
    }

    /**
     * Format seconds into readable time string
     */
    private fun formatTime(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}秒前"
            seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒前"
            else -> "${seconds / 3600}時間以上前"
        }
    }
}
