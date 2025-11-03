package com.hideandseek.listeners

import com.hideandseek.disguise.DisguiseManager
import com.hideandseek.game.GameManager
import com.hideandseek.game.PlayerRole
import com.hideandseek.utils.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDamageEvent

class BlockDamageListener(
    private val disguiseManager: DisguiseManager,
    private val gameManager: GameManager
) : Listener {

    @EventHandler
    fun onBlockDamage(event: BlockDamageEvent) {
        val attacker = event.player
        val block = event.block
        val location = block.location

        val game = gameManager.activeGame ?: return

        val attackerData = game.players[attacker.uniqueId] ?: return

        if (attackerData.role != PlayerRole.SEEKER) {
            return
        }

        val hiderId = disguiseManager.getDisguisedPlayerAt(location) ?: return

        event.isCancelled = true

        val hider = Bukkit.getPlayer(hiderId)
        if (hider == null) {
            disguiseManager.undisguiseByLocation(location)
            return
        }

        val hiderData = game.players[hiderId]
        if (hiderData == null) {
            disguiseManager.undisguise(hider, "capture")
            return
        }

        if (hiderData.isCaptured) {
            return
        }

        disguiseManager.undisguise(hider, "capture")

        hiderData.capture()
        attackerData.recordCapture()

        hider.gameMode = GameMode.SPECTATOR

        MessageUtil.send(attacker, "&aYou captured ${hider.name}!")
        MessageUtil.send(hider, "&cYou were captured by ${attacker.name}!")

        game.players.values.forEach { playerData ->
            Bukkit.getPlayer(playerData.uuid)?.let { player ->
                MessageUtil.send(player, "&e${hider.name} was captured! (${game.getCaptured().size}/${game.getHiders().size})")
            }
        }

        val winCondition = game.checkWinCondition(0)
        if (winCondition != null) {
            gameManager.endGame(winCondition)
        }
    }
}
