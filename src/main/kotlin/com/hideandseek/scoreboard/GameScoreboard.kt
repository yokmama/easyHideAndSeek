package com.hideandseek.scoreboard

import com.hideandseek.game.Game
import com.hideandseek.game.GamePhase
import com.hideandseek.game.PlayerRole
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import java.util.UUID

/**
 * Simple scoreboard display for Hide and Seek game
 */
class GameScoreboard(
    private val plugin: Plugin
) {
    private var updateTask: BukkitTask? = null
    private val playerScoreboards = mutableMapOf<UUID, Scoreboard>()

    /**
     * Start updating scoreboards for all players in game
     */
    fun startUpdating(game: Game) {
        // Cancel any existing task
        stopUpdating()

        // Create scoreboards for all players
        game.players.keys.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                createScoreboard(player, game)
            }
        }

        // Start update task (every second = 20 ticks)
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            game.players.keys.forEach { uuid ->
                Bukkit.getPlayer(uuid)?.let { player ->
                    updateScoreboard(player, game)
                }
            }
        }, 20L, 20L)

        plugin.logger.info("[Scoreboard] Started scoreboard updates")
    }

    /**
     * Stop updating scoreboards
     */
    fun stopUpdating() {
        updateTask?.cancel()
        updateTask = null
        plugin.logger.info("[Scoreboard] Stopped scoreboard updates")
    }

    /**
     * Clear all scoreboards
     */
    fun clearAll() {
        stopUpdating()
        playerScoreboards.keys.toList().forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                clearScoreboard(player)
            }
        }
        playerScoreboards.clear()
        plugin.logger.info("[Scoreboard] Cleared all scoreboards")
    }

    /**
     * Create scoreboard for a player
     */
    private fun createScoreboard(player: Player, game: Game) {
        val scoreboard = Bukkit.getScoreboardManager().newScoreboard

        // Copy teams from main scoreboard to keep name colors
        copyTeamsFromMain(scoreboard)

        // Create objective for sidebar
        val objective = scoreboard.registerNewObjective(
            "hideandseek",
            "dummy",
            "§6§l🎮 Hide and Seek"
        )
        objective.displaySlot = DisplaySlot.SIDEBAR

        player.scoreboard = scoreboard
        playerScoreboards[player.uniqueId] = scoreboard

        plugin.logger.info("[Scoreboard] Created scoreboard for ${player.name} with teams copied")

        // Initial update
        updateScoreboard(player, game)
    }

    /**
     * Copy teams from main scoreboard to individual scoreboard
     * This is necessary to preserve player name colors
     */
    private fun copyTeamsFromMain(targetScoreboard: org.bukkit.scoreboard.Scoreboard) {
        val mainScoreboard = Bukkit.getScoreboardManager().mainScoreboard

        // Team color mapping
        val teamColors = mapOf(
            "hs_seekers" to org.bukkit.ChatColor.RED,
            "hs_hiders" to org.bukkit.ChatColor.GREEN,
            "hs_spectators" to org.bukkit.ChatColor.GRAY
        )

        val teamVisibility = mapOf(
            "hs_seekers" to org.bukkit.scoreboard.Team.OptionStatus.ALWAYS,
            "hs_hiders" to org.bukkit.scoreboard.Team.OptionStatus.NEVER,
            "hs_spectators" to org.bukkit.scoreboard.Team.OptionStatus.ALWAYS
        )

        // Copy each team
        listOf("hs_seekers", "hs_hiders", "hs_spectators").forEach { teamName ->
            mainScoreboard.getTeam(teamName)?.let { mainTeam ->
                // Create team in target scoreboard
                val newTeam = targetScoreboard.registerNewTeam(teamName)

                // Set team properties using known values
                val color = teamColors[teamName] ?: org.bukkit.ChatColor.WHITE
                val visibility = teamVisibility[teamName] ?: org.bukkit.scoreboard.Team.OptionStatus.ALWAYS

                @Suppress("DEPRECATION")
                newTeam.setColor(color)
                @Suppress("DEPRECATION")
                newTeam.prefix = when (color) {
                    org.bukkit.ChatColor.RED -> "§c"
                    org.bukkit.ChatColor.GREEN -> "§a"
                    org.bukkit.ChatColor.GRAY -> "§7"
                    else -> ""
                }
                @Suppress("DEPRECATION")
                newTeam.suffix = ""
                newTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, visibility)

                // Copy all entries (player names)
                mainTeam.entries.forEach { entry ->
                    newTeam.addEntry(entry)
                }

                plugin.logger.info("[Scoreboard] Copied team $teamName with ${mainTeam.entries.size} entries, color=$color")
            }
        }
    }

    /**
     * Update scoreboard content for a player
     */
    private fun updateScoreboard(player: Player, game: Game) {
        val scoreboard = playerScoreboards[player.uniqueId] ?: return
        val objective = scoreboard.getObjective("hideandseek") ?: return

        // Clear existing scores
        scoreboard.entries.forEach { entry ->
            scoreboard.resetScores(entry)
        }

        val playerData = game.players[player.uniqueId]
        val lines = mutableListOf<String>()

        // Add empty line at top
        lines.add("§7")

        // Phase and time
        when (game.phase) {
            GamePhase.PREPARATION -> {
                val elapsed = (System.currentTimeMillis() - game.phaseStartTime) / 1000
                val remaining = 30 - elapsed.toInt() // 30 seconds preparation
                lines.add("§e⏰ 準備時間: §f${remaining}秒")
            }
            GamePhase.SEEKING -> {
                val elapsed = (System.currentTimeMillis() - game.phaseStartTime) / 1000
                val seekTime = 600 // 10 minutes = 600 seconds
                val remaining = (seekTime - elapsed).toInt()
                val minutes = remaining / 60
                val seconds = remaining % 60
                lines.add("§e⏰ 残り時間: §f${minutes}:${seconds.toString().padStart(2, '0')}")
            }
            else -> {
                lines.add("§e⏰ 待機中...")
            }
        }

        lines.add("§8")

        // Player counts
        val seekers = game.getSeekers().size
        val hiders = game.getHiders().size
        val captured = game.getCaptured().size

        lines.add("§c👹 鬼: §f${seekers}人")
        lines.add("§a🏃 人: §f${hiders}人")
        lines.add("§7💀 鬼化済み: §f${captured}人")

        lines.add("§9")

        // Player's role
        if (playerData != null) {
            val roleText = when (playerData.role) {
                PlayerRole.SEEKER -> "§c鬼"
                PlayerRole.HIDER -> if (playerData.isCaptured) "§7鬼化済み" else "§a人"
                PlayerRole.SPECTATOR -> "§7観戦者"
            }
            lines.add("§6あなた: $roleText")
        }

        lines.add("§0")

        // Set scores (reverse order for correct display)
        lines.reversed().forEachIndexed { index, line ->
            objective.getScore(line).score = index
        }
    }

    /**
     * Clear scoreboard for a player
     */
    private fun clearScoreboard(player: Player) {
        val mainScoreboard = Bukkit.getScoreboardManager().mainScoreboard
        player.scoreboard = mainScoreboard
        playerScoreboards.remove(player.uniqueId)
    }

    /**
     * Add player to scoreboard during game
     */
    fun addPlayer(player: Player, game: Game) {
        createScoreboard(player, game)
        plugin.logger.info("[Scoreboard] Added ${player.name} to scoreboard")
    }

    /**
     * Remove player from scoreboard
     */
    fun removePlayer(player: Player) {
        clearScoreboard(player)
        plugin.logger.info("[Scoreboard] Removed ${player.name} from scoreboard")
    }
}
