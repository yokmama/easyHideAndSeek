# Data Model: Disguise System

**Feature**: 001-disguise-system
**Date**: 2025-10-23
**Status**: Complete

## Overview

This document defines the core data entities for the Hide and Seek minigame plugin. All entities are derived from the Key Entities section in spec.md and functional requirements.

---

## Core Entities

### 1. Arena

**Purpose**: Represents a configured game arena with boundaries, spawn points, and metadata.

**Fields**:
- `name: String` - Unique arena identifier (used in commands)
- `displayName: String` - Human-readable name shown to players
- `world: World` - Bukkit World reference
- `boundaries: ArenaBoundaries` - Play area definition
- `spawns: ArenaSpawns` - Player spawn locations

**Relationships**:
- Referenced by `Game` (1:1 - one arena per active game)
- Stored in `arenas.yml` via `ArenaConfig`

**Validation Rules**:
- Name must be unique across all arenas
- World must exist and be loaded
- Boundaries pos1 and pos2 must be in same world
- Spawn points must be within boundaries (validation warning, not error)

**State Transitions**: Immutable (edited by delete + recreate)

```kotlin
data class Arena(
    val name: String,
    val displayName: String,
    val world: World,
    val boundaries: ArenaBoundaries,
    val spawns: ArenaSpawns
) {
    fun isInBounds(location: Location): Boolean {
        return boundaries.contains(location)
    }
}

data class ArenaBoundaries(
    val pos1: Location,
    val pos2: Location
) {
    val center: Location get() = Location(
        pos1.world,
        (pos1.x + pos2.x) / 2,
        (pos1.y + pos2.y) / 2,
        (pos1.z + pos2.z) / 2
    )

    val size: Double get() = pos1.distance(pos2)

    fun contains(location: Location): Boolean {
        val minX = min(pos1.x, pos2.x)
        val maxX = max(pos1.x, pos2.x)
        val minZ = min(pos1.z, pos2.z)
        val maxZ = max(pos1.z, pos2.z)

        return location.x in minX..maxX && location.z in minZ..maxZ
    }
}

data class ArenaSpawns(
    val seeker: Location,  // Includes yaw/pitch
    val hider: Location    // Includes yaw/pitch
)
```

---

### 2. Game

**Purpose**: Represents an active game session with player lists, phase tracking, and state management.

**Fields**:
- `arena: Arena` - Arena being played
- `phase: GamePhase` - Current phase (WAITING, PREPARATION, SEEKING, ENDED)
- `players: Map<UUID, PlayerGameData>` - All participants and their data
- `startTime: Long` - System.currentTimeMillis() when game started
- `phaseStartTime: Long` - When current phase started
- `worldBorderBackup: WorldBorderBackup?` - Original WorldBorder settings

**Relationships**:
- Contains multiple `PlayerGameData` (1:N)
- References one `Arena` (N:1)
- Managed by `GameManager` singleton

**Validation Rules**:
- Minimum 2 players to start
- At least 1 seeker must be assigned
- Cannot start if another game is active (MVP constraint)

**State Transitions**:
```
WAITING → PREPARATION → SEEKING → ENDED
        (start)       (30s)      (win/timeout)
```

```kotlin
data class Game(
    val arena: Arena,
    var phase: GamePhase,
    val players: MutableMap<UUID, PlayerGameData>,
    val startTime: Long,
    var phaseStartTime: Long,
    var worldBorderBackup: WorldBorderBackup? = null
) {
    fun getSeekers(): List<UUID> = players.filter { it.value.role == PlayerRole.SEEKER }.keys.toList()
    fun getHiders(): List<UUID> = players.filter { it.value.role == PlayerRole.HIDER && !it.value.isCaptured }.keys.toList()
    fun getCaptured(): List<UUID> = players.filter { it.value.role == PlayerRole.HIDER && it.value.isCaptured }.keys.toList()

    fun getElapsedTime(): Long = System.currentTimeMillis() - phaseStartTime
    fun getRemainingTime(maxTime: Long): Long = maxTime - getElapsedTime()

    fun checkWinCondition(): GameResult? {
        val remainingHiders = getHiders()

        return when {
            remainingHiders.isEmpty() -> GameResult.SEEKER_WIN
            getRemainingTime(600_000) <= 0 -> GameResult.HIDER_WIN // 10 min default
            else -> null
        }
    }
}

enum class GamePhase {
    WAITING,      // Players joining
    PREPARATION,  // Hiders hiding (30s), seekers blind
    SEEKING,      // Main game phase
    ENDED         // Cleanup and results
}

enum class GameResult {
    SEEKER_WIN,   // All hiders captured
    HIDER_WIN,    // Time expired with hiders remaining
    CANCELLED     // Admin force-stop
}
```

