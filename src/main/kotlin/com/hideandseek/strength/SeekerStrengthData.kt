package com.hideandseek.strength

import java.util.UUID

/**
 * Represents the strength data of a seeker (demon).
 *
 * The strength of a seeker is determined by the number of hiders they have captured.
 * Each capture increases the strength points by a configured amount (default: 200 points).
 *
 * @property playerId The unique identifier of the player
 * @property strengthPoints The accumulated strength points (increases with each capture)
 * @property captureCount The number of hiders captured (for debugging/statistics)
 */
data class SeekerStrengthData(
    val playerId: UUID,
    var strengthPoints: Int = 0,
    var captureCount: Int = 0
) {
    /**
     * Records a capture and increases strength points.
     *
     * @param pointsPerCapture The number of points to add for this capture
     */
    fun addCapture(pointsPerCapture: Int) {
        strengthPoints += pointsPerCapture
        captureCount++
    }

    /**
     * Resets strength data to initial values (0 points, 0 captures).
     *
     * This is called when a seeker is reverted to a hider.
     */
    fun reset() {
        strengthPoints = 0
        captureCount = 0
    }
}
