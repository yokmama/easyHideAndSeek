# Tasks: Seeker Shop System

**Input**: Design documents from `/specs/003-seeker-shop/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US5, US1, US2, US3, US4)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure for seeker shop system

- [x] T001 [P] Create new package `src/main/kotlin/com/hideandseek/effects/` for effect management system
- [x] T002 [P] Create new package `src/main/kotlin/com/hideandseek/items/` for seeker item implementations
- [x] T003 [P] Create new package `src/main/kotlin/com/hideandseek/utils/` for effect scheduler utilities

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core effect management infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Data Structures

- [x] T004 [P] Create `EffectType` enum in `src/main/kotlin/com/hideandseek/effects/EffectType.kt` with VISION, GLOW, SPEED, REACH values, displayName, defaultDuration, defaultIntensity, icon properties
- [x] T005 [P] Create `OverlapPolicy` enum in `src/main/kotlin/com/hideandseek/effects/OverlapPolicy.kt` with REPLACE, EXTEND, REJECT, STACK values
- [x] T006 [P] Create `UsageRestriction` enum in `src/main/kotlin/com/hideandseek/shop/UsageRestriction.kt` with ALWAYS, SEEK_PHASE_ONLY, NOT_WHILE_CAPTURED, ONCE_PER_GAME values
- [x] T007 [P] Create `ActiveEffect` data class in `src/main/kotlin/com/hideandseek/effects/ActiveEffect.kt` with playerId, effectType, startTime, endTime, intensity, taskId, metadata properties and helper methods (getRemainingSeconds, isExpired, getTotalDuration, getProgress)

### Storage Systems

- [x] T008 Create `EffectStorage` class in `src/main/kotlin/com/hideandseek/effects/EffectStorage.kt` with ConcurrentHashMap-based storage for active effects, methods: putEffect, getEffect, getAllEffects, removeEffect, removeAllEffects, hasEffect, getAllPlayers, clearAll, cleanupExpired, estimateMemoryUsage
- [x] T009 Create `PurchaseStorage` class in `src/main/kotlin/com/hideandseek/shop/PurchaseStorage.kt` with ConcurrentHashMap-based storage for purchase records, methods: recordPurchase, getPurchases, getPurchasesForItem, getLatestPurchase, countPurchases, clearPlayer, clearAll, canPurchase
- [x] T010 Create `ItemPurchaseRecord` data class in `src/main/kotlin/com/hideandseek/shop/ItemPurchaseRecord.kt` with playerId, itemId, purchaseTime, gameId, price, used properties and helper methods (getElapsedSeconds, isCooldownExpired, markUsed)

### Core Effect Manager

- [x] T011 Create `EffectManager` interface in `src/main/kotlin/com/hideandseek/effects/EffectManager.kt` with methods: applyEffect, removeEffect, removeAllEffects, hasEffect, getActiveEffect, getRemainingTime, getActiveEffectTypes, cleanupExpired, clearAll
- [x] T012 Implement `EffectManagerImpl` class in `src/main/kotlin/com/hideandseek/effects/EffectManagerImpl.kt` with EffectStorage integration, BukkitScheduler task management, overlap policy handling (REPLACE/EXTEND/REJECT), automatic effect expiration cleanup
- [x] T013 Create `EffectValidator` object in `src/main/kotlin/com/hideandseek/effects/EffectValidator.kt` with validation methods: validateDuration (5-120s), validateIntensity (type-specific ranges), validatePlayer (role check, phase check)
- [x] T014 Create `EffectScheduler` utility class in `src/main/kotlin/com/hideandseek/utils/EffectScheduler.kt` for scheduled task management, effect expiration handling, cleanup task scheduling (every 60 seconds)

### Shop Extensions

- [x] T015 Extend `ShopAction` sealed class in `src/main/kotlin/com/hideandseek/shop/ShopAction.kt` to add `UseEffectItem(effectType: EffectType, duration: Int, intensity: Double)` case
- [x] T016 Extend `ShopItem` data class in `src/main/kotlin/com/hideandseek/shop/ShopItem.kt` to add effectType, effectDuration, effectIntensity, roleFilter, cooldown, maxPurchases, usageRestriction fields with validation method
- [x] T017 Extend `ShopCategory` data class in `src/main/kotlin/com/hideandseek/shop/ShopCategory.kt` to add roleFilter field and methods: isVisibleTo(role), getItemsForRole(role)

### Configuration Loading

- [x] T018 Extend `ShopConfig` class in `src/main/kotlin/com/hideandseek/config/ShopConfig.kt` to load seeker-items category from shop.yml with effect metadata parsing
- [x] T019 Create configuration section in `src/main/resources/shop.yml` for seeker-items category with role-filter: SEEKER
- [x] T020 Add effects configuration section to `src/main/resources/config.yml` with overlap-policies, max-extended-duration, performance settings (glow-update-interval, glow-max-distance, cleanup-task-interval), feedback settings (particles, sounds, action-bar)

### Event Listeners

- [x] T021 Create `EffectCleanupListener` in `src/main/kotlin/com/hideandseek/listeners/EffectCleanupListener.kt` to handle PlayerQuitEvent (remove all effects), GameEndEvent (clear all effects for game players), PlayerCapturedEvent (keep effects active in spectate mode)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 5 - Shop Access and Economy Integration (Priority: P1) 🎯 Foundation

**Goal**: Enable seekers to open shop, view seeker-specific items, and purchase with economy integration

**Independent Test**: Seeker can open shop with `/hs shop`, see 4 seeker items, purchase with sufficient funds, receive error with insufficient funds

### Implementation for User Story 5

- [x] T022 [US5] Extend `ShopManager.openShop()` method in `src/main/kotlin/com/hideandseek/shop/ShopManager.kt` to filter categories by player role using ShopCategory.isVisibleTo()
- [x] T023 [US5] Add validation in `ShopManager.purchaseItem()` to check player role matches item.roleFilter before purchase
- [x] T024 [US5] Create `PurchaseValidator` object in `src/main/kotlin/com/hideandseek/shop/PurchaseValidator.kt` with validatePurchase method checking: role access, economy balance, purchase limit, cooldown, inventory space
- [x] T025 [US5] Define custom exceptions in `src/main/kotlin/com/hideandseek/shop/PurchaseExceptions.kt`: InsufficientFundsException, PurchaseLimitException, CooldownException, InventoryFullException
- [x] T026 [US5] Extend `ShopListener.onShopItemClick()` in `src/main/kotlin/com/hideandseek/listeners/ShopListener.kt` to call PurchaseValidator before processing seeker item purchases
- [x] T027 [US5] Add error message handling in `ShopListener` for purchase validation failures with user-friendly messages (insufficient funds, cooldown remaining, purchase limit reached, inventory full)
- [x] T028 [US5] Integrate Vault economy API in `ShopManager.processSeekerItemPurchase()` for async fund withdrawal and purchase recording to PurchaseStorage
- [x] T029 [US5] Add message configuration to `src/main/resources/config.yml` under messages.effects section: insufficient-funds, cooldown-remaining, purchase-limit, inventory-full, not-seeker, wrong-phase
- [x] T030 [US5] Add 4 seeker items to `src/main/resources/shop.yml` seeker-items category: vision-enhancer (300 coins, 30s), glow-detector (500 coins, 15s, limit 3), speed-boost (200 coins, 30s), reach-extender (250 coins, 30s, intensity 1.5)

**Checkpoint**: Seekers can now access shop, view seeker items, and purchase with economy validation. Items appear in inventory but have no effect yet.

---

## Phase 4: User Story 1 - Enhanced Vision Item (Priority: P1) 🎯 MVP

**Goal**: Vision enhancer item applies Night Vision + increased view distance for 30 seconds

**Independent Test**: Seeker purchases vision-enhancer, uses item (right-click), experiences enhanced vision (brighter, farther view distance) for 30 seconds, then effect expires and item is removed

### Implementation for User Story 1

- [x] T031 [P] [US1] Create `VisionEnhancerItem` class in `src/main/kotlin/com/hideandseek/items/VisionEnhancerItem.kt` implementing item use handler (implemented as VisionEffectHandler)
- [x] T032 [P] [US1] Create `ItemEffectHandler` interface in `src/main/kotlin/com/hideandseek/items/ItemEffectHandler.kt` with methods: handleItemUse(player, item), canUseItem(player, itemId), setCooldown(player, itemId, seconds) (using EffectHandler interface)
- [x] T033 [US1] Implement `VisionEnhancerItem.applyEffect()` method to apply PotionEffectType.NIGHT_VISION with duration from config, amplifier 0, no particles
- [x] T034 [US1] Implement `VisionEnhancerItem.applyEffect()` to call player.sendViewDistance(32) to increase client render distance, store original clientViewDistance in ActiveEffect.metadata
- [x] T035 [US1] Register vision effect in EffectManager using EffectType.VISION with effect duration and intensity from item config
- [x] T036 [US1] Implement `VisionEnhancerItem.removeEffect()` to restore original view distance from ActiveEffect.metadata and remove NIGHT_VISION potion effect
- [x] T037 [US1] Extend `PlayerInteractListener.onPlayerInteract()` in `src/main/kotlin/com/hideandseek/listeners/PlayerInteractListener.kt` to detect right-click with vision-enhancer item and call ItemEffectHandler (handled by ShopListener)
- [x] T038 [US1] Add visual feedback in `VisionEnhancerItem`: play Sound.ENTITY_ENDER_EYE_LAUNCH on activation, spawn Particle.END_ROD particles around player (5 particles)
- [ ] T039 [US1] Add action bar message display in `VisionEnhancerItem.applyEffect()`: "👁 Vision Enhanced (30s remaining)" with countdown every second via BukkitScheduler
- [ ] T040 [US1] Extend `ScoreboardManager.updateScoreboard()` in `src/main/kotlin/com/hideandseek/scoreboard/ScoreboardManager.kt` to display active VISION effect with remaining time
- [ ] T041 [US1] Add effect expiration message in `EffectManagerImpl.removeEffect()`: "&cEffect expired: &eEnhanced Vision" sent to player chat
- [x] T042 [US1] Handle effect overlap in `VisionEnhancerItem`: if VISION already active, apply OverlapPolicy.REPLACE (remove old, apply new with full duration) (handled by EffectManagerImpl)

**Checkpoint**: Vision enhancer item is fully functional - seekers can purchase, use, experience enhanced vision, and see clear feedback

---

## Phase 5: User Story 2 - Disguise Block Detection Item (Priority: P1) 🎯 MVP

**Goal**: Glow detector item makes disguised blocks glow with particles visible only to the seeker for 15 seconds

**Independent Test**: Seeker purchases glow-detector, uses item, sees particles around disguised blocks (visible only to this seeker), effect lasts 15 seconds, then expires

### Implementation for User Story 2

- [ ] T043 [P] [US2] Create `GlowDetectorItem` class in `src/main/kotlin/com/hideandseek/items/GlowDetectorItem.kt` implementing ItemEffectHandler interface
- [ ] T044 [US2] Implement `GlowDetectorItem.applyEffect()` to register GLOW effect in EffectManager with 15 second duration from config
- [ ] T045 [US2] Implement `GlowDetectorItem.startGlowTask()` to create repeating BukkitTask (every 10 ticks = 0.5s) that queries DisguiseManager.getActiveDisguises()
- [ ] T046 [US2] Implement particle spawning in `GlowDetectorItem.spawnGlowParticles()`: for each disguised block location, call player.spawnParticle(Particle.END_ROD, location.center, 5 particles, 0.3 spread)
- [ ] T047 [US2] Add distance culling in `GlowDetectorItem.spawnGlowParticles()`: only show particles for disguises within 50 blocks of seeker (configurable via glow-max-distance in config.yml)
- [ ] T048 [US2] Implement dynamic update in `GlowDetectorItem.glowTask`: listen to PlayerUnDisguiseEvent and stop showing particles for that block immediately (not just on next interval)
- [ ] T049 [US2] Store glow task ID in ActiveEffect.metadata in `GlowDetectorItem.applyEffect()` for cleanup on effect removal
- [ ] T050 [US2] Implement `GlowDetectorItem.removeEffect()` to cancel glow particle task using taskId from ActiveEffect.metadata
- [ ] T051 [US2] Add visual feedback in `GlowDetectorItem`: play Sound.BLOCK_BEACON_ACTIVATE on activation, spawn Particle.GLOW particles around player (10 particles)
- [ ] T052 [US2] Add action bar message display in `GlowDetectorItem.applyEffect()`: "✨ Glow Detector Active (15s remaining)" with countdown
- [ ] T053 [US2] Extend `ScoreboardManager.updateScoreboard()` to display active GLOW effect with remaining time
- [ ] T054 [US2] Handle purchase limit in `PurchaseValidator`: check if player has purchased glow-detector 3 times in current game (maxPurchases config), reject if limit reached
- [ ] T055 [US2] Handle effect overlap in `GlowDetectorItem`: if GLOW already active, apply OverlapPolicy.REPLACE (cancel old task, start new with full duration)

**Checkpoint**: Glow detector item is fully functional - seekers can detect disguised blocks with player-limited glow particles

---

## Phase 6: User Story 3 - Speed Boost Item (Priority: P2)

**Goal**: Speed boost item increases seeker movement speed for 30 seconds

**Independent Test**: Seeker purchases speed-boost, uses item, moves faster than normal for 30 seconds, then speed returns to normal

### Implementation for User Story 3

- [ ] T056 [P] [US3] Create `SpeedBoostItem` class in `src/main/kotlin/com/hideandseek/items/SpeedBoostItem.kt` implementing ItemEffectHandler interface
- [ ] T057 [US3] Implement `SpeedBoostItem.applyEffect()` to apply PotionEffectType.SPEED with duration from config (30s default), amplifier 1 (level 2), no particles
- [ ] T058 [US3] Register speed effect in EffectManager using EffectType.SPEED with effect duration and intensity from item config
- [ ] T059 [US3] Implement `SpeedBoostItem.removeEffect()` to remove PotionEffectType.SPEED potion effect
- [ ] T060 [US3] Add visual feedback in `SpeedBoostItem`: play Sound.ENTITY_GENERIC_DRINK on activation, spawn Particle.SOUL_FIRE_FLAME particles around player feet (10 particles in trail pattern)
- [ ] T061 [US3] Add action bar message display in `SpeedBoostItem.applyEffect()`: "⚡ Speed Boost Active (30s remaining)" with countdown
- [ ] T062 [US3] Extend `ScoreboardManager.updateScoreboard()` to display active SPEED effect with remaining time
- [ ] T063 [US3] Handle effect overlap in `SpeedBoostItem`: if SPEED already active, apply OverlapPolicy.REPLACE (remove old potion effect, apply new with full duration)

**Checkpoint**: Speed boost item is fully functional - seekers can move faster with clear visual and audio feedback

---

## Phase 7: User Story 4 - Extended Reach Item (Priority: P2)

**Goal**: Reach extender item increases attack range by 1.5x for 30 seconds, allowing capture from farther away

**Independent Test**: Seeker purchases reach-extender, uses item, can attack disguised blocks from 6.75 blocks away (vs normal 4.5), captures hider, effect expires after 30 seconds

### Implementation for User Story 4

- [ ] T064 [P] [US4] Create `ReachExtenderItem` class in `src/main/kotlin/com/hideandseek/items/ReachExtenderItem.kt` implementing ItemEffectHandler interface
- [ ] T065 [US4] Create `ReachExtenderEffect` object in `src/main/kotlin/com/hideandseek/effects/ReachExtenderEffect.kt` to track active reach extensions with Map<UUID, Double> for playerId -> multiplier
- [ ] T066 [US4] Implement `ReachExtenderEffect.applyReachExtension(player, multiplier, duration)` to store multiplier (1.5) and schedule removal after duration
- [ ] T067 [US4] Implement `ReachExtenderEffect.getReachMultiplier(playerId)` to return active multiplier or 1.0 if none active
- [ ] T068 [US4] Extend `BlockDamageListener.onBlockDamage()` in `src/main/kotlin/com/hideandseek/listeners/BlockDamageListener.kt` to check for reach extension after normal hit detection fails
- [ ] T069 [US4] Implement `BlockDamageListener.getExtendedTarget(player, multiplier)` using world.rayTraceBlocks(player.eyeLocation, direction, baseReach * multiplier, FluidCollisionMode.NEVER, ignorePassableBlocks: true)
- [ ] T070 [US4] Implement extended hit detection in `BlockDamageListener`: if rayTrace hits block, check DisguiseManager.getDisguisedPlayerAt(hitBlock.location), perform capture if match found
- [ ] T071 [US4] Register reach effect in EffectManager using EffectType.REACH with effect duration and intensity (1.5x multiplier) from item config
- [ ] T072 [US4] Add optional visual feedback in `ReachExtenderItem`: if visual-feedback enabled in config, show particle beam (Particle.CRIT) from player to hit location on extended attacks
- [ ] T073 [US4] Add visual feedback in `ReachExtenderItem`: play Sound.ITEM_TRIDENT_THROW on activation, spawn Particle.SWEEP_ATTACK particles around player (5 particles)
- [ ] T074 [US4] Add action bar message display in `ReachExtenderItem.applyEffect()`: "🎯 Reach Extended (30s remaining)" with countdown
- [ ] T075 [US4] Extend `ScoreboardManager.updateScoreboard()` to display active REACH effect with remaining time
- [ ] T076 [US4] Handle effect overlap in `ReachExtenderItem`: if REACH already active, apply OverlapPolicy.REPLACE (remove old multiplier, apply new with full duration)

**Checkpoint**: Reach extender item is fully functional - seekers can capture from extended distance with accurate raycasting

---

## Phase 8: Polish & Integration

**Purpose**: Cross-cutting improvements, cleanup, and multi-effect coordination

### Multi-Effect Display

- [ ] T077 [P] Implement `ScoreboardManager.formatActiveEffects()` to display all active effects in compact format: "👁30s ✨12s ⚡25s" showing icon + remaining time for each
- [ ] T078 [P] Add action bar cycling in `EffectScheduler`: if player has multiple effects, rotate display every 2 seconds showing each effect's remaining time

### Cleanup & Error Handling

- [ ] T079 Implement global cleanup in `EffectCleanupListener.onGameEnd()`: iterate all game players, call EffectManager.removeAllEffects(player), clear PurchaseStorage for game
- [ ] T080 Add server restart safety in `EffectManagerImpl`: on plugin disable, cancel all scheduled tasks, clear storage, restore view distances for online players
- [ ] T081 Implement memory monitoring in `EffectScheduler.cleanupTask()`: log warning if EffectStorage.estimateMemoryUsage() > 100 KB, trigger cleanupExpired()
- [ ] T082 Add error recovery in `ItemEffectHandler`: if effect application fails (exception), refund player the item purchase price via Vault, log error with stack trace

### Performance Optimization

- [ ] T083 [P] Optimize `GlowDetectorItem.spawnGlowParticles()`: cache disguise locations per update interval, only query DisguiseManager every 10 ticks instead of every call
- [ ] T084 [P] Add particle batching in `GlowDetectorItem`: group particle spawns per chunk, use Paper's async packet sending if available to reduce main thread load

### Configuration Validation

- [ ] T085 Create `ConfigValidator` utility in `src/main/kotlin/com/hideandseek/utils/ConfigValidator.kt` to validate shop.yml on load: check effect-duration in range [5, 120], effect-intensity in range [0.1, 3.0], prices >= 0
- [ ] T086 Add startup validation in plugin onEnable(): validate server.properties view-distance >= 32, warn if insufficient for vision enhancement, check Vault is loaded

### Documentation

- [ ] T087 [P] Add item lore details to `src/main/resources/shop.yml`: describe each effect clearly, mention overlap policy (old effect replaced), include visual feedback description
- [ ] T088 [P] Create comments in `src/main/resources/config.yml` explaining each effect configuration option, performance tuning parameters, overlap policies

### Final Integration Testing

- [ ] T089 Test all 4 items purchased and used simultaneously by one seeker: verify effects stack independently (VISION + GLOW + SPEED + REACH all active), verify action bar cycles through all effects
- [ ] T090 Test purchase validation: insufficient funds rejected, cooldown enforced, purchase limit (glow-detector 3x) enforced, inventory full handled gracefully
- [ ] T091 Test effect cleanup: game end removes all effects, player disconnect cancels tasks, effect expiration precise to within 200ms
- [ ] T092 Test performance under load: 3 seekers each using all 4 items with 10 disguised hiders, verify TPS stays above 18, memory usage under 10 KB per seeker

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 5 (Phase 3)**: Depends on Foundational phase completion - Foundation for all other stories
- **User Stories 1-4 (Phases 4-7)**: All depend on Foundational + US5 completion
  - Can proceed in parallel after US5 (if staffed)
  - Or sequentially in priority order: US1 → US2 → US3 → US4
- **Polish (Phase 8)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 5 (P1 - Foundation)**: Depends on Foundational (Phase 2) - REQUIRED for all other stories (provides shop access)
- **User Story 1 (P1 - Vision)**: Depends on US5 - No other story dependencies
- **User Story 2 (P1 - Glow)**: Depends on US5 + DisguiseManager integration - No other story dependencies
- **User Story 3 (P2 - Speed)**: Depends on US5 - No other story dependencies
- **User Story 4 (P2 - Reach)**: Depends on US5 + DisguiseManager + BlockDamageListener integration - No other story dependencies

### Within Each User Story

- Models/Data structures before managers
- Storage systems before business logic
- Core effect application before visual feedback
- Item implementation before listener integration
- Effect registration before action bar display
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks (T001-T003) can run in parallel
- Foundational data structures (T004-T007) can run in parallel
- Foundational storage systems (T008-T010) can run in parallel after data structures
- Configuration tasks (T018-T020) can run in parallel after data structures
- Once US5 completes:
  - US1 (Vision) can run in parallel with US2 (Glow), US3 (Speed), US4 (Reach)
  - Each user story is independently implementable
- Polish phase tasks (T077-T078, T083-T084, T087-T088) can run in parallel

---

## Implementation Strategy

### MVP First (US5 + US1 + US2 - Core Seeker Advantage)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 5 (Shop access + economy)
4. Complete Phase 4: User Story 1 (Vision enhancement)
5. Complete Phase 5: User Story 2 (Glow detection - most powerful)
6. **STOP and VALIDATE**: Test seekers can find hiders more effectively
7. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US5 (Shop Access) → Test independently → Seekers can purchase (but items do nothing yet)
3. Add US1 (Vision) → Test independently → Deploy/Demo (First playable seeker advantage!)
4. Add US2 (Glow) → Test independently → Deploy/Demo (Core detection mechanic!)
5. Add US3 (Speed) → Test independently → Deploy/Demo (Mobility advantage!)
6. Add US4 (Reach) → Test independently → Deploy/Demo (Complete seeker toolkit!)
7. Add Phase 8 (Polish) → Final production-ready release

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. One developer completes US5 (foundation)
3. Once US5 is done:
   - Developer A: User Story 1 (Vision)
   - Developer B: User Story 2 (Glow)
   - Developer C: User Story 3 (Speed)
   - Developer D: User Story 4 (Reach)
4. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Tests NOT requested in spec - no test tasks included
- All file paths are absolute from repository root
- Effect overlap policy is REPLACE by default (prevents duration stacking exploits)
- Glow detector has purchase limit (3 per game) due to power level
- Performance budget: <3 KB memory per player, TPS 18+, <500ms effect application
- Visual feedback (particles, sounds) is CRITICAL for player experience - don't skip
- Action bar display preferred over scoreboard for effect countdown (less intrusive)

---

## Task Count Summary

- **Phase 1 (Setup)**: 3 tasks
- **Phase 2 (Foundational)**: 18 tasks (T004-T021)
- **Phase 3 (US5 - Shop Access)**: 9 tasks (T022-T030)
- **Phase 4 (US1 - Vision)**: 12 tasks (T031-T042)
- **Phase 5 (US2 - Glow)**: 13 tasks (T043-T055)
- **Phase 6 (US3 - Speed)**: 8 tasks (T056-T063)
- **Phase 7 (US4 - Reach)**: 13 tasks (T064-T076)
- **Phase 8 (Polish)**: 16 tasks (T077-T092)

**Total Tasks**: 92 tasks
