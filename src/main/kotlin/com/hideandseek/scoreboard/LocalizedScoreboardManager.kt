package com.hideandseek.scoreboard

import com.hideandseek.game.Game
import com.hideandseek.game.GamePhase
import com.hideandseek.game.PlayerRole
import com.hideandseek.i18n.MessageManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import java.util.UUID

/**
 * Localized scoreboard display for Hide and Seek game.
 *
 * Each player receives a scoreboard in their own language.
 * Updates every 1-2 seconds to reflect game state changes.
 */
class LocalizedScoreboardManager(
    private val plugin: Plugin,
    private val messageManager: MessageManager
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

        plugin.logger.info("[LocalizedScoreboard] Started scoreboard updates")
    }

    /**
     * Stop updating scoreboards
     */
    fun stopUpdating() {
        updateTask?.cancel()
        updateTask = null
        plugin.logger.info("[LocalizedScoreboard] Stopped scoreboard updates")
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
        plugin.logger.info("[LocalizedScoreboard] Cleared all scoreboards")
    }

    /**
     * Create scoreboard for a player
     */
    private fun createScoreboard(player: Player, game: Game) {
        val scoreboard = Bukkit.getScoreboardManager().newScoreboard

        // Copy teams from main scoreboard to keep name colors
        copyTeamsFromMain(scoreboard)

        // Create objective for sidebar with localized title
        val title = messageManager.getRawMessage(player, "ui.scoreboard.title")
        val objective = scoreboard.registerNewObjective(
            "hideandseek",
            "dummy",
            title.replace('&', '§')
        )
        objective.displaySlot = DisplaySlot.SIDEBAR

        player.scoreboard = scoreboard
        playerScoreboards[player.uniqueId] = scoreboard

        plugin.logger.info("[LocalizedScoreboard] Created scoreboard for ${player.name} with teams copied")

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
            }
        }
    }

    /**
     * Build localized scoreboard lines for a player
     */
    private fun buildLocalizedLines(player: Player, game: Game): List<String> {
        val playerData = game.players[player.uniqueId]
        val lines = mutableListOf<String>()

        // Add empty line at top
        lines.add("§7")

        // Phase and time
        when (game.phase) {
            GamePhase.PREPARATION -> {
                val elapsed = (System.currentTimeMillis() - game.phaseStartTime) / 1000
                val remaining = 30 - elapsed.toInt()
                val timeText = messageManager.getRawMessage(player, "ui.scoreboard.phase.preparation", remaining)
                lines.add(timeText.replace('&', '§'))
            }
            GamePhase.SEEKING -> {
                val elapsed = (System.currentTimeMillis() - game.phaseStartTime) / 1000
                val seekTime = 600 // 10 minutes = 600 seconds
                val remaining = (seekTime - elapsed).toInt()
                val minutes = remaining / 60
                val seconds = remaining % 60
                val timeString = "${minutes}:${seconds.toString().padStart(2, '0')}"
                val timeText = messageManager.getRawMessage(player, "ui.scoreboard.phase.seeking", timeString)
                lines.add(timeText.replace('&', '§'))
            }
            GamePhase.POST_GAME -> {
                val timeText = messageManager.getRawMessage(player, "ui.scoreboard.phase.post_game")
                lines.add(timeText.replace('&', '§'))
            }
            else -> {
                val timeText = messageManager.getRawMessage(player, "ui.scoreboard.phase.waiting")
                lines.add(timeText.replace('&', '§'))
            }
        }

        lines.add("§8")

        // Player counts
        val seekers = game.getSeekers().size
        val hiders = game.getHiders().size
        val captured = game.getCaptured().size

        val seekersText = messageManager.getRawMessage(player, "ui.scoreboard.seekers", seekers)
        lines.add(seekersText.replace('&', '§'))

        val hidersText = messageManager.getRawMessage(player, "ui.scoreboard.hiders", hiders)
        lines.add(hidersText.replace('&', '§'))

        val capturedText = messageManager.getRawMessage(player, "ui.scoreboard.captured", captured)
        lines.add(capturedText.replace('&', '§'))

        lines.add("§9")

        // Player's role
        if (playerData != null) {
            val roleKey = when (playerData.role) {
                PlayerRole.SEEKER -> "ui.scoreboard.role.seeker"
                PlayerRole.HIDER -> if (playerData.isCaptured) "ui.scoreboard.role.captured" else "ui.scoreboard.role.hider"
                PlayerRole.SPECTATOR -> "ui.scoreboard.role.spectator"
            }
            val roleText = messageManager.getRawMessage(player, roleKey)
            val yourRoleText = messageManager.getRawMessage(player, "ui.scoreboard.your_role", roleText)
            lines.add(yourRoleText.replace('&', '§'))

            // Player's points
            val pointMgr = (plugin as? com.hideandseek.HideAndSeekPlugin)?.pointManager
            val points = pointMgr?.getPoints(player.uniqueId) ?: 0
            val pointsText = messageManager.getRawMessage(player, "ui.scoreboard.your_points", points)
            lines.add(pointsText.replace('&', '§'))
        }

        lines.add("§0")

        return lines
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

        // Build localized lines
        val lines = buildLocalizedLines(player, game)

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
        plugin.logger.info("[LocalizedScoreboard] Added ${player.name} to scoreboard")
    }

    /**
     * Remove player from scoreboard
     */
    fun removePlayer(player: Player) {
        clearScoreboard(player)
        plugin.logger.info("[LocalizedScoreboard] Removed ${player.name} from scoreboard")
    }
}
