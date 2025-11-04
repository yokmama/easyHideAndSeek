# Research: Shop Items Review and Rebalance

**Feature**: Shop Items Review and Rebalance
**Date**: 2025-11-04
**Status**: Complete

## Purpose

Research key design decisions for implementing the shop items rebalance, focusing on:
1. Camouflage-based pricing model implementation
2. Item variety and effect system extensions
3. Purchase restriction mechanisms
4. Configuration schema design
5. Performance optimization strategies

## Key Decisions

### 1. Camouflage Tier System

**Decision**: Implement 3-tier pricing system based on block visibility/camouflage effectiveness

**Rationale**:
- Aligns with existing Material enum in Bukkit API (no custom block types needed)
- Simple mapping from Material → Camouflage Tier → Price
- Easily configurable in YAML without code changes
- Allows server admins to adjust pricing based on their specific map environments

**Implementation Approach**:
```kotlin
enum class CamouflageTier(val priceMultiplier: Double) {
    HIGH_VISIBILITY(0.0),    // Free (melons, gold blocks)
    MEDIUM_VISIBILITY(0.5),  // 50 coins base
    LOW_VISIBILITY(1.0)      // 100 coins base
}

data class DisguiseBlockConfig(
    val material: Material,
    val tier: CamouflageTier,
    val customPrice: Int? = null  // Override tier pricing if needed
)
```

**Alternatives Considered**:
- **Dynamic visibility calculation**: Rejected - too complex, map-dependent, and computationally expensive
- **Player voting system**: Rejected - requires data collection period and may not reflect actual concealment
- **Flat pricing with "premium" blocks**: Rejected - doesn't create strategic risk-reward decisions

**Configuration Example**:
```yaml
disguise-blocks:
  high-visibility:
    price: 0
    blocks:
      - MELON
      - PUMPKIN
      - GOLD_BLOCK
      - DIAMOND_BLOCK
      - SPONGE
  medium-visibility:
    price: 50
    blocks:
      - OAK_LOG
      - BRICK
      - SANDSTONE
  low-visibility:
    price: 100
    blocks:
      - STONE
      - COBBLESTONE
      - DIRT
      - GRASS_BLOCK
```

---

### 2. New Item Effect System

**Decision**: Extend existing EffectType enum and create modular item handlers

**Rationale**:
- Leverages existing EffectManager infrastructure
- Each item type gets dedicated handler class (Single Responsibility Principle)
- Easy to add/remove items via configuration without code deployment
- Handlers can be tested independently

**Effect Types to Add**:
```kotlin
enum class EffectType {
    // Existing
    SPEED_BOOST,
    VISION_NIGHT,
    GLOW_DETECTOR,
    REACH_EXTENDER,

    // New for this feature
    SHADOW_SPRINT,      // Hider speed boost
    DECOY_BLOCK,        // Fake disguise block
    SECOND_CHANCE,      // Respawn on capture
    TRACKER_COMPASS,    // Point to nearest hider
    AREA_SCAN,          // Reveal hider count in radius
    EAGLE_EYE,          // Highlight disguised blocks
    TRACKER_INSIGHT,    // Show last-moved times
    CAPTURE_NET         // 2x capture reward
}
```

**Handler Pattern**:
```kotlin
interface ItemEffectHandler {
    fun canApply(player: Player, game: Game): Boolean
    fun apply(player: Player, game: Game, config: ItemConfig): Result<Unit>
    fun onExpire(player: Player, game: Game)
    fun getDisplayLore(config: ItemConfig): List<String>
}
```

**Alternatives Considered**:
- **Scriptable items (Lua/JS)**: Rejected - adds complexity, security concerns, and dependency overhead
- **Command-based items**: Rejected - less type-safe, harder to test, poor error handling
- **Single monolithic handler**: Rejected - violates SRP, difficult to maintain and test

---

### 3. Purchase Restriction System

**Decision**: Implement hybrid restriction model supporting per-player, per-team, and global limits

**Rationale**:
- Different items need different restriction types (Escape Pass = per-player, Tracker Compass = per-team)
- Matches competitive game balance patterns (cooldowns prevent spam, limits create scarcity)
- Can be configured per-item without affecting others

**Restriction Types**:
```kotlin
data class PurchaseRestriction(
    val type: RestrictionType,
    val maxPurchases: Int?,
    val cooldownSeconds: Int?,
    val resetOn: ResetTrigger
)

enum class RestrictionType {
    PER_PLAYER,     // Individual player limit
    PER_TEAM,       // Team-wide limit (all Hiders or all Seekers)
    GLOBAL          // Game-wide limit (across both teams)
}

enum class ResetTrigger {
    NEVER,          // Persists entire game
    ON_USE,         // Cooldown starts after use
    ON_DEATH,       // Resets when player captured/dies
    ON_PHASE_CHANGE // Resets when game phase changes
}
```

