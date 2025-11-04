# Data Model: Shop Items Review and Rebalance

**Feature**: Shop Items Review and Rebalance
**Date**: 2025-11-04
**Source**: Derived from [spec.md](./spec.md) Key Entities section

## Overview

This document defines the data structures for the shop items rebalance feature. All entities are designed for in-memory operation during active games with YAML-based configuration persistence.

## Core Entities

### ShopItem (Extended)

**Purpose**: Represents a purchasable item in the shop

**Fields**:
```kotlin
data class ShopItem(
    // Existing fields
    val id: String,                          // Unique identifier (e.g., "shadow-sprint")
    val material: Material,                  // Display icon in GUI
    val displayName: String,                 // Formatted display name with color codes
    val lore: List<String>,                  // Tooltip lines
    val slot: Int,                           // GUI slot position
    val price: Int,                          // Cost in coins
    val action: ShopAction,                  // What happens on purchase/use

    // Effect-related (existing)
    val effectType: EffectType?,             // Type of effect to apply
    val effectDuration: Int?,                // Duration in seconds
    val effectIntensity: Double?,            // Effect strength multiplier

    // Item-specific (existing)
    val tauntType: String?,                  // "SNOWBALL" or "FIREWORK"
    val tauntBonus: Int?,                    // Bonus points for taunt

    // Restrictions (existing)
    val roleFilter: String?,                 // "SEEKER" or "HIDER"
    val cooldown: Int?,                      // Cooldown in seconds
    val maxPurchases: Int?,                  // Max per-player purchases
    val usageRestriction: UsageRestriction,  // When item can be used

    // NEW: Camouflage-related
    val camouflageTier: CamouflageTier?,     // Pricing tier for disguise blocks
    val camouflageMultiplier: Double?,       // Custom price multiplier (overrides tier)

    // NEW: Advanced restrictions
    val teamMaxPurchases: Int?,              // Max per-team purchases
    val globalMaxPurchases: Int?,            // Max across all players
    val resetTrigger: ResetTrigger?,         // When cooldown/limit resets

    // NEW: Analytics
    val trackUsage: Boolean = true           // Whether to log purchase stats
)
```

**Validation Rules**:
- `id` must be unique across all items
- `price` must be >= 0
- `slot` must be 0-53 (valid inventory slots)
- `effectDuration` and `effectIntensity` required if `effectType` is set
- At least one of `maxPurchases`, `teamMaxPurchases`, `globalMaxPurchases` should be set for limited items
- `camouflageTier` only valid for `ShopAction.Disguise`

**State Transitions**:
- Immutable configuration (loaded from YAML)
- No state changes during runtime
- New instances created on config reload

---

### CamouflageTier (New)

**Purpose**: Categorize disguise blocks by visibility/concealment effectiveness

**Definition**:
```kotlin
enum class CamouflageTier(
    val tierName: String,
    val basePrice: Int,
    val description: String
) {
    HIGH_VISIBILITY(
        tierName = "High Visibility",
        basePrice = 0,
        description = "Easily spotted - Free to use"
    ),
    MEDIUM_VISIBILITY(
        tierName = "Medium Visibility",
        basePrice = 50,
        description = "Somewhat noticeable"
    ),
    LOW_VISIBILITY(
        tierName = "Low Visibility",
        basePrice = 100,
        description = "Blends into environment"
    )
}
```

**Usage**:
- Mapped from Material → CamouflageTier in configuration
- Price calculation: `finalPrice = tier.basePrice * (item.camouflageMultiplier ?: 1.0)`
- Determines display order in shop GUI (free items first)

---

### PlayerPurchaseRecord (Extended)

**Purpose**: Track items purchased by a player in current game

**Fields**:
```kotlin
data class PlayerPurchaseRecord(
    val playerId: UUID,                      // Player UUID
    val itemId: String,                      // Purchased item ID
    val purchaseTime: Long,                  // System.currentTimeMillis()
    val price: Int,                          // Price paid
    val quantity: Int,                       // Number purchased (for stackable items)

    // NEW: Usage tracking
    var activated: Boolean = false,          // Whether item has been used
    var activationTime: Long? = null,        // When item was used
    var expired: Boolean = false,            // Whether effect has expired

    // NEW: Analytics
    val gamePhase: GamePhase,                // Phase when purchased (PREP, SEEK, etc.)
    val playerBalance: Int,                  // Player balance before purchase
    val playerRole: PlayerRole               // HIDER or SEEKER
)
```

