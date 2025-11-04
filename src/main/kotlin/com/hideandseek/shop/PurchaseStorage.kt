package com.hideandseek.shop

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe storage for shop purchase records
 * Tracks purchase history for enforcing limits and cooldowns
 */
class PurchaseStorage {
    /**
     * Primary storage: PlayerId -> List of purchase records
     * List maintains chronological order
     */
    private val purchases = ConcurrentHashMap<UUID, MutableList<ItemPurchaseRecord>>()

    /**
     * Record a new purchase
     * @param record The purchase record to store
     */
    fun recordPurchase(record: ItemPurchaseRecord) {
        purchases.computeIfAbsent(record.playerId) { mutableListOf() }
            .add(record)
    }

    /**
     * Get all purchases for a player
     * @param playerId Player UUID
     * @return List of purchase records (chronologically ordered)
     */
    fun getPurchases(playerId: UUID): List<ItemPurchaseRecord> {
        return purchases[playerId]?.toList() ?: emptyList()
    }

    /**
     * Get purchases for a specific item
     * @param playerId Player UUID
     * @param itemId Item identifier
     * @return List of purchase records for that item
     */
    fun getPurchasesForItem(playerId: UUID, itemId: String): List<ItemPurchaseRecord> {
        return purchases[playerId]?.filter { it.itemId == itemId } ?: emptyList()
    }

    /**
     * Get the most recent purchase for an item
     * @param playerId Player UUID
     * @param itemId Item identifier
     * @return Latest purchase record, or null if none
     */
    fun getLatestPurchase(playerId: UUID, itemId: String): ItemPurchaseRecord? {
        return getPurchasesForItem(playerId, itemId).maxByOrNull { it.purchaseTime }
    }

    /**
     * Count purchases for a specific item in the current game
     * @param playerId Player UUID
     * @param itemId Item identifier
     * @param gameId Current game session ID
     * @return Number of purchases in this game
     */
    fun countPurchases(playerId: UUID, itemId: String, gameId: String): Int {
        return purchases[playerId]
            ?.count { it.itemId == itemId && it.gameId == gameId }
            ?: 0
    }

    /**
     * Clear all purchase records for a player
     * Used when player leaves or game resets
     * @param playerId Player UUID
     */
    fun clearPlayer(playerId: UUID) {
        purchases.remove(playerId)
    }

    /**
     * Clear all purchase records
     * Used during game reset or plugin reload
     */
    fun clearAll() {
        purchases.clear()
    }

    /**
     * Check if a player can purchase an item based on limits
     * @param playerId Player UUID
     * @param itemId Item identifier
     * @param gameId Current game session ID
     * @param purchaseLimit Max purchases per game (null = unlimited)
     * @return true if purchase is allowed
     */
    fun canPurchase(
        playerId: UUID,
        itemId: String,
        gameId: String,
        purchaseLimit: Int?
    ): Boolean {
        if (purchaseLimit == null) return true

        val count = countPurchases(playerId, itemId, gameId)
        return count < purchaseLimit
    }

    /**
     * Mark a purchase record as used
     * @param playerId Player UUID
     * @param itemId Item identifier
     * @param gameId Game session ID
     * @return true if record was found and updated
     */
    fun markAsUsed(playerId: UUID, itemId: String, gameId: String): Boolean {
        val playerPurchases = purchases[playerId] ?: return false

        // Find the most recent unused purchase for this item in this game
        val index = playerPurchases.indexOfLast {
            it.itemId == itemId && it.gameId == gameId && !it.used
        }

        if (index == -1) return false

        playerPurchases[index] = playerPurchases[index].markUsed()
        return true
    }

    /**
     * Get storage statistics for debugging
     * @return Map of metric name to value
     */
    fun getStats(): Map<String, Any> {
        val totalPurchases = purchases.values.sumOf { it.size }
        val usedPurchases = purchases.values.sumOf { list ->
            list.count { it.used }
        }

        return mapOf(
            "total_players" to purchases.size,
            "total_purchases" to totalPurchases,
            "used_purchases" to usedPurchases,
            "unused_purchases" to (totalPurchases - usedPurchases)
        )
    }
}
