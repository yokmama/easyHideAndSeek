# Implementation Plan: Shop Items Review and Rebalance

**Branch**: `004-shop-items-review` | **Date**: 2025-11-04 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-shop-items-review/spec.md`

## Summary

Review and rebalance the Hide and Seek shop items system to improve strategic depth, pricing fairness, and item variety. Key innovations include:

1. **Camouflage-based pricing**: Disguise blocks priced by concealment effectiveness (visible blocks free, camouflaged blocks expensive)
2. **Enhanced item variety**: New items supporting different playstyles (mobile vs defensive Hiders, pursuit vs area-control Seekers)
3. **Purchase restrictions**: Per-player and team-wide limits to prevent dominant strategies
4. **Dynamic economy**: Items remain relevant throughout match phases (early, mid, late game)
5. **Clear value communication**: Improved tooltips and feedback for informed purchasing decisions

Technical approach: Extend existing ShopItem/ShopManager system with configurable camouflage tiers, enhanced purchase tracking, and new item handlers for expanded item types.

## Technical Context

**Language/Version**: Kotlin 2.2.21 / JVM 21
**Primary Dependencies**: Paper API 1.21.5, Vault API 1.7, Kotlin stdlib-jdk8
**Storage**: YAML configuration files (config.yml, shop.yml), in-memory state management during games
**Testing**: JUnit 5, MockBukkit for Bukkit API mocking, kotlin-test-junit5
**Target Platform**: Paper 1.21.5 Minecraft server (Java 21+)
**Project Type**: Single Minecraft plugin project
**Performance Goals**:
- Shop GUI load < 500ms for 30 concurrent players
- Purchase transaction < 100ms
- No TPS drop during shop operations
**Constraints**:
- Must integrate with existing Vault economy system
- Must maintain compatibility with existing game loop and disguise system
- Config changes must be hot-reloadable without server restart
**Scale/Scope**:
- 10-30 concurrent players per game
- 15-20 total shop items (10 Hider, 10 Seeker)
- Support for multiple arenas with different shop configurations

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Status**: N/A - Constitution file is currently a template without defined principles

**Note**: Project does not have ratified constitution yet. Using standard Minecraft plugin development best practices:
- Minimize dependencies (Paper API + Vault only)
- Use data-driven configuration (YAML)
- Event-driven architecture for game interactions
- In-memory state for active games (no persistent storage required for shop transactions)

## Project Structure

### Documentation (this feature)

```text
specs/004-shop-items-review/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── shop-config-schema.yml
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
src/main/kotlin/com/hideandseek/
├── shop/
│   ├── ShopItem.kt              # [EXTEND] Add camouflage tier field
│   ├── ShopManager.kt           # [EXTEND] Add new item handlers
│   ├── ShopCategory.kt          # [EXISTING] Item categories
│   ├── ShopAction.kt            # [EXTEND] Add new action types
│   ├── PurchaseValidator.kt     # [EXTEND] Add team-wide limits
│   ├── PurchaseStorage.kt       # [EXTEND] Track usage statistics
│   ├── ItemPurchaseRecord.kt    # [EXISTING] Purchase tracking
│   └── UsageRestriction.kt      # [EXISTING] Restriction rules
├── items/                       # [NEW] New item effect handlers
│   ├── DecoyBlockHandler.kt
│   ├── ShadowSprintHandler.kt
│   ├── SecondChanceHandler.kt
│   ├── TrackerCompassHandler.kt
│   ├── AreaScanHandler.kt
│   ├── EagleEyeHandler.kt
│   ├── TrackerInsightHandler.kt
│   └── CaptureNetHandler.kt
├── config/
│   ├── ShopConfig.kt            # [EXTEND] Add camouflage tiers
│   └── ConfigManager.kt         # [EXISTING] Config loading
├── effects/
│   ├── EffectManager.kt         # [EXISTING] Effect system
│   ├── EffectType.kt            # [EXTEND] Add new effect types
│   └── ActiveEffect.kt          # [EXISTING] Active effects
├── listeners/
│   └── ShopListener.kt          # [EXTEND] Handle new item purchases
└── utils/
    └── ItemBuilder.kt           # [EXISTING] Item creation utility

src/main/resources/
├── config.yml                   # [EXISTING] Main config
└── shop.yml                     # [NEW] Shop items configuration

tests/
├── shop/
│   ├── CamouflageParsingTest.kt     # [NEW] Test pricing tiers
│   ├── PurchaseLimitTest.kt         # [NEW] Test purchase restrictions
│   └── ShopBalanceTest.kt           # [NEW] Test pricing balance
└── items/
    └── NewItemHandlerTests.kt       # [NEW] Test new item effects
```

**Structure Decision**: Single project structure is appropriate for this Minecraft plugin. The codebase uses package-based organization with clear separation of concerns (shop/, items/, config/, listeners/). This feature extends existing shop system without requiring architectural changes.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations to track - constitution not yet defined.
