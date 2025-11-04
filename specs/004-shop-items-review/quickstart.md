# Quickstart: Shop Items Review and Rebalance

**Feature**: Shop Items Review and Rebalance
**Audience**: Developers implementing this feature
**Prerequisites**: Familiarity with Kotlin, Paper API, and existing codebase

## Overview

This guide helps you implement the shop items rebalance feature. You'll extend the existing shop system with:
1. Camouflage-based pricing for disguise blocks
2. New item types and effect handlers
3. Enhanced purchase restrictions
4. Usage analytics

**Estimated Effort**: 3-5 days for core implementation + 2-3 days for testing/polish

---

## Quick Reference

### Key Files to Modify
```
src/main/kotlin/com/hideandseek/
├── shop/
│   ├── ShopItem.kt              [EXTEND] Add camouflage fields
│   ├── ShopManager.kt           [EXTEND] Add new item handlers
│   ├── PurchaseValidator.kt     [EXTEND] Team-wide limits
│   ├── PurchaseStorage.kt       [EXTEND] Analytics tracking
├── config/
│   └── ShopConfig.kt            [EXTEND] Parse disguise tiers
├── effects/
│   └── EffectType.kt            [EXTEND] Add new effect types
└── items/                       [NEW PACKAGE]
    ├── DecoyBlockHandler.kt
    ├── ShadowSprintHandler.kt
    └── ... (8 new handlers)

src/main/resources/
└── shop.yml                     [NEW FILE]
```

### New Dependencies
None - uses existing Paper API, Vault, and Kotlin stdlib.

### API Changes
- `ShopItem` data class: +4 new optional fields (backward compatible)
- `EffectType` enum: +8 new effect types
- `PurchaseRestriction`: new data class
- `ItemStatistics`: new data class

---

## Phase 1: Camouflage-Based Pricing (Priority P1)

**Goal**: Implement disguise block pricing based on visibility tiers.

### Step 1.1: Extend ShopItem

```kotlin
// File: src/main/kotlin/com/hideandseek/shop/ShopItem.kt

data class ShopItem(
    // ... existing fields ...

    // NEW: Camouflage pricing
    val camouflageTier: CamouflageTier? = null,
    val camouflageMultiplier: Double? = null,
) {
    /**
     * Calculate effective price based on camouflage tier
     */
    fun getEffectivePrice(): Int {
        return when {
            camouflageTier != null -> {
                val basePrice = camouflageTier.basePrice
                val multiplier = camouflageMultiplier ?: 1.0
                (basePrice * multiplier).toInt()
            }
            else -> price
        }
    }
}

enum class CamouflageTier(val basePrice: Int) {
    HIGH_VISIBILITY(0),
    MEDIUM_VISIBILITY(50),
    LOW_VISIBILITY(100)
}
```

### Step 1.2: Update ShopConfig

```kotlin
// File: src/main/kotlin/com/hideandseek/config/ShopConfig.kt

data class ShopConfig(
    val version: Int,
    val categories: Map<String, ShopCategory>,
    val items: Map<String, ShopItem>,

    // NEW: Disguise blocks configuration
    val disguiseBlocks: Map<CamouflageTier, List<Material>>
)

object ShopConfigLoader {
    fun load(file: File): ShopConfig {
        val yaml = YamlConfiguration.loadConfiguration(file)

        // Parse disguise blocks
        val disguiseBlocks = CamouflageTier.values().associateWith { tier ->
            val tierName = tier.name.lowercase().replace('_', '-')
            val path = "shop.disguise-blocks.$tierName.blocks"
            yaml.getStringList(path).mapNotNull { Material.getMaterial(it) }
        }

        // Generate ShopItems for disguise blocks
        val disguiseItems = disguiseBlocks.flatMap { (tier, materials) ->
            materials.map { material ->
                createDisguiseBlockItem(material, tier)
            }
        }

        // ... rest of config parsing ...

        return ShopConfig(/* ... */)
    }

    private fun createDisguiseBlockItem(
        material: Material,
        tier: CamouflageTier
    ): ShopItem {
        return ShopItem(
            id = "disguise-${material.name.lowercase()}",
            material = material,
            displayName = "&e${material.name.replace('_', ' ')}",
            lore = listOf(
                "&7Camouflage: ${tier.name.replace('_', ' ')}",
                "&7Cost: &6${tier.basePrice} coins"
            ),
            slot = -1,  // Auto-assigned
            price = tier.basePrice,
            action = ShopAction.Disguise(material),
            camouflageTier = tier,
            roleFilter = "HIDER"
        )
    }
}
```

