package com.hideandseek.commands

import com.hideandseek.game.GameManager
import com.hideandseek.shop.ShopManager
import com.hideandseek.utils.MessageUtil
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ShopCommand(
    private val shopManager: ShopManager,
    private val gameManager: GameManager
) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            MessageUtil.send(sender, "&cOnly players can use this command")
            return true
        }

        val game = gameManager.activeGame
        if (game == null) {
            MessageUtil.send(sender, "&cNo active game")
            return true
        }

        val playerData = game.players[sender.uniqueId]
        if (playerData == null) {
            MessageUtil.send(sender, "&cYou are not in this game")
            return true
        }

        // Pass player role to filter shop items
        val playerRole = playerData.role.name // "SEEKER", "HIDER", or "SPECTATOR"
        sender.sendMessage("§a[DEBUG ShopCommand] Player role: $playerRole")
        sender.sendMessage("§a[DEBUG ShopCommand] PlayerData.role: ${playerData.role}")
        shopManager.openMainMenu(sender, playerRole)
        return true
    }
}
