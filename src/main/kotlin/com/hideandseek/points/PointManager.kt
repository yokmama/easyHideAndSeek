package com.hideandseek.points

import com.hideandseek.game.Game
import com.hideandseek.game.PlayerRole
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.UUID

/**
 * Manages player points in the game
 * - Hiders earn points over time while alive
 * - Hiders earn bonus points for using taunt items
 * - Seekers steal points from hiders when capturing them
 */
class PointManager(private val plugin: Plugin) {

    private val playerPoints = mutableMapOf<UUID, Int>()
    private var pointAccumulationTaskId: Int? = null

    /**
     * Get points for a player
     */
    fun getPoints(playerId: UUID): Int {
        return playerPoints[playerId] ?: 0
    }

    /**
     * Add points to a player
     */
    fun addPoints(playerId: UUID, points: Int) {
        val current = playerPoints[playerId] ?: 0
        playerPoints[playerId] = current + points
    }

    /**
     * Set points for a player
     */
    fun setPoints(playerId: UUID, points: Int) {
        playerPoints[playerId] = points
    }

    /**
     * Reset all points
     */
    fun resetAll() {
        playerPoints.clear()
    }

    /**
     * Start accumulating points for alive hiders
     * @param game The active game
     * @param pointsPerSecond Points earned per second
     * @param updateInterval Update interval in ticks (default 15 seconds = 300 ticks)
     */
    fun startPointAccumulation(game: Game, pointsPerSecond: Double, updateInterval: Long = 300L) {
        stopPointAccumulation()

        pointAccumulationTaskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            // Calculate points for this interval
            val secondsElapsed = updateInterval / 20.0
            val pointsToAdd = (pointsPerSecond * secondsElapsed).toInt()

            // Award points to all alive hiders who are ONLINE
            val alivehiders = game.players.values
                .filter { it.role == PlayerRole.HIDER && !it.isCaptured }

            plugin.logger.info("[PointAccumulation] Awarding $pointsToAdd points to ${alivehiders.size} alive hiders")

            alivehiders.forEach { playerData ->
                // Only award points if player is online
                val player = Bukkit.getPlayer(playerData.uuid)
                if (player != null && player.isOnline) {
                    addPoints(playerData.uuid, pointsToAdd)
                    // Record points earned as Hider
                    playerData.hiderPoints += pointsToAdd
                    plugin.logger.info("[PointAccumulation] ${player.name}: +$pointsToAdd (total: ${getPoints(playerData.uuid)}, hiderPoints: ${playerData.hiderPoints})")
                }
            }
        }, updateInterval, updateInterval).taskId
    }

    /**
     * Stop point accumulation
     */
    fun stopPointAccumulation() {
        pointAccumulationTaskId?.let { taskId ->
            Bukkit.getScheduler().cancelTask(taskId)
            pointAccumulationTaskId = null
        }
    }

    /**
     * Award taunt bonus points
     */
    fun awardTauntBonus(playerId: UUID, bonusPoints: Int) {
        addPoints(playerId, bonusPoints)
    }

    /**
     * Handle point stealing when a hider is captured
     * @param game The active game
     * @param seekerId Seeker who captured
     * @param hiderId Hider who was captured
     * @param basePoints Base points awarded for capture
     * @param stealPercentage Percentage of points to steal (0.0 to 1.0)
     * @return Points awarded (base + stolen)
     */
    fun handleCapture(game: Game, seekerId: UUID, hiderId: UUID, basePoints: Int, stealPercentage: Double): Int {
        val hiderPoints = getPoints(hiderId)
        val pointsStolen = (hiderPoints * stealPercentage).toInt()
        val totalPoints = basePoints + pointsStolen

        // Transfer stolen points from hider
        addPoints(seekerId, totalPoints)
        setPoints(hiderId, hiderPoints - pointsStolen)

        // Record points earned as Seeker
        game.players[seekerId]?.let { seekerData ->
            seekerData.seekerPoints += totalPoints
            val seekerPlayer = Bukkit.getPlayer(seekerId)
            plugin.logger.info("[PointCapture] ${seekerPlayer?.name ?: seekerId} captured: +$totalPoints (base: $basePoints, stolen: $pointsStolen, seekerPoints: ${seekerData.seekerPoints})")
        }

        return totalPoints
    }

    /**
     * Get ranked players (descending order by points)
     */
    fun getRankedPlayers(): List<Pair<UUID, Int>> {
        return playerPoints.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
    }

    /**
     * Get ranked players filtered by role
     */
    fun getRankedPlayersByRole(game: Game, role: PlayerRole): List<Pair<UUID, Int>> {
        return playerPoints.entries
            .filter { (uuid, _) -> game.players[uuid]?.role == role }
            .sortedByDescending { it.value }
            .map { it.key to it.value }
    }

    /**
     * Get ranked players by points earned in a specific role
     * Players can appear in both Hider and Seeker rankings if they played both roles
     * Shows all players who played the role, even if they earned 0 points
     */
    fun getRankedPlayersByRolePoints(game: Game, role: PlayerRole): List<Pair<UUID, Int>> {
        return game.players.values
            .mapNotNull { playerData ->
                when (role) {
                    PlayerRole.HIDER -> {
                        // Show if they started as Hider OR earned points as Hider
                        if (playerData.originalRole == PlayerRole.HIDER || playerData.hiderPoints > 0) {
                            playerData.uuid to playerData.hiderPoints
                        } else null
                    }
                    PlayerRole.SEEKER -> {
                        // Show if they started as Seeker OR earned points as Seeker
                        if (playerData.originalRole == PlayerRole.SEEKER || playerData.seekerPoints > 0) {
                            playerData.uuid to playerData.seekerPoints
                        } else null
                    }
                    else -> null
                }
            }
            .sortedByDescending { it.second }
    }
}