### Step 1.3: Create shop.yml

```yaml
# File: src/main/resources/shop.yml

shop:
  version: 2

  disguise-blocks:
    high-visibility:
      price: 0
      blocks:
        - MELON
        - PUMPKIN
        - SPONGE
        - GOLD_BLOCK
        - DIAMOND_BLOCK
        - GLOWSTONE
        - SEA_LANTERN

    medium-visibility:
      price: 50
      blocks:
        - OAK_LOG
        - BRICK
        - SANDSTONE
        - GRAVEL
        - GLASS

    low-visibility:
      price: 100
      blocks:
        - STONE
        - COBBLESTONE
        - DIRT
        - GRASS_BLOCK
        - ANDESITE
```

### Step 1.4: Update ShopManager GUI

```kotlin
// File: src/main/kotlin/com/hideandseek/shop/ShopManager.kt

fun openShop(player: Player, role: PlayerRole) {
    val items = getShopItems(role)
    val balance = economy.getBalance(player)

    // Group disguise blocks by tier for better organization
    val disguiseItems = items.filter { it.camouflageTier != null }
        .sortedBy { it.camouflageTier?.ordinal ?: 999 }

    val otherItems = items.filter { it.camouflageTier == null }

    val inventory = Bukkit.createInventory(null, 54, "&eShop - Balance: ${balance}c")

    // Place items in GUI
    var slot = 0
    for (item in disguiseItems + otherItems) {
        inventory.setItem(slot++, item.toItemStack(player, balance))
    }

    player.openInventory(inventory)
}

private fun ShopItem.toItemStack(player: Player, balance: Int): ItemStack {
    val effectivePrice = getEffectivePrice()
    val canAfford = balance >= effectivePrice

    val itemStack = ItemStack(material)
    val meta = itemStack.itemMeta ?: return itemStack

    meta.setDisplayName(displayName)

    val loreWithPrice = lore.toMutableList().apply {
        add("")
        add(if (canAfford) {
            "&aCost: ${effectivePrice} coins"
        } else {
            "&cCost: ${effectivePrice} coins (Cannot afford)"
        })

        // Show tier information
        camouflageTier?.let {
            add("&7Tier: ${it.name.replace('_', ' ')}")
        }
    }

    meta.lore = loreWithPrice.map { ChatColor.translateAlternateColorCodes('&', it) }
    itemStack.itemMeta = meta

    return itemStack
}
```

**Testing Checklist**:
- [ ] Disguise blocks load from config correctly
- [ ] Prices match configured tiers
- [ ] Shop GUI groups items by tier
- [ ] Free blocks show "0 coins"
- [ ] Cannot purchase when insufficient funds

---

## Phase 2: Enhanced Item Variety (Priority P2)

**Goal**: Add new item types with dedicated effect handlers.

### Step 2.1: Add New Effect Types

```kotlin
// File: src/main/kotlin/com/hideandseek/effects/EffectType.kt

enum class EffectType {
    // Existing
    SPEED_BOOST,
    VISION_NIGHT,
    GLOW_DETECTOR,
    REACH_EXTENDER,

    // NEW: Hider items
    SHADOW_SPRINT,      // Speed boost for hiders
    DECOY_BLOCK,        // Place fake disguise
    SECOND_CHANCE,      // Respawn on capture

    // NEW: Seeker items
    TRACKER_COMPASS,    // Points to nearest hider
    AREA_SCAN,          // Reveals hider count
    EAGLE_EYE,          // Highlights disguised blocks
    TRACKER_INSIGHT,    // Shows last-moved times
    CAPTURE_NET         // 2x capture reward
}
```

### Step 2.2: Create Item Handler Interface

```kotlin
// File: src/main/kotlin/com/hideandseek/items/ItemEffectHandler.kt

interface ItemEffectHandler {
    /**
     * Check if effect can be applied to player
     */
    fun canApply(player: Player, game: Game): Boolean

    /**
     * Apply effect to player
     */
    fun apply(player: Player, game: Game, config: ItemConfig): Result<Unit>

    /**
     * Called when effect expires
     */
    fun onExpire(player: Player, game: Game)

    /**
     * Get tooltip lines for shop GUI
     */
    fun getDisplayLore(config: ItemConfig): List<String>
}

data class ItemConfig(
    val duration: Int,
    val intensity: Double,
    val metadata: Map<String, Any> = emptyMap()
)
```

### Step 2.3: Implement Example Handler

