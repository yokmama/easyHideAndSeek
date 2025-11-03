package com.hideandseek.commands

import com.hideandseek.arena.ArenaManager
import com.hideandseek.game.GameManager
import com.hideandseek.utils.MessageUtil
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class AdminCommand(
    private val arenaManager: ArenaManager,
    private val gameManager: GameManager
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission("hideandseek.admin")) {
            MessageUtil.send(sender, "&cNo permission")
            return true
        }

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "setpos1" -> handleSetPos1(sender)
            "setpos2" -> handleSetPos2(sender)
            "setspawn" -> handleSetSpawn(sender, args)
            "creategame" -> handleCreateGame(sender, args)
            "list" -> handleList(sender)
            "delete" -> handleDelete(sender, args)
            "start" -> handleStart(sender, args)
            else -> sendHelp(sender)
        }

        return true
    }

    private fun handleSetPos1(sender: CommandSender) {
        if (sender !is Player) {
            MessageUtil.send(sender, "&cPlayer only")
            return
        }

        arenaManager.setPos1(sender, sender.location)
        MessageUtil.send(
            sender,
            "&aPos1 set: X=${sender.location.blockX}, Y=${sender.location.blockY}, Z=${sender.location.blockZ}"
        )
    }

    private fun handleSetPos2(sender: CommandSender) {
        if (sender !is Player) {
            MessageUtil.send(sender, "&cPlayer only")
            return
        }

        val session = arenaManager.getSetupSession(sender)
        arenaManager.setPos2(sender, sender.location)

        MessageUtil.send(
            sender,
            "&aPos2 set: X=${sender.location.blockX}, Y=${sender.location.blockY}, Z=${sender.location.blockZ}"
        )

        session?.pos1?.let { pos1 ->
            val distance = pos1.distance(sender.location)
            MessageUtil.send(sender, "&7Area size: ${String.format("%.1f", distance)} blocks (diagonal)")
        }
    }

    private fun handleSetSpawn(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            MessageUtil.send(sender, "&cPlayer only")
            return
        }

        if (args.size < 2) {
            MessageUtil.send(sender, "&cUsage: /hs admin setspawn <seeker|hider>")
            return
        }

        val role = args[1].lowercase()
        if (role != "seeker" && role != "hider") {
            MessageUtil.send(sender, "&cUsage: /hs admin setspawn <seeker|hider>")
            return
        }

        try {
            arenaManager.setSpawn(sender, role, sender.location)
            MessageUtil.send(
                sender,
                "&a${role.replaceFirstChar { it.uppercase() }} spawn set: X=${sender.location.blockX}, Y=${sender.location.blockY}, Z=${sender.location.blockZ}"
            )
        } catch (e: Exception) {
            MessageUtil.send(sender, "&c${e.message}")
        }
    }

    private fun handleCreateGame(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            MessageUtil.send(sender, "&cPlayer only")
            return
        }

        if (args.size < 2) {
            MessageUtil.send(sender, "&cUsage: /hs admin creategame <name>")
            return
        }

        val name = args[1]

        if (!name.matches(Regex("[a-zA-Z0-9_-]+"))) {
            MessageUtil.send(sender, "&cName can only contain alphanumeric characters, hyphens, and underscores")
            return
        }

        try {
            val arena = arenaManager.createArena(sender, name)
            MessageUtil.send(
                sender,
                "&aArena '$name' created!",
                "&7- World: ${arena.world.name}",
                "&7- Size: ${String.format("%.1f", arena.boundaries.size)} blocks",
                "&7- Seeker spawn: ${arena.spawns.seeker.blockX}, ${arena.spawns.seeker.blockY}, ${arena.spawns.seeker.blockZ}",
                "&7- Hider spawn: ${arena.spawns.hider.blockX}, ${arena.spawns.hider.blockY}, ${arena.spawns.hider.blockZ}"
            )
        } catch (e: Exception) {
            MessageUtil.send(sender, "&c${e.message}")
        }
    }

    private fun handleList(sender: CommandSender) {
        val arenas = arenaManager.getAllArenas()

        if (arenas.isEmpty()) {
            MessageUtil.send(sender, "&7No arenas configured. Use /hs admin creategame to create one.")
            return
        }

        MessageUtil.send(sender, "&e===[ Arenas ]===")
        arenas.values.forEach { arena ->
            MessageUtil.send(sender, "&a- ${arena.name} &7(${arena.world.name})")
        }
        MessageUtil.send(sender, "&7Total: ${arenas.size}")
    }

    private fun handleDelete(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            MessageUtil.send(sender, "&cUsage: /hs admin delete <name>")
            return
        }

        val name = args[1]

        try {
            if (arenaManager.deleteArena(name)) {
                MessageUtil.send(sender, "&aArena '$name' deleted")
            } else {
                MessageUtil.send(sender, "&cArena '$name' not found")
            }
        } catch (e: Exception) {
            MessageUtil.send(sender, "&c${e.message}")
        }
    }

    private fun handleStart(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            MessageUtil.send(sender, "&cUsage: /hs admin start <arena-name>")
            return
        }

        val arenaName = args[1]
        val arena = arenaManager.getArena(arenaName)

        if (arena == null) {
            MessageUtil.send(sender, "&cArena '$arenaName' not found. Use /hs admin list to see available arenas.")
            return
        }

        if (gameManager.activeGame != null) {
            MessageUtil.send(sender, "&cGame already in progress")
            return
        }

        val waitingCount = gameManager.getWaitingPlayers().size
        if (waitingCount < 2) {
            MessageUtil.send(sender, "&cMinimum 2 players required (current: $waitingCount)")
            return
        }

        val game = gameManager.startGame(arena)
        if (game != null) {
            MessageUtil.send(sender, "&aGame started on arena '$arenaName'!")
            MessageUtil.send(sender, "&7Players: $waitingCount")
            MessageUtil.send(sender, "&7Seekers: ${game.getSeekers().size}, Hiders: ${game.getHiders().size}")
        } else {
            MessageUtil.send(sender, "&cFailed to start game")
        }
    }

    private fun sendHelp(sender: CommandSender) {
        MessageUtil.send(
            sender,
            "&e===[ Hide and Seek Admin ]===",
            "&7/hs admin setpos1 &f- Set first corner",
            "&7/hs admin setpos2 &f- Set second corner",
            "&7/hs admin setspawn <seeker|hider> &f- Set spawn point",
            "&7/hs admin creategame <name> &f- Create arena",
            "&7/hs admin list &f- List arenas",
            "&7/hs admin delete <name> &f- Delete arena",
            "&7/hs admin start <arena> &f- Start game"
        )
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (!sender.hasPermission("hideandseek.admin")) return emptyList()

        return when (args.size) {
            1 -> listOf("setpos1", "setpos2", "setspawn", "creategame", "list", "delete", "start")
                .filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "setspawn" -> listOf("seeker", "hider").filter { it.startsWith(args[1].lowercase()) }
                "delete", "start" -> arenaManager.getArenaNames().filter { it.startsWith(args[1].lowercase()) }
                else -> null
            }
            else -> null
        }
    }
}
