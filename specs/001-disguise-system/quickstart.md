# Quickstart Guide: Disguise System Development

**Feature**: 001-disguise-system
**Date**: 2025-10-23

## Overview

This guide helps developers quickly set up the development environment and understand the implementation flow for the Hide and Seek minigame plugin.

---

## Prerequisites

### Required Tools

- **Java 21 JDK** (OpenJDK or Oracle)
- **Kotlin 1.9+** compiler (via Gradle)
- **Gradle 8.x** or **Maven 3.9+**
- **Paper 1.21.x** test server
- **Git** for version control
- **IDE**: IntelliJ IDEA (recommended) or Eclipse with Kotlin plugin

### Required Dependencies

- Paper API 1.21-R0.1-SNAPSHOT
- Vault API 1.7
- JUnit 5 (testing)
- MockBukkit (testing)

---

## Project Setup

### 1. Clone Repository

```bash
git clone <repository-url>
cd EasyHideAndSeek
git checkout 001-disguise-system
```

### 2. Build Configuration

Create `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "1.9.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.hideandseek"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")

    // Vault API
    compileOnly("net.milkbowl.vault:VaultAPI:1.7")

    // Kotlin stdlib
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.20:3.9.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("kotlin", "com.hideandseek.libs.kotlin")
    }

    test {
        useJUnitPlatform()
    }

    processResources {
        filesMatching("plugin.yml") {
            expand(project.properties)
        }
    }
}

kotlin {
    jvmToolchain(21)
}
```

### 3. Plugin Metadata

Create `src/main/resources/plugin.yml`:

```yaml
name: HideAndSeek
version: ${version}
main: com.hideandseek.HideAndSeekPlugin
api-version: '1.21'
author: YourName
description: Hide and Seek minigame plugin
website: https://github.com/yourname/EasyHideAndSeek

depend:
  - Vault

commands:
  hideandseek:
    description: Hide and Seek main command
    aliases: [hs]
    usage: /hs <subcommand>

permissions:
  hideandseek.use:
    description: Basic plugin access
    default: true
  hideandseek.play:
    description: Join and play games
    default: true
  hideandseek.admin:
    description: Admin commands
    default: op
```

### 4. Build and Test

```bash
./gradlew clean build

# Run tests
./gradlew test

# Create plugin JAR
./gradlew shadowJar
# Output: build/libs/HideAndSeek-1.0.0-SNAPSHOT.jar
```

---

## Development Workflow

### Phase 1: Core Infrastructure (Week 1)

**Goal**: Set up plugin skeleton and configuration system

**Tasks**:
1. Create main plugin class (`HideAndSeekPlugin.kt`)
2. Implement `ConfigManager` for YAML loading
3. Create `ArenaConfig` for arenas.yml
4. Create `ShopConfig` for shop.yml
5. Write unit tests for config loading

**Files to Create**:
- `HideAndSeekPlugin.kt`
- `config/ConfigManager.kt`
- `config/ArenaConfig.kt`
- `config/ShopConfig.kt`
- `resources/config.yml`
- `resources/arenas.yml`
- `resources/shop.yml`

**Test First**:
```kotlin
@Test
fun `config should load default values`() {
    val server = MockBukkit.mock()
    val plugin = MockBukkit.load(HideAndSeekPlugin::class.java)

    val config = plugin.configManager.config

    assertEquals(30, config.getInt("game.preparation-time"))
    assertEquals(600, config.getInt("game.seek-time"))
}
```

### Phase 2: Arena Management (Week 2)

**Goal**: Implement arena setup and storage

**Tasks**:
1. Create `Arena` data class
2. Implement `ArenaManager` with CRUD operations
3. Create `AdminCommand` for arena setup
4. Implement `ArenaSetupSession` state tracking
5. Write tests for arena creation/validation

**Files to Create**:
- `arena/Arena.kt`
- `arena/ArenaManager.kt`
- `arena/ArenaSetupSession.kt`
- `commands/AdminCommand.kt`

**Test First**:
```kotlin
@Test
fun `arena should validate all positions set`() {
    val session = ArenaSetupSession(UUID.randomUUID())
    session.pos1 = Location(world, 0.0, 64.0, 0.0)
    session.pos2 = Location(world, 100.0, 100.0, 100.0)
    // Missing spawns

    val errors = session.validate()
    assertTrue(errors.any { it.contains("seeker spawn") })
}
```

### Phase 3: Game Lifecycle (Week 3)

**Goal**: Implement game start, role assignment, and basic game loop

**Tasks**:
1. Create `Game` state machine
2. Implement `GameManager` lifecycle
3. Create `PlayerGameData` and `PlayerBackup`
4. Implement join/leave commands
5. Implement start command with role assignment
6. Write tests for game phases

