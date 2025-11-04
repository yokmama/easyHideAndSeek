package com.hideandseek.shop

/**
 * Scope of purchase restrictions
 *
 * Determines whether limits apply per individual player, per team, or globally
 */
enum class RestrictionType {
    /**
     * Restriction applies per individual player
     * Example: Each player can buy max 2 Speed Boosts
     */
    PER_PLAYER,

    /**
     * Restriction applies per team (all Hiders or all Seekers)
     * Example: Seeker team can buy max 3 Tracker Compasses total
     */
    PER_TEAM,

    /**
     * Restriction applies globally across all players in the game
     * Example: Only 5 total Second Chance items available per game
     */
    GLOBAL;

    /**
     * Get human-readable description
     */
    fun getDisplayName(): String {
        return when (this) {
            PER_PLAYER -> "Per Player"
            PER_TEAM -> "Per Team"
            GLOBAL -> "Global"
        }
    }
}
