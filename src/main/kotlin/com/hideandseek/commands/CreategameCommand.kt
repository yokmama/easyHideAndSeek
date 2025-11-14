package com.hideandseek.commands

import com.hideandseek.arena.ArenaManager
import com.hideandseek.i18n.MessageManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CreategameCommand(
    private val arenaManager: ArenaManager,
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

        if (!sender.hasPermission("hideandseek.admin")) {
            messageManager.send(sender, "error.no_permission")
            return true
        }

        // Handle setpos1
        if (args.isNotEmpty() && args[0].equals("setpos1", ignoreCase = true)) {
            arenaManager.setPos1(sender, sender.location)
            messageManager.send(
                sender,
                "admin.setpos1.success",
                sender.location.blockX,
                sender.location.blockY,
                sender.location.blockZ
            )
            return true
        }

        // Handle setpos2
        if (args.isNotEmpty() && args[0].equals("setpos2", ignoreCase = true)) {
            val session = arenaManager.getSetupSession(sender)
            arenaManager.setPos2(sender, sender.location)

            messageManager.send(
                sender,
                "admin.setpos2.success",
                sender.location.blockX,
                sender.location.blockY,
                sender.location.blockZ
            )

            session?.pos1?.let { pos1 ->
                val distance = pos1.distance(sender.location)
                messageManager.send(sender, "admin.setpos2.distance", String.format("%.1f", distance))
            }
            return true
        }

        // Handle arena creation
        if (args.isEmpty()) {
            messageManager.send(sender, "admin.creategame.usage")
            return true
        }

        val name = args[0]

        if (!name.matches(Regex("[a-zA-Z0-9_-]+"))) {
            messageManager.send(sender, "admin.creategame.invalid_name")
            return true
        }

        try {
            val arena = arenaManager.createArena(sender, name)
            messageManager.send(sender, "admin.creategame.success", name)
            messageManager.send(sender, "admin.creategame.success.world", arena.world.name)
            messageManager.send(sender, "admin.creategame.success.size", String.format("%.1f", arena.boundaries.size))
            messageManager.send(
                sender,
                "admin.creategame.success.center",
                arena.boundaries.center.blockX,
                arena.boundaries.center.blockY,
                arena.boundaries.center.blockZ
            )
        } catch (e: Exception) {
            messageManager.send(sender, "admin.creategame.error", e.message ?: "Unknown error")
        }

        return true
    }
}
