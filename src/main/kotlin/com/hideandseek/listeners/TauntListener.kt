package com.hideandseek.listeners

import com.hideandseek.game.GameManager
import com.hideandseek.game.PlayerRole
import com.hideandseek.points.PointManager
import com.hideandseek.utils.MessageUtil
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
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

        // Award snowball taunt bonus
        val bonusPoints = plugin.config.getInt("points.taunt-bonuses.snowball", 50)
        pointManager.awardTauntBonus(shooter.uniqueId, bonusPoints)

        MessageUtil.send(shooter, "&e+${bonusPoints} ポイント &7(雪玉挑発)")
        shooter.sendMessage("§c警告: シーカーに位置がバレやすくなりました！")
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return

        // Check if firework rocket
        if (item.type != Material.FIREWORK_ROCKET) return

        // Check if player is in game
        val game = gameManager.activeGame ?: return
        val playerData = game.players[player.uniqueId] ?: return

        // Only hiders can earn points from taunts
        if (playerData.role != PlayerRole.HIDER) return

        // Check if hider is not captured
        if (playerData.isCaptured) return

        // Award firework taunt bonus
        val bonusPoints = plugin.config.getInt("points.taunt-bonuses.firework", 100)
        pointManager.awardTauntBonus(player.uniqueId, bonusPoints)

        MessageUtil.send(player, "&e+${bonusPoints} ポイント &7(花火挑発)")
        player.sendMessage("§c警告: シーカーに位置が非常にバレやすくなりました！")

        // Spawn colorful firework
        spawnTauntFirework(player)
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
