# Research: Disguise System Implementation

**Feature**: 001-disguise-system
**Date**: 2025-10-23
**Status**: Complete

## Overview

This document consolidates technical research for implementing the Hide and Seek minigame plugin for Minecraft Paper 1.21.x. All "NEEDS CLARIFICATION" items from the Technical Context have been resolved through research into Minecraft plugin best practices, Paper API capabilities, and ecosystem standards.

## Technology Decisions

### 1. Programming Language: Kotlin vs Java

**Decision**: **Kotlin 1.9+ targeting JVM 21**

**Rationale**:
- Modern language features: null safety, data classes, extension functions
- Concise syntax reduces boilerplate (especially for Bukkit event listeners)
- Full Java interoperability - can use all Bukkit/Paper APIs
- Kotlin coroutines for async operations (Vault calls)
- Industry trend: Many modern Minecraft plugins migrating to Kotlin

**Alternatives Considered**:
- **Java 21**: More traditional, larger codebase, verbose event handlers
  - Rejected: Kotlin provides better developer experience without compatibility issues
- **Scala**: Too complex, slow compilation
  - Rejected: Overkill for plugin development

**Implementation Notes**:
- Use Kotlin data classes for entities (Arena, DisguiseBlock, etc.)
- Extension functions for Bukkit API (e.g., `Player.sendColoredMessage()`)
- Sealed classes for game state (GamePhase enum with behavior)

---

### 2. Build System: Gradle vs Maven

**Decision**: **Gradle with Kotlin DSL (build.gradle.kts)**

**Rationale**:
- Kotlin DSL provides type-safe build configuration
- Faster builds than Maven for incremental compilation
- Better dependency management with version catalogs
- Shadow plugin for fat JAR with dependencies
- Industry standard for modern Minecraft plugins

**Alternatives Considered**:
- **Maven**: XML configuration, slower builds
  - Rejected: Gradle provides better Kotlin support and modern tooling

**Implementation Notes**:
```kotlin
plugins {
    kotlin("jvm") version "1.9.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("net.milkbowl.vault:VaultAPI:1.7")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.20:3.9.0")
}
```

---

### 3. Testing Framework: MockBukkit vs Paper Test Server

**Decision**: **MockBukkit for unit tests + Paper test harness for integration**

**Rationale**:
- MockBukkit mocks Bukkit API for fast unit testing
- No need for full server startup in unit tests
- Paper test harness for end-to-end integration tests
- JUnit 5 for assertions and test structure

**Alternatives Considered**:
- **Full server tests only**: Too slow for TDD workflow
  - Rejected: Need fast feedback loop for unit tests

**Implementation Notes**:
```kotlin
@Test
fun `disguise should be removed when player moves`() {
    val server = MockBukkit.mock()
    val plugin = MockBukkit.load(HideAndSeekPlugin::class.java)
    val player = server.addPlayer()

    // Test disguise logic
    disguiseManager.disguise(player, Material.STONE)
    player.simulateMovement(Location(world, 1.0, 0.0, 0.0))

    assertFalse(disguiseManager.isDisguised(player))
}
```

---

### 4. Scoreboard Implementation: Bukkit Scoreboard vs Packets

**Decision**: **Bukkit Scoreboard API**

**Rationale**:
- Built-in API with team/objective support
- No need for protocol hacks or packet manipulation
- Compatible with other plugins (can backup/restore)
- Sufficient for 1 Hz update rate requirement

**Alternatives Considered**:
- **Direct packets (ProtocolLib)**: More control but unnecessary complexity
  - Rejected: Bukkit API meets all requirements

**Implementation Notes**:
```kotlin
class ScoreboardManager(private val plugin: Plugin) {
    private val scoreboards = mutableMapOf<UUID, Scoreboard>()

    fun createGameScoreboard(player: Player, game: Game) {
        val scoreboard = Bukkit.getScoreboardManager().newScoreboard
        val objective = scoreboard.registerNewObjective(
            "hideandseek",
            "dummy",
            Component.text("===[ Hide and Seek ]===")
        )
        objective.displaySlot = DisplaySlot.SIDEBAR
        // Set lines...
        player.scoreboard = scoreboard
    }
}
```

---

### 5. WorldBorder Management: Per-Arena vs Global

**Decision**: **Global WorldBorder with backup/restore**

**Rationale**:
- Minecraft limitation: 1 WorldBorder per world
- Specification requires single arena at a time (MVP)
- Backup original settings, apply arena settings, restore on end
- Simple implementation without workarounds

**Alternatives Considered**:
- **Virtual boundaries (cancel movement)**: No visual feedback
  - Rejected: WorldBorder provides built-in visual warning and damage
- **Per-world arenas**: Requires world copying/management
  - Rejected: Excessive complexity for MVP

