package com.hideandseek.commands

import com.hideandseek.game.GameManager
import com.hideandseek.utils.MessageUtil
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class LeaveCommand(
    private val gameManager: GameManager
) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            MessageUtil.send(sender, "&cPlayer only")
            return true
        }

        if (!sender.hasPermission("hideandseek.play")) {
            MessageUtil.send(sender, "&cNo permission")
            return true
        }

        gameManager.leaveGame(sender)
        return true
    }
}
