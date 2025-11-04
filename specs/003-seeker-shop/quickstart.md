# Seeker Shop System - Developer Quickstart Guide

**Feature Branch**: `003-seeker-shop`
**Goal**: Get you implementing seeker shop items within 30 minutes
**Target Audience**: Developers new to this codebase

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Project Setup](#2-project-setup)
3. [Architecture Overview](#3-architecture-overview)
4. [Development Workflow](#4-development-workflow)
5. [Quick Start Examples](#5-quick-start-examples)
6. [Testing Guide](#6-testing-guide)
7. [Configuration](#7-configuration)
8. [Debugging Tips](#8-debugging-tips)

---

## 1. Prerequisites

### Required Tools

- **JDK 21** - Required by Paper API 1.21.5
  ```bash
  java -version  # Should show 21.x
  ```

- **Gradle 8.8+** - Build system (wrapper included)
  ```bash
  ./gradlew --version
  ```

- **IDE** - IntelliJ IDEA recommended (Community Edition works)
  - Install Kotlin plugin if not already included

- **Git** - Version control
  ```bash
  git --version
  ```

### Required Knowledge

- **Kotlin basics** - Classes, data classes, functions, null safety
- **Paper API fundamentals** - Events, Scheduler, Player API
- **Minecraft concepts** - Potion effects, particles, raycasting (helpful but not critical)

### Dependencies Familiarity

- **Paper API 1.21.5** - Server platform
- **Vault API** - Economy system integration
- **MockBukkit** - Testing framework

### Time Estimate

- **Initial setup**: 10 minutes
- **First effect implementation**: 20 minutes
- **Testing setup**: 10 minutes

---

## 2. Project Setup

### Step 1: Clone and Branch

```bash
# Navigate to project directory
cd /Users/masafumi_t/Develop/hacklab/kikaku2/EasyHideAndSeek

# Verify you're on the feature branch
git branch
# Should show: * 003-seeker-shop

# If not, checkout the branch
git checkout 003-seeker-shop

# Pull latest changes
git pull origin 003-seeker-shop
```

### Step 2: Build the Project

```bash
# Clean and build
./gradlew clean build

# Expected output: BUILD SUCCESSFUL
```

### Step 3: Verify Dependencies

```bash
# Check dependencies
./gradlew dependencies --configuration compileClasspath

# Verify these are present:
# - io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT
# - com.github.MilkBowl:VaultAPI:1.7
# - org.jetbrains.kotlin:kotlin-stdlib-jdk8
```

### Step 4: IDE Setup (IntelliJ IDEA)

1. **Open Project**
   - File → Open → Select project root directory
   - Wait for Gradle sync to complete

2. **Configure SDK**
   - File → Project Structure → Project
   - Set SDK to Java 21
   - Set language level to 21

3. **Enable Kotlin**
   - Should be automatic with Kotlin plugin installed
   - Verify: Right-click on `src/main/kotlin` → Mark Directory as → Sources Root

4. **Install Recommended Plugins**
   - Minecraft Development (optional but helpful)
   - YAML/Ansible support

### Step 5: Test Server Setup

```bash
# Run test server (downloads Paper automatically)
./gradlew runServer

# Server will start in run/ directory
# Stop with /stop command

# Verify plugin loaded:
# Console should show: [EasyHideAndSeek] Enabling EasyHideAndSeek v1.0-SNAPSHOT
```

---

## 3. Architecture Overview

### High-Level Component Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    Seeker Shop System                    │
└─────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│   Shop GUI   │   │    Effect    │   │     Item     │
│   Extension  │   │   Manager    │   │  Handlers    │
│              │   │              │   │              │
│ • Role-based │   │ • Apply      │   │ • Vision     │
│   filtering  │   │ • Remove     │   │ • Glow       │
│ • Purchase   │   │ • Track      │   │ • Speed      │
│   validation │   │ • Cleanup    │   │ • Reach      │
└──────────────┘   └──────────────┘   └──────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
                ┌───────────┴───────────┐
                │                       │
                ▼                       ▼
        ┌──────────────┐       ┌──────────────┐
        │   Existing   │       │   Existing   │
        │    Shop      │       │   Disguise   │
        │   System     │       │   System     │
        └──────────────┘       └──────────────┘
```

### Key Packages and Responsibilities

```
src/main/kotlin/com/hideandseek/
│
├── effects/                    # NEW - Effect management system
│   ├── EffectManager.kt        # Core API for managing effects
│   ├── EffectType.kt           # Enum: VISION, GLOW, SPEED, REACH
│   ├── ActiveEffect.kt         # Data class: effect state
│   ├── EffectStorage.kt        # In-memory storage
│   └── OverlapPolicy.kt        # Enum: REPLACE, EXTEND, REJECT
│
├── items/                      # NEW - Seeker item implementations
│   ├── SeekerItemHandler.kt    # Base interface
│   ├── VisionEnhancerItem.kt   # Night Vision + View Distance
│   ├── GlowDetectorItem.kt     # Disguise block highlighting
│   ├── SpeedBoostItem.kt       # Movement speed boost
│   └── ReachExtenderItem.kt    # Extended attack range
│
├── shop/                       # EXTEND - Existing shop system
│   ├── ShopManager.kt          # Extend with role filtering
│   ├── ShopCategory.kt         # Add roleFilter field
│   ├── ShopItem.kt             # Add effect metadata
│   └── ShopAction.kt           # Add UseEffectItem action
│
├── listeners/                  # EXTEND - Event handlers
│   ├── ShopListener.kt         # Add purchase logic
│   ├── PlayerInteractListener.kt # Add item use detection
│   └── EffectCleanupListener.kt  # NEW - Game end cleanup
│
├── config/                     # EXTEND - Configuration
│   └── ShopConfig.kt           # Load seeker items from shop.yml
│
└── utils/
    └── EffectScheduler.kt      # NEW - Scheduled cleanup tasks
```

### Integration Points

**With Existing Systems**:

1. **Shop System** (`shop/`)
   - Extends `ShopCategory` to filter by role (SEEKER/HIDER)
   - Adds effect metadata to `ShopItem`
   - Purchase flow unchanged, but validates role

2. **Disguise System** (`disguise/`)
   - Glow Detector queries `DisguiseManager.getActiveDisguises()`
   - Listens to `PlayerUnDisguiseEvent` to update particles

3. **Game Manager** (`game/`)
   - Validates player is in-game and correct phase
   - Cleans up effects on game end via `GameEndEvent`

4. **Economy** (Vault API)
   - Purchase uses existing `VaultAPI` integration
   - Balance checks and withdrawals

### Data Flow: Item Purchase → Use → Expire

```
[Player] → [/hs shop] → [ShopGUI (role-filtered)]
                              │
                              ↓
                    [Click seeker item]
                              │
                ┌─────────────┴─────────────┐
                │ ShopManager.purchaseItem()│
                └─────────────┬─────────────┘
                              │
            ┌─────────────────┼─────────────────┐
            ↓                 ↓                 ↓
    [Role check]    [Balance check]   [Cooldown check]
            │                 │                 │
            └─────────────────┴─────────────────┘
                              │
                              ↓
                    [Vault withdraw coins]
                              │
                              ↓
                    [Add item to inventory]
                              │
                              ↓
[Player right-clicks item] → [PlayerInteractListener]
                              │
                              ↓
                    [ItemHandler.useItem()]
                              │
                              ↓
                    [EffectManager.applyEffect()]
                              │
            ┌─────────────────┼─────────────────┐
            ↓                 ↓                 ↓
    [Apply Minecraft   [Store in         [Schedule
     effect]            EffectStorage]    cleanup task]
            │                 │                 │
            └─────────────────┴─────────────────┘
                              │
                    [Effect active for 30s]
                              │
                              ↓
                    [BukkitTask fires]
                              │
                              ↓
                    [EffectManager.removeEffect()]
                              │
                              ↓
                    [Cleanup & notify player]
```

---

## 4. Development Workflow

### Implementation Order (Following plan.md)

Implement features in this order to minimize dependencies:

#### Phase 1: Foundation (Week 1)

1. **EffectManager Core** (Day 1-2)
   - Create `EffectManager` interface
   - Implement `EffectStorage` (ConcurrentHashMap-based)
   - Implement `ActiveEffect` data class
   - Implement `EffectType` enum

2. **Shop System Extension** (Day 2-3)
   - Extend `ShopCategory` with `roleFilter`
   - Extend `ShopItem` with effect metadata
   - Update `ShopManager` to filter by role
   - Add purchase validation logic

#### Phase 2: Simple Effects (Week 1-2)

3. **Speed Boost Item** (Day 3-4)
   - Implement `SpeedBoostItem`
   - Simple potion effect application
   - Good first implementation to understand patterns

4. **Vision Enhancer Item** (Day 4-5)
   - Implement `VisionEnhancerItem`
   - Night Vision + view distance modification
   - Learn client-side API interactions

#### Phase 3: Complex Effects (Week 2)

5. **Glow Detector Item** (Day 6-7)
   - Implement `GlowDetectorItem`
   - Particle system with repeating tasks
   - Integration with `DisguiseManager`

6. **Reach Extender Item** (Day 8-9)
   - Implement `ReachExtenderItem`
   - Raycast mechanics
   - Extend `BlockDamageListener`

#### Phase 4: Testing & Polish (Week 3)

7. **Unit Tests** (Day 10-11)
   - Test each item implementation
   - Test EffectManager operations
   - Test edge cases

8. **Integration Testing** (Day 12-13)
   - Full flow tests (purchase → use → expire)
   - Multi-player scenarios
   - Performance testing

9. **Configuration & Docs** (Day 14)
   - Finalize shop.yml structure
   - Update plugin.yml if needed
   - Write inline documentation

### Testing Each Component

After implementing each component:

1. **Unit test** - Test in isolation with mocks
2. **Manual test** - Run on test server, verify behavior
3. **Integration test** - Test with other components
4. **Edge case test** - Try boundary conditions

### Common Pitfalls and Solutions

| Pitfall | Solution |
|---------|----------|
| Calling Bukkit API from async thread | Always use `Bukkit.getScheduler().runTask()` for sync operations |
| Forgetting to cancel BukkitTask | Store task ID in `ActiveEffect` and cancel in cleanup |
| Not checking player online status | Always check `player.isOnline` before operations |
| Memory leaks from uncleaned effects | Implement cleanup listeners for quit/disconnect |
| Race conditions in effect storage | Use `ConcurrentHashMap` and synchronize critical sections |
| View distance not taking effect | Use Paper's `player.sendViewDistance()`, not Bukkit method |
| Particles visible to all players | Use player-specific `player.spawnParticle()` method |

---

## 5. Quick Start Examples

### 5-Minute Example: Create a Simple Effect

**Goal**: Create a "Night Vision" effect that lasts 10 seconds.

**File**: `src/main/kotlin/com/hideandseek/effects/SimpleNightVisionEffect.kt`

```kotlin
package com.hideandseek.effects

import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.plugin.Plugin
import org.bukkit.Bukkit

class SimpleNightVisionEffect(private val plugin: Plugin) {

    fun apply(player: Player, durationSeconds: Int) {
        // 1. Create potion effect
        val nightVision = PotionEffect(
            PotionEffectType.NIGHT_VISION,  // Effect type
            durationSeconds * 20,            // Duration in ticks (20 ticks = 1 second)
            0,                                // Amplifier (0 = level 1)
            false,                            // No ambient particles
            false,                            // No visible particles
            true                              // Show icon in player UI
        )

        // 2. Apply to player
        player.addPotionEffect(nightVision)

        // 3. Notify player
        player.sendMessage("§aNight Vision activated for ${durationSeconds}s!")

        // 4. Schedule removal (optional - potion expires automatically)
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            player.sendMessage("§eNight Vision expired")
        }, (durationSeconds * 20).toLong())
    }
}
```

**Test it**:
```kotlin
// In your plugin's onEnable() or command handler
val effect = SimpleNightVisionEffect(this)
effect.apply(player, 10)
```

---

### 10-Minute Example: Add a Shop Item

**Goal**: Add "Night Vision Potion" to the seeker shop.

**Step 1**: Add to `src/main/resources/shop.yml`

```yaml
categories:
  seeker-items:
    display-name: "&cSeeker Tools"
    icon: ENDER_EYE
    slot: 11
    role-filter: SEEKER

    items:
      # Add this item
      night-vision-potion:
        material: POTION
        display-name: "&b&lNight Vision Potion"
        slot: 10
        price: 100
        effect-type: VISION
        effect-duration: 10
        effect-intensity: 1.0
        lore:
          - "&7See in the dark for 10 seconds"
          - "&e&lPrice: &f100 coins"
```

**Step 2**: Create item handler class

**File**: `src/main/kotlin/com/hideandseek/items/NightVisionPotionItem.kt`

```kotlin
package com.hideandseek.items

import com.hideandseek.effects.EffectType
import com.hideandseek.effects.ActiveEffect
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.time.Instant

class NightVisionPotionItem(
    private val plugin: Plugin
) : SeekerEffectItem {

    override val effectType = EffectType.VISION

    override fun applyEffect(
        player: Player,
        duration: Int,
        intensity: Double
    ): Result<ActiveEffect> {

        // Apply the potion effect
        val nightVision = PotionEffect(
            PotionEffectType.NIGHT_VISION,
            duration * 20,
            0,
            false,
            false,
            true
        )
        player.addPotionEffect(nightVision)

        // Create effect tracking object
        val effect = ActiveEffect(
            playerId = player.uniqueId,
            effectType = EffectType.VISION,
            startTime = Instant.now(),
            endTime = Instant.now().plusSeconds(duration.toLong()),
            intensity = intensity
        )

        // Schedule cleanup
        val taskId = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            removeEffect(player, effect)
        }, (duration * 20).toLong())

        effect.taskId = taskId.taskId

        // Feedback
        player.sendMessage("§aNight Vision activated!")

        return Result.success(effect)
    }

    override fun removeEffect(player: Player, effect: ActiveEffect) {
        player.removePotionEffect(PotionEffectType.NIGHT_VISION)
        player.sendMessage("§eNight Vision expired")
        effect.taskId?.let { Bukkit.getScheduler().cancelTask(it) }
    }
}
```

**Step 3**: Register in shop manager

```kotlin
// In ShopManager initialization
val nightVisionItem = NightVisionPotionItem(plugin)
registerItem("night-vision-potion", nightVisionItem)
```

**Test it**:
1. Start test server: `./gradlew runServer`
2. Join game as seeker
3. Run `/hs shop`
4. Purchase Night Vision Potion (needs 100 coins)
5. Right-click item in inventory
6. Verify night vision applied for 10 seconds

---

### 15-Minute Example: Test the Purchase Flow

**Goal**: Write an integration test for item purchase and usage.

**File**: `src/test/kotlin/com/hideandseek/items/NightVisionPotionItemTest.kt`

```kotlin
package com.hideandseek.items

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import be.seeseemelk.mockbukkit.entity.PlayerMock
import com.hideandseek.HideAndSeekPlugin
import com.hideandseek.effects.EffectType
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class NightVisionPotionItemTest {

    private lateinit var server: ServerMock
    private lateinit var plugin: HideAndSeekPlugin
    private lateinit var player: PlayerMock
    private lateinit var item: NightVisionPotionItem

    @BeforeEach
    fun setUp() {
        // Initialize MockBukkit
        server = MockBukkit.mock()
        plugin = MockBukkit.load(HideAndSeekPlugin::class.java)

        // Create test player
        player = server.addPlayer("TestSeeker")

        // Initialize item
        item = NightVisionPotionItem(plugin)
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `applyEffect should add night vision to player`() {
        // Act
        val result = item.applyEffect(player, 10, 1.0)

        // Assert
        assertTrue(result.isSuccess, "Effect application should succeed")
        assertTrue(player.hasPotionEffect(PotionEffectType.NIGHT_VISION),
                   "Player should have Night Vision")

        val effect = result.getOrNull()
        assertNotNull(effect)
        assertEquals(EffectType.VISION, effect?.effectType)
    }

    @Test
    fun `removeEffect should remove night vision from player`() {
        // Arrange
        val effect = item.applyEffect(player, 10, 1.0).getOrThrow()
        assertTrue(player.hasPotionEffect(PotionEffectType.NIGHT_VISION))

        // Act
        item.removeEffect(player, effect)

        // Assert
        assertFalse(player.hasPotionEffect(PotionEffectType.NIGHT_VISION),
                    "Player should not have Night Vision after removal")
    }

    @Test
    fun `effect should auto-expire after duration`() {
        // Arrange
        val durationSeconds = 2
        item.applyEffect(player, durationSeconds, 1.0)

        // Act - advance server time
        server.scheduler.performTicks((durationSeconds * 20 + 10).toLong())

        // Assert
        assertFalse(player.hasPotionEffect(PotionEffectType.NIGHT_VISION),
                    "Effect should expire after duration")
    }
}
```

**Run the test**:
```bash
./gradlew test --tests NightVisionPotionItemTest

# Expected output: BUILD SUCCESSFUL
```

---

## 6. Testing Guide

### Unit Test Setup

**Step 1**: Add MockBukkit dependency (already in build.gradle.kts)

```kotlin
testImplementation("com.github.seeseemelk:MockBukkit-v1.20:3.9.0")
```

**Step 2**: Create test class structure

```kotlin
import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import org.junit.jupiter.api.*

class YourComponentTest {

    private lateinit var server: ServerMock
    private lateinit var plugin: HideAndSeekPlugin

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(HideAndSeekPlugin::class.java)
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `test something`() {
        // Arrange
        val player = server.addPlayer("TestPlayer")

        // Act
        // ... your code

        // Assert
        assertTrue(someCondition)
    }
}
```

**Step 3**: Run tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests YourComponentTest

# Run with detailed output
./gradlew test --info

# Generate test report
./gradlew test
# Open: build/reports/tests/test/index.html
```

### Integration Testing with Test Server

**Step 1**: Start test server

```bash
./gradlew runServer

# Server starts in run/ directory
# Paper will be downloaded automatically
```

**Step 2**: Connect and test

1. Open Minecraft 1.21.5
2. Add server: `localhost:25565`
3. Join server
4. Give yourself permission: `/op YourUsername`
5. Test commands:
   ```
   /hs join        # Join a game
   /hs shop        # Open shop
   ```

**Step 3**: Test with multiple players

Use multiple Minecraft clients or these test commands:
```
# Create fake players (if you have a dev plugin)
/debugplayer spawn TestSeeker
/debugplayer spawn TestHider

# Or use alt accounts
```

### Manual Test Checklist

When implementing each effect, verify:

- [ ] Item appears in shop GUI
- [ ] Item has correct icon and lore
- [ ] Price is charged correctly
- [ ] Insufficient funds shows error
- [ ] Item added to inventory
- [ ] Right-click activates effect
- [ ] Effect visible (potion icon, particles, etc.)
- [ ] Effect lasts correct duration
- [ ] Effect expires automatically
- [ ] Expiration message shown
- [ ] Item removed from inventory after use
- [ ] Cooldown works if configured
- [ ] Purchase limit works if configured
- [ ] Effect clears on player disconnect
- [ ] Effect clears on game end

### Performance Testing

**Test with 30 concurrent players**:

```bash
# Use stress test plugin or script
# Monitor server TPS
/tps

# Should maintain TPS >= 18.0
```

**Monitor memory usage**:
```bash
# In server console
/memory

# Check heap usage doesn't grow unbounded
```

**Profile with JVisualVM** (advanced):
```bash
# Start server with JMX enabled
java -Dcom.sun.management.jmxremote -jar server.jar

# Connect with JVisualVM to monitor:
# - Thread count (should be stable)
# - Heap usage (should not leak)
# - CPU usage (should be <50% with 30 players)
```

---

## 7. Configuration

### shop.yml Structure

**Location**: `src/main/resources/shop.yml`

```yaml
# Seeker Shop Configuration
categories:
  # Seeker items category
  seeker-items:
    display-name: "&cSeeker Tools"
    icon: ENDER_EYE
    slot: 11
    role-filter: SEEKER  # NEW FIELD - Only show to seekers
    description:
      - "&7Special items to help find hiders"

    items:
      # Template for adding new seeker items
      item-id:                          # Unique ID (lowercase, hyphens)
        material: MATERIAL_NAME         # Minecraft material
        display-name: "&color&lName"    # Formatted name
        slot: 10                        # GUI slot (0-53)
        price: 300                      # Cost in coins

        # Effect configuration
        effect-type: EFFECT_TYPE        # VISION, GLOW, SPEED, REACH
        effect-duration: 30             # Seconds
        effect-intensity: 1.0           # Multiplier (1.0 = normal)

        # Optional restrictions
        cooldown: 10                    # Seconds before repurchase (0 = none)
        max-purchases: -1               # Per game (-1 = unlimited)
        usage-restriction: SEEK_PHASE_ONLY  # When usable

        # Item-specific settings (optional)
        settings:
          custom-key: value

        # Lore (description lines)
        lore:
          - "&7Description line 1"
          - "&7Description line 2"
          - ""
          - "&e&lPrice: &f300 coins"

# Effect system configuration
effects:
  # How to handle applying same effect twice
  overlap-policies:
    vision: REPLACE   # REPLACE, EXTEND, REJECT, STACK
    glow: REPLACE
    speed: REPLACE
    reach: REPLACE

  # Max duration when using EXTEND policy
  max-extended-duration: 120

  # Performance tuning
  performance:
    glow-update-interval: 10    # Ticks between particle updates
    glow-max-distance: 50       # Max render distance for particles
    cleanup-task-interval: 60   # Seconds between cleanup runs

# User feedback
feedback:
  enable-particles: true
  enable-sounds: true
  show-action-bar: true
  show-boss-bar: false
```

### Adding a New Seeker Item

**Step-by-step**:

1. **Design the effect**
   - What does it do?
   - How long does it last?
   - What's fair pricing?

2. **Add to shop.yml**
   ```yaml
   seeker-items:
     items:
       your-new-item:
         material: GLOWSTONE
         display-name: "&e&lYour New Item"
         slot: 14
         price: 400
         effect-type: VISION
         effect-duration: 20
         effect-intensity: 1.0
         lore:
           - "&7Does something cool"
           - "&e&lPrice: &f400 coins"
   ```

3. **Create implementation class**
   ```kotlin
   // src/main/kotlin/com/hideandseek/items/YourNewItem.kt
   class YourNewItem(private val plugin: Plugin) : SeekerEffectItem {
       override val effectType = EffectType.VISION

       override fun applyEffect(...): Result<ActiveEffect> {
           // Implementation
       }

       override fun removeEffect(...) {
           // Cleanup
       }
   }
   ```

4. **Register in ShopManager**
   ```kotlin
   shopManager.registerItem("your-new-item", YourNewItem(plugin))
   ```

5. **Test**
   - Start test server
   - Buy and use item
   - Verify effect works as expected

### Adjusting Effect Parameters

**Common adjustments**:

```yaml
# Make effect stronger
effect-intensity: 2.0    # 2x strength

# Make effect last longer
effect-duration: 60      # 60 seconds

# Make more expensive
price: 1000

# Add cooldown
cooldown: 30             # 30 seconds

# Limit purchases
max-purchases: 3         # 3 per game

# Change overlap behavior
effects:
  overlap-policies:
    vision: EXTEND       # Add time instead of replacing
```

---

## 8. Debugging Tips

### Common Issues and Fixes

#### Issue: Effect not applying

**Symptoms**: Item consumed but no effect visible

**Debug steps**:
1. Check console for errors
2. Verify player role is SEEKER
3. Verify game phase is SEEKING
4. Check EffectManager logs
5. Add debug logging:
   ```kotlin
   plugin.logger.info("Applying effect ${effectType} to ${player.name}")
   ```

**Common causes**:
- Player not in correct role
- Wrong game phase
- Exception thrown (check logs)
- Effect duration = 0

---

#### Issue: Effect doesn't expire

**Symptoms**: Effect stays active forever

**Debug steps**:
1. Check if cleanup task was scheduled
2. Verify task ID stored in `ActiveEffect`
3. Check if task was cancelled prematurely
4. Add logging to cleanup:
   ```kotlin
   override fun removeEffect(player: Player, effect: ActiveEffect) {
       plugin.logger.info("Removing effect ${effect.effectType} from ${player.name}")
       // ... cleanup code
   }
   ```

**Common causes**:
- Task ID not stored
- Task cancelled early
- Server TPS drop (tasks delayed)
- Effect removed but Minecraft effect not cleared

---

#### Issue: Particles not visible

**Symptoms**: Glow detector shows no particles

**Debug steps**:
1. Verify disguised hiders exist
2. Check particle spawn distance
3. Verify player-specific spawn:
   ```kotlin
   // Correct: only this player sees particles
   player.spawnParticle(Particle.GLOW, location, 5)

   // Wrong: all players see particles
   world.spawnParticle(Particle.GLOW, location, 5)
   ```
4. Add debug particles at player location:
   ```kotlin
   player.spawnParticle(Particle.FLAME, player.location, 10)
   ```

**Common causes**:
- Using world.spawnParticle() instead of player.spawnParticle()
- No disguised hiders in range
- Particle type not supported in 1.21.5
- Render distance too low

---

#### Issue: Memory leak

**Symptoms**: Server memory usage grows over time

**Debug steps**:
1. Check if effects cleaned up on game end
2. Verify no circular references
3. Monitor EffectStorage size:
   ```kotlin
   plugin.logger.info("Active effects: ${effectStorage.size()}")
   ```
4. Dump memory with JVisualVM

**Common causes**:
- Effects not removed on player quit
- Tasks not cancelled
- Strong references preventing GC
- Maps not cleared on game end

---

#### Issue: Race condition / ConcurrentModificationException

**Symptoms**: Random crashes or exceptions

**Debug steps**:
1. Check if modifying collection during iteration
2. Verify thread safety
3. Add synchronization:
   ```kotlin
   synchronized(effectStorage) {
       // Critical section
   }
   ```

**Common causes**:
- Modifying map during forEach
- Async task accessing sync data
- Multiple threads writing to same storage

---

### Logging Best Practices

**Log levels**:
```kotlin
// DEBUG - Detailed trace
plugin.logger.fine("Effect applied: $effectType duration=$duration")

// INFO - Important events
plugin.logger.info("Seeker shop initialized with ${items.size} items")

// WARNING - Recoverable issues
plugin.logger.warning("Player ${player.name} tried to use effect while not seeker")

// SEVERE - Critical errors
plugin.logger.severe("Failed to apply effect: ${e.message}")
```

**Conditional debug logging**:
```kotlin
// In config
debug-mode: false

// In code
if (config.isDebugMode()) {
    plugin.logger.info("Debug: Effect storage size = ${storage.size()}")
}
```

**Structured logging**:
```kotlin
plugin.logger.info(
    "Effect applied | " +
    "player=${player.name} " +
    "type=$effectType " +
    "duration=${duration}s " +
    "intensity=$intensity"
)
```

---

### Performance Monitoring

**Monitor TPS in-game**:
```
/tps
# Should show: TPS from last 1m, 5m, 15m
# Target: >= 18.0 TPS
```

**Monitor specific operations**:
```kotlin
val startTime = System.nanoTime()
// ... operation
val duration = (System.nanoTime() - startTime) / 1_000_000  // ms
if (duration > 100) {
    plugin.logger.warning("Slow operation: ${duration}ms")
}
```

**Profile particle spawn rate**:
```kotlin
var particleCount = 0
val task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
    plugin.logger.info("Particles spawned: $particleCount/second")
    particleCount = 0
}, 0L, 20L)
```

---

### IntelliJ Debugging

**Set breakpoints**:
1. Click left gutter next to line number
2. Run server in debug mode:
   ```bash
   ./gradlew runServer --debug-jvm
   ```
3. Attach IntelliJ debugger:
   - Run → Attach to Process
   - Select Java process

**Conditional breakpoints**:
- Right-click breakpoint → Add condition
- Example: `player.name == "TestPlayer"`

**Evaluate expressions**:
- While paused, Alt+F8 to evaluate expressions
- Check variable values
- Call methods to test behavior

---

## Summary

You should now be able to:

- ✅ Set up the development environment (10 min)
- ✅ Understand the architecture and data flow
- ✅ Implement a simple effect (5 min example)
- ✅ Add a new shop item (10 min example)
- ✅ Write integration tests (15 min example)
- ✅ Configure items in shop.yml
- ✅ Debug common issues

### Next Steps

1. **Read the detailed specs**:
   - [spec.md](./spec.md) - User stories and requirements
   - [plan.md](./plan.md) - Implementation plan
   - [data-model.md](./data-model.md) - Data structures
   - [contracts/](./contracts/) - API specifications

2. **Start implementing**:
   - Begin with EffectManager (foundation)
   - Move to SpeedBoostItem (simplest effect)
   - Gradually tackle more complex items

3. **Join discussions**:
   - Ask questions in team chat
   - Review PRs from other developers
   - Share your implementations

### Useful Resources

- **Paper API Docs**: https://docs.papermc.io/
- **Spigot API Docs**: https://hub.spigotmc.org/javadocs/spigot/
- **Kotlin Docs**: https://kotlinlang.org/docs/
- **MockBukkit Docs**: https://github.com/MockBukkit/MockBukkit

### Getting Help

**If stuck**:
1. Check this quickstart guide
2. Read relevant spec documents
3. Search Paper API documentation
4. Ask in team chat with:
   - What you're trying to do
   - What you've tried
   - Error messages / logs
   - Relevant code snippet

**File Location**: `/Users/masafumi_t/Develop/hacklab/kikaku2/EasyHideAndSeek/specs/003-seeker-shop/quickstart.md`

---

Good luck, and happy coding! 🚀