```kotlin
// File: src/main/kotlin/com/hideandseek/items/ShadowSprintHandler.kt

class ShadowSprintHandler(
    private val effectManager: EffectManager
) : ItemEffectHandler {

    override fun canApply(player: Player, game: Game): Boolean {
        // Check if player is hider
        val playerData = game.getPlayerData(player.uniqueId) ?: return false
        if (playerData.role != PlayerRole.HIDER) return false

        // Check if game is in valid phase
        if (game.phase != GamePhase.SEEK) return false

        // Check if player already has speed effect
        if (effectManager.hasEffect(player.uniqueId, EffectType.SHADOW_SPRINT)) {
            player.sendMessage("§cYou already have Shadow Sprint active!")
            return false
        }

        return true
    }

    override fun apply(player: Player, game: Game, config: ItemConfig): Result<Unit> {
        return runCatching {
            // Apply potion effect
            player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.SPEED,
                    config.duration * 20,  // Convert to ticks
                    (config.intensity - 1.0).toInt(),  // Speed level
                    false,
                    false
                )
            )

            // Register with effect manager
            effectManager.applyEffect(ItemEffect(
                effectId = UUID.randomUUID(),
                playerId = player.uniqueId,
                itemId = "shadow-sprint",
                effectType = EffectType.SHADOW_SPRINT,
                startTime = System.currentTimeMillis(),
                duration = config.duration,
                intensity = config.intensity
            ))

            // Feedback
            player.sendMessage("§aShadow Sprint activated! (${config.duration}s)")
            player.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f)
        }
    }

    override fun onExpire(player: Player, game: Game) {
        player.sendMessage("§7Shadow Sprint has expired.")
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.8f)
    }

    override fun getDisplayLore(config: ItemConfig): List<String> {
        return listOf(
            "§7Duration: §e${config.duration}s",
            "§7Speed: §e${(config.intensity * 100).toInt()}%",
            "",
            "§7Grants enhanced movement speed",
            "§7Can be used while visible"
        )
    }
}
```

### Step 2.4: Register Handlers

```kotlin
// File: src/main/kotlin/com/hideandseek/shop/ShopManager.kt

class ShopManager(
    private val plugin: HideAndSeekPlugin,
    private val effectManager: EffectManager
) {
    private val handlers = mutableMapOf<EffectType, ItemEffectHandler>()

    init {
        registerHandlers()
    }

    private fun registerHandlers() {
        handlers[EffectType.SHADOW_SPRINT] = ShadowSprintHandler(effectManager)
        handlers[EffectType.DECOY_BLOCK] = DecoyBlockHandler(effectManager, plugin)
        handlers[EffectType.TRACKER_COMPASS] = TrackerCompassHandler(effectManager)
        // ... register all handlers ...
    }

    fun handleItemPurchase(player: Player, item: ShopItem, game: Game): Result<Unit> {
        // ... existing purchase validation ...

        // Apply effect if item has one
        item.effectType?.let { effectType ->
            val handler = handlers[effectType] ?: return Result.failure(
                IllegalStateException("No handler for effect type: $effectType")
            )

            if (!handler.canApply(player, game)) {
                return Result.failure(IllegalStateException("Cannot apply effect"))
            }

            val config = ItemConfig(
                duration = item.effectDuration ?: 0,
                intensity = item.effectIntensity ?: 1.0,
                metadata = emptyMap()
            )

            return handler.apply(player, game, config)
        }

        return Result.success(Unit)
    }
}
```

**Testing Checklist**:
- [ ] All 8 new effect types have handlers
- [ ] Handlers validate preconditions correctly
- [ ] Effects apply and expire as expected
- [ ] Feedback messages display properly
- [ ] Conflicting effects are prevented

---

## Phase 3: Purchase Restrictions (Priority P3)

**Goal**: Implement per-player, per-team, and global purchase limits.

### Step 3.1: Define Restriction Types

```kotlin
// File: src/main/kotlin/com/hideandseek/shop/PurchaseRestriction.kt

data class PurchaseRestriction(
    val type: RestrictionType,
    val maxQuantity: Int,
    val cooldownSeconds: Int? = null,
    val resetTrigger: ResetTrigger = ResetTrigger.NEVER
)

enum class RestrictionType {
    PER_PLAYER,
    PER_TEAM,
    GLOBAL
}

enum class ResetTrigger {
    NEVER,
    ON_USE,
    ON_DEATH,
    ON_PHASE_CHANGE,
    ON_PURCHASE
}
```

### Step 3.2: Extend PurchaseStorage