**Relationships**:
- Many `PlayerPurchaseRecord` → One `Player` (via `playerId`)
- Many `PlayerPurchaseRecord` → One `ShopItem` (via `itemId`)
- Stored in `PurchaseStorage` during active game

**Lifecycle**:
1. Created on successful purchase
2. Updated when item is activated (`activated = true`, `activationTime` set)
3. Updated when effect expires (`expired = true`)
4. Cleared when game ends
5. Optionally written to analytics log before clearing

---

### PurchaseRestriction (New)

**Purpose**: Define purchase and usage limits for shop items

**Fields**:
```kotlin
data class PurchaseRestriction(
    val restrictionType: RestrictionType,    // Scope of restriction
    val maxQuantity: Int,                    // Maximum purchases allowed
    val cooldownSeconds: Int?,               // Cooldown between purchases/uses
    val resetTrigger: ResetTrigger           // When limit/cooldown resets
)

enum class RestrictionType {
    PER_PLAYER,      // Individual player limit
    PER_TEAM,        // Team-wide limit (all Hiders or all Seekers)
    GLOBAL           // Game-wide limit (across both teams)
}

enum class ResetTrigger {
    NEVER,           // Persists entire game
    ON_USE,          // Cooldown starts after activation
    ON_DEATH,        // Resets when player is captured/dies
    ON_PHASE_CHANGE, // Resets when game phase changes (PREP→SEEK)
    ON_PURCHASE      // Cooldown starts immediately after purchase
}
```

**Usage Example**:
```kotlin
// Escape Pass: 1 per player per game, no cooldown
PurchaseRestriction(
    restrictionType = RestrictionType.PER_PLAYER,
    maxQuantity = 1,
    cooldownSeconds = null,
    resetTrigger = ResetTrigger.NEVER
)

// Speed Boost: 2 per player, 60s cooldown after use
PurchaseRestriction(
    restrictionType = RestrictionType.PER_PLAYER,
    maxQuantity = 2,
    cooldownSeconds = 60,
    resetTrigger = ResetTrigger.ON_USE
)

// Tracker Compass: 3 per team, no individual cooldown
PurchaseRestriction(
    restrictionType = RestrictionType.PER_TEAM,
    maxQuantity = 3,
    cooldownSeconds = null,
    resetTrigger = ResetTrigger.NEVER
)
```

---

### ItemEffect (Existing - Extended)

**Purpose**: Represent an active effect applied to a player

**Fields**:
```kotlin
data class ItemEffect(
    val effectId: UUID,                      // Unique effect instance ID
    val playerId: UUID,                      // Player receiving effect
    val itemId: String,                      // Item that granted effect
    val effectType: EffectType,              // Type of effect
    val startTime: Long,                     // When effect was applied
    val duration: Int,                       // Duration in seconds
    val intensity: Double,                   // Effect magnitude

    // NEW: Metadata
    val metadata: Map<String, Any> = emptyMap()  // Effect-specific data
)
```

**Metadata Examples**:
```kotlin
// Tracker Compass: Store target hider UUID
metadata = mapOf("targetHiderId" to UUID)

// Decoy Block: Store fake block location
metadata = mapOf("decoyLocation" to Location)

// Area Scan: Store scan results
metadata = mapOf(
    "hiderCount" to Int,
    "directions" to List<String>  // ["NORTH", "WEST"]
)
```

---

### ShopConfiguration (Extended)

**Purpose**: Container for all shop settings loaded from YAML

**Fields**:
```kotlin
data class ShopConfiguration(
    val version: Int,                        // Config schema version
    val categories: Map<String, ShopCategory>,  // Item categories
    val items: Map<String, ShopItem>,        // All available items

    // NEW: Camouflage config
    val disguiseBlocks: Map<CamouflageTier, List<Material>>,
    val camouflagePricing: Map<CamouflageTier, Int>,

    // NEW: Global settings
    val enableStatistics: Boolean = true,
    val statisticsFile: String = "shop-stats.csv",
    val cacheGUI: Boolean = true,
    val cacheExpiry: Long = 60000,           // Cache TTL in ms

    // NEW: Arena-specific overrides
    val arenaOverrides: Map<String, ArenaShopConfig> = emptyMap()
)

data class ArenaShopConfig(
    val arenaId: String,
    val itemPriceMultiplier: Double = 1.0,   // Scale all prices
    val disabledItems: List<String> = emptyList(),
    val customItems: Map<String, ShopItem> = emptyMap()
)
```

