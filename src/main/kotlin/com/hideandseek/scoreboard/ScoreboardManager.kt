package com.hideandseek.scoreboard

import com.hideandseek.game.Game
import com.hideandseek.game.GamePhase
import com.hideandseek.game.PlayerRole
import com.hideandseek.utils.MessageUtil
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.DisplaySlot

class ScoreboardManager(
    private val plugin: Plugin
) {
    private var gameUpdateTask: BukkitTask? = null
    private var lobbyUpdateTask: BukkitTask? = null
    private val legacySerializer = LegacyComponentSerializer.legacySection()

    fun startUpdating(game: Game) {
        stopGameUpdating()

        gameUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            updateScoreboards(game)
        }, 0L, 20L)
    }

    fun stopGameUpdating() {
        gameUpdateTask?.cancel()
        gameUpdateTask = null
    }

    fun startLobbyUpdating(waitingPlayers: List<java.util.UUID>, minPlayers: Int) {
        stopLobbyUpdating()

        lobbyUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            updateLobbyScoreboards(waitingPlayers, minPlayers)
        }, 0L, 20L)
    }

    fun stopLobbyUpdating() {
        lobbyUpdateTask?.cancel()
        lobbyUpdateTask = null
    }

    fun stopAllUpdating() {
        stopGameUpdating()
        stopLobbyUpdating()
    }

    private fun updateScoreboards(game: Game) {
        game.players.forEach { (uuid, playerData) ->
            Bukkit.getPlayer(uuid)?.let { player ->
                updatePlayerScoreboard(player, game, playerData.role)
            }
        }
    }

    private fun updatePlayerScoreboard(player: Player, game: Game, role: PlayerRole) {
        val scoreboard = Bukkit.getScoreboardManager().newScoreboard
        val objective = scoreboard.registerNewObjective(
            "hideandseek",
            "dummy",
            MessageUtil.colorize("&e&l[ Hide and Seek ]")
        )
        objective.displaySlot = DisplaySlot.SIDEBAR

        val lines = mutableListOf<String>()

        when (game.phase) {
            GamePhase.WAITING -> {
                lines.add("&7Status: &eWaiting")
                lines.add("&7Players: &a${game.players.size}")
            }
            GamePhase.PREPARATION -> {
                val elapsed = (System.currentTimeMillis() - game.phaseStartTime) / 1000
                val remaining = 30 - elapsed
                lines.add("&7Phase: &ePreparation")
                lines.add("&7Time: &a${remaining}s")
            }
            GamePhase.SEEKING -> {
                val elapsed = (System.currentTimeMillis() - game.phaseStartTime) / 1000
                val remaining = 600 - elapsed
                val minutes = remaining / 60
                val seconds = remaining % 60
                lines.add("&7Phase: &cSeeking")
                lines.add("&7Time: &a${minutes}:${String.format("%02d", seconds)}")
            }
            GamePhase.ENDED -> {
                lines.add("&7Phase: &cEnded")
            }
            GamePhase.POST_GAME -> {
                lines.add("&7Phase: &eRestarting")
                lines.add("&7Next game soon...")
            }
        }

        lines.add("&7")

        when (role) {
            PlayerRole.SEEKER -> {
                lines.add("&7Role: &cSeeker")
                val captured = game.getCaptured().size
                val total = game.getHiders().size
                lines.add("&7Captured: &a$captured/$total")

                val captureCount = game.players[player.uniqueId]?.captureCount ?: 0
                lines.add("&7Your captures: &e$captureCount")

                // Display points
                val pointMgr = (plugin as? com.hideandseek.HideAndSeekPlugin)?.pointManager
                val points = pointMgr?.getPoints(player.uniqueId) ?: 0
                lines.add("&7Points: &6$points")
            }
            PlayerRole.HIDER -> {
                lines.add("&7Role: &aHider")
                val remaining = game.getHiders().size - game.getCaptured().size
                lines.add("&7Remaining: &a$remaining")

                val isCaptured = game.players[player.uniqueId]?.isCaptured ?: false
                if (isCaptured) {
                    lines.add("&7Status: &cCaptured")
                } else {
                    lines.add("&7Status: &aHiding")
                }

                // Display points
                val pointMgr = (plugin as? com.hideandseek.HideAndSeekPlugin)?.pointManager
                val points = pointMgr?.getPoints(player.uniqueId) ?: 0
                lines.add("&7Points: &6$points")
            }
            PlayerRole.SPECTATOR -> {
                lines.add("&7Role: &7Spectator")
                val captured = game.getCaptured().size
                val total = game.getHiders().size
                lines.add("&7Captured: &a$captured/$total")
            }
        }

        lines.add("&8")
        lines.add("&7Arena: &f${game.arena.displayName}")

        // Convert Component to legacy string for scoreboard entries
        lines.asReversed().forEachIndexed { index, line ->
            val component = MessageUtil.colorize(line)
            val legacyString = legacySerializer.serialize(component)
            objective.getScore(legacyString).score = index
        }

        player.scoreboard = scoreboard
    }

    private fun updateLobbyScoreboards(waitingPlayers: List<java.util.UUID>, minPlayers: Int) {
        waitingPlayers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                updateLobbyScoreboard(player, waitingPlayers.size, minPlayers)
            }
        }
    }

    private fun updateLobbyScoreboard(player: Player, playerCount: Int, minPlayers: Int) {
        val scoreboard = Bukkit.getScoreboardManager().newScoreboard
        val objective = scoreboard.registerNewObjective(
            "hideandseek",
            "dummy",
            MessageUtil.colorize("&e&l[ Hide and Seek ]")
        )
        objective.displaySlot = DisplaySlot.SIDEBAR

        val lines = mutableListOf<String>()

        lines.add("&7Status: &eWaiting for players")
        lines.add("&7")
        lines.add("&7Players: &a$playerCount")
        lines.add("&7Minimum: &e$minPlayers")
        lines.add("&7")

        if (playerCount >= minPlayers) {
            lines.add("&aReady to start!")
            lines.add("&7Waiting for admin to")
            lines.add("&7use: &e/hs admin start")
        } else {
            val needed = minPlayers - playerCount
            lines.add("&cNeed $needed more player${if (needed > 1) "s" else ""}")
        }

        lines.add("&8")
        lines.add("&7Use &e/hs leave &7to exit")

        // Convert Component to legacy string for scoreboard entries
        lines.asReversed().forEachIndexed { index, line ->
            val component = MessageUtil.colorize(line)
            val legacyString = legacySerializer.serialize(component)
            objective.getScore(legacyString).score = index
        }

        player.scoreboard = scoreboard
    }

    fun clearScoreboard(player: Player) {
        player.scoreboard = Bukkit.getScoreboardManager().mainScoreboard
    }

    fun clearAllScoreboards(game: Game) {
        game.players.keys.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                clearScoreboard(player)
            }
        }
    }
}
