package com.hideandseek.commands

import com.hideandseek.arena.ArenaManager
import com.hideandseek.game.GameManager
import com.hideandseek.i18n.MessageManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class StartCommand(
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

        // Check if game is already running
        if (gameManager.activeGame != null) {
            messageManager.send(sender, "admin.start.already_in_progress")
            return true
        }

        // Get arena: use specified name or random
        val arena = if (args.isNotEmpty()) {
            val arenaName = args[0]
            val specifiedArena = arenaManager.getArena(arenaName)

            if (specifiedArena == null) {
                messageManager.send(sender, "admin.start.arena_not_found", arenaName)
                return true
            }
            specifiedArena
        } else {
            // No arena specified, choose random
            val randomArena = arenaManager.getRandomArena()
            if (randomArena == null) {
                messageManager.send(sender, "admin.start.no_arenas")
                return true
            }
            messageManager.send(sender, "admin.start.random_arena", randomArena.displayName)
            randomArena
        }

        // Check minimum players
        val waitingCount = gameManager.getWaitingPlayers().size
        val minPlayers = gameManager.configManager.getMinPlayers()
        if (waitingCount < minPlayers) {
            messageManager.send(sender, "admin.start.min_players", waitingCount, minPlayers)
            return true
        }

        // Start game
        val game = gameManager.startGame(arena)
        if (game != null) {
            messageManager.send(sender, "admin.start.success", arena.displayName)
            messageManager.send(sender, "admin.start.success.players", waitingCount)
            messageManager.send(sender, "admin.start.success.teams", game.getSeekers().size, game.getHiders().size)
        } else {
            messageManager.send(sender, "admin.start.failed")
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (!sender.hasPermission("hideandseek.admin")) return emptyList()

        return when (args.size) {
            1 -> arenaManager.getArenaNames().filter { it.startsWith(args[0].lowercase()) }
            else -> null
        }
    }
}
