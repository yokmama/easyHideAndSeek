package com.hideandseek.listeners

import com.hideandseek.disguise.DisguiseManager
import com.hideandseek.game.GameManager
import com.hideandseek.game.PlayerRole
import com.hideandseek.utils.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

class EntityDamageByEntityListener(
    private val disguiseManager: DisguiseManager,
    private val gameManager: GameManager,
    private val pointManager: com.hideandseek.points.PointManager?,
    private val plugin: org.bukkit.plugin.Plugin
) : Listener {

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val victim = event.entity as? Player ?: return

        val game = gameManager.activeGame ?: return

        val attackerData = game.players[attacker.uniqueId] ?: return
        val victimData = game.players[victim.uniqueId] ?: return

        event.isCancelled = true

        if (attackerData.role != PlayerRole.SEEKER) {
            return
        }

        if (victimData.role != PlayerRole.HIDER) {
            return
        }

        if (victimData.isCaptured) {
            return
        }

        if (disguiseManager.isDisguised(victim.uniqueId)) {
            disguiseManager.undisguise(victim, "capture")
        }

        victimData.capture()
        attackerData.recordCapture()

        // Handle point stealing
        val stealPercentage = plugin.config.getDouble("points.capture-steal-percentage", 0.5)
        val pointsStolen = pointManager?.handleCapture(attacker.uniqueId, victim.uniqueId, stealPercentage) ?: 0

        victim.gameMode = GameMode.SPECTATOR

        MessageUtil.send(attacker, "&aYou captured ${victim.name}!")
        if (pointsStolen > 0) {
            MessageUtil.send(attacker, "&e+${pointsStolen} ポイント獲得 &7(${victim.name}のポイントを奪取)")
        }
        MessageUtil.send(victim, "&cYou were captured by ${attacker.name}!")
        if (pointsStolen > 0) {
            MessageUtil.send(victim, "&c-${pointsStolen} ポイント &7(${attacker.name}に奪われた)")
        }

        game.players.values.forEach { playerData ->
            Bukkit.getPlayer(playerData.uuid)?.let { player ->
                MessageUtil.send(player, "&e${victim.name} was captured! (${game.getCaptured().size}/${game.getHiders().size})")
            }
        }

        val winCondition = game.checkWinCondition(0)
        if (winCondition != null) {
            gameManager.endGame(winCondition)
        }
    }
}
