package com.hideandseek.commands

import com.hideandseek.arena.ArenaManager
import com.hideandseek.i18n.MessageManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class ListCommand(
    private val arenaManager: ArenaManager,
    private val messageManager: MessageManager
) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission("hideandseek.admin")) {
            messageManager.send(sender, "error.no_permission")
            return true
        }

        val arenas = arenaManager.getAllArenas()

        if (arenas.isEmpty()) {
            messageManager.send(sender, "admin.list.empty")
            return true
        }

        messageManager.send(sender, "admin.list.title")
        arenas.values.forEach { arena ->
            messageManager.send(sender, "admin.list.item", arena.name, arena.world.name)
        }
        messageManager.send(sender, "admin.list.total", arenas.size)

        return true
    }
}
