package com.hideandseek.shop

/**
 * Usage restrictions for shop items
 * Determines when an item can be purchased/used
 */
enum class UsageRestriction {
    /**
     * Can be used anytime during the game
     */
    ALWAYS,

    /**
     * Can only be used during the seek phase (not during preparation)
     */
    SEEK_PHASE_ONLY,

    /**
     * Cannot be used while player is captured
     */
    NOT_WHILE_CAPTURED,

    /**
     * Can only be purchased/used once per game
     */
    ONCE_PER_GAME
}