**Files to Create**:
- `game/Game.kt`
- `game/GameManager.kt`
- `game/GamePhase.kt`
- `game/PlayerRole.kt`
- `game/PlayerGameData.kt`
- `game/PlayerBackup.kt`
- `commands/JoinCommand.kt`
- `commands/LeaveCommand.kt`

**Test First**:
```kotlin
@Test
fun `game should assign roles with correct ratio`() {
    val players = (1..13).map { server.addPlayer() }
    val game = gameManager.createGame(arena, players)

    gameManager.assignRoles(game)

    val seekers = game.getSeekers().size
    val hiders = game.getHiders().size

    assertEquals(3, seekers) // ~23% of 13
    assertEquals(10, hiders) // ~77% of 13
}
```

### Phase 4: Disguise System (Week 4)

**Goal**: Implement block transformation and movement detection

**Tasks**:
1. Create `DisguiseBlock` data class
2. Implement `DisguiseManager` with tracking
3. Create `DisguiseListener` for movement/attacks
4. Integrate with shop system
5. Write tests for disguise mechanics

**Files to Create**:
- `disguise/DisguiseBlock.kt`
- `disguise/DisguiseManager.kt`
- `disguise/DisguiseListener.kt`

**Test First**:
```kotlin
@Test
fun `disguise should be removed on player movement`() {
    val player = server.addPlayer()
    disguiseManager.disguise(player, Material.STONE)

    assertTrue(disguiseManager.isDisguised(player))

    // Simulate movement
    player.simulatePlayerMove(Location(world, 1.0, 0.0, 0.0))

    assertFalse(disguiseManager.isDisguised(player))
}
```

### Phase 5: Shop System (Week 5)

**Goal**: Implement category-based GUI shop

**Tasks**:
1. Create `ShopCategory` and `ShopItem` data classes
2. Implement `ShopManager` for GUI creation
3. Create `ShopListener` for interactions
4. Implement shop item protection
5. Write tests for shop navigation

**Files to Create**:
- `shop/ShopCategory.kt`
- `shop/ShopItem.kt`
- `shop/ShopManager.kt`
- `shop/ShopListener.kt`
- `listeners/InventoryListener.kt`

**Test First**:
```kotlin
@Test
fun `shop should prevent dropping shop item`() {
    val player = server.addPlayer()
    val shopItem = shopManager.createShopItem()

    player.inventory.setItem(8, shopItem)

    val dropEvent = PlayerDropItemEvent(player, shopItem)
    inventoryListener.onPlayerDropItem(dropEvent)

    assertTrue(dropEvent.isCancelled)
}
```

### Phase 6: HUD System (Week 6)

**Goal**: Implement scoreboard, titles, and action bars

**Tasks**:
1. Create `ScoreboardManager` for sidebar
2. Create `TitleManager` for announcements
3. Create `ActionBarManager` for quick feedback
4. Implement update loops
5. Write tests for HUD updates

**Files to Create**:
- `hud/ScoreboardManager.kt`
- `hud/TitleManager.kt`
- `hud/ActionBarManager.kt`

**Test First**:
```kotlin
@Test
fun `scoreboard should show role-specific information`() {
    val hider = server.addPlayer()
    val seeker = server.addPlayer()

    game.players[hider.uniqueId] = PlayerGameData(hider.uniqueId, PlayerRole.HIDER)
    game.players[seeker.uniqueId] = PlayerGameData(seeker.uniqueId, PlayerRole.SEEKER)

    scoreboardManager.updateScoreboard(hider, game)
    scoreboardManager.updateScoreboard(seeker, game)

    val hiderLines = hider.scoreboard.getObjective("hideandseek")!!.getScoreboard().getEntries()
    val seekerLines = seeker.scoreboard.getObjective("hideandseek")!!.getScoreboard().getEntries()

    assertTrue(hiderLines.any { it.contains("偽装") })
    assertTrue(seekerLines.any { it.contains("捕獲数") })
}
```

### Phase 7: Economy Integration (Week 7)

**Goal**: Implement Vault integration for rewards

**Tasks**:
1. Create `RewardManager` with Vault API
2. Implement reward distribution on game end
3. Add async economy transactions
4. Write tests for reward calculations

**Files to Create**:
- `economy/RewardManager.kt`

**Test First**:
```kotlin
@Test
fun `seeker should receive capture bonuses`() {
    val seeker = server.addPlayer()
    val game = createTestGame()
    game.players[seeker.uniqueId] = PlayerGameData(seeker.uniqueId, PlayerRole.SEEKER, captureCount = 5)

    rewardManager.distributeRewards(game, GameResult.SEEKER_WIN)

    // Verify: 1000 (win) + 5*200 (captures) = 2000
    verify(economy).depositPlayer(seeker, 2000.0)
}
```