```kotlin
// File: src/main/kotlin/com/hideandseek/shop/PurchaseStorage.kt

class PurchaseStorage {
    // Existing: per-player purchases
    private val playerPurchases = ConcurrentHashMap<UUID, MutableList<PlayerPurchaseRecord>>()

    // NEW: per-team purchases
    private val teamPurchases = ConcurrentHashMap<PlayerRole, MutableMap<String, Int>>()

    // NEW: global purchases
    private val globalPurchases = ConcurrentHashMap<String, Int>()

    fun recordPurchase(record: PlayerPurchaseRecord) {
        // Player tracking
        playerPurchases.getOrPut(record.playerId) { mutableListOf() }.add(record)

        // Team tracking
        teamPurchases.getOrPut(record.playerRole) { mutableMapOf() }
            .merge(record.itemId, 1, Int::plus)

        // Global tracking
        globalPurchases.merge(record.itemId, 1, Int::plus)
    }

    fun canPurchase(
        player: UUID,
        item: ShopItem,
        role: PlayerRole
    ): Result<Unit> {
        // Check per-player limit
        item.maxPurchases?.let { maxPurchases ->
            val purchased = getPurchaseCount(player, item.id)
            if (purchased >= maxPurchases) {
                return Result.failure(PurchaseLimitException(
                    "You have reached the purchase limit for this item ($maxPurchases)"
                ))
            }
        }

        // Check per-team limit
        item.teamMaxPurchases?.let { maxTeamPurchases ->
            val teamCount = teamPurchases[role]?.get(item.id) ?: 0
            if (teamCount >= maxTeamPurchases) {
                return Result.failure(PurchaseLimitException(
                    "Your team has reached the limit for this item ($maxTeamPurchases)"
                ))
            }
        }

        // Check global limit
        item.globalMaxPurchases?.let { maxGlobalPurchases ->
            val globalCount = globalPurchases[item.id] ?: 0
            if (globalCount >= maxGlobalPurchases) {
                return Result.failure(PurchaseLimitException(
                    "This item is sold out ($maxGlobalPurchases total)"
                ))
            }
        }

        return Result.success(Unit)
    }

    fun clear() {
        playerPurchases.clear()
        teamPurchases.clear()
        globalPurchases.clear()
    }
}
```

### Step 3.3: Update ShopItem with Restrictions

```kotlin
// File: src/main/kotlin/com/hideandseek/shop/ShopItem.kt

data class ShopItem(
    // ... existing fields ...

    // NEW: Advanced restrictions
    val teamMaxPurchases: Int? = null,
    val globalMaxPurchases: Int? = null,
    val resetTrigger: ResetTrigger? = null
)
```

**Testing Checklist**:
- [ ] Per-player limits enforced correctly
- [ ] Per-team limits enforced correctly
- [ ] Global limits enforced correctly
- [ ] Appropriate error messages shown
- [ ] Limits cleared on game end

---

## Phase 4: Usage Analytics (Optional)

**Goal**: Log purchase statistics for balance analysis.

### Step 4.1: Define Statistics Model

```kotlin
// File: src/main/kotlin/com/hideandseek/shop/ItemStatistics.kt

data class ItemStatistics(
    val itemId: String,
    val gameId: String,
    val timestamp: Long,
    val totalPurchases: Int,
    val usageRate: Double,
    val winRateWithItem: Double
)
```

### Step 4.2: Implement CSV Logger

```kotlin
// File: src/main/kotlin/com/hideandseek/shop/StatisticsLogger.kt

class StatisticsLogger(private val plugin: HideAndSeekPlugin) {
    private val logFile = File(plugin.dataFolder, "shop-stats.csv")

    init {
        if (!logFile.exists()) {
            logFile.writeText("timestamp,game_id,item_id,purchases,usage_rate,win_rate\n")
        }
    }

    fun logGameStatistics(game: Game, purchases: List<PlayerPurchaseRecord>) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val stats = aggregateStatistics(game, purchases)
            stats.forEach { stat ->
                logFile.appendText(
                    "${stat.timestamp},${stat.gameId},${stat.itemId}," +
                    "${stat.totalPurchases},${stat.usageRate},${stat.winRateWithItem}\n"
                )
            }
        })
    }

    private fun aggregateStatistics(
        game: Game,
        purchases: List<PlayerPurchaseRecord>
    ): List<ItemStatistics> {
        // Group by item ID and calculate metrics
        return purchases.groupBy { it.itemId }.map { (itemId, records) ->
            ItemStatistics(
                itemId = itemId,
                gameId = game.id,
                timestamp = System.currentTimeMillis(),
                totalPurchases = records.size,
                usageRate = records.count { it.activated }.toDouble() / records.size,
                winRateWithItem = calculateWinRate(game, records)
            )
        }
    }
}
```

