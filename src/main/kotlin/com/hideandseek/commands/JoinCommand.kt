package com.hideandseek.commands

import com.hideandseek.game.GameManager
import com.hideandseek.i18n.MessageManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class JoinCommand(
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

        if (!sender.hasPermission("hideandseek.play")) {
            messageManager.send(sender, "error.no_permission")
            return true
        }

        gameManager.joinGame(sender)
        return true
    }
}