**Storage Strategy**:
- In-memory tracking during active game (HashMap<UUID, PlayerPurchases>)
- Cleared on game end
- No persistent storage needed (purchases don't carry over between games)

**Alternatives Considered**:
- **Database persistence**: Rejected - unnecessary overhead, purchases are per-game only
- **Simple cooldown-only model**: Rejected - doesn't prevent dominant strategies (buy everything)
- **Credit system (purchase tokens)**: Rejected - adds complexity without benefit for this use case

---

### 4. Configuration Schema Design

**Decision**: Create dedicated shop.yml with hierarchical structure for items, categories, and restrictions

**Rationale**:
- Separates shop configuration from game mechanics (config.yml)
- Allows hot-reloading of shop without restarting server
- Human-readable format for server admins
- Supports multiple shop configurations per arena (future extensibility)

**Schema Structure**:
```yaml
shop:
  version: 2

  categories:
    hider-disguise:
      name: "Disguise Blocks"
      icon: GRASS_BLOCK
      slot: 0

    hider-utility:
      name: "Utility Items"
      icon: ENDER_PEARL
      slot: 1

    seeker-tracking:
      name: "Tracking Tools"
      icon: COMPASS
      slot: 0

    seeker-combat:
      name: "Combat Items"
      icon: IRON_SWORD
      slot: 1

  items:
    shadow-sprint:
      category: hider-utility
      material: FEATHER
      display-name: "&eShadow Sprint"
      lore:
        - "&720 seconds of speed boost"
        - "&7Can be used while visible"
      price: 350
      effect:
        type: SHADOW_SPRINT
        duration: 20
        intensity: 1.5
      restrictions:
        type: PER_PLAYER
        max-purchases: 2
        cooldown: 60
      role-filter: HIDER
```

**Validation Strategy**:
- Schema validation on load (check required fields, valid enum values)
- Fail-fast with descriptive error messages
- Fallback to default configuration if parse fails

**Alternatives Considered**:
- **JSON format**: Rejected - less human-readable, no comments support
- **Database storage**: Rejected - overkill for static configuration
- **Code-based configuration**: Rejected - requires recompilation for item changes

---

### 5. Performance Optimization

**Decision**: Implement caching layer and async economy operations

**Rationale**:
- 30 concurrent players opening shops could create TPS lag
- Economy operations (Vault) may involve external plugins/databases
- GUI rendering can be pre-computed for static content

**Optimization Strategies**:

**1. Shop GUI Caching**:
```kotlin
class ShopGUICache {
    private val cache = ConcurrentHashMap<String, CachedGUI>()

    fun getOrCreate(role: String, balance: Int): Inventory {
        val cacheKey = "$role:$balance"
        return cache.getOrPut(cacheKey) {
            CachedGUI(buildShopGUI(role, balance), System.currentTimeMillis())
        }.inventory
    }

    fun invalidate(role: String? = null) {
        if (role == null) cache.clear()
        else cache.keys.removeIf { it.startsWith("$role:") }
    }
}
```

**2. Async Economy Operations**:
```kotlin
suspend fun purchaseItem(player: Player, item: ShopItem): Result<Unit> = withContext(Dispatchers.IO) {
    val economy = plugin.economy ?: return@withContext Result.failure(Exception("Economy unavailable"))

    if (!economy.has(player, item.price.toDouble())) {
        return@withContext Result.failure(InsufficientFundsException(item.price))
    }

    economy.withdrawPlayer(player, item.price.toDouble())
    Result.success(Unit)
}
```

**3. Purchase Limit Caching**:
- Store restrictions in memory (loaded once at startup)
- Track active purchases in HashMap (O(1) lookup)
- Batch clear on game end (avoid per-player cleanup)

**Performance Targets**:
- GUI open: < 50ms for cached, < 500ms for first load
- Purchase transaction: < 100ms including economy operation
- No measurable TPS impact during normal shop usage

**Alternatives Considered**:
- **No caching**: Rejected - would cause TPS drops with many concurrent shops
- **Full page caching**: Rejected - balance changes require per-player rendering
- **Synchronous economy**: Rejected - blocks main thread, causes lag spikes

---

### 6. Item Balance Analytics

**Decision**: Implement purchase statistics logging for post-release balance tuning

**Rationale**:
- Initial pricing is educated guess, needs real-world data
- Balance issues may only emerge with player population
- A/B testing requires statistical comparison

**Metrics to Track**:
```kotlin
data class ItemStatistics(
    val itemId: String,
    val totalPurchases: Int,
    val purchasesByPhase: Map<GamePhase, Int>,
    val averagePurchaseTime: Double,  // Seconds into game
    val usageRate: Double,            // % of purchases actually used
    val winRateWithItem: Double,      // Win correlation
    val averagePlayerBalance: Double  // Balance when purchased
)
```

**Logging Strategy**:
- Append to CSV file per game session
- Minimal performance impact (async write)
- Easy to analyze with spreadsheet tools
- Can be disabled via config

**Analytics Output Format**:
```csv
timestamp,game_id,player_uuid,item_id,price,phase,balance_before,balance_after,used,game_result
2025-11-04T10:15:30,game-123,uuid-456,shadow-sprint,350,SEEK,1200,850,true,HIDER_WIN
```

**Alternatives Considered**:
- **No analytics**: Rejected - would require guess-work for balance adjustments
- **Real-time dashboard**: Rejected - overkill, adds complexity
- **Database storage**: Rejected - adds dependency, harder to export/analyze

---

## Open Questions (Resolved)

### Q1: How to handle mid-game config reloads?
**Resolution**: Defer changes until next game. Active games use snapshot of config from game start. This prevents exploits and maintains consistency.

### Q2: Should item prices scale based on player count?
**Resolution**: No automatic scaling. Allow server admins to create arena-specific shop configs if needed. Keeps system simple and predictable.

### Q3: What happens to purchased items when player disconnects?
**Resolution**: Items are forfeit (no refund). Player state is cleared from game. Simple rule that prevents exploits and edge case complexity.

### Q4: Should Hiders see Seeker items in shop (grayed out)?
**Resolution**: No. Only show role-appropriate items. Cleaner UI, no confusion, simpler implementation.

## Implementation Priority

Based on spec priorities (P1, P2, P3), implement in this order:

1. **Phase 1 (P1 - Core Balance)**:
   - Camouflage tier system
   - Price configuration loading
   - Updated shop GUI with pricing

2. **Phase 2 (P1 - UX)**:
   - Enhanced tooltips (descriptions, durations, limits)
   - Purchase feedback (success, failure, cooldown messages)
   - Active effect indicators

3. **Phase 3 (P2 - Item Variety)**:
   - New Hider items (Shadow Sprint, Decoy Block, Second Chance)
   - New Seeker items (Eagle Eye, Tracker Insight, Capture Net)
   - Item effect handlers

4. **Phase 4 (P3 - Restrictions)**:
   - Purchase limits (per-player, per-team)
   - Cooldown system
   - Usage restrictions

5. **Phase 5 (Analytics & Tuning)**:
   - Statistics logging
   - Balance analysis tools
   - Config hot-reload

## Technical Risks & Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Vault economy integration issues | Medium | High | Test with common economy plugins (EssentialsX, CMI), provide fallback modes |
| Performance degradation with 30 players | Low | High | Implement caching, async operations, load testing |
| Config parsing errors crash server | Low | Medium | Comprehensive validation, fallback to defaults, clear error messages |
| Item effect conflicts/bugs | Medium | Medium | Extensive unit testing, effect isolation, rollback capability |
| Balance issues post-release | High | Low | Analytics system, easy config changes, no code deployment needed |

## Dependencies

**External**:
- Paper API 1.21.5 (stable)
- Vault API 1.7 (stable)
- Economy plugin (EssentialsX, CMI, or similar)

**Internal**:
- Existing DisguiseManager
- Existing EffectManager
- Existing ShopManager
- Existing GameManager

**No new external dependencies required**.

## Success Criteria Mapping

Research decisions support spec success criteria:

- **SC-001-003 (Purchase distribution)**: Analytics system tracks this
- **SC-004 (Comprehension)**: Enhanced tooltips address this
- **SC-005-006 (Performance)**: Caching and async operations ensure this
- **SC-007 (Item variety usage)**: New items and restrictions encourage this
- **SC-008 (Dynamic economy)**: Tiered pricing supports this
- **SC-009 (Exploits)**: Validation and restrictions prevent this
- **SC-010 (Player satisfaction)**: Overall balance improvements target this

## Next Steps

Proceed to Phase 1:
1. Generate data-model.md (entity definitions)
2. Generate contracts/ (config schema)
3. Generate quickstart.md (developer guide)
4. Update agent context with new technologies/patterns
