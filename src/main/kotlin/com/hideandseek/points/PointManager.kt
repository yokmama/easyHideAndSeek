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
            game.players.values
                .filter { it.role == PlayerRole.HIDER && !it.isCaptured }
                .forEach { playerData ->
                    // Only award points if player is online
                    val player = Bukkit.getPlayer(playerData.uuid)
                    if (player != null && player.isOnline) {
                        addPoints(playerData.uuid, pointsToAdd)
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
     * @param seekerId Seeker who captured
     * @param hiderId Hider who was captured
     * @param stealPercentage Percentage of points to steal (0.0 to 1.0)
     * @return Points stolen
     */
    fun handleCapture(seekerId: UUID, hiderId: UUID, stealPercentage: Double): Int {
        val hiderPoints = getPoints(hiderId)
        val pointsStolen = (hiderPoints * stealPercentage).toInt()

        // Transfer points
        addPoints(seekerId, pointsStolen)
        setPoints(hiderId, hiderPoints - pointsStolen)

        return pointsStolen
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
}
