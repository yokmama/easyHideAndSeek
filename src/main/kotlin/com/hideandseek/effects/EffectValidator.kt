package com.hideandseek.effects

import org.bukkit.entity.Player

/**
 * Validation utilities for effect parameters
 * Ensures effects are applied with valid values and to valid players
 */
object EffectValidator {
    // Duration constraints (seconds)
    private const val MIN_DURATION = 5
    private const val MAX_DURATION = 120

    // Intensity ranges by effect type
    private val INTENSITY_RANGES = mapOf(
        EffectType.VISION to (0.5 to 2.0),     // 0.5x to 2x (currently only 1.0 used)
        EffectType.GLOW to (0.5 to 2.0),       // 0.5x to 2x particle density
        EffectType.SPEED to (0.5 to 2.0),      // 0.5x to 2x speed (Speed I-III)
        EffectType.REACH to (1.0 to 2.0),      // 1.0x to 2.0x reach multiplier

        // New effect types (T012)
        EffectType.SHADOW_SPRINT to (0.5 to 2.0),    // 0.5x to 2x speed for hiders
        EffectType.DECOY_BLOCK to (1.0 to 1.0),      // N/A - fixed at 1.0
        EffectType.SECOND_CHANCE to (1.0 to 1.0),    // N/A - fixed at 1.0
        EffectType.TRACKER_COMPASS to (1.0 to 1.0),  // N/A - fixed at 1.0
        EffectType.AREA_SCAN to (5.0 to 30.0),       // 5 to 30 block radius
        EffectType.EAGLE_EYE to (5.0 to 30.0),       // 5 to 30 block detection radius
        EffectType.TRACKER_INSIGHT to (1.0 to 1.0),  // N/A - fixed at 1.0
        EffectType.CAPTURE_NET to (1.5 to 3.0)       // 1.5x to 3x reward multiplier
    )

    /**
     * Validate effect duration
     *
     * @param durationSeconds Duration to validate
     * @return true if valid (5-120 seconds)
     * @throws IllegalArgumentException if out of range
     */
    fun validateDuration(durationSeconds: Int): Boolean {
        require(durationSeconds in MIN_DURATION..MAX_DURATION) {
            "Effect duration must be between $MIN_DURATION and $MAX_DURATION seconds, got: $durationSeconds"
        }
        return true
    }

    /**
     * Validate effect intensity for a specific effect type
     *
     * @param effectType The effect type
     * @param intensity Intensity value to validate
     * @return true if valid for the effect type
     * @throws IllegalArgumentException if out of range
     */
    fun validateIntensity(effectType: EffectType, intensity: Double): Boolean {
        val range = INTENSITY_RANGES[effectType]
            ?: throw IllegalStateException("No intensity range defined for effect type: $effectType")

        require(intensity >= range.first && intensity <= range.second) {
            "Intensity for $effectType must be between ${range.first} and ${range.second}, got: $intensity"
        }
        return true
    }

    /**
     * Validate that a player can receive effects
     * Checks role, game phase, and player state
     *
     * @param player The player to validate
     * @return true if player can receive effects
     * @throws IllegalStateException if player is in invalid state
     */
    fun validatePlayer(player: Player): Boolean {
        require(player.isOnline) {
            "Cannot apply effect to offline player: ${player.name}"
        }

        // Note: Role and phase validation will be implemented when
        // game manager integration is added in later phases
        // For now, we only validate online state

        return true
    }

    /**
     * Validate all parameters for effect application
     *
     * @param player The player
     * @param effectType Effect type
     * @param durationSeconds Duration in seconds
     * @param intensity Intensity multiplier
     * @return true if all validations pass
     * @throws IllegalArgumentException if any validation fails
     */
    fun validateEffectApplication(
        player: Player,
        effectType: EffectType,
        durationSeconds: Int,
        intensity: Double
    ): Boolean {
        validatePlayer(player)
        validateDuration(durationSeconds)
        validateIntensity(effectType, intensity)
        return true
    }

    /**
     * Get valid duration range
     * @return Pair of (min, max) seconds
     */
    fun getDurationRange(): Pair<Int, Int> = MIN_DURATION to MAX_DURATION

    /**
     * Get valid intensity range for an effect type
     * @param effectType The effect type
     * @return Pair of (min, max) intensity
     */
    fun getIntensityRange(effectType: EffectType): Pair<Double, Double> {
        return INTENSITY_RANGES[effectType]
            ?: throw IllegalStateException("No intensity range defined for effect type: $effectType")
    }
}
