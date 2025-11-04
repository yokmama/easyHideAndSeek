package com.hideandseek.items

import com.hideandseek.game.Game
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * T038: Handler for Eagle Eye effect
 *
 * Highlights disguised blocks (where Hiders are hiding) with particles
 * Shows glowing particles around disguised blocks within detection radius
 * Duration: 20 seconds (default)
 * Intensity: Detection radius in blocks (default: 15)
 */
class EagleEyeHandler : ItemEffectHandler {

    companion object {
        // Track active eagle eyes: playerId -> BukkitTask
        private val activeEagleEyes = ConcurrentHashMap<UUID, Int>()

        /**
         * Stop eagle eye for a player
         */
        fun stopEagleEye(playerId: UUID, plugin: org.bukkit.plugin.Plugin) {
            activeEagleEyes.remove(playerId)?.let { taskId ->
                org.bukkit.Bukkit.getScheduler().cancelTask(taskId)
            }
        }

        /**
         * Clean up all eagle eyes
         */
        fun stopAllEagleEyes(plugin: org.bukkit.plugin.Plugin) {
            activeEagleEyes.values.forEach { taskId ->
                org.bukkit.Bukkit.getScheduler().cancelTask(taskId)
            }
            activeEagleEyes.clear()
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
            val plugin = player.server.pluginManager.getPlugin("EasyHideAndSeek")!!
            val detectionRadius = config.intensity

            // Visual feedback
            player.sendMessage("§a✦ 鷹の目が発動しました")
            player.sendMessage("§7偽装ブロックが光って見えます（範囲${detectionRadius.toInt()}ブロック）")
            player.playSound(player.location, org.bukkit.Sound.ENTITY_ENDER_EYE_DEATH, 1.0f, 1.5f)

            // Start particle effect task (updates every 10 ticks / 0.5 seconds)
            val task = object : BukkitRunnable() {
                var ticksLeft = config.duration * 20

                override fun run() {
                    if (ticksLeft <= 0 || !player.isOnline) {
                        stopEagleEye(player.uniqueId, plugin)
                        cancel()
                        return
                    }

                    // Find all disguised Hiders within radius
                    val disguisedLocations = findDisguisedHiders(player, game, detectionRadius)

                    // Show particles at each disguised location
                    for (location in disguisedLocations) {
                        // Only show particles to the player using Eagle Eye
                        player.spawnParticle(
                            org.bukkit.Particle.END_ROD,
                            location.clone().add(0.5, 0.5, 0.5),
                            3,
                            0.3, 0.3, 0.3,
                            0.02
                        )

                        player.spawnParticle(
                            org.bukkit.Particle.HAPPY_VILLAGER,
                            location.clone().add(0.5, 1.0, 0.5),
                            1,
                            0.2, 0.2, 0.2,
                            0.0
                        )
                    }

                    // Update action bar
                    val secondsLeft = ticksLeft / 20
                    if (disguisedLocations.isEmpty()) {
                        player.sendActionBar("§7鷹の目 - 検出なし §e残り${secondsLeft}秒")
                    } else {
                        player.sendActionBar("§e鷹の目 - ${disguisedLocations.size}個検出 §7残り${secondsLeft}秒")
                    }

                    ticksLeft -= 10
                }
            }

            val taskId = task.runTaskTimer(plugin, 0L, 10L).taskId // Every 0.5 seconds
            activeEagleEyes[player.uniqueId] = taskId

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun onExpire(player: Player, game: Game) {
        val plugin = player.server.pluginManager.getPlugin("EasyHideAndSeek")!!
        stopEagleEye(player.uniqueId, plugin)

        player.sendMessage("§7鷹の目の効果が切れました")
        player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 0.8f)
    }

    override fun getDisplayLore(config: ItemConfig): List<String> {
        return listOf(
            "§7偽装ブロックがパーティクルで光る",
            "§7ハイダーを見つけやすくなる強力な効果",
            "",
            "§b効果: §f偽装ブロックを視覚化",
            "§b範囲: §f${config.intensity.toInt()}ブロック",
            "§b持続時間: §f${config.duration}秒",
            "",
            "§7パーティクルは使用者にのみ見えます"
        )
    }

    /**
     * Find locations of disguised Hiders within radius
     */
    private fun findDisguisedHiders(seeker: Player, game: Game, radius: Double): List<Location> {
        val locations = mutableListOf<Location>()

        // Get disguise manager to check for disguised blocks
        // Note: This would need to integrate with the actual DisguiseManager
        // For now, we'll check for Hiders near their last known location

        for ((playerId, playerData) in game.players) {
            if (playerData.role.name == "HIDER" && !playerData.isCaptured) {
                val hider = org.bukkit.Bukkit.getPlayer(playerId) ?: continue

                // Check if Hider is within detection radius
                val distance = seeker.location.distance(hider.location)
                if (distance <= radius) {
                    // Check if player is disguised (has invisibility effect)
                    if (hider.hasPotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY)) {
                        // Add the block location at their feet
                        val blockLocation = hider.location.block.location
                        locations.add(blockLocation)
                    }
                }
            }
        }

        return locations
    }
}