---

## Testing Guide

### Unit Tests

```kotlin
// File: src/test/kotlin/shop/CamouflagePricingTest.kt

class CamouflagePricingTest {
    @Test
    fun `high visibility blocks should be free`() {
        val item = ShopItem(
            id = "disguise-melon",
            material = Material.MELON,
            displayName = "Melon",
            lore = emptyList(),
            slot = 0,
            price = 0,
            action = ShopAction.Disguise(Material.MELON),
            camouflageTier = CamouflageTier.HIGH_VISIBILITY
        )

        assertEquals(0, item.getEffectivePrice())
    }

    @Test
    fun `low visibility blocks should cost 100`() {
        val item = ShopItem(
            id = "disguise-stone",
            material = Material.STONE,
            displayName = "Stone",
            lore = emptyList(),
            slot = 0,
            price = 100,
            action = ShopAction.Disguise(Material.STONE),
            camouflageTier = CamouflageTier.LOW_VISIBILITY
        )

        assertEquals(100, item.getEffectivePrice())
    }
}
```

### Integration Tests

```kotlin
// File: src/test/kotlin/shop/PurchaseFlowTest.kt

class PurchaseFlowTest {
    @Test
    fun `should enforce per-player purchase limits`() {
        val storage = PurchaseStorage()
        val item = ShopItem(/* ... */, maxPurchases = 1)
        val player = UUID.randomUUID()

        // First purchase should succeed
        storage.recordPurchase(PlayerPurchaseRecord(/* ... */))

        // Second purchase should fail
        val result = storage.canPurchase(player, item, PlayerRole.HIDER)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PurchaseLimitException)
    }
}
```

---

## Common Pitfalls

1. **Forgetting to clear purchase storage**: Always call `PurchaseStorage.clear()` on game end
2. **Not handling async economy operations**: Use `runTaskAsynchronously` for Vault calls
3. **Hardcoding prices**: Use `getEffectivePrice()` instead of `item.price`
4. **Missing effect expiration**: Register cleanup tasks with Bukkit scheduler
5. **Thread safety**: Use `ConcurrentHashMap` for shared data structures

---

## Performance Tips

1. **Cache shop GUIs**: Generate once per role-balance combination
2. **Batch statistics writes**: Write all game stats in single async task
3. **Pre-compute lore**: Build item lore at config load, not per-open
4. **Index purchases by player**: Use HashMap for O(1) limit checks
5. **Lazy-load handlers**: Create handler instances on first use

---

## Configuration Example

Complete `shop.yml` example with all features:

```yaml
shop:
  version: 2

  categories:
    hider-disguise:
      name: "&eDisguise Blocks"
      icon: "GRASS_BLOCK"
      slot: 0
      role-filter: "HIDER"

  disguise-blocks:
    high-visibility:
      price: 0
      blocks: [MELON, PUMPKIN, GOLD_BLOCK]

    medium-visibility:
      price: 50
      blocks: [OAK_LOG, BRICK]

    low-visibility:
      price: 100
      blocks: [STONE, DIRT, GRASS_BLOCK]

  items:
    shadow-sprint:
      category: "hider-utility"
      material: "FEATHER"
      display-name: "&eShadow Sprint"
      lore:
        - "&720 seconds of speed boost"
      price: 350
      effect:
        type: "SHADOW_SPRINT"
        duration: 20
        intensity: 1.5
      restrictions:
        type: "PER_PLAYER"
        max-purchases: 2
        cooldown: 60
      role-filter: "HIDER"

  settings:
    enable-statistics: true
    cache-gui: true
```

---

## Next Steps

After completing this implementation:
1. Run `/speckit.tasks` to generate detailed task breakdown
2. Implement handlers for all 8 new item types
3. Add comprehensive unit tests (target: 80% coverage)
4. Playtest with 10+ players to validate balance
5. Analyze statistics logs and adjust pricing
6. Document admin commands in plugin documentation

---

## Support Resources

- **Spec**: [spec.md](./spec.md) - Full requirements
- **Research**: [research.md](./research.md) - Design decisions
- **Data Model**: [data-model.md](./data-model.md) - Entity definitions
- **Schema**: [contracts/shop-config-schema.yml](./contracts/shop-config-schema.yml) - Config validation

**Questions?** Review research.md "Open Questions" section for design rationale.
