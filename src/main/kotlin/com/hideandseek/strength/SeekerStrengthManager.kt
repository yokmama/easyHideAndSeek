package com.hideandseek.strength

import org.bukkit.plugin.Plugin
import java.util.UUID

/**
 * Manages the strength points of seekers (demons) during a game.
 *
 * Strength points are accumulated when a seeker captures hiders.
 * These points are used to determine the outcome when seekers fight each other:
 * - If a weaker seeker (lower strength) attacks a stronger seeker (higher strength),
 *   the attacker is reverted to a hider.
 * - If a stronger seeker (higher or equal strength) attacks a weaker seeker,
 *   the weaker seeker is captured normally.
 *
 * All strength data is stored in memory (HashMap) and is cleared when the game ends.
 *
 * @property plugin The plugin instance for logging
 */
class SeekerStrengthManager(private val plugin: Plugin) {

    /**
     * In-memory storage of strength data for each player.
     * Key: Player UUID
     * Value: SeekerStrengthData
     */
    private val strengthData = mutableMapOf<UUID, SeekerStrengthData>()

    /**
     * Gets the strength points of a player.
     *
     * @param playerId The UUID of the player
     * @return The strength points, or 0 if the player has no strength data
     */
    fun getStrength(playerId: UUID): Int {
        return strengthData[playerId]?.strengthPoints ?: 0
    }

    /**
     * Adds strength points to a player (called when they capture a hider).
     *
     * @param playerId The UUID of the player
     * @param points The number of points to add (typically from config: economy.rewards.per-capture)
     */
    fun addStrength(playerId: UUID, points: Int) {
        val data = strengthData.getOrPut(playerId) {
            SeekerStrengthData(playerId)
        }
        data.addCapture(points)

        plugin.logger.info("[SeekerStrength] ${playerId} captured hider: +$points strength (total: ${data.strengthPoints}, captures: ${data.captureCount})")
    }

    /**
     * Resets the strength points of a player to 0 (called when they revert to hider).
     *
     * @param playerId The UUID of the player
     */
    fun resetStrength(playerId: UUID) {
        strengthData[playerId]?.reset()
        plugin.logger.info("[SeekerStrength] ${playerId} strength reset to 0")
    }

    /**
     * Compares the strength of two players.
     *
     * @param attackerId The UUID of the attacker
     * @param victimId The UUID of the victim
     * @return Negative if attacker is weaker, 0 if equal, positive if attacker is stronger
     */
    fun compareStrength(attackerId: UUID, victimId: UUID): Int {
        val attackerStrength = getStrength(attackerId)
        val victimStrength = getStrength(victimId)

        plugin.logger.info("[SeekerStrength] Comparing strength: attacker=$attackerStrength vs victim=$victimStrength")

        return attackerStrength - victimStrength
    }

    /**
     * Clears all strength data (called when the game ends).
     */
    fun clearAll() {
        val count = strengthData.size
        strengthData.clear()
        plugin.logger.info("[SeekerStrength] Cleared all strength data ($count entries)")
    }

    /**
     * Gets the capture count of a player (for debugging/statistics).
     *
     * @param playerId The UUID of the player
     * @return The capture count, or 0 if the player has no strength data
     */
    fun getCaptureCount(playerId: UUID): Int {
        return strengthData[playerId]?.captureCount ?: 0
    }
}