**Loading**:
1. Read from `shop.yml`
2. Validate schema version
3. Parse categories and items
4. Build camouflage tier mappings
5. Validate item references
6. Cache in memory

**Reloading**:
- Triggered by admin command `/hs admin reload`
- Only affects new games (active games use snapshot)
- Invalid config falls back to previous valid config

---

### ItemStatistics (New)

**Purpose**: Aggregate purchase and usage metrics for balance analysis

**Fields**:
```kotlin
data class ItemStatistics(
    val itemId: String,
    val gameId: String,
    val timestamp: Long,

    // Purchase metrics
    val totalPurchases: Int,
    val purchasesByRole: Map<PlayerRole, Int>,
    val purchasesByPhase: Map<GamePhase, Int>,
    val averagePurchaseTime: Double,         // Seconds into game
    val medianPrice: Int,

    // Usage metrics
    val timesActivated: Int,
    val usageRate: Double,                   // activated / totalPurchases
    val averageEffectDuration: Double,

    // Outcome metrics
    val winRateWithItem: Double,             // % of games won with this item
    val averagePlayerBalanceOnPurchase: Double,

    // Context
    val totalPlayers: Int,
    val totalGames: Int
)
```

**Calculation**:
- Computed at game end from `PlayerPurchaseRecord` list
- Written to CSV file asynchronously
- Aggregation done offline via spreadsheet or scripts

---

## Entity Relationships

```
ShopConfiguration
├── categories: Map<String, ShopCategory>
├── items: Map<String, ShopItem>
│   ├── camouflageTier → CamouflageTier
│   ├── action → ShopAction
│   ├── effectType → EffectType
│   └── restrictions → PurchaseRestriction
└── disguiseBlocks: Map<CamouflageTier, List<Material>>

Game (Active)
├── players: List<Player>
│   └── purchaseRecords: List<PlayerPurchaseRecord>
│       └── itemId → ShopItem
└── activeEffects: List<ItemEffect>
    ├── playerId → Player
    └── itemId → ShopItem

PurchaseStorage (Per-Game)
├── playerPurchases: Map<UUID, List<PlayerPurchaseRecord>>
├── teamPurchases: Map<PlayerRole, Map<String, Int>>
└── globalPurchases: Map<String, Int>

Statistics (Post-Game)
└── records: List<ItemStatistics>
    └── itemId → ShopItem (historical reference)
```

---

## Data Flow

### Purchase Flow
```
1. Player opens shop GUI
   ↓
2. ShopManager.getShopItems(player.role) → List<ShopItem>
   ↓
3. Player clicks item
   ↓
4. PurchaseValidator.canPurchase(player, item) → Result
   ├─ Check balance (Vault)
   ├─ Check purchase limits (PurchaseStorage)
   ├─ Check cooldowns (PurchaseStorage)
   └─ Check role filter (ShopItem.roleFilter)
   ↓
5. If valid: Economy.withdraw(player, item.price)
   ↓
6. PurchaseStorage.recordPurchase(PlayerPurchaseRecord)
   ↓
7. Item handler applies effect/action
   ↓
8. GUI updates (balance, purchased items)
```

### Effect Activation Flow
```
1. Player uses item (right-click, auto-trigger, etc.)
   ↓
2. ItemHandler.canActivate(player, item) → Boolean
   ├─ Check game phase restrictions
   ├─ Check existing effects (conflicts)
   └─ Check activation conditions
   ↓
3. If valid: ItemHandler.activate(player, item)
   ↓
4. EffectManager.applyEffect(ItemEffect)
   ↓
5. Update PlayerPurchaseRecord.activated = true
   ↓
6. Schedule expiration (if duration-based)
   ↓
7. On expiry: ItemHandler.onExpire(player, effect)
   ↓
8. Update PlayerPurchaseRecord.expired = true
```

### Analytics Flow
```
1. Game ends
   ↓
2. Collect all PlayerPurchaseRecords for game
   ↓
3. Aggregate by item ID → ItemStatistics
   ↓
4. Async write to CSV
   ↓
5. Clear in-memory records
```