---

### 3. PlayerGameData

**Purpose**: Stores per-player game state including role, captures, and backup data.

**Fields**:
- `uuid: UUID` - Player unique ID
- `role: PlayerRole` - HIDER, SEEKER, or SPECTATOR
- `isCaptured: Boolean` - For hiders only
- `captureCount: Int` - For seekers only
- `backup: PlayerBackup?` - Pre-game state for restoration

**Relationships**:
- Owned by `Game` (N:1)
- References `PlayerBackup` (1:1)

**Validation Rules**:
- UUID must correspond to online player (checked on access)
- Spectators cannot have isCaptured or captureCount
- Backup must exist for restoration (or fallback to spawn)

```kotlin
data class PlayerGameData(
    val uuid: UUID,
    var role: PlayerRole,
    var isCaptured: Boolean = false,
    var captureCount: Int = 0,
    var backup: PlayerBackup? = null
) {
    fun capture() {
        require(role == PlayerRole.HIDER) { "Only hiders can be captured" }
        isCaptured = true
        role = PlayerRole.SPECTATOR
    }

    fun recordCapture() {
        require(role == PlayerRole.SEEKER) { "Only seekers can record captures" }
        captureCount++
    }
}

enum class PlayerRole {
    HIDER,      // Hiding from seekers
    SEEKER,     // Searching for hiders
    SPECTATOR   // Captured or observing
}
```

---

### 4. PlayerBackup

**Purpose**: Stores player state before game start for restoration after game end.

**Fields**:
- `inventory: Array<ItemStack?>` - Main inventory contents
- `armor: Array<ItemStack?>` - Armor slots
- `location: Location` - Pre-game position
- `gameMode: GameMode` - Pre-game game mode
- `scoreboard: Scoreboard?` - Previous scoreboard (if any)

**Relationships**:
- Owned by `PlayerGameData` (1:1)

**Validation Rules**:
- Arrays must preserve null slots (empty inventory positions)
- Location world must still exist on restore (fallback to spawn)

```kotlin
data class PlayerBackup(
    val inventory: Array<ItemStack?>,
    val armor: Array<ItemStack?>,
    val location: Location,
    val gameMode: GameMode,
    val scoreboard: Scoreboard?
) {
    fun restore(player: Player) {
        player.inventory.contents = inventory.clone()
        player.inventory.armorContents = armor.clone()
        player.gameMode = gameMode

        // Safe teleport (check if location still valid)
        if (location.world != null && location.isChunkLoaded) {
            player.teleport(location)
        } else {
            player.teleport(location.world?.spawnLocation ?: player.world.spawnLocation)
        }

        if (scoreboard != null) {
            player.scoreboard = scoreboard
        }
    }
}
```

---

### 5. DisguiseBlock

**Purpose**: Represents a placed block that conceals a hider player.

**Fields**:
- `location: Location` - Block position in world
- `material: Material` - Block type (STONE, DIRT, etc.)
- `playerUuid: UUID` - Owning player
- `timestamp: Long` - Creation time

**Relationships**:
- Owned by `DisguiseManager` tracking (N:1)
- References player via UUID

**Validation Rules**:
- Location must be currently AIR or valid placeholder block
- Player must be a HIDER role
- Only one disguise per player at a time
- Only one player per location

```kotlin
data class DisguiseBlock(
    val location: Location,
    val material: Material,
    val playerUuid: UUID,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun place() {
        location.block.type = material
    }

    fun remove() {
        location.block.type = Material.AIR
    }

    fun isValid(): Boolean {
        // Check if block hasn't been modified
        return location.block.type == material
    }
}
```

