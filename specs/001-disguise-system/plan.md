# Implementation Plan: Disguise System (Hide and Seek Game)

**Branch**: `001-disguise-system` | **Date**: 2025-10-23 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-disguise-system/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

A complete Hide and Seek minigame plugin for Minecraft Paper 1.21.x featuring:
- **Disguise System**: Hiders transform into blocks by selecting from shop GUI, remaining invisible until they move
- **Shop System**: Category-based GUI with configurable items (shop.yml), non-droppable shop item
- **Game Lifecycle**: Arena setup (WorldEdit-style pos selection), player join/start, role assignment (23% seekers), inventory backup/restoration, WorldBorder boundaries
- **HUD System**: Real-time scoreboard, title announcements, action bar feedback
- **Economy Integration**: Vault-based rewards for wins, captures, and participation

Technical approach: Event-driven Minecraft plugin architecture using Paper API, YAML configuration, Vault economy integration, and WorldBorder API for boundary enforcement.

## Technical Context

**Language/Version**: Kotlin 1.9+ (JVM target 21) or Java 21
**Primary Dependencies**:
- Paper API 1.21.x (Minecraft server platform)
- Vault API (economy integration, required)
- PlaceholderAPI (optional, for placeholders)
- YAML configuration (Bukkit ConfigurationSection)

**Storage**:
- YAML files (arenas.yml, shop.yml, config.yml)
- In-memory game state (HashMap-based tracking)
- No database required (stats.yml for persistence if needed)

**Testing**:
- JUnit 5 for unit tests
- MockBukkit for Bukkit/Paper API mocking
- Integration tests with test server (Paper test harness)

**Target Platform**:
- Minecraft Paper 1.21.x server
- Java 21+ runtime
- Multi-threaded server environment (20 TPS game loop)

**Project Type**: Single Minecraft plugin (JAR artifact)

**Performance Goals**:
- Handle 30 concurrent players without TPS drop (<20 TPS)
- Event processing <50ms per event
- Scoreboard updates 1/second without lag
- WorldBorder calculations <10ms

**Constraints**:
- Must run on main server thread for Bukkit API calls
- Async tasks for economy operations (Vault)
- No block persistence beyond game session
- Memory efficient: <50MB for active game state

**Scale/Scope**:
- Support 2-30 players per game
- Single arena active at a time (MVP)
- ~15-20 event listeners
- ~5,000-8,000 LOC estimated

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Status**: ✅ PASS (No constitution file defined - using Minecraft plugin best practices)

Since no project constitution exists, applying standard Minecraft plugin development principles:

1. **Event-Driven Architecture**: ✅ Plugin uses Bukkit event system
2. **Thread Safety**: ✅ Main thread for API calls, async for economy
3. **Configuration-Driven**: ✅ YAML configs for all customizable behavior
4. **No Database Dependency**: ✅ YAML file storage only
5. **Vault Integration Standard**: ✅ Using standard Vault economy API

**Note**: Project constitution template exists but is not filled. Proceeding with Minecraft plugin conventions.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
src/main/
├── kotlin/ (or java/)
│   └── com/hideandseek/
│       ├── HideAndSeekPlugin.kt           # Main plugin class (JavaPlugin)
│       ├── commands/                       # Command handlers
│       │   ├── AdminCommand.kt             # /hs admin subcommands
│       │   ├── JoinCommand.kt              # /hs join
│       │   └── LeaveCommand.kt             # /hs leave
│       ├── game/                           # Core game logic
│       │   ├── Game.kt                     # Game state machine
│       │   ├── GameManager.kt              # Game lifecycle manager
│       │   ├── GamePhase.kt                # Enum: WAITING, PREPARATION, SEEKING, ENDED
│       │   └── PlayerRole.kt               # Enum: HIDER, SEEKER, SPECTATOR
│       ├── arena/                          # Arena management
│       │   ├── Arena.kt                    # Arena data model
│       │   ├── ArenaManager.kt             # CRUD operations
│       │   └── ArenaSetup.kt               # Position selection state
│       ├── disguise/                       # Disguise system
│       │   ├── DisguiseManager.kt          # Disguise tracking
│       │   ├── DisguiseBlock.kt            # Disguise block entity
│       │   └── DisguiseListener.kt         # Movement/attack events
│       ├── shop/                           # Shop system
│       │   ├── ShopManager.kt              # Shop GUI management
│       │   ├── ShopItem.kt                 # Shop item entity
│       │   └── ShopListener.kt             # Item interaction events
│       ├── hud/                            # HUD system
│       │   ├── ScoreboardManager.kt        # Sidebar scoreboard
│       │   ├── TitleManager.kt             # Title announcements
│       │   └── ActionBarManager.kt         # Action bar messages
│       ├── economy/                        # Economy integration
│       │   └── RewardManager.kt            # Vault integration
│       ├── config/                         # Configuration
│       │   ├── ConfigManager.kt            # Config loader
│       │   ├── ArenaConfig.kt              # arenas.yml handler
│       │   └── ShopConfig.kt               # shop.yml handler
│       ├── listeners/                      # Event listeners
│       │   ├── PlayerJoinListener.kt       # Join/quit handling
│       │   ├── InventoryListener.kt        # Inventory protection
│       │   └── WorldBorderListener.kt      # Boundary enforcement
│       └── utils/                          # Utilities
│           ├── MessageUtil.kt              # Colored messages
│           ├── ItemBuilder.kt              # ItemStack builder
│           └── TaskScheduler.kt            # Bukkit scheduler wrapper
│
└── resources/
    ├── plugin.yml                          # Plugin metadata
    ├── config.yml                          # Default configuration
    ├── arenas.yml                          # Arena definitions (created on first run)
    ├── shop.yml                            # Shop configuration (created on first run)
    └── messages.yml                        # Localized messages (optional)

