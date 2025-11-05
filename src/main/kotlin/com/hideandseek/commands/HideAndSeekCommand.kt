package com.hideandseek.commands

import com.hideandseek.utils.MessageUtil
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class HideAndSeekCommand(
    private val joinCommand: JoinCommand,
    private val leaveCommand: LeaveCommand,
    private val adminCommand: AdminCommand,
    private val shopCommand: ShopCommand,
    private val spectatorCommand: SpectatorCommand,
    private val langCommand: LangCommand
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "join" -> joinCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "leave" -> leaveCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "shop" -> shopCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "spectator" -> spectatorCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "lang", "language" -> langCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "admin" -> adminCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> sendHelp(sender)
        }

        return true
    }

    private fun sendHelp(sender: CommandSender) {
        MessageUtil.send(
            sender,
            "&e===[ Hide and Seek ]===",
            "&7/hs join &f- Join game",
            "&7/hs leave &f- Leave game",
            "&7/hs shop &f- Open shop (in-game only)",
            "&7/hs lang [language|list] &f- Change language"
        )

        if (sender.hasPermission("hideandseek.spectate")) {
            MessageUtil.send(sender, "&7/hs spectator <on|off> &f- Toggle spectator mode")
        }

        if (sender.hasPermission("hideandseek.admin")) {
            MessageUtil.send(sender, "&7/hs admin &f- Admin commands")
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (args.size == 1) {
            val subcommands = mutableListOf("join", "leave", "shop", "lang")
            if (sender.hasPermission("hideandseek.spectate")) {
                subcommands.add("spectator")
            }
            if (sender.hasPermission("hideandseek.admin")) {
                subcommands.add("admin")
            }
            return subcommands.filter { it.startsWith(args[0].lowercase()) }
        }

        if (args.size > 1 && args[0].lowercase() == "admin") {
            return adminCommand.onTabComplete(sender, command, alias, args.drop(1).toTypedArray())
        }

        if (args.size > 1 && args[0].lowercase() == "spectator") {
            return spectatorCommand.onTabComplete(sender, command, alias, args.drop(1).toTypedArray())
        }

        if (args.size > 1 && (args[0].lowercase() == "lang" || args[0].lowercase() == "language")) {
            return langCommand.onTabComplete(sender, command, alias, args.drop(1).toTypedArray())
        }

        return null
    }
}