---

### 6. ShopCategory

**Purpose**: Groups shop items into categories (e.g., "Disguise Blocks", "Power-ups").

**Fields**:
- `id: String` - Category ID (used in config)
- `displayName: String` - Shown in GUI
- `icon: Material` - Display icon in category menu
- `slot: Int` - Position in main shop menu (0-53)
- `items: List<ShopItem>` - Items in this category

**Relationships**:
- Contains multiple `ShopItem` (1:N)
- Loaded from `shop.yml`

**Validation Rules**:
- ID must be unique
- Slot must be valid inventory position (0-53)
- Items list can be empty (category exists but no items yet)

```kotlin
data class ShopCategory(
    val id: String,
    val displayName: String,
    val icon: Material,
    val slot: Int,
    val items: List<ShopItem>
)
```

---

### 7. ShopItem

**Purpose**: Represents a purchasable/selectable item in the shop.

**Fields**:
- `id: String` - Item ID
- `material: Material` - Display/function material
- `displayName: String` - Item name in GUI
- `lore: List<String>` - Description lines
- `slot: Int` - Position in category GUI
- `price: Int` - Cost in currency (0 for free items like disguise blocks)
- `action: ShopAction` - What happens on click

**Relationships**:
- Owned by `ShopCategory` (N:1)

**Validation Rules**:
- Slot must be valid (0-53)
- Price >= 0
- Material must be valid item

```kotlin
data class ShopItem(
    val id: String,
    val material: Material,
    val displayName: String,
    val lore: List<String>,
    val slot: Int,
    val price: Int,
    val action: ShopAction
)

sealed class ShopAction {
    data class Disguise(val blockType: Material) : ShopAction()
    data class PurchaseItem(val itemType: ItemType) : ShopAction()
    object GoBack : ShopAction()
    object Close : ShopAction()
}

enum class ItemType {
    ESCAPE_PASS,    // Future: 見逃しアイテム
    SPEED_BOOST,    // Future: スピードブースト
    COMPASS         // Future: コンパス
}
```

---

### 8. WorldBorderBackup

**Purpose**: Stores original WorldBorder settings for restoration after game.

**Fields**:
- `center: Location` - Original center point
- `size: Double` - Original border size
- `damageAmount: Double` - Damage per second
- `damageBuffer: Double` - Distance inside border before damage
- `warningDistance: Int` - Distance for visual warning
- `warningTime: Int` - Time before border moves (0 for stationary)

**Relationships**:
- Owned by `Game` (1:1)

**Validation Rules**:
- All fields captured before modification
- Fallback to Minecraft defaults if backup fails

```kotlin
data class WorldBorderBackup(
    val center: Location,
    val size: Double,
    val damageAmount: Double,
    val damageBuffer: Double,
    val warningDistance: Int,
    val warningTime: Int
) {
    fun restore(world: World) {
        val border = world.worldBorder
        border.center = center
        border.size = size
        border.damageAmount = damageAmount
        border.damageBuffer = damageBuffer
        border.warningDistance = warningDistance
        border.warningTime = warningTime
    }

    companion object {
        fun capture(world: World): WorldBorderBackup {
            val border = world.worldBorder
            return WorldBorderBackup(
                border.center,
                border.size,
                border.damageAmount,
                border.damageBuffer,
                border.warningDistance,
                border.warningTime
            )
        }

        fun defaultBackup(world: World): WorldBorderBackup {
            return WorldBorderBackup(
                world.spawnLocation,
                29999984.0, // Minecraft default
                0.2,
                5.0,
                5,
                15
            )
        }
    }
}
```

---

### 9. ArenaSetupSession

**Purpose**: Temporary state for admin creating an arena (position selection).

**Fields**:
- `adminUuid: UUID` - Admin executing setup
- `pos1: Location?` - First corner
- `pos2: Location?` - Second corner
- `seekerSpawn: Location?` - Seeker spawn point
- `hiderSpawn: Location?` - Hider spawn point