**Implementation Notes**:
```kotlin
data class WorldBorderBackup(
    val center: Location,
    val size: Double,
    val damageAmount: Double,
    val damageBuffer: Double,
    val warningDistance: Int,
    val warningTime: Int
)

fun applyArenaBorder(arena: Arena) {
    val world = arena.world
    val border = world.worldBorder

    // Backup current settings
    val backup = WorldBorderBackup(
        border.center,
        border.size,
        border.damageAmount,
        border.damageBuffer,
        border.warningDistance,
        border.warningTime
    )

    // Apply arena settings
    border.center = arena.center
    border.size = arena.size
    border.damageAmount = 0.2 // Minecraft default
    border.warningDistance = 5
}
```

---

### 6. Disguise Block Storage: Block vs Entity

**Decision**: **Real block placement with HashMap tracking**

**Rationale**:
- Spec requires: "ブロックが設置される" (block is placed)
- Real blocks provide automatic collision/interaction
- HashMap<Location, UUID> maps block location to player
- BlockDamageEvent for click detection

**Alternatives Considered**:
- **Armor stands**: Visible entities, not true blocks
  - Rejected: Doesn't match spec requirement for block placement
- **Falling blocks**: Complex, physics issues
  - Rejected: Unnecessary complexity

**Implementation Notes**:
```kotlin
class DisguiseManager {
    private val disguises = mutableMapOf<Location, UUID>()
    private val playerDisguises = mutableMapOf<UUID, DisguiseBlock>()

    fun disguise(player: Player, blockType: Material) {
        val location = player.location.block.location

        // Place block
        location.block.type = blockType

        // Track disguise
        val disguise = DisguiseBlock(location, blockType, player.uniqueId)
        disguises[location] = player.uniqueId
        playerDisguises[player.uniqueId] = disguise

        // Apply invisibility
        player.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false))
    }
}
```

---

### 7. Movement Detection: Distance vs Event Analysis

**Decision**: **PlayerMoveEvent with move cause checking**

**Rationale**:
- Event provides `from` and `to` locations
- Can differentiate player input from external forces (knockback)
- Check if X/Z changed (ignore Y for slight ground variations)
- Cancel teleport events to prevent breaking disguise

**Alternatives Considered**:
- **Distance threshold**: Can't distinguish player vs external
  - Rejected: Spec requires "WASD only" breaks disguise

**Implementation Notes**:
```kotlin
@EventHandler
fun onPlayerMove(event: PlayerMoveEvent) {
    val player = event.player
    if (!disguiseManager.isDisguised(player)) return

    val from = event.from
    val to = event.to ?: return

    // Check horizontal movement only
    if (from.x != to.x || from.z != to.z) {
        // Player initiated movement
        disguiseManager.removeDisguise(player, "移動により解除")
    }
}
```

---

### 8. Shop GUI: Inventory vs Custom GUI

**Decision**: **Bukkit Inventory API with custom holder**

**Rationale**:
- Native Minecraft inventory UI
- Two-tier navigation: main menu → category → items
- InventoryHolder pattern for state tracking
- InventoryClickEvent for interactions

**Alternatives Considered**:
- **Chest GUI libraries**: External dependency
  - Rejected: Bukkit API sufficient for requirements

**Implementation Notes**:
```kotlin
class ShopGUI(private val player: Player, private val category: ShopCategory?) : InventoryHolder {
    private val inventory: Inventory

    init {
        inventory = if (category == null) {
            // Main menu
            Bukkit.createInventory(this, 54, Component.text("ショップメニュー"))
        } else {
            // Category view
            Bukkit.createInventory(this, 54, Component.text(category.displayName))
        }
        populateItems()
    }

    override fun getInventory() = inventory
}

@EventHandler
fun onInventoryClick(event: InventoryClickEvent) {
    val holder = event.inventory.holder
    if (holder !is ShopGUI) return

    event.isCancelled = true
    // Handle click...
}
```

---

### 9. Economy Integration: Vault API Pattern

**Decision**: **Vault Economy service with async transactions**

**Rationale**:
- Industry standard for Minecraft economy
- Supports multiple economy plugins (Essentials, etc.)
- Async operations to avoid blocking main thread
- Null-safe provider checking

**Alternatives Considered**:
- **Direct plugin integration**: Couples to specific economy plugin
  - Rejected: Vault provides abstraction

**Implementation Notes**:
```kotlin
class RewardManager(private val plugin: Plugin) {
    private val economy: Economy? = plugin.server.servicesManager
        .getRegistration(Economy::class.java)?.provider

    fun giveReward(player: Player, amount: Double, reason: String) {
        if (economy == null) {
            plugin.logger.warning("Economy not available for reward: $amount to ${player.name}")
            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            economy.depositPlayer(player, amount)
            Bukkit.getScheduler().runTask(plugin, Runnable {
                player.sendMessage("§a+${amount}コイン (${reason})")
            })
        })
    }
}
```

---

### 10. Configuration: Single YAML vs Multiple Files

