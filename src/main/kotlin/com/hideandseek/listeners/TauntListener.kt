package com.hideandseek.listeners

import com.hideandseek.game.GameManager
import com.hideandseek.game.PlayerRole
import com.hideandseek.points.PointManager
import com.hideandseek.utils.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin

/**
 * Handles taunt item usage and awards bonus points
 */
class TauntListener(
    private val plugin: Plugin,
    private val gameManager: GameManager,
    private val pointManager: PointManager
) : Listener {

    @EventHandler
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val shooter = event.entity.shooter as? Player ?: return

        // Check if snowball
        if (event.entityType != EntityType.SNOWBALL) return

        // Check if player is in game
        val game = gameManager.activeGame ?: return
        val playerData = game.players[shooter.uniqueId] ?: return

        // Only hiders can earn points from taunts
        if (playerData.role != PlayerRole.HIDER) return

        // Check if hider is not captured
        if (playerData.isCaptured) return

        // Award base snowball taunt bonus (will be doubled if hits seeker)
        val bonusPoints = plugin.config.getInt("points.taunt-bonuses.snowball", 50)
        pointManager.awardTauntBonus(shooter.uniqueId, bonusPoints)

        MessageUtil.send(shooter, "&e+${bonusPoints} ポイント &7(雪玉挑発)")
        shooter.sendMessage("§c警告: シーカーに位置がバレやすくなりました！")
    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        // Check if snowball hit a player
        if (event.entityType != EntityType.SNOWBALL) return

        plugin.logger.info("[Taunt] Snowball hit detected")

        val hitPlayer = event.hitEntity as? Player
        if (hitPlayer == null) {
            plugin.logger.info("[Taunt] Hit entity is not a player: ${event.hitEntity?.type}")
            return
        }

        val shooter = event.entity.shooter as? Player
        if (shooter == null) {
            plugin.logger.info("[Taunt] Shooter is not a player")
            return
        }

        plugin.logger.info("[Taunt] ${shooter.name} hit ${hitPlayer.name} with snowball")

        // Check if both are in game
        val game = gameManager.activeGame
        if (game == null) {
            plugin.logger.info("[Taunt] No active game")
            return
        }

        val shooterData = game.players[shooter.uniqueId]
        if (shooterData == null) {
            plugin.logger.info("[Taunt] Shooter ${shooter.name} not in game")
            return
        }

        val hitData = game.players[hitPlayer.uniqueId]
        if (hitData == null) {
            plugin.logger.info("[Taunt] Hit player ${hitPlayer.name} not in game")
            return
        }

        plugin.logger.info("[Taunt] Shooter role: ${shooterData.role}, isCaptured: ${shooterData.isCaptured}")
        plugin.logger.info("[Taunt] Hit player role: ${hitData.role}")

        // Only award bonus if hider hit a seeker
        if (shooterData.role != PlayerRole.HIDER || shooterData.isCaptured) {
            plugin.logger.info("[Taunt] Shooter is not an active hider")
            return
        }
        if (hitData.role != PlayerRole.SEEKER) {
            plugin.logger.info("[Taunt] Hit player is not a seeker")
            return
        }

        // Award bonus points for hitting seeker (same as base, making it double total)
        val bonusPoints = plugin.config.getInt("points.taunt-bonuses.snowball", 50)
        pointManager.awardTauntBonus(shooter.uniqueId, bonusPoints)

        MessageUtil.send(shooter, "&a&l雪玉が鬼に命中！ &e+${bonusPoints} ボーナスポイント！")
        MessageUtil.send(hitPlayer, "&c${shooter.name} &7の雪玉が命中しました！")

        // Visual and audio feedback
        hitPlayer.playSound(hitPlayer.location, Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f)
        shooter.playSound(shooter.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f)

        // Particle effect at hit location
        hitPlayer.world.spawnParticle(
            Particle.CLOUD,
            hitPlayer.location.add(0.0, 1.0, 0.0),
            20,
            0.5, 0.5, 0.5,
            0.1
        )

        plugin.logger.info("[Taunt] ${shooter.name} hit seeker ${hitPlayer.name} with snowball, awarded ${bonusPoints} bonus points")
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return

        // Check if firework rocket
        if (item.type != Material.FIREWORK_ROCKET) return

        plugin.logger.info("[Taunt] Firework usage detected by ${player.name}")

        // Check if player is in game
        val game = gameManager.activeGame
        if (game == null) {
            plugin.logger.info("[Taunt] No active game")
            return
        }

        val playerData = game.players[player.uniqueId]
        if (playerData == null) {
            plugin.logger.info("[Taunt] Player ${player.name} not in game")
            return
        }

        plugin.logger.info("[Taunt] Player role: ${playerData.role}, isCaptured: ${playerData.isCaptured}")

        // Only hiders can earn points from taunts
        if (playerData.role != PlayerRole.HIDER) {
            plugin.logger.info("[Taunt] Player is not a hider")
            return
        }

        // Check if hider is not captured
        if (playerData.isCaptured) {
            plugin.logger.info("[Taunt] Player is captured")
            return
        }

        // Calculate bonus points based on nearby seekers
        val bonusPoints = calculateFireworkBonus(player, game)
        pointManager.awardTauntBonus(player.uniqueId, bonusPoints)

        MessageUtil.send(player, "&e+${bonusPoints} ポイント &7(花火挑発)")
        player.sendMessage("§c警告: シーカーに位置が非常にバレやすくなりました！")

        // Audio feedback
        player.playSound(player.location, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f)

        // Spawn colorful firework
        spawnTauntFirework(player)

        plugin.logger.info("[Taunt] ${player.name} used firework, awarded ${bonusPoints} points")
    }

    /**
     * Calculate firework bonus based on proximity to seekers
     * Base: 100 points
     * +100 points per seeker within 10 blocks
     * +50 points per seeker within 20 blocks
     * +25 points per seeker within 30 blocks
     */
    private fun calculateFireworkBonus(player: Player, game: com.hideandseek.game.Game): Int {
        val basePoints = plugin.config.getInt("points.taunt-bonuses.firework", 100)
        var totalBonus = basePoints

        val seekers = game.getSeekers().mapNotNull { Bukkit.getPlayer(it) }
        plugin.logger.info("[Taunt] Calculating firework bonus - ${seekers.size} seekers in game")

        var closeCount = 0
        var mediumCount = 0
        var farCount = 0

        for (seeker in seekers) {
            if (seeker.world != player.world) {
                plugin.logger.info("[Taunt] Seeker ${seeker.name} in different world")
                continue
            }

            val distance = seeker.location.distance(player.location)
            plugin.logger.info("[Taunt] Seeker ${seeker.name} distance: ${distance.toInt()}m")

            when {
                distance <= 10.0 -> {
                    totalBonus += 100
                    closeCount++
                }
                distance <= 20.0 -> {
                    totalBonus += 50
                    mediumCount++
                }
                distance <= 30.0 -> {
                    totalBonus += 25
                    farCount++
                }
            }
        }

        plugin.logger.info("[Taunt] Firework bonus: base=$basePoints, close=$closeCount, medium=$mediumCount, far=$farCount, total=$totalBonus")

        // Send detailed breakdown
        if (closeCount > 0 || mediumCount > 0 || farCount > 0) {
            val breakdown = mutableListOf<String>()
            if (closeCount > 0) breakdown.add("&c極近 x${closeCount} (+${closeCount * 100})")
            if (mediumCount > 0) breakdown.add("&e近 x${mediumCount} (+${mediumCount * 50})")
            if (farCount > 0) breakdown.add("&7中 x${farCount} (+${farCount * 25})")

            MessageUtil.send(player, "&7距離ボーナス: ${breakdown.joinToString(" ")}")
        } else {
            MessageUtil.send(player, "&7付近に鬼なし（基本ポイントのみ）")
        }

        return totalBonus
    }

    private fun spawnTauntFirework(player: Player) {
        val location = player.location.clone().add(0.0, 1.0, 0.0)
        val firework = player.world.spawnEntity(location, EntityType.FIREWORK_ROCKET) as Firework
        val meta = firework.fireworkMeta

        // Create colorful effect
        val effect = FireworkEffect.builder()
            .withColor(Color.RED, Color.YELLOW, Color.LIME, Color.AQUA, Color.FUCHSIA)
            .with(FireworkEffect.Type.BALL_LARGE)
            .withTrail()
            .withFlicker()
            .build()

        meta.addEffect(effect)
        meta.power = 1
        firework.fireworkMeta = meta
    }
}
