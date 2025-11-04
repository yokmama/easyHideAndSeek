package com.hideandseek.shop

import net.milkbowl.vault.economy.Economy
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Validates shop purchases before processing
 * Checks all requirements: role, economy, limits, cooldown, inventory
 */
object PurchaseValidator {
    /**
     * Validate a purchase attempt
     *
     * @param player The player attempting purchase
     * @param item The shop item being purchased
     * @param playerRole Player's current role ("SEEKER" or "HIDER")
     * @param gameId Current game session ID
     * @param economy Vault economy provider (null if economy disabled)
     * @param purchaseStorage Purchase history storage
     * @throws InsufficientFundsException if player can't afford item
     * @throws PurchaseLimitException if purchase limit reached
     * @throws CooldownException if item on cooldown
     * @throws InventoryFullException if inventory full
     * @throws RoleRestrictionException if role doesn't match
     * @throws UsageRestrictionException if usage restriction violated
     */
    fun validatePurchase(
        player: Player,
        item: ShopItem,
        playerRole: String,
        gameId: String,
        economy: Economy?,
        purchaseStorage: PurchaseStorage
    ) {
        // 1. Check role access
        if (item.roleFilter != null && !item.roleFilter.equals(playerRole, ignoreCase = true)) {
            throw RoleRestrictionException(
                itemId = item.id,
                requiredRole = item.roleFilter,
                actualRole = playerRole
            )
        }

        // 2. Check usage restrictions
        validateUsageRestriction(player, item, gameId)

        // 3. Check economy balance
        if (economy != null && item.price > 0) {
            val balance = economy.getBalance(player)
            if (balance < item.price) {
                throw InsufficientFundsException(
                    required = item.price,
                    actual = balance
                )
            }
        }

        // 4. Check purchase limit
        if (item.maxPurchases != null) {
            val purchaseCount = purchaseStorage.countPurchases(player.uniqueId, item.id, gameId)
            if (purchaseCount >= item.maxPurchases) {
                throw PurchaseLimitException(
                    itemId = item.id,
                    limit = item.maxPurchases,
                    current = purchaseCount
                )
            }
        }

        // 5. Check cooldown
        if (item.cooldown != null) {
            val latestPurchase = purchaseStorage.getLatestPurchase(player.uniqueId, item.id)
            if (latestPurchase != null && !latestPurchase.isCooldownExpired(item.cooldown)) {
                val remaining = item.cooldown - latestPurchase.getElapsedSeconds()
                throw CooldownException(
                    itemId = item.id,
                    remainingSeconds = remaining
                )
            }
        }

        // 6. Check inventory space (for effect items that give physical items)
        // Note: Effect items are consumed immediately, so they don't need inventory space
        // This check is primarily for future item types
        if (!item.isEffectItem() && player.inventory.firstEmpty() == -1) {
            throw InventoryFullException(itemId = item.id)
        }
    }

    /**
     * Validate usage restrictions based on game state
     */
    private fun validateUsageRestriction(
        player: Player,
        item: ShopItem,
        gameId: String
    ) {
        when (item.usageRestriction) {
            UsageRestriction.ALWAYS -> {
                // No restrictions
            }

            UsageRestriction.SEEK_PHASE_ONLY -> {
                // TODO: Check game phase when GameManager integration is added
                // For now, allow if in a game
                // Future: if (gameManager.getCurrentPhase(gameId) != GamePhase.SEEK) throw exception
            }

            UsageRestriction.NOT_WHILE_CAPTURED -> {
                // TODO: Check player capture status when integration is added
                // Future: if (gameManager.isPlayerCaptured(player.uniqueId)) throw exception
            }

            UsageRestriction.ONCE_PER_GAME -> {
                // This is handled by purchase limit check (maxPurchases = 1)
                // No additional validation needed here
            }
        }
    }

    /**
     * Quick check if purchase is likely to succeed
     * Returns false with no exception if validation would fail
     * Useful for UI display (graying out unavailable items)
     */
    fun canPurchase(
        player: Player,
        item: ShopItem,
        playerRole: String,
        gameId: String,
        economy: Economy?,
        purchaseStorage: PurchaseStorage
    ): Boolean {
        return try {
            validatePurchase(player, item, playerRole, gameId, economy, purchaseStorage)
            true
        } catch (e: Exception) {
            false
        }
    }
}