**Decision**: **Multiple YAML files (config.yml, arenas.yml, shop.yml)**

**Rationale**:
- Separation of concerns: game settings vs arena data vs shop config
- arenas.yml: runtime-modified (arena creation commands)
- shop.yml: admin-configured (block types, prices)
- config.yml: server settings (timings, rewards)

**Alternatives Considered**:
- **Single config.yml**: Hard to manage, merge conflicts
  - Rejected: Spec implies separate configurations

**Implementation Notes**:
```kotlin
class ConfigManager(private val plugin: Plugin) {
    lateinit var config: FileConfiguration
    lateinit var arenas: FileConfiguration
    lateinit var shop: FileConfiguration

    fun load() {
        plugin.saveDefaultConfig()
        config = plugin.config

        arenas = loadOrCreate("arenas.yml")
        shop = loadOrCreate("shop.yml")
    }

    private fun loadOrCreate(fileName: String): FileConfiguration {
        val file = File(plugin.dataFolder, fileName)
        if (!file.exists()) {
            plugin.saveResource(fileName, false)
        }
        return YamlConfiguration.loadConfiguration(file)
    }
}
```

---

## Performance Considerations

### Event Processing Optimization

**Research Finding**: Event listeners are the main performance bottleneck in Minecraft plugins.

**Best Practices**:
1. Use `EventPriority.MONITOR` for logging only
2. Minimize work in HIGHEST priority (last chance to cancel)
3. Use `@EventHandler(ignoreCancelled = true)` where appropriate
4. Cache frequently accessed data (don't query every event)

**Implementation Strategy**:
```kotlin
@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
fun onPlayerMove(event: PlayerMoveEvent) {
    // Early return if player not in game
    val player = event.player
    if (!gameManager.isInGame(player)) return

    // Cached lookup instead of iteration
    val game = gameManager.getGame(player) ?: return

    // Process event...
}
```

### Scoreboard Update Batching

**Research Finding**: Individual scoreboard updates can cause packet spam.

**Best Practice**: Batch updates on a 1-second ticker.

**Implementation Strategy**:
```kotlin
class ScoreboardManager(private val plugin: Plugin) {
    private var updateTask: BukkitTask? = null

    fun startUpdates(game: Game) {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            game.players.forEach { updateScoreboard(it, game) }
        }, 0L, 20L) // Every 1 second (20 ticks)
    }
}
```

---

## Security Considerations

### Command Permission Bypass

**Research Finding**: Players can exploit command aliases or tab completion.

**Best Practice**: Always check permissions in command executors, not just plugin.yml.

**Implementation Strategy**:
```kotlin
class AdminCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!sender.hasPermission("hideandseek.admin")) {
            sender.sendMessage("§c権限がありません")
            return true
        }
        // Process command...
    }
}
```

### Inventory Duplication

**Research Finding**: Players can duplicate items by exploiting inventory save/restore.

**Best Practice**: Clear inventory after backup, restore from backup (don't trust current inventory).

**Implementation Strategy**:
```kotlin
data class PlayerBackup(
    val inventory: Array<ItemStack?>,
    val armor: Array<ItemStack?>,
    val location: Location,
    val gameMode: GameMode
)

fun backup(player: Player): PlayerBackup {
    val backup = PlayerBackup(
        player.inventory.contents.clone(),
        player.inventory.armorContents.clone(),
        player.location.clone(),
        player.gameMode
    )

    // Clear immediately to prevent duplication
    player.inventory.clear()

    return backup
}
```

---

## Integration Points

### Vault Economy

**Service Registration**:
```kotlin
val rsp = Bukkit.getServicesManager().getRegistration(Economy::class.java)
if (rsp == null) {
    logger.severe("Vault economy not found! Rewards disabled.")
    // Continue without economy (optional feature)
}
```

### PlaceholderAPI (Optional)

**Placeholder Expansion**:
```kotlin
class HideAndSeekExpansion(private val plugin: HideAndSeekPlugin) : PlaceholderExpansion() {
    override fun getIdentifier() = "hideandseek"
    override fun getAuthor() = "YourName"
    override fun getVersion() = plugin.description.version

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return null

        return when (params) {
            "role" -> gameManager.getRole(player)?.name ?: "なし"
            "disguise" -> disguiseManager.getDisguiseBlock(player)?.material?.name ?: "なし"
            else -> null
        }
    }
}
```

---

## Conclusion

All technical decisions have been researched and documented. The implementation will use:

- **Language**: Kotlin 1.9+ (JVM 21)
- **Build**: Gradle with Kotlin DSL
- **Testing**: MockBukkit + JUnit 5
- **Architecture**: Event-driven with domain separation
- **Storage**: YAML configuration files
- **Economy**: Vault API (optional)
- **Performance**: Event optimization, batched updates
- **Security**: Permission checks, inventory protection

No NEEDS CLARIFICATION items remain. Ready to proceed to Phase 1 (Design & Contracts).