src/test/
├── kotlin/ (or java/)
│   └── com/hideandseek/
│       ├── game/                           # Game logic tests
│       │   ├── GameTest.kt
│       │   └── GameManagerTest.kt
│       ├── arena/                          # Arena tests
│       │   └── ArenaManagerTest.kt
│       ├── disguise/                       # Disguise tests
│       │   └── DisguiseManagerTest.kt
│       └── integration/                    # Integration tests
│           ├── GameFlowTest.kt             # Full game lifecycle
│           └── ShopIntegrationTest.kt      # Shop + disguise flow
│
└── resources/
    └── test-config.yml                     # Test configuration

build.gradle.kts (or pom.xml)               # Build configuration
```

**Structure Decision**: Standard Minecraft plugin structure using Kotlin (or Java) with Maven/Gradle build system. Single JAR artifact deployed to server plugins directory. Package structure follows domain-driven design with clear separation of concerns: game logic, arena management, disguise system, shop, HUD, and economy integration.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

**Status**: ✅ No violations - standard Minecraft plugin architecture with appropriate separation of concerns.

---

## Phase 0 Complete: Research

**Output**: [research.md](research.md)

**Key Decisions**:
- Language: Kotlin 1.9+ (JVM 21)
- Build: Gradle with Kotlin DSL
- Testing: MockBukkit + JUnit 5
- Scoreboard: Bukkit API (no packets)
- WorldBorder: Global with backup/restore
- Disguise: Real blocks with HashMap tracking
- Movement: PlayerMoveEvent with cause checking
- Shop: Bukkit Inventory API
- Economy: Vault async transactions
- Config: Multiple YAML files

All NEEDS CLARIFICATION items resolved. Ready for implementation.

---

## Phase 1 Complete: Design & Contracts

**Outputs**:
- [data-model.md](data-model.md) - 9 core entities defined
- [contracts/commands.md](contracts/commands.md) - Complete command API
- [quickstart.md](quickstart.md) - 8-phase development guide

**Key Entities**:
- Arena (boundaries, spawns)
- Game (state machine, player tracking)
- PlayerGameData (role, captures, backup)
- DisguiseBlock (location, material, player)
- ShopCategory & ShopItem (GUI navigation)
- WorldBorderBackup (restoration data)

**API Surface**:
- Player commands: `/hs join`, `/hs leave`, `/hs shop`
- Admin commands: 9 commands for arena setup, game control
- Events: 15-20 listeners for game mechanics

---

## Implementation Roadmap

### Phase 1: Core Infrastructure (Week 1)
- Plugin skeleton
- Configuration system (config.yml, arenas.yml, shop.yml)
- Basic command registration

### Phase 2: Arena Management (Week 2)
- Arena data model
- Position selection (WorldEdit-style)
- Arena CRUD operations

### Phase 3: Game Lifecycle (Week 3)
- Game state machine
- Join/leave/start commands
- Role assignment (23% seekers)
- Inventory backup/restore

### Phase 4: Disguise System (Week 4)
- Block placement/removal
- Invisibility management
- Movement detection
- Capture mechanics

### Phase 5: Shop System (Week 5)
- GUI category navigation
- Shop item protection
- Disguise block selection

### Phase 6: HUD System (Week 6)
- Scoreboard updates (1Hz)
- Title announcements
- Action bar messages

### Phase 7: Economy Integration (Week 7)
- Vault API integration
- Async reward distribution
- Win/capture bonuses

### Phase 8: WorldBorder & Cleanup (Week 8)
- Boundary enforcement
- Complete game cleanup
- State restoration

**Estimated Total**: 8 weeks for full implementation with testing

---

## Next Steps

1. **Review**: Stakeholder approval of design artifacts
2. **Tasks**: Run `/speckit.tasks` to generate dependency-ordered task list
3. **Implementation**: Begin Phase 1 (Core Infrastructure)
4. **Testing**: TDD approach - write tests before implementation
