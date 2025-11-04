package com.hideandseek.shop

/**
 * Defines when purchase limits or cooldowns should reset
 *
 * Used to create dynamic restrictions that reset under specific conditions
 */
enum class ResetTrigger {
    /**
     * Limit/cooldown never resets during the game
     * Most restrictive option - lasts entire game session
     */
    NEVER,

    /**
     * Cooldown starts after item is activated/used
     * Example: Speed Boost cooldown starts when player uses it, not when purchased
     */
    ON_USE,

    /**
     * Limit/cooldown resets when player dies or is captured
     * Allows players to get new purchases after respawn
     */
    ON_DEATH,

    /**
     * Limit/cooldown resets when game phase changes
     * Example: Resets when PREPARATION phase transitions to SEEK phase
     */
    ON_PHASE_CHANGE,

    /**
     * Cooldown starts immediately upon purchase
     * Example: Can't buy another item of same type for X seconds
     */
    ON_PURCHASE;

    /**
     * Get human-readable description
     */
    fun getDisplayName(): String {
        return when (this) {
            NEVER -> "Never resets"
            ON_USE -> "Resets on use"
            ON_DEATH -> "Resets on death"
            ON_PHASE_CHANGE -> "Resets on phase change"
            ON_PURCHASE -> "Resets on purchase"
        }
    }

    /**
     * Get short description for tooltips
     */
    fun getShortDescription(): String {
        return when (this) {
            NEVER -> "Permanent"
            ON_USE -> "After use"
            ON_DEATH -> "After death"
            ON_PHASE_CHANGE -> "Next phase"
            ON_PURCHASE -> "After buy"
        }
    }
}
