package com.hideandseek.commands

import com.hideandseek.i18n.MessageManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class HideAndSeekCommand(
    private val adminCommand: AdminCommand,
    private val shopCommand: ShopCommand,
    private val spectatorCommand: SpectatorCommand,
    private val langCommand: LangCommand,
    private val creategameCommand: CreategameCommand,
    private val listCommand: ListCommand,
    private val startCommand: StartCommand,
    private val messageManager: MessageManager
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
            "shop" -> shopCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "spectator" -> spectatorCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "lang", "language" -> langCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "admin" -> adminCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "creategame" -> creategameCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "list" -> listCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "start" -> startCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> sendHelp(sender)
        }

        return true
    }

    private fun sendHelp(sender: CommandSender) {
        messageManager.send(sender, "command.help.title")
        messageManager.send(sender, "command.help.auto_join")
        messageManager.send(sender, "command.help.shop")
        messageManager.send(sender, "command.help.lang")

        if (sender.hasPermission("hideandseek.spectate")) {
            messageManager.send(sender, "command.help.spectator")
        }

        if (sender.hasPermission("hideandseek.admin")) {
            messageManager.send(sender, "command.help.admin")
            messageManager.send(sender, "command.help.admin.creategame")
            messageManager.send(sender, "command.help.admin.creategame_pos")
            messageManager.send(sender, "command.help.admin.list")
            messageManager.send(sender, "command.help.admin.start")
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (args.size == 1) {
            val subcommands = mutableListOf("shop", "lang")
            if (sender.hasPermission("hideandseek.spectate")) {
                subcommands.add("spectator")
            }
            if (sender.hasPermission("hideandseek.admin")) {
                subcommands.addAll(listOf("admin", "creategame", "list", "start"))
            }
            return subcommands.filter { it.startsWith(args[0].lowercase()) }
        }

        if (args.size > 1) {
            return when (args[0].lowercase()) {
                "admin" -> adminCommand.onTabComplete(sender, command, alias, args.drop(1).toTypedArray())
                "spectator" -> spectatorCommand.onTabComplete(sender, command, alias, args.drop(1).toTypedArray())
                "lang", "language" -> langCommand.onTabComplete(sender, command, alias, args.drop(1).toTypedArray())
                "start" -> startCommand.onTabComplete(sender, command, alias, args.drop(1).toTypedArray())
                else -> null
            }
        }

        return null
    }
}