---

## Storage Strategy

### Configuration (Persistent)
- **Format**: YAML
- **Location**: `plugins/EasyHideAndSeek/shop.yml`
- **Reload**: On admin command, affects new games only
- **Validation**: On load with fallback to defaults

### Game State (In-Memory)
- **Storage**: HashMap<UUID, GameState>
- **Lifecycle**: Created on game start, cleared on game end
- **Access**: O(1) lookup by player UUID
- **Concurrency**: ConcurrentHashMap for thread safety

### Analytics (Append-Only)
- **Format**: CSV
- **Location**: `plugins/EasyHideAndSeek/logs/shop-stats.csv`
- **Write**: Asynchronous batch write on game end
- **Rotation**: Daily rotation via filename suffix (shop-stats-2025-11-04.csv)

---

## Validation Rules

### On Configuration Load
1. All item IDs must be unique
2. All referenced categories must exist
3. Material enums must be valid
4. Price must be >= 0
5. Effect fields consistent with action type
6. Slot numbers within valid range (0-53)
7. Camouflage tier must be valid for disguise items
8. Cooldown and max purchases must be positive if set

### On Purchase
1. Player must have sufficient balance
2. Player must not exceed purchase limits
3. Item must not be on cooldown
4. Player role must match item's role filter
5. Game must be in valid phase for purchase
6. Item must be enabled in current arena

### On Activation
1. Effect must not conflict with existing effects
2. Game phase must allow activation
3. Player must be alive (not captured/spectating)
4. Item-specific conditions (e.g., target availability for Tracker Compass)

---

## Migration Strategy

**From Current System**:
- Existing `ShopItem` class extended with new fields (backward compatible)
- Default values for new fields (no breaking changes)
- Old config format still loads (missing fields use defaults)
- Gradual migration: add new fields to config as features are implemented

**Versioning**:
```yaml
shop:
  version: 2  # Increment when schema changes

  # v2 additions:
  disguise-blocks: ...
  statistics: ...
```

**Compatibility**:
- Version 1 configs load with warnings
- Missing fields filled with sensible defaults
- Admin notified of deprecated fields

---

## Performance Considerations

### Memory Usage
- ~100 bytes per ShopItem (configuration)
- ~200 bytes per PlayerPurchaseRecord
- ~150 bytes per ItemEffect
- **Estimate**: 10 players × 5 purchases × 200 bytes = 10KB per game (negligible)

### Lookup Performance
- ShopItem lookup: O(1) via HashMap (id → item)
- Purchase limit check: O(1) via HashMap (player → purchases)
- Active effects: O(n) linear scan (typically < 10 effects)

### Caching Strategy
- GUI inventory cached per role-balance combination
- Cache invalidated on purchase (balance change)
- Cache expires after 60 seconds (configurable)
- Pre-compute lore and display names at config load

---

## Testing Considerations

### Unit Tests
- ShopItem validation logic
- CamouflageTier pricing calculation
- PurchaseRestriction enforcement
- ItemStatistics aggregation

### Integration Tests
- Config loading and validation
- Purchase flow end-to-end
- Effect application and expiration
- Analytics logging

### Test Data
```kotlin
// Example test fixtures
val testShopItem = ShopItem(
    id = "test-item",
    material = Material.FEATHER,
    displayName = "Test Item",
    lore = listOf("Test description"),
    slot = 0,
    price = 100,
    action = ShopAction.UseEffectItem,
    effectType = EffectType.SPEED_BOOST,
    effectDuration = 30,
    effectIntensity = 1.5,
    roleFilter = "HIDER",
    camouflageTier = null,
    maxPurchases = 2
)

val testPurchaseRecord = PlayerPurchaseRecord(
    playerId = UUID.randomUUID(),
    itemId = "test-item",
    purchaseTime = System.currentTimeMillis(),
    price = 100,
    quantity = 1,
    gamePhase = GamePhase.SEEK,
    playerBalance = 500,
    playerRole = PlayerRole.HIDER
)
```

---

## Next Steps

1. Generate configuration contracts (shop.yml schema)
2. Implement extended data classes
3. Write unit tests for new entities
4. Update database migrations (if applicable)
5. Document API changes in quickstart.md
