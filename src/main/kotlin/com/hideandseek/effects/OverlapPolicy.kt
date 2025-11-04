package com.hideandseek.effects

/**
 * Policies for handling effect overlap when applying a new effect of the same type
 */
enum class OverlapPolicy {
    /**
     * Replace existing effect with new one
     * - Cancel existing effect's scheduler task
     * - Start new effect with full duration
     * - Default policy for all seeker shop effects
     */
    REPLACE,

    /**
     * Extend existing effect's duration
     * - Keep existing effect
     * - Add new duration to remaining time
     * - Not used in current implementation (reserved for future features)
     */
    EXTEND,

    /**
     * Reject new effect if one already exists
     * - Keep existing effect unchanged
     * - Fail purchase/application
     * - Not used in current implementation (reserved for future features)
     */
    REJECT,

    /**
     * Stack effects (allow multiple instances)
     * - Both effects active simultaneously
     * - Intensities may combine
     * - Not used in current implementation (reserved for future features)
     */
    STACK
}
