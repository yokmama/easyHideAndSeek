# API Contract: Shop System Extension

**Feature**: Seeker Shop System
**Component**: Shop System Role-Based Filtering & Purchase Flow
**Created**: 2025-11-03
**Status**: Design Phase

## Overview

This document specifies the extensions to the existing shop system to support role-based item filtering (SEEKER vs HIDER), enhanced purchase flow with cooldowns and limits, and integration with the new seeker effect items. The design maintains backward compatibility with the existing hider shop while adding new capabilities.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Role-Based Filtering API](#2-role-based-filtering-api)
3. [Purchase Flow API](#3-purchase-flow-api)
4. [Integration Points](#4-integration-points)
5. [Migration Strategy](#5-migration-strategy)
6. [Usage Examples](#6-usage-examples)
7. [Testing Requirements](#7-testing-requirements)

---

## 1. Architecture Overview

### System Components

```
┌────────────────────────────────────────────────────────────┐
│                       ShopManager                          │
│  - Manages shop categories and items                       │
│  - Provides role-based filtering                           │
│  - Handles purchase transactions                           │
└────────────┬───────────────────────────────────────────────┘
             │
             ├── ShopCategory (extended with roleFilter)
             │   ├── Hider Items (disguise blocks)
             │   └── Seeker Items (effect items) [NEW]
             │
             ├── ShopItem (extended with effect metadata)
             │   ├── effectType: EffectType?
             │   ├── effectDuration: Int?
             │   ├── effectIntensity: Double?
             │   ├── roleFilter: PlayerRole?
             │   ├── cooldown: Int
             │   └── maxPurchases: Int
             │
             └── PurchaseManager [NEW]
                 ├── Tracks purchase history
                 ├── Enforces cooldowns
                 └── Enforces purchase limits
```

### Design Principles

1. **Backward Compatibility**: Existing hider shop continues to work without changes
2. **Role Separation**: Seekers and hiders see different items
3. **Configuration-Driven**: All items defined in YAML, no code changes for new items
4. **Performance**: O(1) role filtering, minimal overhead
5. **Extensibility**: Easy to add new roles or item types

---

## 2. Role-Based Filtering API

### ShopCategory Extension

**Current Implementation**:
```kotlin
data class ShopCategory(
    val id: String,
    val displayName: String,
    val icon: Material,
    val slot: Int,
    val items: List<ShopItem>,
    val description: List<String> = emptyList()
)
```

**Extended Implementation**:
```kotlin
package com.hideandseek.shop

import com.hideandseek.game.PlayerRole
import org.bukkit.Material

/**
 * Extended shop category with role-based filtering.
 *
 * New fields:
 * - roleFilter: Restricts visibility to specific role (null = all roles)
 */
data class ShopCategory(
    val id: String,
    val displayName: String,
    val icon: Material,
    val slot: Int,
    val items: List<ShopItem>,
    val description: List<String> = emptyList(),

    // NEW: Role filtering
    val roleFilter: PlayerRole? = null
) {
    /**
     * Check if this category should be visible to a player with given role.
     *
     * @param role The player's current role
     * @return true if category is visible, false otherwise
     */
    fun isVisibleTo(role: PlayerRole): Boolean {
        return roleFilter == null || roleFilter == role
    }

    /**
     * Get items from this category that are usable by a specific role.
     *
     * Applies both category-level and item-level filtering.
     *
     * @param role The player's current role
     * @return List of usable items (may be empty)
     */
    fun getItemsForRole(role: PlayerRole): List<ShopItem> {
        if (!isVisibleTo(role)) {
            return emptyList()
        }
        return items.filter { it.isUsableBy(role) }
    }

    /**
     * Check if category has any items for a given role.
     *
     * @param role The player's current role
     * @return true if category has at least one usable item
     */
    fun hasItemsForRole(role: PlayerRole): Boolean {
        return getItemsForRole(role).isNotEmpty()
    }
}
```

### ShopItem Extension

**Extended Implementation**:
```kotlin
package com.hideandseek.shop

import com.hideandseek.effects.EffectType
import com.hideandseek.game.PlayerRole
import org.bukkit.Material

/**
 * Extended shop item with effect metadata and role filtering.
 *
 * New fields:
 * - effectType: Type of effect this item provides (null for non-effect items)
 * - effectDuration: Effect duration in seconds
 * - effectIntensity: Effect strength multiplier
 * - roleFilter: Which role can use this item
 * - cooldown: Seconds before repurchase allowed
 * - maxPurchases: Purchase limit per game (-1 = unlimited)
 * - usageRestriction: When item can be used
 */
data class ShopItem(
    // Core fields (existing)
    val id: String,
    val material: Material,
    val displayName: String,
    val lore: List<String>,
    val slot: Int,
    val price: Int,
    val action: ShopAction,

    // Effect fields (NEW)
    val effectType: EffectType? = null,
    val effectDuration: Int? = null,
    val effectIntensity: Double? = null,

    // Access control (NEW)
    val roleFilter: PlayerRole? = null,
    val cooldown: Int = 0,
    val maxPurchases: Int = -1,
    val usageRestriction: UsageRestriction = UsageRestriction.ALWAYS
) {
    /**
     * Check if this item can be used by a player with given role.
     *
     * @param role The player's role
     * @return true if usable, false otherwise
     */
    fun isUsableBy(role: PlayerRole): Boolean {
        return roleFilter == null || roleFilter == role
    }

    /**
     * Check if this is an effect item (vs disguise or other action).
     *
     * @return true if item provides an effect
     */
    fun isEffectItem(): Boolean {
        return effectType != null
    }

    /**
     * Validate item configuration.
     *
     * Checks that all required fields for effect items are present
     * and within valid ranges.
     *
     * @return Result.success if valid, Result.failure with error otherwise
     */
    fun validate(): Result<Unit> {
        // Price validation
        if (price < 0) {
            return Result.failure(
                IllegalArgumentException("Price cannot be negative: $price")
            )
        }

        // Effect item validation
        if (effectType != null) {
            // Duration required
            val duration = effectDuration ?: return Result.failure(
                IllegalArgumentException("Effect items must specify duration: $id")
            )

            // Duration range
            if (duration !in 5..120) {
                return Result.failure(
                    IllegalArgumentException(
                        "Effect duration must be 5-120 seconds: $duration (item: $id)"
                    )
                )
            }

            // Intensity validation
            val intensity = effectIntensity ?: 1.0
            if (intensity !in 0.1..3.0) {
                return Result.failure(
                    IllegalArgumentException(
                        "Effect intensity must be 0.1-3.0: $intensity (item: $id)"
                    )
                )
            }
        }

        // Cooldown validation
        if (cooldown < 0) {
            return Result.failure(
                IllegalArgumentException("Cooldown cannot be negative: $cooldown")
            )
        }

        return Result.success(Unit)
    }

    /**
     * Get display lore with dynamic values replaced.
     *
     * Replaces placeholders like {price}, {duration}, etc.
     *
     * @return Formatted lore lines
     */
    fun getFormattedLore(): List<String> {
        return lore.map { line ->
            line.replace("{price}", price.toString())
                .replace("{duration}", (effectDuration ?: 0).toString())
                .replace("{cooldown}", cooldown.toString())
                .replace("{max}", if (maxPurchases > 0) maxPurchases.toString() else "∞")
        }
    }
}

/**
 * Restrictions on when an item can be used.
 */
enum class UsageRestriction {
    /** Can use anytime during game */
    ALWAYS,

    /** Only during active seek phase */
    SEEK_PHASE_ONLY,

    /** Cannot use if captured */
    NOT_WHILE_CAPTURED,

    /** Single use per game session */
    ONCE_PER_GAME
}
```

### ShopManager Extensions

**New Methods**:
```kotlin
package com.hideandseek.shop

import com.hideandseek.game.PlayerRole
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Extended ShopManager with role-based filtering and purchase management.
 */
interface ShopManager {

    // === Existing Methods (unchanged) ===

    /**
     * Get all shop categories.
     */
    fun getCategories(): List<ShopCategory>

    /**
     * Get a specific category by ID.
     */
    fun getCategory(id: String): ShopCategory?

    /**
     * Get a specific item by ID.
     */
    fun getItem(itemId: String): ShopItem?

    /**
     * Open shop GUI for a player.
     */
    fun openShop(player: Player)

    // === NEW: Role-Based Methods ===

    /**
     * Get all categories visible to a specific role.
     *
     * Filters categories based on roleFilter field.
     *
     * @param role The player's role
     * @return List of visible categories (may be empty)
     */
    fun getCategoriesForRole(role: PlayerRole): List<ShopCategory>

    /**
     * Get all items usable by a specific role.
     *
     * Aggregates items from all visible categories.
     *
     * @param role The player's role
     * @return List of usable items
     */
    fun getItemsForRole(role: PlayerRole): List<ShopItem>

    /**
     * Get items from a specific category for a role.
     *
     * @param categoryId Category ID
     * @param role Player's role
     * @return List of usable items from that category
     */
    fun getCategoryItemsForRole(categoryId: String, role: PlayerRole): List<ShopItem>

    /**
     * Open shop GUI with role-based filtering.
     *
     * Shows only categories and items relevant to player's role.
     *
     * @param player The player
     * @param role The player's current role
     */
    fun openShopForRole(player: Player, role: PlayerRole)

    // === NEW: Purchase Management ===

    /**
     * Attempt to purchase an item.
     *
     * Handles full purchase flow:
     * 1. Validation (role, phase, funds, cooldown, limits)
     * 2. Economy transaction
     * 3. Item delivery
     * 4. Purchase recording
     *
     * @param player The purchasing player
     * @param itemId Item to purchase
     * @param gameId Current game session ID
     * @return PurchaseResult with success/failure and details
     */
    fun purchaseItem(
        player: Player,
        itemId: String,
        gameId: UUID
    ): PurchaseResult

    /**
     * Check if a player can purchase an item.
     *
     * Non-destructive validation check.
     *
     * @param player The player
     * @param itemId Item to check
     * @param gameId Current game session ID
     * @return PurchaseValidation with can_purchase flag and reasons
     */
    fun canPurchase(
        player: Player,
        itemId: String,
        gameId: UUID
    ): PurchaseValidation

    /**
     * Get purchase history for a player in current game.
     *
     * @param playerId Player UUID
     * @param gameId Game session ID
     * @return List of purchase records
     */
    fun getPurchaseHistory(playerId: UUID, gameId: UUID): List<ItemPurchaseRecord>

    /**
     * Clear purchase history for a game (on game end).
     *
     * @param gameId Game session ID
     */
    fun clearPurchaseHistory(gameId: UUID)
}
```

---

## 3. Purchase Flow API

### Purchase Result Types

```kotlin
package com.hideandseek.shop

import java.time.Instant
import java.util.UUID

/**
 * Result of a purchase attempt.
 */
sealed class PurchaseResult {

    /**
     * Purchase succeeded.
     *
     * @property record The purchase record
     * @property item The purchased item (for immediate use)
     */
    data class Success(
        val record: ItemPurchaseRecord,
        val item: ShopItem
    ) : PurchaseResult()

    /**
     * Purchase failed.
     *
     * @property reason Why the purchase failed
     * @property details Additional context (e.g., remaining cooldown)
     */
    data class Failure(
        val reason: FailureReason,
        val details: Map<String, Any> = emptyMap()
    ) : PurchaseResult()

    /**
     * Reasons for purchase failure.
     */
    enum class FailureReason {
        ITEM_NOT_FOUND,
        PLAYER_NOT_IN_GAME,
        WRONG_ROLE,
        WRONG_PHASE,
        INSUFFICIENT_FUNDS,
        INVENTORY_FULL,
        COOLDOWN_ACTIVE,
        PURCHASE_LIMIT_REACHED,
        ECONOMY_ERROR
    }
}

/**
 * Validation result for purchase eligibility.
 */
data class PurchaseValidation(
    val canPurchase: Boolean,
    val reasons: List<ValidationIssue> = emptyList()
) {
    data class ValidationIssue(
        val type: IssueType,
        val message: String,
        val details: Map<String, Any> = emptyMap()
    )

    enum class IssueType {
        ERROR,      // Blocks purchase
        WARNING     // Allowed but suboptimal
    }
}

/**
 * Record of an item purchase.
 */
data class ItemPurchaseRecord(
    val playerId: UUID,
    val itemId: String,
    val purchaseTime: Instant,
    val gameId: UUID,
    val price: Int,
    var used: Boolean = false
) {
    /**
     * Get time elapsed since purchase in seconds.
     */
    fun getElapsedSeconds(): Long {
        return Instant.now().epochSecond - purchaseTime.epochSecond
    }

    /**
     * Check if cooldown period has passed.
     *
     * @param cooldownSeconds Cooldown duration
     * @return true if cooldown expired
     */
    fun isCooldownExpired(cooldownSeconds: Int): Boolean {
        return getElapsedSeconds() >= cooldownSeconds
    }

    /**
     * Mark this item as used.
     */
    fun markUsed() {
        used = true
    }
}
```

### Purchase Flow Implementation

```kotlin
package com.hideandseek.shop

import com.hideandseek.economy.EconomyManager
import com.hideandseek.game.GameManager
import com.hideandseek.game.GamePhase
import com.hideandseek.game.PlayerRole
import org.bukkit.entity.Player
import java.time.Instant
import java.util.UUID

class ShopManagerImpl(
    private val economyManager: EconomyManager,
    private val gameManager: GameManager,
    private val purchaseStorage: PurchaseStorage
) : ShopManager {

    override fun purchaseItem(
        player: Player,
        itemId: String,
        gameId: UUID
    ): PurchaseResult {

        // 1. Get item
        val item = getItem(itemId)
            ?: return PurchaseResult.Failure(
                PurchaseResult.FailureReason.ITEM_NOT_FOUND
            )

        // 2. Validate purchase
        val validation = validatePurchase(player, item, gameId)
        if (!validation.canPurchase) {
            val firstError = validation.reasons.firstOrNull { it.type == PurchaseValidation.IssueType.ERROR }
            val reason = firstError?.let { mapIssueToReason(it) }
                ?: PurchaseResult.FailureReason.ECONOMY_ERROR

            return PurchaseResult.Failure(
                reason = reason,
                details = firstError?.details ?: emptyMap()
            )
        }

        // 3. Process economy transaction
        val economyResult = economyManager.withdrawAsync(player, item.price).get()
        if (!economyResult.success) {
            return PurchaseResult.Failure(
                reason = PurchaseResult.FailureReason.ECONOMY_ERROR,
                details = mapOf("message" to economyResult.errorMessage)
            )
        }

        // 4. Create purchase record
        val record = ItemPurchaseRecord(
            playerId = player.uniqueId,
            itemId = itemId,
            purchaseTime = Instant.now(),
            gameId = gameId,
            price = item.price
        )
        purchaseStorage.recordPurchase(record)

        // 5. Give item to player
        val itemStack = createItemStack(item)
        player.inventory.addItem(itemStack)

        // 6. Send feedback
        player.sendMessage(
            "${ChatColor.GREEN}Purchased ${item.displayName} " +
            "${ChatColor.YELLOW}for ${item.price} coins"
        )
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)

        return PurchaseResult.Success(record, item)
    }

    override fun canPurchase(
        player: Player,
        itemId: String,
        gameId: UUID
    ): PurchaseValidation {
        val item = getItem(itemId) ?: return PurchaseValidation(
            canPurchase = false,
            reasons = listOf(
                PurchaseValidation.ValidationIssue(
                    type = PurchaseValidation.IssueType.ERROR,
                    message = "Item not found: $itemId"
                )
            )
        )

        return validatePurchase(player, item, gameId)
    }

    private fun validatePurchase(
        player: Player,
        item: ShopItem,
        gameId: UUID
    ): PurchaseValidation {
        val issues = mutableListOf<PurchaseValidation.ValidationIssue>()

        // Check in game
        val gameData = gameManager.getPlayerGameData(player.uniqueId)
        if (gameData == null) {
            issues.add(
                PurchaseValidation.ValidationIssue(
                    type = PurchaseValidation.IssueType.ERROR,
                    message = "You are not in a game"
                )
            )
            return PurchaseValidation(canPurchase = false, reasons = issues)
        }

        // Check role
        if (!item.isUsableBy(gameData.role)) {
            issues.add(
                PurchaseValidation.ValidationIssue(
                    type = PurchaseValidation.IssueType.ERROR,
                    message = "This item is not available for your role",
                    details = mapOf(
                        "requiredRole" to (item.roleFilter?.name ?: "ANY"),
                        "playerRole" to gameData.role.name
                    )
                )
            )
        }

        // Check phase
        val game = gameManager.getActiveGame()
        if (game == null) {
            issues.add(
                PurchaseValidation.ValidationIssue(
                    type = PurchaseValidation.IssueType.ERROR,
                    message = "No active game"
                )
            )
            return PurchaseValidation(canPurchase = false, reasons = issues)
        }

        if (item.usageRestriction == UsageRestriction.SEEK_PHASE_ONLY &&
            game.phase != GamePhase.SEEKING) {
            issues.add(
                PurchaseValidation.ValidationIssue(
                    type = PurchaseValidation.IssueType.ERROR,
                    message = "This item can only be purchased during seek phase",
                    details = mapOf("currentPhase" to game.phase.name)
                )
            )
        }

        // Check economy
        val balance = economyManager.getBalance(player)
        if (balance < item.price) {
            issues.add(
                PurchaseValidation.ValidationIssue(
                    type = PurchaseValidation.IssueType.ERROR,
                    message = "Insufficient funds",
                    details = mapOf(
                        "required" to item.price,
                        "balance" to balance,
                        "shortfall" to (item.price - balance)
                    )
                )
            )
        }

        // Check purchase limit
        if (item.maxPurchases > 0) {
            val count = purchaseStorage.countPurchases(player.uniqueId, item.id, gameId)
            if (count >= item.maxPurchases) {
                issues.add(
                    PurchaseValidation.ValidationIssue(
                        type = PurchaseValidation.IssueType.ERROR,
                        message = "Purchase limit reached",
                        details = mapOf(
                            "limit" to item.maxPurchases,
                            "current" to count
                        )
                    )
                )
            }
        }

        // Check cooldown
        if (item.cooldown > 0) {
            val latest = purchaseStorage.getLatestPurchase(player.uniqueId, item.id)
            if (latest != null && !latest.isCooldownExpired(item.cooldown)) {
                val remaining = item.cooldown - latest.getElapsedSeconds()
                issues.add(
                    PurchaseValidation.ValidationIssue(
                        type = PurchaseValidation.IssueType.ERROR,
                        message = "Cooldown active",
                        details = mapOf(
                            "cooldown" to item.cooldown,
                            "remaining" to remaining
                        )
                    )
                )
            }
        }

        // Check inventory space
        if (player.inventory.firstEmpty() == -1) {
            issues.add(
                PurchaseValidation.ValidationIssue(
                    type = PurchaseValidation.IssueType.ERROR,
                    message = "Inventory full"
                )
            )
        }

        val canPurchase = issues.none { it.type == PurchaseValidation.IssueType.ERROR }
        return PurchaseValidation(canPurchase, issues)
    }

    private fun createItemStack(item: ShopItem): ItemStack {
        return ItemStack(item.material).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(item.displayName)
                lore = item.getFormattedLore()

                // Add persistent data for identification
                persistentDataContainer.set(
                    NamespacedKey(plugin, "shop_item_id"),
                    PersistentDataType.STRING,
                    item.id
                )
            }
        }
    }
}
```

---

## 4. Integration Points

### GameManager Integration

**Required Interface**:
```kotlin
interface GameManager {
    /**
     * Get player's game data (role, team, status).
     */
    fun getPlayerGameData(playerId: UUID): PlayerGameData?

    /**
     * Get the currently active game.
     */
    fun getActiveGame(): Game?

    /**
     * Check if player is in an active game.
     */
    fun isInGame(playerId: UUID): Boolean
}

data class PlayerGameData(
    val playerId: UUID,
    val role: PlayerRole,
    val teamId: UUID?,
    val isCaptured: Boolean
)
```

### EconomyManager Integration

**Required Interface**:
```kotlin
interface EconomyManager {
    /**
     * Get player's balance.
     */
    fun getBalance(player: Player): Double

    /**
     * Withdraw funds from player's account (async).
     */
    fun withdrawAsync(player: Player, amount: Int): CompletableFuture<EconomyResult>

    /**
     * Deposit funds to player's account (async).
     */
    fun depositAsync(player: Player, amount: Int): CompletableFuture<EconomyResult>
}

data class EconomyResult(
    val success: Boolean,
    val newBalance: Double = 0.0,
    val errorMessage: String = ""
)
```

### EffectManager Integration

**Item Usage Flow**:
```kotlin
@EventHandler
fun onPlayerUseItem(event: PlayerInteractEvent) {
    if (event.action != Action.RIGHT_CLICK_AIR &&
        event.action != Action.RIGHT_CLICK_BLOCK) return

    val item = event.item ?: return
    val player = event.player

    // Check if this is a shop item
    val itemId = item.itemMeta?.persistentDataContainer?.get(
        NamespacedKey(plugin, "shop_item_id"),
        PersistentDataType.STRING
    ) ?: return

    val shopItem = shopManager.getItem(itemId) ?: return

    // If it's an effect item, apply the effect
    if (shopItem.isEffectItem()) {
        event.isCancelled = true

        val result = effectManager.applyEffect(
            player = player,
            effectType = shopItem.effectType!!,
            duration = shopItem.effectDuration!!,
            intensity = shopItem.effectIntensity ?: 1.0
        )

        result.onSuccess {
            // Remove item from inventory
            item.amount -= 1

            // Mark as used in purchase record
            val records = shopManager.getPurchaseHistory(
                player.uniqueId,
                gameManager.getActiveGame()!!.id
            )
            records.lastOrNull { it.itemId == itemId && !it.used }?.markUsed()
        }

        result.onFailure { error ->
            player.sendMessage("${ChatColor.RED}Cannot use item: ${error.message}")
        }
    }
}
```

---

## 5. Migration Strategy

### Backward Compatibility

**Existing Hider Categories**:
```yaml
# OLD: Existing hider category (no roleFilter)
categories:
  disguise-blocks:
    display-name: "&aDisguise Blocks"
    icon: GRASS_BLOCK
    slot: 10
    description:
      - "&7Choose a block to disguise as"

    items:
      # ... existing items ...
```

**Behavior**: If `roleFilter` is not specified, category is visible to ALL roles (maintains existing behavior).

**NEW: Seeker Categories**:
```yaml
categories:
  # NEW: Seeker-only category
  seeker-items:
    display-name: "&cSeeker Tools"
    icon: ENDER_EYE
    slot: 11
    role-filter: SEEKER  # Only visible to seekers
    description:
      - "&7Special items for seekers"

    items:
      # ... seeker items ...
```

### Configuration Migration

**Step 1**: Validate existing config
```kotlin
fun validateShopConfig(config: ShopConfig): List<ValidationError> {
    val errors = mutableListOf<ValidationError>()

    config.categories.forEach { category ->
        // Check for invalid role filters
        if (category.roleFilter != null &&
            category.roleFilter !in PlayerRole.values()) {
            errors.add(
                ValidationError(
                    "Invalid roleFilter in category ${category.id}: ${category.roleFilter}"
                )
            )
        }

        // Validate items
        category.items.forEach { item ->
            item.validate().onFailure { error ->
                errors.add(ValidationError("Item ${item.id}: ${error.message}"))
            }
        }
    }

    return errors
}
```

**Step 2**: Load config with defaults
```kotlin
fun loadShopConfig(): ShopConfig {
    val yamlConfig = loadYaml("shop.yml")

    return ShopConfig(
        categories = yamlConfig.getMapList("categories").map { categoryMap ->
            ShopCategory(
                id = categoryMap["id"] as String,
                displayName = categoryMap["display-name"] as String,
                icon = Material.valueOf(categoryMap["icon"] as String),
                slot = categoryMap["slot"] as Int,
                items = loadItems(categoryMap["items"]),
                description = categoryMap["description"] as? List<String> ?: emptyList(),
                roleFilter = (categoryMap["role-filter"] as? String)?.let {
                    PlayerRole.valueOf(it)
                }  // NEW: Optional field
            )
        }
    )
}
```

---

## 6. Usage Examples

### Example 1: Open Role-Filtered Shop

```kotlin
fun openShop(player: Player) {
    // Get player's role
    val gameData = gameManager.getPlayerGameData(player.uniqueId)
    if (gameData == null) {
        player.sendMessage("${ChatColor.RED}You must be in a game to use the shop")
        return
    }

    // Open shop with role filtering
    shopManager.openShopForRole(player, gameData.role)
}
```

### Example 2: Purchase with Validation

```kotlin
fun attemptPurchase(player: Player, itemId: String) {
    val gameId = gameManager.getActiveGame()?.id
    if (gameId == null) {
        player.sendMessage("${ChatColor.RED}No active game")
        return
    }

    // Check if can purchase
    val validation = shopManager.canPurchase(player, itemId, gameId)

    if (!validation.canPurchase) {
        // Show errors
        validation.reasons.forEach { issue ->
            if (issue.type == PurchaseValidation.IssueType.ERROR) {
                player.sendMessage("${ChatColor.RED}${issue.message}")

                // Show specific details
                when {
                    "cooldown" in issue.details -> {
                        val remaining = issue.details["remaining"] as Long
                        player.sendMessage("${ChatColor.YELLOW}Cooldown: ${remaining}s remaining")
                    }
                    "shortfall" in issue.details -> {
                        val shortfall = issue.details["shortfall"] as Double
                        player.sendMessage("${ChatColor.YELLOW}Need $shortfall more coins")
                    }
                }
            }
        }
        return
    }

    // Attempt purchase
    val result = shopManager.purchaseItem(player, itemId, gameId)

    when (result) {
        is PurchaseResult.Success -> {
            player.sendMessage("${ChatColor.GREEN}Purchase successful!")
        }
        is PurchaseResult.Failure -> {
            player.sendMessage("${ChatColor.RED}Purchase failed: ${result.reason}")
        }
    }
}
```

### Example 3: Display Purchase History

```kotlin
fun showPurchaseHistory(player: Player) {
    val gameId = gameManager.getActiveGame()?.id ?: return
    val history = shopManager.getPurchaseHistory(player.uniqueId, gameId)

    if (history.isEmpty()) {
        player.sendMessage("${ChatColor.GRAY}No purchases yet")
        return
    }

    player.sendMessage("${ChatColor.GOLD}Purchase History:")
    history.forEach { record ->
        val item = shopManager.getItem(record.itemId)
        val usedStatus = if (record.used) "${ChatColor.GREEN}✓" else "${ChatColor.GRAY}○"
        player.sendMessage(
            "  $usedStatus ${item?.displayName ?: record.itemId} - ${record.price} coins"
        )
    }
}
```

### Example 4: Admin Commands

```kotlin
@Command("hs shop reload")
fun reloadShopConfig(sender: CommandSender) {
    // Reload from YAML
    val newConfig = loadShopConfig()

    // Validate
    val errors = validateShopConfig(newConfig)
    if (errors.isNotEmpty()) {
        sender.sendMessage("${ChatColor.RED}Config validation errors:")
        errors.forEach { error ->
            sender.sendMessage("  ${ChatColor.YELLOW}${error.message}")
        }
        return
    }

    // Apply
    shopManager.updateConfig(newConfig)
    sender.sendMessage("${ChatColor.GREEN}Shop config reloaded successfully")
}

@Command("hs shop clear [player]")
fun clearPurchaseHistory(sender: CommandSender, playerName: String?) {
    val gameId = gameManager.getActiveGame()?.id ?: return

    if (playerName == null) {
        // Clear all
        shopManager.clearPurchaseHistory(gameId)
        sender.sendMessage("${ChatColor.GREEN}Cleared all purchase history")
    } else {
        // Clear specific player
        val player = Bukkit.getPlayer(playerName)
        if (player == null) {
            sender.sendMessage("${ChatColor.RED}Player not found: $playerName")
            return
        }

        purchaseStorage.clearPlayer(player.uniqueId)
        sender.sendMessage("${ChatColor.GREEN}Cleared purchase history for $playerName")
    }
}
```

---

## 7. Testing Requirements

### Unit Tests

```kotlin
class ShopManagerTest {

    @Test
    fun `getCategoriesForRole should filter by role`() {
        val seekerCategory = ShopCategory(
            id = "seeker-items",
            displayName = "Seeker Items",
            icon = Material.ENDER_EYE,
            slot = 0,
            items = emptyList(),
            roleFilter = PlayerRole.SEEKER
        )

        val hiderCategory = ShopCategory(
            id = "hider-items",
            displayName = "Hider Items",
            icon = Material.GRASS_BLOCK,
            slot = 1,
            items = emptyList(),
            roleFilter = PlayerRole.HIDER
        )

        val allCategories = ShopCategory(
            id = "all-items",
            displayName = "All Items",
            icon = Material.CHEST,
            slot = 2,
            items = emptyList(),
            roleFilter = null
        )

        val manager = ShopManagerImpl(
            categories = listOf(seekerCategory, hiderCategory, allCategories)
        )

        // Seeker sees seeker + all categories
        val seekerCategories = manager.getCategoriesForRole(PlayerRole.SEEKER)
        assertEquals(2, seekerCategories.size)
        assertTrue(seekerCategories.any { it.id == "seeker-items" })
        assertTrue(seekerCategories.any { it.id == "all-items" })

        // Hider sees hider + all categories
        val hiderCategories = manager.getCategoriesForRole(PlayerRole.HIDER)
        assertEquals(2, hiderCategories.size)
        assertTrue(hiderCategories.any { it.id == "hider-items" })
        assertTrue(hiderCategories.any { it.id == "all-items" })
    }

    @Test
    fun `purchaseItem should enforce cooldown`() {
        val item = ShopItem(
            id = "test-item",
            material = Material.DIAMOND,
            displayName = "Test Item",
            lore = emptyList(),
            slot = 0,
            price = 100,
            action = ShopAction.Close,
            cooldown = 10  // 10 second cooldown
        )

        val manager = ShopManagerImpl(
            items = listOf(item),
            economyManager = mockEconomy,
            gameManager = mockGame,
            purchaseStorage = PurchaseStorage()
        )

        val player = mockPlayer()
        val gameId = UUID.randomUUID()

        // First purchase succeeds
        val result1 = manager.purchaseItem(player, "test-item", gameId)
        assertTrue(result1 is PurchaseResult.Success)

        // Immediate second purchase fails (cooldown)
        val result2 = manager.purchaseItem(player, "test-item", gameId)
        assertTrue(result2 is PurchaseResult.Failure)
        assertEquals(
            PurchaseResult.FailureReason.COOLDOWN_ACTIVE,
            (result2 as PurchaseResult.Failure).reason
        )

        // After 11 seconds, succeeds
        Thread.sleep(11000)
        val result3 = manager.purchaseItem(player, "test-item", gameId)
        assertTrue(result3 is PurchaseResult.Success)
    }
}
```

### Integration Tests

```kotlin
@Test
fun `full purchase flow should work correctly`() {
    val shopManager = ShopManagerImpl(...)
    val effectManager = EffectManagerImpl(...)
    val gameManager = GameManagerImpl(...)

    // Setup game
    val game = gameManager.createGame(arena)
    val player = createTestPlayer()
    gameManager.addPlayer(game.id, player, PlayerRole.SEEKER)
    gameManager.startGame(game.id)

    // Purchase vision enhancer
    val result = shopManager.purchaseItem(player, "vision-enhancer", game.id)
    assertTrue(result is PurchaseResult.Success)

    // Item should be in inventory
    val itemInInventory = player.inventory.contents.any {
        it?.itemMeta?.persistentDataContainer?.get(
            NamespacedKey(plugin, "shop_item_id"),
            PersistentDataType.STRING
        ) == "vision-enhancer"
    }
    assertTrue(itemInInventory)

    // Use item
    val itemStack = player.inventory.first { /* find vision enhancer */ }
    player.inventory.setItemInMainHand(itemStack)
    player.performAction(PlayerAction.RIGHT_CLICK)

    // Effect should be active
    assertTrue(effectManager.hasEffect(player, EffectType.VISION))

    // Item should be removed
    assertFalse(player.inventory.contains(itemStack))
}
```

---

## Summary

This shop extension API provides:

- **Role-based filtering** with backward compatibility
- **Comprehensive purchase flow** with validation, cooldowns, and limits
- **Clean integration points** with existing systems
- **Type-safe APIs** with Result types and sealed classes
- **Flexible configuration** via YAML with validation
- **Complete test coverage** requirements

**Implementation Status**: Ready for development
**Next Steps**: Implement ShopManager extensions and PurchaseStorage following this contract