**Relationships**:
- Managed by `ArenaManager` (ephemeral, not persisted)

**Validation Rules**:
- All positions must be set before creating arena
- Positions must be in same world

```kotlin
data class ArenaSetupSession(
    val adminUuid: UUID,
    var pos1: Location? = null,
    var pos2: Location? = null,
    var seekerSpawn: Location? = null,
    var hiderSpawn: Location? = null
) {
    fun isComplete(): Boolean {
        return pos1 != null && pos2 != null && seekerSpawn != null && hiderSpawn != null
    }

    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (pos1 == null) errors.add("pos1 が設定されていません")
        if (pos2 == null) errors.add("pos2 が設定されていません")
        if (seekerSpawn == null) errors.add("seeker spawn が設定されていません")
        if (hiderSpawn == null) errors.add("hider spawn が設定されていません")

        // Check same world
        val worlds = listOfNotNull(pos1?.world, pos2?.world, seekerSpawn?.world, hiderSpawn?.world).distinct()
        if (worlds.size > 1) {
            errors.add("すべての位置は同じワールドに設定してください (worlds: ${worlds.joinToString()})")
        }

        return errors
    }

    fun toArena(name: String, displayName: String): Arena {
        require(isComplete()) { "Setup session not complete" }
        require(validate().isEmpty()) { "Validation errors: ${validate()}" }

        return Arena(
            name = name,
            displayName = displayName,
            world = pos1!!.world!!,
            boundaries = ArenaBoundaries(pos1!!, pos2!!),
            spawns = ArenaSpawns(seekerSpawn!!, hiderSpawn!!)
        )
    }
}
```

---

## Entity Lifecycle Summary

| Entity | Created | Modified | Destroyed |
|--------|---------|----------|-----------|
| Arena | `/hs admin creategame` | Never (delete + recreate) | `/hs admin delete` |
| Game | `/hs start` | Phase transitions | Game end or `/hs stop` |
| PlayerGameData | Player joins game | Capture, role change | Game end |
| PlayerBackup | Game start | Never | Restored on game end |
| DisguiseBlock | Player selects from shop | Never | Movement or capture |
| ShopCategory | Plugin load (shop.yml) | Config reload | Plugin disable |
| ShopItem | Plugin load (shop.yml) | Config reload | Plugin disable |
| WorldBorderBackup | Game start | Never | Restored on game end |
| ArenaSetupSession | `/hs admin setpos1` | Position commands | Arena creation or timeout |

---

## Persistence Strategy

### YAML Storage (arenas.yml)

```yaml
arenas:
  main-arena:
    display-name: "メインアリーナ"
    world: "world"
    boundaries:
      pos1: {x: 0, y: 64, z: 0}
      pos2: {x: 100, y: 100, z: 100}
    spawns:
      seeker: {x: 50.0, y: 64.0, z: 50.0, yaw: 0.0, pitch: 0.0}
      hider: {x: 50.0, y: 64.0, z: 80.0, yaw: 180.0, pitch: 0.0}
```

### In-Memory Only

- Game
- PlayerGameData
- PlayerBackup
- DisguiseBlock
- WorldBorderBackup
- ArenaSetupSession

These entities exist only during runtime and are not persisted.

---

## Data Access Patterns

### Managers (Singleton Services)

- `ArenaManager`: CRUD operations for arenas
- `GameManager`: Game lifecycle, player tracking
- `DisguiseManager`: Disguise block tracking, placement/removal
- `ShopManager`: GUI creation, category/item access
- `ScoreboardManager`: Per-player scoreboard updates

### Queries

- Get active game: `GameManager.activeGame: Game?`
- Get player's game: `GameManager.getGame(player): Game?`
- Is player disguised: `DisguiseManager.isDisguised(player): Boolean`
- Get disguise at location: `DisguiseManager.getDisguise(location): DisguiseBlock?`
- Get arena by name: `ArenaManager.getArena(name): Arena?`

---

## Conclusion

All entities are defined with fields, relationships, validations, and state transitions. Data model is ready for implementation and aligns with all functional requirements from spec.md.
