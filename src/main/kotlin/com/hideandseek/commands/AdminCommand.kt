package com.hideandseek.commands

import com.hideandseek.arena.ArenaManager
import com.hideandseek.game.GameManager
import com.hideandseek.i18n.MessageManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class AdminCommand(
    private val arenaManager: ArenaManager,
    private val gameManager: GameManager,
    private val messageManager: MessageManager
) : CommandExecutor, TabCompleter {

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

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "setpos1" -> handleSetPos1(sender)
            "setpos2" -> handleSetPos2(sender)
            "creategame" -> handleCreateGame(sender, args)
            "list" -> handleList(sender)
            "delete" -> handleDelete(sender, args)
            "start" -> handleStart(sender, args)
            "reload" -> handleReload(sender)
            else -> sendHelp(sender)
        }

        return true
    }

    private fun handleSetPos1(sender: CommandSender) {
        if (sender !is Player) {
            messageManager.send(sender, "error.player_only")
            return
        }

        arenaManager.setPos1(sender, sender.location)
        messageManager.send(
            sender,
            "admin.setpos1.success",
            sender.location.blockX,
            sender.location.blockY,
            sender.location.blockZ
        )
    }

    private fun handleSetPos2(sender: CommandSender) {
        if (sender !is Player) {
            messageManager.send(sender, "error.player_only")
            return
        }

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
    }

    private fun handleCreateGame(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            messageManager.send(sender, "error.player_only")
            return
        }

        if (args.size < 2) {
            messageManager.send(sender, "admin.creategame.usage")
            return
        }

        val name = args[1]

        if (!name.matches(Regex("[a-zA-Z0-9_-]+"))) {
            messageManager.send(sender, "admin.creategame.invalid_name")
            return
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
    }

    private fun handleList(sender: CommandSender) {
        val arenas = arenaManager.getAllArenas()

        if (arenas.isEmpty()) {
            messageManager.send(sender, "admin.list.empty")
            return
        }

        messageManager.send(sender, "admin.list.title")
        arenas.values.forEach { arena ->
            messageManager.send(sender, "admin.list.item", arena.name, arena.world.name)
        }
        messageManager.send(sender, "admin.list.total", arenas.size)
    }

    private fun handleDelete(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            messageManager.send(sender, "admin.delete.usage")
            return
        }

        val name = args[1]

        try {
            if (arenaManager.deleteArena(name)) {
                messageManager.send(sender, "admin.delete.success", name)
            } else {
                messageManager.send(sender, "admin.delete.not_found", name)
            }
        } catch (e: Exception) {
            messageManager.send(sender, "admin.delete.error", e.message ?: "Unknown error")
        }
    }

    private fun handleStart(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            messageManager.send(sender, "admin.start.usage")
            return
        }

        val arenaName = args[1]
        val arena = arenaManager.getArena(arenaName)

        if (arena == null) {
            messageManager.send(sender, "admin.start.arena_not_found", arenaName)
            return
        }

        if (gameManager.activeGame != null) {
            messageManager.send(sender, "admin.start.already_in_progress")
            return
        }

        val waitingCount = gameManager.getWaitingPlayers().size
        if (waitingCount < 2) {
            messageManager.send(sender, "admin.start.min_players", waitingCount)
            return
        }

        val game = gameManager.startGame(arena)
        if (game != null) {
            messageManager.send(sender, "admin.start.success", arenaName)
            messageManager.send(sender, "admin.start.success.players", waitingCount)
            messageManager.send(sender, "admin.start.success.teams", game.getSeekers().size, game.getHiders().size)
        } else {
            messageManager.send(sender, "admin.start.failed")
        }
    }

    private fun handleReload(sender: CommandSender) {
        try {
            messageManager.reload()
            val languages = messageManager.getAvailableLanguages().joinToString(", ")
            messageManager.send(sender, "admin.reload.success")
            messageManager.send(sender, "admin.reload.i18n_reloaded", languages)
        } catch (e: Exception) {
            messageManager.send(sender, "admin.reload.error", e.message ?: "Unknown error")
        }
    }

    private fun sendHelp(sender: CommandSender) {
        messageManager.send(sender, "admin.help.title")
        messageManager.send(sender, "admin.help.setpos1")
        messageManager.send(sender, "admin.help.setpos2")
        messageManager.send(sender, "admin.help.creategame")
        messageManager.send(sender, "admin.help.list")
        messageManager.send(sender, "admin.help.delete")
        messageManager.send(sender, "admin.help.start")
        messageManager.send(sender, "admin.help.reload")
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (!sender.hasPermission("hideandseek.admin")) return emptyList()

        return when (args.size) {
            1 -> listOf("setpos1", "setpos2", "creategame", "list", "delete", "start", "reload")
                .filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "delete", "start" -> arenaManager.getArenaNames().filter { it.startsWith(args[1].lowercase()) }
                else -> null
            }
            else -> null
        }
    }
}
