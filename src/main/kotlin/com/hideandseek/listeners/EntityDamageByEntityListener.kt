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
    private val gameManager: GameManager
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

        victim.gameMode = GameMode.SPECTATOR

        MessageUtil.send(attacker, "&aYou captured ${victim.name}!")
        MessageUtil.send(victim, "&cYou were captured by ${attacker.name}!")

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
