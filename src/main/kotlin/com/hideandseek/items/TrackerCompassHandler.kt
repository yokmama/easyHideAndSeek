package com.hideandseek.items

import com.hideandseek.game.Game
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * T036: Handler for Tracker Compass effect
 *
 * Points to the nearest Hider's location
 * Updates every 2 seconds during the effect duration
 * Duration: 30 seconds (default)
 */
class TrackerCompassHandler : ItemEffectHandler {

    companion object {
        // Track active compasses: playerId -> BukkitTask
        private val activeCompasses = ConcurrentHashMap<UUID, Int>()

        /**
         * Stop tracking for a player
         */
        fun stopTracking(playerId: UUID, plugin: org.bukkit.plugin.Plugin) {
            activeCompasses.remove(playerId)?.let { taskId ->
                org.bukkit.Bukkit.getScheduler().cancelTask(taskId)
            }
        }

        /**
         * Clean up all trackers
         */
        fun stopAllTracking(plugin: org.bukkit.plugin.Plugin) {
            activeCompasses.values.forEach { taskId ->
                org.bukkit.Bukkit.getScheduler().cancelTask(taskId)
            }
            activeCompasses.clear()
        }
    }

    override fun canApply(player: Player, game: Game): Boolean {
        val playerData = game.players[player.uniqueId] ?: return false

        // Must be a Seeker
        if (playerData.role.name != "SEEKER") {
            return false
        }

        // Check if there are any Hiders left
        val hidersRemaining = game.players.values.count {
            it.role.name == "HIDER" && !it.isCaptured
        }

        if (hidersRemaining == 0) {
            player.sendMessage("§c隠れているハイダーがいません")
            return false
        }

        return true
    }

    override fun apply(player: Player, game: Game, config: ItemConfig): Result<Unit> {
        return try {
            val plugin = player.server.pluginManager.getPlugin("EasyHideAndSeek")!!

            // Give player a compass
            val compass = ItemStack(Material.COMPASS)
            val meta = compass.itemMeta!!
            meta.setDisplayName("§6追跡コンパス")
            meta.lore = listOf(
                "§7最も近いハイダーを指します",
                "§7残り: §e${config.duration}秒"
            )
            compass.itemMeta = meta

            // Add compass to inventory or drop if full
            val remaining = player.inventory.addItem(compass)
            if (remaining.isNotEmpty()) {
                player.world.dropItem(player.location, compass)
            }

            // Start tracking task (updates every 2 seconds)
            val task = object : BukkitRunnable() {
                var secondsLeft = config.duration

                override fun run() {
                    if (secondsLeft <= 0 || !player.isOnline) {
                        stopTracking(player.uniqueId, plugin)
                        cancel()
                        return
                    }

                    // Find nearest Hider
                    val nearestHider = findNearestHider(player, game)

                    if (nearestHider != null) {
                        // Update compass target
                        player.compassTarget = nearestHider.location

                        // Calculate distance
                        val distance = player.location.distance(nearestHider.location)

                        // Send action bar update
                        player.sendActionBar("§6コンパス: §f最も近いハイダー §7(${distance.toInt()}m) §e残り${secondsLeft}秒")
                    } else {
                        player.sendActionBar("§cハイダーが見つかりません §e残り${secondsLeft}秒")
                    }

                    secondsLeft -= 2
                }
            }

            val taskId = task.runTaskTimer(plugin, 0L, 40L).taskId // Every 2 seconds
            activeCompasses[player.uniqueId] = taskId

            // Visual feedback
            player.sendMessage("§a✦ 追跡コンパスを取得しました")
            player.sendMessage("§7コンパスが最も近いハイダーを指します")
            player.playSound(player.location, org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun onExpire(player: Player, game: Game) {
        val plugin = player.server.pluginManager.getPlugin("EasyHideAndSeek")!!
        stopTracking(player.uniqueId, plugin)

        // Reset compass target
        player.compassTarget = player.world.spawnLocation

        player.sendMessage("§7追跡コンパスの効果が切れました")
        player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 0.8f)
    }

    override fun getDisplayLore(config: ItemConfig): List<String> {
        return listOf(
            "§7最も近いハイダーの方向を指すコンパス",
            "§72秒ごとに自動更新",
            "",
            "§b効果: §fコンパスが最寄りのハイダーを指す",
            "§b持続時間: §f${config.duration}秒",
            "§b更新間隔: §f2秒",
            "",
            "§7アクションバーに距離が表示されます"
        )
    }

    /**
     * Find the nearest uncaptured Hider to the seeker
     */
    private fun findNearestHider(seeker: Player, game: Game): Player? {
        var nearestHider: Player? = null
        var nearestDistance = Double.MAX_VALUE

        for ((playerId, playerData) in game.players) {
            if (playerData.role.name == "HIDER" && !playerData.isCaptured) {
                val hider = org.bukkit.Bukkit.getPlayer(playerId) ?: continue

                val distance = seeker.location.distance(hider.location)
                if (distance < nearestDistance) {
                    nearestDistance = distance
                    nearestHider = hider
                }
            }
        }

        return nearestHider
    }
}
