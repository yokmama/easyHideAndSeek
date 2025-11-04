package com.hideandseek.shop

import java.time.Instant
import java.util.UUID

/**
 * Record of a shop item purchase
 *
 * @property playerId UUID of the player who made the purchase
 * @property itemId Unique identifier for the purchased item (e.g., "vision-enhancer")
 * @property purchaseTime When the purchase was made
 * @property gameId ID of the game session (for per-game limits)
 * @property price Amount paid in currency
 * @property used Whether the item has been used/consumed
 * @property gamePhase T052: Game phase when purchased (PREPARATION or SEEK)
 * @property playerBalance T053: Player's balance after purchase
 */
data class ItemPurchaseRecord(
    val playerId: UUID,
    val itemId: String,
    val purchaseTime: Instant,
    val gameId: String,
    val price: Int,
    val used: Boolean = false,
    val gamePhase: String? = null,
    val playerBalance: Double? = null
) {
    /**
     * Get elapsed time since purchase in seconds
     * @return Seconds since purchase
     */
    fun getElapsedSeconds(): Long {
        return java.time.Duration.between(purchaseTime, Instant.now()).seconds
    }

    /**
     * Check if cooldown has expired
     * @param cooldownSeconds Cooldown duration in seconds
     * @return true if enough time has passed
     */
    fun isCooldownExpired(cooldownSeconds: Int): Boolean {
        return getElapsedSeconds() >= cooldownSeconds
    }

    /**
     * Create a copy marked as used
     * @return New record with used=true
     */
    fun markUsed(): ItemPurchaseRecord {
        return copy(used = true)
    }

    /**
     * Check if this purchase is for the current game
     * @param currentGameId The current game session ID
     * @return true if purchase was made in this game
     */
    fun isCurrentGame(currentGameId: String): Boolean {
        return gameId == currentGameId
    }
}