### Phase 8: WorldBorder & Cleanup (Week 8)

**Goal**: Implement boundary enforcement and game cleanup

**Tasks**:
1. Create `WorldBorderBackup` data class
2. Implement boundary application/restoration
3. Create cleanup routines for game end
4. Write tests for cleanup completeness

**Files to Create**:
- `game/WorldBorderBackup.kt`
- `listeners/WorldBorderListener.kt`

**Test First**:
```kotlin
@Test
fun `game end should restore all player state`() {
    val player = server.addPlayer()
    val originalLocation = player.location.clone()
    val originalInventory = player.inventory.contents.clone()

    gameManager.startGame(arena, listOf(player))
    gameManager.endGame(game, GameResult.HIDER_WIN)

    assertEquals(originalLocation, player.location)
    assertArrayEquals(originalInventory, player.inventory.contents)
}
```

---

## Testing Strategy

### Unit Tests (Fast Feedback)

Use MockBukkit for isolated component testing:

```kotlin
@BeforeEach
fun setUp() {
    server = MockBukkit.mock()
    plugin = MockBukkit.load(HideAndSeekPlugin::class.java)
    world = server.addSimpleWorld("world")
}

@AfterEach
fun tearDown() {
    MockBukkit.unmock()
}
```

### Integration Tests (Full Flow)

Test complete game lifecycle with MockBukkit:

```kotlin
@Test
fun `full game flow should work end to end`() {
    // Setup
    val players = (1..5).map { server.addPlayer() }
    val arena = createTestArena()

    // Join
    players.forEach { gameManager.joinGame(it) }

    // Start
    gameManager.startGame(arena)

    // Play
    val hider = game.getHiders().first()
    val player = server.getPlayer(hider)!!
    disguiseManager.disguise(player, Material.STONE)

    // Capture
    val seeker = game.getSeekers().first()
    disguiseManager.handleCapture(server.getPlayer(seeker)!!, player.location.block)

    // End
    assertTrue(game.checkWinCondition() != null)
}
```

### Manual Testing (Real Server)

1. Build JAR: `./gradlew shadowJar`
2. Copy to Paper server: `cp build/libs/*.jar ~/paper/plugins/`
3. Start server: `cd ~/paper && java -jar paper.jar`
4. Test in-game commands

---

## Debugging Tips

### Enable Debug Logging

Add to `plugin.yml`:
```yaml
logging:
  level: FINE
```

### Common Issues

**Issue**: Events not firing
- Check listener registration in `onEnable()`
- Verify event priority and `ignoreCancelled`

**Issue**: Config not loading
- Check YAML syntax (indentation!)
- Verify `saveDefaultConfig()` called

**Issue**: WorldBorder not working
- Ensure WorldBorder applied to correct world
- Check if another plugin is modifying WorldBorder

---

## Performance Profiling

Use Spark plugin for profiling:

```bash
# In-game command
/spark profiler start
# Play game for 2 minutes
/spark profiler stop
/spark profiler open
```

Target metrics:
- TPS: >19.5 with 30 players
- Event processing: <50ms per event
- Scoreboard updates: <10ms per player

---

## Deployment

### Build Release

```bash
./gradlew clean shadowJar
# Output: build/libs/HideAndSeek-1.0.0-SNAPSHOT.jar
```

### Installation

1. Place JAR in `plugins/` directory
2. Install Vault and economy plugin (e.g., Essentials)
3. Start server to generate configs
4. Edit `plugins/HideAndSeek/config.yml`, `shop.yml`
5. Create arenas with `/hs admin` commands
6. Restart server

### Configuration

Default `config.yml`:
```yaml
game:
  preparation-time: 30
  seek-time: 600
  min-players: 2
  max-players: 30
  seeker-ratio: 0.23

economy:
  enabled: true
  rewards:
    hider-win: 1000
    seeker-win: 1000
    per-capture: 200
    participation: 100
```

---

## Next Steps

After completing Phase 8:
1. Run full test suite: `./gradlew test`
2. Manual QA testing with 5+ players
3. Performance profiling under load
4. Documentation review
5. Create GitHub release

For implementation details, see:
- [Data Model](data-model.md)
- [Command Contracts](contracts/commands.md)
- [Research Decisions](research.md)

---

## Support Resources

- [Paper API Docs](https://jd.papermc.io/paper/1.21/)
- [Vault API Docs](https://github.com/MilkBowl/VaultAPI)
- [MockBukkit Wiki](https://github.com/MockBukkit/MockBukkit)
- [Kotlin Bukkit Guide](https://kotlinlang.org/docs/jvm-get-started.html)
