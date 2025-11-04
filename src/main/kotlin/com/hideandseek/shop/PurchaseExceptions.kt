package com.hideandseek.shop

/**
 * Custom exceptions for shop purchase validation failures
 * Each exception represents a specific reason why a purchase cannot proceed
 */

/**
 * Thrown when player doesn't have enough currency to purchase an item
 * @property required Amount required for purchase
 * @property actual Player's current balance
 */
class InsufficientFundsException(
    val required: Int,
    val actual: Double
) : Exception("Insufficient funds: required $required, have $actual")

/**
 * Thrown when player has reached the purchase limit for an item
 * @property itemId The item identifier
 * @property limit Maximum purchases allowed
 * @property current Current purchase count
 */
class PurchaseLimitException(
    val itemId: String,
    val limit: Int,
    val current: Int
) : Exception("Purchase limit reached for '$itemId': $current/$limit")

/**
 * Thrown when player tries to purchase an item that's still on cooldown
 * @property itemId The item identifier
 * @property remainingSeconds Seconds remaining on cooldown
 */
class CooldownException(
    val itemId: String,
    val remainingSeconds: Long
) : Exception("Item '$itemId' is on cooldown: $remainingSeconds seconds remaining")

/**
 * Thrown when player's inventory is full and cannot receive the item
 * @property itemId The item identifier
 */
class InventoryFullException(
    val itemId: String
) : Exception("Inventory is full, cannot receive '$itemId'")

/**
 * Thrown when player's role doesn't match the item's role filter
 * @property itemId The item identifier
 * @property requiredRole Required role ("SEEKER" or "HIDER")
 * @property actualRole Player's actual role
 */
class RoleRestrictionException(
    val itemId: String,
    val requiredRole: String,
    val actualRole: String
) : Exception("Item '$itemId' requires role '$requiredRole', but player is '$actualRole'")

/**
 * Thrown when item usage restriction prevents purchase
 * @property itemId The item identifier
 * @property restriction The usage restriction
 * @property reason Human-readable explanation
 */
class UsageRestrictionException(
    val itemId: String,
    val restriction: UsageRestriction,
    val reason: String
) : Exception("Cannot use '$itemId': $reason")
