# Data Model: Seeker Shop System

**Feature Branch**: `003-seeker-shop`
**Created**: 2025-11-03
**Status**: Design Phase
**Related Documents**: [spec.md](./spec.md) | [plan.md](./plan.md) | [research.md](./research.md)

## Overview

This document defines the complete data model for the Seeker Shop System, including entity definitions, data structures, relationships, validation rules, state transitions, and storage strategies. The model extends the existing shop system to support role-based items with time-limited effects.

---

## Table of Contents

1. [Entity Definitions](#1-entity-definitions)
2. [Data Structures in Kotlin](#2-data-structures-in-kotlin)
3. [Relationships](#3-relationships)
4. [Validation Rules](#4-validation-rules)
5. [State Transitions](#5-state-transitions)
6. [Storage Strategy](#6-storage-strategy)
7. [Memory Management](#7-memory-management)
8. [Data Flow Diagrams](#8-data-flow-diagrams)

---

## 1. Entity Definitions

### 1.1 Core Entities

#### ActiveEffect

**Purpose**: Represents a time-limited effect currently applied to a player.

**Attributes**:
- `playerId: UUID` - The player who has this effect
- `effectType: EffectType` - The type of effect (vision, glow, speed, reach)
- `startTime: Instant` - When the effect was applied
- `endTime: Instant` - When the effect will expire
- `intensity: Double` - Effect strength/multiplier (e.g., 1.5 for reach extension)
- `taskId: Int?` - BukkitScheduler task ID for cleanup
- `metadata: Map<String, Any>` - Additional effect-specific data (optional)

**Lifecycle**: Created when item used → Active during effect period → Expired → Removed

**Constraints**:
- `endTime > startTime` (always)
- `intensity > 0.0` (must be positive)
- `taskId` must be valid BukkitTask ID or null
- Only one effect of each type per player

---

#### SeekerItem (Extension of ShopItem)

**Purpose**: Configuration data for seeker-specific purchasable items.

**Attributes** (extends ShopItem):
- Inherited from ShopItem:
  - `id: String` - Unique identifier (e.g., "vision-enhancer")
  - `material: Material` - Display icon
  - `displayName: String` - Formatted name
  - `lore: List<String>` - Item description
  - `slot: Int` - GUI position
  - `price: Int` - Cost in economy currency
  - `action: ShopAction` - Action type

- New fields:
  - `effectType: EffectType` - Which effect this item provides
  - `effectDuration: Int` - Duration in seconds
  - `effectIntensity: Double` - Effect strength (default: 1.0)
  - `roleFilter: PlayerRole` - Which role can see/buy this item
  - `cooldown: Int` - Seconds before repurchase allowed (default: 0)
  - `maxPurchases: Int` - Purchase limit per game (default: -1 = unlimited)
  - `usageRestriction: UsageRestriction` - When item can be used

**Constraints**:
- `effectDuration` in range [5, 120] seconds
- `effectIntensity` in range [0.1, 3.0]
- `price >= 0`
- `cooldown >= 0`

---

#### ItemPurchaseRecord

**Purpose**: Tracks purchases for cooldown and limit enforcement.

**Attributes**:
- `playerId: UUID` - Purchaser
- `itemId: String` - Which item was purchased
- `purchaseTime: Instant` - When purchased
- `gameId: UUID` - Which game session
- `price: Int` - Amount paid
- `used: Boolean` - Whether item has been consumed

**Lifecycle**: Created on purchase → Updated on use → Cleared on game end

**Constraints**:
- `price >= 0`
- `purchaseTime` must be within active game period

---

### 1.2 Enumeration Types

#### EffectType

**Purpose**: Categorizes the different types of effects available.

**Values**:
- `VISION` - Enhanced vision (Night Vision + view distance)
- `GLOW` - Disguised blocks glow with particles
- `SPEED` - Movement speed boost
- `REACH` - Extended attack range

**Properties per type**:

| Type   | Default Duration | Default Intensity | Minecraft Effect Used          |
|--------|------------------|-------------------|--------------------------------|
| VISION | 30s              | 1.0 (32 chunks)   | PotionEffectType.NIGHT_VISION  |
| GLOW   | 15s              | 1.0 (particle/s)  | Custom particle system         |
| SPEED  | 30s              | 1.0 (level 2)     | PotionEffectType.SPEED         |
| REACH  | 30s              | 1.5 (1.5x range)  | Custom raycast extension       |

---

#### PlayerRole (Extension)

**Purpose**: Defines player roles for filtering shop items.

**Values** (existing):
- `SEEKER` - Player hunting hiders
- `HIDER` - Player hiding from seekers
- `SPECTATOR` - Observer (no shop access)

**Usage**: Extended to support role-based shop filtering.

---

#### UsageRestriction

**Purpose**: Defines when an item can be used.

**Values**:
- `ALWAYS` - Can use anytime during game
- `SEEK_PHASE_ONLY` - Only during active seek phase
- `NOT_WHILE_CAPTURED` - Cannot use if captured
- `ONCE_PER_GAME` - Single use per game session

---

#### OverlapPolicy

**Purpose**: Defines behavior when applying duplicate effects.

**Values**:
- `REPLACE` - New effect overwrites existing (default)
- `EXTEND` - Add remaining time to new duration
- `REJECT` - Refuse to apply if already active
- `STACK` - Allow multiple instances (not recommended)

---

## 2. Data Structures in Kotlin

### 2.1 Core Data Classes

```kotlin
package com.hideandseek.effects

import org.bukkit.entity.Player
import java.time.Instant
import java.util.UUID

/**
 * Represents an active effect applied to a player.
 *
 * @property playerId The UUID of the player with this effect
 * @property effectType The type of effect applied
 * @property startTime When the effect was activated
 * @property endTime When the effect will expire
 * @property intensity Effect strength multiplier
 * @property taskId BukkitScheduler task ID for cleanup (nullable)
 * @property metadata Additional effect-specific data
 */
data class ActiveEffect(
    val playerId: UUID,
    val effectType: EffectType,
    val startTime: Instant,
    val endTime: Instant,
    val intensity: Double = 1.0,
    var taskId: Int? = null,
    val metadata: MutableMap<String, Any> = mutableMapOf()
) {
    /**
     * Get remaining duration in seconds.
     */
    fun getRemainingSeconds(): Long {
        val now = Instant.now()
        return if (now.isBefore(endTime)) {
            endTime.epochSecond - now.epochSecond
        } else {
            0L
        }
    }

    /**
     * Check if effect has expired.
     */
    fun isExpired(): Boolean {
        return Instant.now().isAfter(endTime)
    }

    /**
     * Get total duration in seconds.
     */
    fun getTotalDuration(): Long {
        return endTime.epochSecond - startTime.epochSecond
    }

    /**
     * Get progress percentage (0.0 to 1.0).
     */
    fun getProgress(): Double {
        val total = getTotalDuration()
        val remaining = getRemainingSeconds()
        return if (total > 0) {
            1.0 - (remaining.toDouble() / total.toDouble())
        } else {
            1.0
        }
    }
}
```

---

```kotlin
package com.hideandseek.effects

/**
 * Types of effects that can be applied to players.
 */
enum class EffectType(
    val displayName: String,
    val defaultDuration: Int,
    val defaultIntensity: Double,
    val icon: String
) {
    VISION(
        displayName = "Enhanced Vision",
        defaultDuration = 30,
        defaultIntensity = 1.0,
        icon = "👁"
    ),

    GLOW(
        displayName = "Glow Detector",
        defaultDuration = 15,
        defaultIntensity = 1.0,
        icon = "✨"
    ),

    SPEED(
        displayName = "Speed Boost",
        defaultDuration = 30,
        defaultIntensity = 1.0,
        icon = "⚡"
    ),

    REACH(
        displayName = "Reach Extender",
        defaultDuration = 30,
        defaultIntensity = 1.5,
        icon = "🎯"
    );

    /**
     * Get the amplifier level for potion effects (0-indexed).
     */
    fun getPotionAmplifier(): Int {
        return when (this) {
            VISION -> 0 // Night Vision level 1
            SPEED -> 1  // Speed level 2
            else -> 0
        }
    }

    /**
     * Check if this effect type uses a Minecraft potion effect.
     */
    fun usesPotionEffect(): Boolean {
        return this == VISION || this == SPEED
    }

    /**
     * Get formatted display string with icon.
     */
    fun getDisplayString(): String {
        return "$icon $displayName"
    }
}
```

---

```kotlin
package com.hideandseek.effects

/**
 * Policies for handling overlapping effects of the same type.
 */
enum class OverlapPolicy {
    /** New effect replaces existing effect */
    REPLACE,

    /** Add remaining time to new duration (capped at max) */
    EXTEND,

    /** Reject new effect if one is already active */
    REJECT,

    /** Allow multiple instances (not recommended) */
    STACK;

    companion object {
        /** Maximum total duration when extending (in seconds) */
        const val MAX_EXTENDED_DURATION = 120
    }
}
```

---

```kotlin
package com.hideandseek.shop

import com.hideandseek.effects.EffectType
import com.hideandseek.game.PlayerRole
import org.bukkit.Material

/**
 * Extended shop action types to support effect items.
 */
sealed class ShopAction {
    /** Disguise as a block (hider action) */
    data class Disguise(val blockType: Material) : ShopAction()

    /** Use an effect item (seeker action) */
    data class UseEffectItem(
        val effectType: EffectType,
        val duration: Int,
        val intensity: Double
    ) : ShopAction()

    /** Navigate back to category menu */
    object GoBack : ShopAction()

    /** Close the shop GUI */
    object Close : ShopAction()
}
```

---

```kotlin
package com.hideandseek.shop

import com.hideandseek.effects.EffectType
import com.hideandseek.game.PlayerRole
import org.bukkit.Material

/**
 * Extended shop category with role filtering.
 */
data class ShopCategory(
    val id: String,
    val displayName: String,
    val icon: Material,
    val slot: Int,
    val items: List<ShopItem>,
    val roleFilter: PlayerRole? = null, // null = visible to all roles
    val description: List<String> = emptyList()
) {
    /**
     * Check if this category is visible to a player with given role.
     */
    fun isVisibleTo(role: PlayerRole): Boolean {
        return roleFilter == null || roleFilter == role
    }

    /**
     * Get items visible to a specific role.
     */
    fun getItemsForRole(role: PlayerRole): List<ShopItem> {
        return items.filter { it.isUsableBy(role) }
    }
}
```

---

```kotlin
package com.hideandseek.shop

import com.hideandseek.effects.EffectType
import com.hideandseek.game.PlayerRole
import org.bukkit.Material

/**
 * Extended shop item with effect metadata.
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

    // Effect fields (new)
    val effectType: EffectType? = null,
    val effectDuration: Int? = null,
    val effectIntensity: Double? = null,
    val roleFilter: PlayerRole? = null,
    val cooldown: Int = 0,
    val maxPurchases: Int = -1, // -1 = unlimited
    val usageRestriction: UsageRestriction = UsageRestriction.ALWAYS
) {
    /**
     * Check if item is usable by a given role.
     */
    fun isUsableBy(role: PlayerRole): Boolean {
        return roleFilter == null || roleFilter == role
    }

    /**
     * Check if this is an effect item.
     */
    fun isEffectItem(): Boolean {
        return effectType != null
    }

    /**
     * Validate item configuration.
     */
    fun validate(): Result<Unit> {
        if (price < 0) {
            return Result.failure(IllegalArgumentException("Price cannot be negative: $price"))
        }

        if (effectType != null) {
            val duration = effectDuration ?: return Result.failure(
                IllegalArgumentException("Effect items must specify duration")
            )

            if (duration !in 5..120) {
                return Result.failure(
                    IllegalArgumentException("Duration must be between 5 and 120 seconds: $duration")
                )
            }

            val intensity = effectIntensity ?: 1.0
            if (intensity !in 0.1..3.0) {
                return Result.failure(
                    IllegalArgumentException("Intensity must be between 0.1 and 3.0: $intensity")
                )
            }
        }

        return Result.success(Unit)
    }
}

/**
 * Restrictions on when an item can be used.
 */
enum class UsageRestriction {
    ALWAYS,
    SEEK_PHASE_ONLY,
    NOT_WHILE_CAPTURED,
    ONCE_PER_GAME
}
```

---

```kotlin
package com.hideandseek.shop

import java.time.Instant
import java.util.UUID

/**
 * Record of an item purchase for tracking cooldowns and limits.
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

---

### 2.2 In-Memory Storage Structures

```kotlin
package com.hideandseek.effects

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Storage for active effects.
 *
 * Structure: Map<PlayerUUID, Map<EffectType, ActiveEffect>>
 *
 * Example:
 * {
 *   "player-uuid-1": {
 *     VISION: ActiveEffect(type=VISION, remaining=25s),
 *     SPEED: ActiveEffect(type=SPEED, remaining=10s)
 *   },
 *   "player-uuid-2": {
 *     GLOW: ActiveEffect(type=GLOW, remaining=12s)
 *   }
 * }
 */
class EffectStorage {
    private val activeEffects = ConcurrentHashMap<UUID, MutableMap<EffectType, ActiveEffect>>()

    /**
     * Add or update an effect for a player.
     */
    fun putEffect(playerId: UUID, effect: ActiveEffect) {
        activeEffects.getOrPut(playerId) { mutableMapOf() }[effect.effectType] = effect
    }

    /**
     * Get a specific effect for a player.
     */
    fun getEffect(playerId: UUID, effectType: EffectType): ActiveEffect? {
        return activeEffects[playerId]?.get(effectType)
    }

    /**
     * Get all effects for a player.
     */
    fun getAllEffects(playerId: UUID): Map<EffectType, ActiveEffect> {
        return activeEffects[playerId]?.toMap() ?: emptyMap()
    }

    /**
     * Remove a specific effect.
     */
    fun removeEffect(playerId: UUID, effectType: EffectType): ActiveEffect? {
        val effects = activeEffects[playerId] ?: return null
        val removed = effects.remove(effectType)

        // Clean up empty player entries
        if (effects.isEmpty()) {
            activeEffects.remove(playerId)
        }

        return removed
    }

    /**
     * Remove all effects for a player.
     */
    fun removeAllEffects(playerId: UUID): Map<EffectType, ActiveEffect> {
        return activeEffects.remove(playerId) ?: emptyMap()
    }

    /**
     * Check if player has a specific effect.
     */
    fun hasEffect(playerId: UUID, effectType: EffectType): Boolean {
        return activeEffects[playerId]?.containsKey(effectType) ?: false
    }

    /**
     * Get all players with any active effect.
     */
    fun getAllPlayers(): Set<UUID> {
        return activeEffects.keys.toSet()
    }

    /**
     * Clear all effects (for game end).
     */
    fun clearAll() {
        activeEffects.clear()
    }

    /**
     * Remove expired effects.
     * @return List of expired effects that were removed
     */
    fun cleanupExpired(): List<ActiveEffect> {
        val expired = mutableListOf<ActiveEffect>()

        activeEffects.forEach { (playerId, effects) ->
            effects.entries.removeIf { (_, effect) ->
                if (effect.isExpired()) {
                    expired.add(effect)
                    true
                } else {
                    false
                }
            }

            // Remove player entry if no effects remain
            if (effects.isEmpty()) {
                activeEffects.remove(playerId)
            }
        }

        return expired
    }

    /**
     * Get memory usage estimate in bytes.
     */
    fun estimateMemoryUsage(): Long {
        // UUID: 16 bytes
        // EffectType: 4 bytes (enum reference)
        // ActiveEffect: ~150 bytes (includes Instant objects, task ID, metadata)
        val bytesPerEffect = 150L
        val bytesPerPlayer = 16L // UUID in map key

        var total = 0L
        activeEffects.forEach { (_, effects) ->
            total += bytesPerPlayer + (effects.size * bytesPerEffect)
        }

        return total
    }
}
```

---

```kotlin
package com.hideandseek.shop

import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Storage for purchase records.
 *
 * Structure: Map<PlayerUUID, List<ItemPurchaseRecord>>
 *
 * Cleared at end of each game session.
 */
class PurchaseStorage {
    private val purchases = ConcurrentHashMap<UUID, MutableList<ItemPurchaseRecord>>()

    /**
     * Record a purchase.
     */
    fun recordPurchase(record: ItemPurchaseRecord) {
        purchases.getOrPut(record.playerId) { mutableListOf() }.add(record)
    }

    /**
     * Get all purchases for a player.
     */
    fun getPurchases(playerId: UUID): List<ItemPurchaseRecord> {
        return purchases[playerId]?.toList() ?: emptyList()
    }

    /**
     * Get purchases for a specific item.
     */
    fun getPurchasesForItem(playerId: UUID, itemId: String): List<ItemPurchaseRecord> {
        return purchases[playerId]?.filter { it.itemId == itemId } ?: emptyList()
    }

    /**
     * Get most recent purchase of an item.
     */
    fun getLatestPurchase(playerId: UUID, itemId: String): ItemPurchaseRecord? {
        return getPurchasesForItem(playerId, itemId).maxByOrNull { it.purchaseTime }
    }

    /**
     * Count purchases for an item in current game.
     */
    fun countPurchases(playerId: UUID, itemId: String, gameId: UUID): Int {
        return purchases[playerId]?.count { it.itemId == itemId && it.gameId == gameId } ?: 0
    }

    /**
     * Clear purchases for a player.
     */
    fun clearPlayer(playerId: UUID) {
        purchases.remove(playerId)
    }

    /**
     * Clear all purchases.
     */
    fun clearAll() {
        purchases.clear()
    }

    /**
     * Check if player can purchase item (cooldown check).
     */
    fun canPurchase(playerId: UUID, itemId: String, cooldownSeconds: Int): Boolean {
        if (cooldownSeconds <= 0) return true

        val latest = getLatestPurchase(playerId, itemId) ?: return true
        return latest.isCooldownExpired(cooldownSeconds)
    }
}
```

---

## 3. Relationships

### 3.1 Entity Relationship Diagram

```
┌─────────────────┐         1:N         ┌──────────────────┐
│     Player      │────────────────────>│  ActiveEffect    │
│  (UUID)         │                     │                  │
└─────────────────┘                     │ - playerId       │
                                        │ - effectType     │
                                        │ - startTime      │
                                        │ - endTime        │
                                        │ - intensity      │
                                        │ - taskId         │
                                        └──────────────────┘
                                               │
                                               │ N:1
                                               │
                                               v
        ┌──────────────────┐           ┌──────────────────┐
        │  ShopCategory    │───1:N────>│    ShopItem      │
        │                  │           │                  │
        │ - id             │           │ - id             │
        │ - displayName    │           │ - material       │
        │ - icon           │           │ - displayName    │
        │ - roleFilter     │<──┐       │ - price          │
        │ - items          │   │       │ - effectType     │──┐
        └──────────────────┘   │       │ - effectDuration │  │
                               │       │ - effectIntensity│  │ N:1
                               │       │ - roleFilter     │  │
                               │       │ - action         │  │
                        1:1    │       └──────────────────┘  │
                               │              │              │
        ┌──────────────────┐   │              │ 1:N          │
        │   PlayerRole     │───┘              │              │
        │                  │                  v              │
        │ - SEEKER         │       ┌──────────────────┐     │
        │ - HIDER          │       │ ItemPurchaseRec  │     │
        │ - SPECTATOR      │       │                  │     │
        └──────────────────┘       │ - playerId       │     │
                                   │ - itemId         │     │
                                   │ - purchaseTime   │     │
                                   │ - gameId         │     │
                                   │ - used           │     │
                                   └──────────────────┘     │
                                                            │
                                   ┌──────────────────┐     │
                                   │   EffectType     │<────┘
                                   │  (Enum)          │
                                   │ - VISION         │
                                   │ - GLOW           │
                                   │ - SPEED          │
                                   │ - REACH          │
                                   └──────────────────┘
```

### 3.2 Key Relationships

#### Player → ActiveEffect (1:N)
- **Cardinality**: One player can have multiple active effects (up to 4, one per type)
- **Constraint**: Only one effect of each type per player
- **Cascade**: When player leaves game, all effects are removed
- **Validation**: Effect must have valid playerId that exists in game

#### ShopItem → EffectType (1:1)
- **Cardinality**: Each seeker item maps to exactly one effect type
- **Constraint**: effectType must not be null for seeker items
- **Validation**: effectDuration and effectIntensity must be specified

#### ShopCategory → PlayerRole (1:1 filter)
- **Cardinality**: Each category can filter by one role (or null for all)
- **Constraint**: roleFilter must be valid PlayerRole value
- **Behavior**: Only matching roles see the category in shop GUI

#### Player → ItemPurchaseRecord (1:N)
- **Cardinality**: One player can have multiple purchase records
- **Lifecycle**: Records cleared at game end
- **Purpose**: Track cooldowns and purchase limits

---

## 4. Validation Rules

### 4.1 Effect Validation

```kotlin
/**
 * Validates effect parameters before application.
 */
object EffectValidator {

    fun validateDuration(duration: Int): Result<Int> {
        return when {
            duration < 5 -> Result.failure(
                IllegalArgumentException("Duration too short: $duration (min: 5s)")
            )
            duration > 120 -> Result.failure(
                IllegalArgumentException("Duration too long: $duration (max: 120s)")
            )
            else -> Result.success(duration)
        }
    }

    fun validateIntensity(effectType: EffectType, intensity: Double): Result<Double> {
        val range = when (effectType) {
            EffectType.VISION -> 0.5..2.0  // 0.5x to 2x view distance
            EffectType.GLOW -> 0.5..2.0    // 0.5x to 2x particle frequency
            EffectType.SPEED -> 0.5..2.0   // 0.5x to 2x speed amplifier
            EffectType.REACH -> 1.0..2.0   // 1x to 2x reach distance
        }

        return if (intensity in range) {
            Result.success(intensity)
        } else {
            Result.failure(
                IllegalArgumentException(
                    "Intensity $intensity out of range $range for $effectType"
                )
            )
        }
    }

    fun validatePlayer(player: Player, effectType: EffectType): Result<Unit> {
        // Must be in game
        val gameData = GameManager.getPlayerGameData(player.uniqueId)
            ?: return Result.failure(IllegalStateException("Player not in game"))

        // Must be seeker
        if (gameData.role != PlayerRole.SEEKER) {
            return Result.failure(IllegalStateException("Only seekers can use effect items"))
        }

        // Must be in seek phase
        val game = GameManager.getActiveGame()
            ?: return Result.failure(IllegalStateException("No active game"))

        if (game.phase != GamePhase.SEEKING) {
            return Result.failure(IllegalStateException("Effects only usable during seek phase"))
        }

        return Result.success(Unit)
    }
}
```

### 4.2 Purchase Validation

```kotlin
/**
 * Validates purchase requests before processing.
 */
object PurchaseValidator {

    fun validatePurchase(
        player: Player,
        item: ShopItem,
        gameId: UUID,
        purchaseStorage: PurchaseStorage
    ): Result<Unit> {

        // Check role access
        val gameData = GameManager.getPlayerGameData(player.uniqueId)
            ?: return Result.failure(IllegalStateException("Player not in game"))

        if (!item.isUsableBy(gameData.role)) {
            return Result.failure(IllegalStateException("Item not available for your role"))
        }

        // Check economy
        val balance = EconomyManager.getBalance(player)
        if (balance < item.price) {
            return Result.failure(InsufficientFundsException("Need ${item.price}, have $balance"))
        }

        // Check purchase limit
        if (item.maxPurchases > 0) {
            val count = purchaseStorage.countPurchases(player.uniqueId, item.id, gameId)
            if (count >= item.maxPurchases) {
                return Result.failure(
                    PurchaseLimitException("Reached purchase limit: ${item.maxPurchases}")
                )
            }
        }

        // Check cooldown
        if (item.cooldown > 0) {
            if (!purchaseStorage.canPurchase(player.uniqueId, item.id, item.cooldown)) {
                val latest = purchaseStorage.getLatestPurchase(player.uniqueId, item.id)
                val remaining = item.cooldown - (latest?.getElapsedSeconds() ?: 0L)
                return Result.failure(
                    CooldownException("Cooldown remaining: ${remaining}s")
                )
            }
        }

        // Check inventory space
        if (player.inventory.firstEmpty() == -1) {
            return Result.failure(InventoryFullException("Inventory full"))
        }

        return Result.success(Unit)
    }
}

// Custom exceptions
class InsufficientFundsException(message: String) : Exception(message)
class PurchaseLimitException(message: String) : Exception(message)
class CooldownException(message: String) : Exception(message)
class InventoryFullException(message: String) : Exception(message)
```

### 4.3 Validation Rules Summary

| Validation Type | Rule | Error Handling |
|----------------|------|----------------|
| Effect Duration | 5 ≤ duration ≤ 120 seconds | Reject with error message |
| Effect Intensity | Type-specific ranges (see above) | Reject with error message |
| Player Role | Must be SEEKER for seeker items | Reject with error message |
| Game Phase | Must be SEEKING phase | Reject with error message |
| Purchase Limit | count < maxPurchases | Show remaining purchases |
| Cooldown | elapsed ≥ cooldown | Show remaining cooldown time |
| Economy | balance ≥ price | Show insufficient funds |
| Inventory | Has empty slot | Prompt to clear inventory |
| Effect Overlap | Based on OverlapPolicy | Replace/Extend/Reject as configured |

---

## 5. State Transitions

### 5.1 Effect Lifecycle State Machine

```
                    [Item Used]
                         │
                         v
                 ┌───────────────┐
                 │   CREATED     │
                 │               │
                 │ - Validated   │
                 │ - Task sched. │
                 └───────┬───────┘
                         │
              [Applied Successfully]
                         │
                         v
                 ┌───────────────┐
                 │    ACTIVE     │<────────┐
                 │               │         │
                 │ - Effect on   │    [Replaced/Extended]
                 │ - Timer runs  │         │
                 └───────┬───────┘         │
                         │                 │
            ┌────────────┼────────────┐    │
            │            │            │    │
     [Time Expired] [Manually    [Game     │
                     Removed]     Ended]   │
            │            │            │    │
            v            v            v    │
        ┌───────────────────────────────┐  │
        │          EXPIRED              │  │
        │                               │  │
        │ - Effect removed              │  │
        │ - Task cancelled              │  │
        │ - Memory freed                │  │
        └───────────────────────────────┘  │
                                           │
        [Same effect reapplied]────────────┘
```

### 5.2 Purchase Flow State Machine

```
                [/hs shop command]
                         │
                         v
                 ┌───────────────┐
                 │  SHOP OPENED  │
                 │               │
                 │ - GUI shown   │
                 │ - Role filter │
                 └───────┬───────┘
                         │
                  [Item clicked]
                         │
                         v
                 ┌───────────────┐
                 │  VALIDATING   │
                 │               │
                 │ - Role check  │
                 │ - Balance chk │
                 │ - Cooldown    │
                 │ - Limit check │
                 └───────┬───────┘
                         │
            ┌────────────┼────────────┐
            │                         │
     [Validation       [Validation
      Success]          Failed]
            │                         │
            v                         v
    ┌───────────────┐         ┌───────────────┐
    │  PURCHASING   │         │    REJECTED   │
    │               │         │               │
    │ - Vault txn   │         │ - Error msg   │
    │ - Record save │         │ - GUI refresh │
    └───────┬───────┘         └───────────────┘
            │
    [Transaction
     Complete]
            │
            v
    ┌───────────────┐
    │   PURCHASED   │
    │               │
    │ - Item given  │
    │ - Msg sent    │
    │ - GUI update  │
    └───────┬───────┘
            │
     [Item used]
            │
            v
    ┌───────────────┐
    │     USED      │
    │               │
    │ - Effect on   │
    │ - Item gone   │
    │ - Record upd. │
    └───────────────┘
```

### 5.3 Shop Category Visibility States

```
         [Player opens shop]
                 │
                 v
         ┌───────────────┐
         │  GET PLAYER   │
         │     ROLE      │
         └───────┬───────┘
                 │
    ┌────────────┼────────────┬────────────┐
    │            │            │            │
[SEEKER]    [HIDER]    [SPECTATOR]   [NOT IN GAME]
    │            │            │            │
    v            v            v            v
┌───────┐  ┌───────┐  ┌───────┐  ┌───────────┐
│Seeker │  │Hider  │  │ Empty │  │  ERROR    │
│Items  │  │Items  │  │ Shop  │  │  Message  │
│Shown  │  │Shown  │  │       │  │           │
└───────┘  └───────┘  └───────┘  └───────────┘
```

### 5.4 Effect Overlap Resolution

```
    [Apply new effect]
            │
            v
    ┌───────────────┐
    │ Check if same │
    │  type exists  │
    └───────┬───────┘
            │
    ┌───────┴───────┐
    │ YES           │ NO
    │               │
    v               v
┌──────────┐  ┌─────────┐
│ Check    │  │ Apply   │
│ Policy   │  │ Direct  │
└────┬─────┘  └─────────┘
     │
     │  [Policy = REPLACE]
     ├────────────────────> Remove old → Apply new
     │
     │  [Policy = EXTEND]
     ├────────────────────> Calculate total duration → Reapply with extended time
     │
     │  [Policy = REJECT]
     ├────────────────────> Show error → Keep existing
     │
     │  [Policy = STACK]
     └────────────────────> Apply alongside (not recommended)
```

---

## 6. Storage Strategy

### 6.1 YAML Configuration (shop.yml)

```yaml
# Seeker Shop Configuration Extension

categories:
  # Existing hider category
  disguise-blocks:
    # ... existing configuration ...

  # NEW: Seeker Items Category
  seeker-items:
    display-name: "&cSeeker Tools"
    icon: ENDER_EYE
    slot: 11
    role-filter: SEEKER  # Only visible to seekers
    description:
      - "&7Special items to help you find hiders"
      - "&7Effects are temporary but powerful!"

    items:
      # Vision Enhancer
      vision-enhancer:
        material: ENDER_PEARL
        display-name: "&b&lVision Enhancer"
        slot: 10
        price: 300
        effect-type: VISION
        effect-duration: 30
        effect-intensity: 1.0
        cooldown: 10
        max-purchases: -1  # Unlimited
        usage-restriction: SEEK_PHASE_ONLY
        lore:
          - "&7Enhances your vision for &e30 seconds"
          - "&7See farther and through darkness"
          - ""
          - "&e&lPrice: &f300 coins"
          - "&c&lCooldown: &f10 seconds"

      # Glow Detector
      glow-detector:
        material: GLOWSTONE_DUST
        display-name: "&e&lGlow Detector"
        slot: 11
        price: 500
        effect-type: GLOW
        effect-duration: 15
        effect-intensity: 1.0
        cooldown: 30
        max-purchases: 3
        usage-restriction: SEEK_PHASE_ONLY
        lore:
          - "&7Makes disguised blocks &eglow &7for &e15 seconds"
          - "&7Only you can see the glow effect"
          - ""
          - "&e&lPrice: &f500 coins"
          - "&c&lCooldown: &f30 seconds"
          - "&c&lLimit: &f3 per game"

      # Speed Boost
      speed-boost:
        material: SUGAR
        display-name: "&a&lSpeed Boost"
        slot: 12
        price: 200
        effect-type: SPEED
        effect-duration: 30
        effect-intensity: 1.0
        cooldown: 5
        max-purchases: -1
        usage-restriction: SEEK_PHASE_ONLY
        lore:
          - "&7Increases movement speed for &e30 seconds"
          - "&7Chase down hiders faster!"
          - ""
          - "&e&lPrice: &f200 coins"
          - "&c&lCooldown: &f5 seconds"

      # Reach Extender
      reach-extender:
        material: STICK
        display-name: "&6&lReach Extender"
        slot: 13
        price: 250
        effect-type: REACH
        effect-duration: 30
        effect-intensity: 1.5
        cooldown: 15
        max-purchases: -1
        usage-restriction: SEEK_PHASE_ONLY
        lore:
          - "&7Extends attack range by &e50% &7for &e30 seconds"
          - "&7Hit disguised blocks from farther away"
          - ""
          - "&e&lPrice: &f250 coins"
          - "&c&lCooldown: &f15 seconds"

# Effect Configuration
effects:
  # Overlap policy per effect type
  overlap-policies:
    vision: REPLACE
    glow: REPLACE
    speed: REPLACE
    reach: REPLACE

  # Maximum total duration when using EXTEND policy
  max-extended-duration: 120

  # Performance settings
  performance:
    glow-update-interval: 10      # ticks (0.5s)
    glow-max-distance: 50         # blocks
    glow-particle-count: 5
    cleanup-task-interval: 20     # ticks (1s)

  # Visual feedback
  feedback:
    enable-particles: true
    enable-sounds: true
    show-action-bar: true
    show-boss-bar: false

# Messages
messages:
  effects:
    applied: "&aEffect activated: &e{effect} &7({duration}s)"
    expired: "&cEffect expired: &e{effect}"
    replaced: "&eEffect replaced: &6{old} &7→ &6{new}"
    extended: "&aEffect extended: &e{effect} &7(+{added}s)"
    already-active: "&cYou already have this effect active!"
    cooldown-remaining: "&cCooldown remaining: &e{time}s"
    purchase-limit: "&cPurchase limit reached: &e{current}/{max}"
    insufficient-funds: "&cInsufficient funds! Need &e{price} &7(have &e{balance}&7)"
    inventory-full: "&cYour inventory is full!"
    wrong-phase: "&cCan only use during seek phase!"
    not-seeker: "&cOnly seekers can use this item!"
```

### 6.2 Runtime Memory Storage

#### EffectManager Storage

```kotlin
/**
 * Singleton manager for all active effects.
 */
object EffectManager {
    private val storage = EffectStorage()

    // Memory footprint estimate:
    // - 30 players × 4 effects × 150 bytes = 18 KB (best case)
    // - Worst case with all seekers using all items: ~50 KB
    // - Well within 500 KB/player budget
}
```

#### PurchaseManager Storage

```kotlin
/**
 * Singleton manager for purchase records.
 */
object PurchaseManager {
    private val storage = PurchaseStorage()

    // Memory footprint estimate:
    // - ItemPurchaseRecord: ~120 bytes
    // - 30 players × 10 purchases/game = 36 KB per game
    // - Cleared at game end
}
```

### 6.3 Cleanup Policies

#### Automatic Cleanup Triggers

| Trigger | Action | Timing |
|---------|--------|--------|
| Effect expires | Remove from storage, cancel task | Immediate (task callback) |
| Player disconnects | Remove all player's effects | PlayerQuitEvent |
| Game ends | Clear all effects and purchases | Game.end() |
| Player captured | Keep effects active (spectate mode) | PlayerCapturedEvent |
| Periodic cleanup | Remove orphaned/expired entries | Every 60 seconds |

#### Manual Cleanup Methods

```kotlin
// Clear specific player
EffectManager.removeAllEffects(playerId: UUID)

// Clear entire game
game.players.keys.forEach { uuid ->
    EffectManager.removeAllEffects(uuid)
}

// Force cleanup of expired (safety check)
EffectManager.cleanupExpired()

// Memory pressure handler
if (EffectStorage.estimateMemoryUsage() > THRESHOLD) {
    EffectManager.cleanupExpired()
    PurchaseStorage.clearOldRecords(olderThan = 30.minutes)
}
```

---

## 7. Memory Management

### 7.1 Memory Budget

**Per-Player Budget**: 500 KB
**Component Allocation**:

| Component | Memory Usage | Calculation |
|-----------|--------------|-------------|
| ActiveEffect (×4 max) | ~600 bytes | 150 bytes × 4 effects |
| ItemPurchaseRecord (×10 avg) | ~1.2 KB | 120 bytes × 10 purchases |
| ShopItem references | ~200 bytes | Shared instances (minimal) |
| GUI inventory data | ~500 bytes | Temporary, cleared on close |
| **Total per player** | **~2.5 KB** | **0.5% of budget** |

**Server Total (30 players)**:
- 30 players × 2.5 KB = **75 KB total**
- Negligible impact on server heap

### 7.2 Memory Optimization Strategies

#### Object Pooling (Not needed)
- Effect and Purchase objects are lightweight (<200 bytes each)
- JVM garbage collection handles these efficiently
- Pooling would add complexity without benefit

#### Lazy Initialization
```kotlin
// Config data loaded once on plugin enable
private val shopConfig: ShopConfig by lazy {
    ShopConfig.load(plugin)
}

// Effects storage created only when first effect applied
private val effectStorage: EffectStorage by lazy {
    EffectStorage()
}
```

#### Weak References (Not recommended)
- Effects must remain strongly reachable until explicitly removed
- Weak references could cause premature GC, breaking active effects

#### Cleanup Scheduling
```kotlin
/**
 * Scheduled task to remove expired effects and old purchase records.
 */
class CleanupTask(private val plugin: Plugin) : Runnable {
    override fun run() {
        // Remove expired effects
        val expired = EffectManager.cleanupExpired()
        if (expired.isNotEmpty()) {
            plugin.logger.info("Cleaned up ${expired.size} expired effects")
        }

        // Remove purchase records older than current game
        val currentGame = GameManager.getActiveGame()
        if (currentGame != null) {
            PurchaseManager.clearOldGames(exceptGameId = currentGame.id)
        }

        // Log memory usage
        val memUsage = EffectStorage.estimateMemoryUsage()
        if (memUsage > 100_000) { // 100 KB threshold
            plugin.logger.warning("High memory usage in effects: ${memUsage / 1024} KB")
        }
    }
}

// Schedule every 60 seconds
Bukkit.getScheduler().runTaskTimer(plugin, CleanupTask(plugin), 0L, 1200L)
```

---

## 8. Data Flow Diagrams

### 8.1 Purchase Flow

```
Player                ShopGUI              ShopManager         EffectManager         VaultAPI
  │                      │                      │                    │                   │
  │──/hs shop───────────>│                      │                    │                   │
  │                      │                      │                    │                   │
  │                      │──getItemsForRole────>│                    │                   │
  │                      │<─────items───────────┤                    │                   │
  │                      │                      │                    │                   │
  │<─────GUI opened──────┤                      │                    │                   │
  │                      │                      │                    │                   │
  │──click item─────────>│                      │                    │                   │
  │                      │                      │                    │                   │
  │                      │──validatePurchase───>│                    │                   │
  │                      │                      │───checkBalance────────────────────────>│
  │                      │                      │<──balance─────────────────────────────┤
  │                      │<─────valid/error─────┤                    │                   │
  │                      │                      │                    │                   │
  │                      │──processPurchase────>│                    │                   │
  │                      │                      │───withdrawFunds───────────────────────>│
  │                      │                      │<──success─────────────────────────────┤
  │                      │                      │                    │                   │
  │                      │                      │──giveItem to Player                    │
  │<─────item in inv─────┴──────────────────────┤                    │                   │
  │                      │                      │                    │                   │
  │──right-click item───────────────────────────────────────────────>│                   │
  │                      │                      │                    │                   │
  │                      │                      │                    │──apply effect     │
  │<─────effect active───────────────────────────────────────────────┤                   │
  │                      │                      │                    │                   │
  │                      │                      │<──schedule removal──┤                   │
  │                      │                      │                    │                   │
 [30s later]             │                      │                    │                   │
  │                      │                      │                    │──auto remove      │
  │<─────effect expired──────────────────────────────────────────────┤                   │
```

### 8.2 Effect Application Flow

```
ItemUseEvent           ItemHandler           EffectManager         EffectStorage        MinecraftAPI
     │                      │                      │                      │                   │
     │──item used──────────>│                      │                      │                   │
     │                      │                      │                      │                   │
     │                      │──validate player────>│                      │                   │
     │                      │<─────valid───────────┤                      │                   │
     │                      │                      │                      │                   │
     │                      │──check overlap──────>│                      │                   │
     │                      │                      │──get existing────────>│                  │
     │                      │                      │<─────effect──────────┤                   │
     │                      │<─────policy──────────┤                      │                   │
     │                      │                      │                      │                   │
     │                [apply policy logic]         │                      │                   │
     │                      │                      │                      │                   │
     │                      │──apply effect───────>│                      │                   │
     │                      │                      │──create ActiveEffect │                   │
     │                      │                      │                      │                   │
     │                      │                      │──apply Minecraft effect──────────────────>│
     │                      │                      │                      │<──PotionEffect────┤
     │                      │                      │                      │                   │
     │                      │                      │──store effect────────>│                  │
     │                      │                      │                      │                   │
     │                      │                      │──schedule removal    │                   │
     │<─────feedback────────┴──────────────────────┤                      │                   │
     │  (sound, particles,                         │                      │                   │
     │   action bar message)                       │                      │                   │
     │                                              │                      │                   │
   [duration elapsed]                               │                      │                   │
     │                                              │──task callback       │                   │
     │                                              │                      │                   │
     │                                              │──remove effect───────>│                  │
     │                                              │                      │                   │
     │                                              │──remove Minecraft eff─────────────────────>│
     │<─────effect expired notification─────────────┤                      │                   │
```

### 8.3 Game Lifecycle Integration

```
Game Start          Preparation Phase        Seek Phase          Game End
     │                     │                      │                  │
     │                     │                      │                  │
     │──init game─────────>│                      │                  │
     │                     │                      │                  │
     │                     │──seekers wait        │                  │
     │                     │  (no shop access)    │                  │
     │                     │                      │                  │
     │                     │──phase transition───>│                  │
     │                     │                      │                  │
     │                     │                      │──enable shop     │
     │                     │                      │  for seekers     │
     │                     │                      │                  │
     │                     │                      │──allow purchases │
     │                     │                      │                  │
     │                     │                      │──effects active  │
     │                     │                      │                  │
     │                     │                      │                  │
     │                     │                      │──end game───────>│
     │                     │                      │                  │
     │                     │                      │                  │──remove all effects
     │                     │                      │                  │
     │                     │                      │                  │──clear purchases
     │                     │                      │                  │
     │                     │                      │                  │──cancel tasks
     │                     │                      │                  │
     │                     │                      │                  │──show results
```

---

## Summary

This data model provides:

1. **Clear Entity Definitions**: All entities have well-defined attributes, constraints, and lifecycles
2. **Type-Safe Kotlin Structures**: Strongly-typed data classes with validation built-in
3. **Efficient Relationships**: O(1) lookups using HashMap-based storage
4. **Comprehensive Validation**: Multi-layer validation (config, purchase, effect application)
5. **Robust State Management**: Clear state machines for effects and purchases
6. **Memory-Efficient Storage**: <3 KB per player, <100 KB total for 30 players
7. **Clean Integration**: Extends existing shop system without breaking changes
8. **Production-Ready**: Includes error handling, cleanup policies, and monitoring

**Next Steps**:
1. Implement `EffectManager` class following the data model
2. Extend `ShopItem` and `ShopCategory` with new fields
3. Create YAML parser for seeker items configuration
4. Implement validation utilities
5. Add cleanup tasks and memory monitoring
6. Write unit tests for all data structures

**File Location**: `/Users/masafumi_t/Develop/hacklab/kikaku2/EasyHideAndSeek/specs/003-seeker-shop/data-model.md`
