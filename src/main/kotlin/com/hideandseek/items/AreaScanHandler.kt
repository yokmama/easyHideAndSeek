package com.hideandseek.items

import com.hideandseek.game.Game
import org.bukkit.entity.Player
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * T037: Handler for Area Scan effect
 *
 * Reveals the count and approximate directions of Hiders within a radius
 * This is an instant effect with a display duration
 * Intensity: Scan radius in blocks (default: 12)
 * Duration: Display duration in seconds (default: 5)
 */
class AreaScanHandler : ItemEffectHandler {

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
            val scanRadius = config.intensity  // Radius in blocks

            // Find all Hiders within radius
            val nearbyHiders = mutableListOf<Pair<Player, String>>()

            for ((playerId, playerData) in game.players) {
                if (playerData.role.name == "HIDER" && !playerData.isCaptured) {
                    val hider = org.bukkit.Bukkit.getPlayer(playerId) ?: continue

                    val distance = player.location.distance(hider.location)
                    if (distance <= scanRadius) {
                        val direction = getDirection(player.location, hider.location)
                        nearbyHiders.add(Pair(hider, direction))
                    }
                }
            }

            // Create scan visualization (particle ring)
            createScanParticles(player, scanRadius)

            // Display results
            if (nearbyHiders.isEmpty()) {
                player.sendMessage("§c✦ エリアスキャン")
                player.sendMessage("§7範囲${scanRadius.toInt()}ブロック内にハイダーはいません")
            } else {
                player.sendMessage("§a✦ エリアスキャン")
                player.sendMessage("§e${nearbyHiders.size}人§7のハイダーを検出 §7(半径${scanRadius.toInt()}ブロック)")
                player.sendMessage("")

                // Group by direction
                val byDirection = nearbyHiders.groupBy { it.second }
                for ((direction, hiders) in byDirection) {
                    player.sendMessage("  §7$direction: §e${hiders.size}人")
                }
            }

            // Play sound
            player.playSound(player.location, org.bukkit.Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.5f)

            // Display in action bar for duration
            val plugin = player.server.pluginManager.getPlugin("EasyHideAndSeek")!!
            val message = if (nearbyHiders.isEmpty()) {
                "§cスキャン結果: 0人"
            } else {
                "§aスキャン結果: §e${nearbyHiders.size}人§7のハイダー検出"
            }

            // Show message for duration
            var secondsLeft = config.duration
            val task = object : org.bukkit.scheduler.BukkitRunnable() {
                override fun run() {
                    if (secondsLeft <= 0 || !player.isOnline) {
                        cancel()
                        return
                    }

                    player.sendActionBar("$message §7(${secondsLeft}秒)")
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
            "§7周囲のハイダー数と方向を表示",
            "§7壁越しでも検出可能",
            "",
            "§b効果: §f範囲内のハイダー数と方向",
            "§b範囲: §f${config.intensity.toInt()}ブロック",
            "§b表示時間: §f${config.duration}秒",
            "",
            "§7方向は8方位で表示されます"
        )
    }

    /**
     * Get cardinal direction from seeker to hider
     */
    private fun getDirection(from: org.bukkit.Location, to: org.bukkit.Location): String {
        val dx = to.x - from.x
        val dz = to.z - from.z

        val angle = Math.toDegrees(atan2(dz, dx))
        val normalizedAngle = (angle + 360) % 360

        return when {
            normalizedAngle < 22.5 || normalizedAngle >= 337.5 -> "東"
            normalizedAngle < 67.5 -> "南東"
            normalizedAngle < 112.5 -> "南"
            normalizedAngle < 157.5 -> "南西"
            normalizedAngle < 202.5 -> "西"
            normalizedAngle < 247.5 -> "北西"
            normalizedAngle < 292.5 -> "北"
            else -> "北東"
        }
    }

    /**
     * Create particle ring effect for scan
     */
    private fun createScanParticles(player: Player, radius: Double) {
        val center = player.location.clone().add(0.0, 0.5, 0.0)

        // Create expanding ring
        for (i in 0..3) {
            val delay = (i * 5).toLong()
            val currentRadius = radius * (i + 1) / 4.0

            org.bukkit.Bukkit.getScheduler().runTaskLater(
                player.server.pluginManager.getPlugin("EasyHideAndSeek")!!,
                Runnable {
                    val points = 32
                    for (j in 0 until points) {
                        val angle = 2 * Math.PI * j / points
                        val x = currentRadius * kotlin.math.cos(angle)
                        val z = currentRadius * kotlin.math.sin(angle)

                        val particleLocation = center.clone().add(x, 0.0, z)
                        player.world.spawnParticle(
                            org.bukkit.Particle.ELECTRIC_SPARK,
                            particleLocation,
                            1,
                            0.0, 0.0, 0.0,
                            0.0
                        )
                    }
                },
                delay
            )
        }
    }
}
