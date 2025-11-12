package com.hideandseek.commands

import com.hideandseek.game.GameManager
import com.hideandseek.i18n.MessageManager
import com.hideandseek.shop.ShopManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ShopCommand(
    private val shopManager: ShopManager,
    private val gameManager: GameManager,
    private val messageManager: MessageManager
) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            messageManager.send(sender, "error.player_only")
            return true
        }

        val game = gameManager.activeGame
        if (game == null) {
            messageManager.send(sender, "error.no_active_game")
            return true
        }

        val playerData = game.players[sender.uniqueId]
        if (playerData == null) {
            messageManager.send(sender, "error.not_in_game")
            return true
        }

        // Pass player role to filter shop items
        val playerRole = playerData.role.name // "SEEKER", "HIDER", or "SPECTATOR"
        shopManager.openMainMenu(sender, playerRole)
        return true
    }
}
