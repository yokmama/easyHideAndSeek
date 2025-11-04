# Tasks: Shop Items Review and Rebalance

**Input**: Design documents from `/specs/004-shop-items-review/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are not explicitly requested in the specification, so test tasks are excluded per template guidelines.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/`, `tests/` at repository root
- Kotlin source: `src/main/kotlin/com/hideandseek/`
- Resources: `src/main/resources/`
- Tests: `src/test/kotlin/com/hideandseek/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and configuration setup

- [x] T001 Create shop.yml configuration file in src/main/resources/
- [x] T002 [P] Add CamouflageTier enum to src/main/kotlin/com/hideandseek/shop/CamouflageTier.kt
- [x] T003 [P] Add PurchaseRestriction data class to src/main/kotlin/com/hideandseek/shop/PurchaseRestriction.kt
- [x] T004 [P] Add RestrictionType enum to src/main/kotlin/com/hideandseek/shop/RestrictionType.kt
- [x] T005 [P] Add ResetTrigger enum to src/main/kotlin/com/hideandseek/shop/ResetTrigger.kt

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T006 Extend ShopItem data class with camouflageTier, camouflageMultiplier, teamMaxPurchases, globalMaxPurchases, resetTrigger fields in src/main/kotlin/com/hideandseek/shop/ShopItem.kt
- [x] T007 Add getEffectivePrice() method to ShopItem for camouflage pricing calculation in src/main/kotlin/com/hideandseek/shop/ShopItem.kt
- [x] T008 Extend ShopConfig data class with disguiseBlocks map in src/main/kotlin/com/hideandseek/config/ShopConfig.kt
- [x] T009 Implement disguise blocks configuration parsing in ShopConfigLoader in src/main/kotlin/com/hideandseek/config/ShopConfig.kt
- [x] T010 Create ItemEffectHandler interface in src/main/kotlin/com/hideandseek/items/ItemEffectHandler.kt
- [x] T011 Create ItemConfig data class in src/main/kotlin/com/hideandseek/items/ItemConfig.kt
- [x] T012 Add new EffectType enum values (SHADOW_SPRINT, DECOY_BLOCK, SECOND_CHANCE, TRACKER_COMPASS, AREA_SCAN, EAGLE_EYE, TRACKER_INSIGHT, CAPTURE_NET) in src/main/kotlin/com/hideandseek/effects/EffectType.kt
- [x] T013 Create createDisguiseBlockItem() helper method in ShopConfigLoader for generating disguise block items in src/main/kotlin/com/hideandseek/config/ShopConfig.kt

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Balanced Pricing for Strategic Choices (Priority: P1) 🎯 MVP

**Goal**: Implement camouflage-based pricing for disguise blocks to create meaningful economic choices

**Independent Test**:
1. Start game with 1000 coins
2. Open shop and verify multiple disguise options at different price tiers (free, 50, 100)
3. Verify free blocks (melon, pumpkin) vs paid blocks (stone, dirt) pricing
4. Purchase a low-visibility block and verify 100 coins deducted
5. Verify shop GUI displays prices correctly for each tier

### Implementation for User Story 1

- [x] T014 [P] [US1] Add disguise-blocks section to shop.yml with high/medium/low visibility tier configurations in src/main/resources/shop.yml
- [x] T015 [P] [US1] Implement disguise block tier mapping in ShopManager.loadDisguiseBlocks() in src/main/kotlin/com/hideandseek/shop/ShopManager.kt
- [x] T016 [US1] Update ShopManager.buildShopGUI() to group disguise blocks by camouflage tier in src/main/kotlin/com/hideandseek/shop/ShopManager.kt
- [x] T017 [US1] Update ShopItem.toItemStack() to display effective price and tier information in tooltips in src/main/kotlin/com/hideandseek/shop/ShopManager.kt
- [x] T018 [US1] Update PurchaseValidator to use getEffectivePrice() instead of direct price field in src/main/kotlin/com/hideandseek/shop/PurchaseValidator.kt
- [x] T019 [US1] Add tier indicator to shop GUI lore (e.g., "§7Tier: Low Visibility") in src/main/kotlin/com/hideandseek/shop/ShopManager.kt

**Checkpoint**: At this point, camouflage-based pricing should be fully functional - players can see and purchase disguise blocks at correct tier-based prices

---

## Phase 4: User Story 2 - Clear Item Value Communication (Priority: P1)

**Goal**: Provide clear tooltips and feedback for all shop items

**Independent Test**:
1. Open shop as Hider
2. Hover over each item and verify tooltip shows: description, duration, cost, purchase limits
3. Purchase item with cooldown and verify feedback message with countdown
4. Activate item and verify activation message and duration display
5. Wait for expiration and verify expiration message

### Implementation for User Story 2

- [x] T020 [P] [US2] Extend ShopItem lore to include duration, cooldown, and purchase limit information in src/main/kotlin/com/hideandseek/shop/ShopItem.kt
- [x] T021 [P] [US2] Add getDisplayLore() method to ItemEffectHandler interface for dynamic tooltip generation in src/main/kotlin/com/hideandseek/items/ItemEffectHandler.kt
- [x] T022 [US2] Implement purchase feedback messages in ShopManager.handlePurchase() in src/main/kotlin/com/hideandseek/shop/ShopManager.kt
- [x] T023 [US2] Add item activation feedback (sound + message) in item effect handlers in src/main/kotlin/com/hideandseek/items/ (update base implementation)
- [x] T024 [US2] Add item expiration feedback (sound + message) in EffectManager.onEffectExpire() in src/main/kotlin/com/hideandseek/effects/EffectManager.kt
- [x] T025 [US2] Implement cooldown countdown display in action bar for active effects in src/main/kotlin/com/hideandseek/effects/EffectManager.kt
- [ ] T026 [US2] Add "Cannot afford" indicator in shop GUI for items exceeding player balance in src/main/kotlin/com/hideandseek/shop/ShopManager.kt

**Checkpoint**: All shop items should have clear, informative tooltips and players receive clear feedback for all purchase/activation/expiration events

---

## Phase 5: User Story 3 - Camouflage-Based Disguise Block Pricing (Priority: P1)

**Goal**: Ensure new players always have free disguise options and skilled players can strategize with tier choices

**Independent Test**:
1. Join game with 0 coins
2. Open shop and verify free high-visibility blocks are available
3. Verify melon/pumpkin show "0 coins"
4. Verify stone/grass show "100 coins" with "Cannot afford" indicator
5. Use free disguise and verify it works correctly
6. Earn 100 coins and purchase low-visibility block successfully

### Implementation for User Story 3

- [x] T027 [P] [US3] Configure high-visibility tier blocks (MELON, PUMPKIN, SPONGE, GOLD_BLOCK, DIAMOND_BLOCK, GLOWSTONE, SEA_LANTERN) at 0 price in shop.yml in src/main/resources/shop.yml
- [x] T028 [P] [US3] Configure medium-visibility tier blocks (OAK_LOG, BRICK, SANDSTONE, GRAVEL, GLASS) at 50 price in shop.yml in src/main/resources/shop.yml
- [x] T029 [P] [US3] Configure low-visibility tier blocks (STONE, COBBLESTONE, DIRT, GRASS_BLOCK, ANDESITE) at 100 price in shop.yml in src/main/resources/shop.yml
- [ ] T030 [US3] Implement tier sorting in shop GUI (high → medium → low visibility order) in src/main/kotlin/com/hideandseek/shop/ShopManager.kt
- [ ] T031 [US3] Add camouflage effectiveness description to each tier in shop GUI tooltips in src/main/kotlin/com/hideandseek/shop/ShopManager.kt
- [ ] T032 [US3] Validate that at least one free disguise option always exists on config load in src/main/kotlin/com/hideandseek/config/ShopConfig.kt

**Checkpoint**: All players can always disguise (free options available), and tier pricing creates strategic choices

---

## Phase 6: User Story 4 - Expanded Item Variety for Role Identity (Priority: P2)

**Goal**: Add new Hider and Seeker items supporting different playstyles

**Independent Test**:
1. Start as Hider and verify Shadow Sprint, Decoy Block, Second Chance available in shop
2. Start as Seeker and verify Eagle Eye, Tracker Insight, Capture Net available in shop
3. Purchase and use Shadow Sprint - verify speed boost effect for 20 seconds
4. Purchase and use Tracker Compass - verify it points to nearest Hider
5. Verify each item has unique effect matching its description

### Implementation for User Story 4

**Hider Items**:

- [ ] T033 [P] [US4] Implement ShadowSprintHandler in src/main/kotlin/com/hideandseek/items/ShadowSprintHandler.kt
- [ ] T034 [P] [US4] Implement DecoyBlockHandler in src/main/kotlin/com/hideandseek/items/DecoyBlockHandler.kt
- [ ] T035 [P] [US4] Implement SecondChanceHandler in src/main/kotlin/com/hideandseek/items/SecondChanceHandler.kt
- [ ] T036 [P] [US4] Add shadow-sprint item config to shop.yml (350 coins, 20s duration, speed 1.5x) in src/main/resources/shop.yml
- [ ] T037 [P] [US4] Add decoy-block item config to shop.yml (400 coins, limit 2, places fake disguise) in src/main/resources/shop.yml
- [ ] T038 [P] [US4] Add second-chance item config to shop.yml (800 coins, limit 1, respawn on capture) in src/main/resources/shop.yml

**Seeker Items**:

- [ ] T039 [P] [US4] Implement TrackerCompassHandler in src/main/kotlin/com/hideandseek/items/TrackerCompassHandler.kt
- [ ] T040 [P] [US4] Implement AreaScanHandler in src/main/kotlin/com/hideandseek/items/AreaScanHandler.kt
- [ ] T041 [P] [US4] Implement EagleEyeHandler in src/main/kotlin/com/hideandseek/items/EagleEyeHandler.kt
- [ ] T042 [P] [US4] Implement TrackerInsightHandler in src/main/kotlin/com/hideandseek/items/TrackerInsightHandler.kt
- [ ] T043 [P] [US4] Implement CaptureNetHandler in src/main/kotlin/com/hideandseek/items/CaptureNetHandler.kt
- [ ] T044 [P] [US4] Add tracker-compass item config to shop.yml (250 coins, 30s, points to nearest hider) in src/main/resources/shop.yml
- [ ] T045 [P] [US4] Add area-scan item config to shop.yml (450 coins, 12 block radius) in src/main/resources/shop.yml
- [ ] T046 [P] [US4] Add eagle-eye item config to shop.yml (500 coins, 20s, highlights disguised blocks) in src/main/resources/shop.yml
- [ ] T047 [P] [US4] Add tracker-insight item config to shop.yml (200 coins, consumable, shows last-moved times) in src/main/resources/shop.yml
- [ ] T048 [P] [US4] Add capture-net item config to shop.yml (600 coins, limit 2, 2x capture reward) in src/main/resources/shop.yml

**Integration**:

- [ ] T049 [US4] Register all 8 new item handlers in ShopManager.registerHandlers() in src/main/kotlin/com/hideandseek/shop/ShopManager.kt
- [ ] T050 [US4] Update ShopManager.handleItemPurchase() to invoke appropriate handler based on effect type in src/main/kotlin/com/hideandseek/shop/ShopManager.kt
- [ ] T051 [US4] Add hider-utility and seeker-tracking categories to shop.yml in src/main/resources/shop.yml

**Checkpoint**: All 8 new items are purchasable and functional, supporting diverse Hider and Seeker playstyles

---

## Phase 7: User Story 5 - Dynamic Economy Throughout Match (Priority: P2)

**Goal**: Create item pricing and availability that evolves through game phases

**Independent Test**:
1. Join game at start (0-2min) and verify low-cost items dominate shop display
2. Progress to mid-game (3-6min) as Seeker with 600+ coins from captures
3. Verify tactical items (350-500 range) are now affordable
4. Progress to late-game (7-10min) with 1500+ coins
5. Verify high-value "power items" (800+) are now purchasable and impactful

### Implementation for User Story 5

- [ ] T052 [P] [US5] Add gamePhase field to PlayerPurchaseRecord in src/main/kotlin/com/hideandseek/shop/ItemPurchaseRecord.kt
- [ ] T053 [P] [US5] Add playerBalance field to PlayerPurchaseRecord in src/main/kotlin/com/hideandseek/shop/ItemPurchaseRecord.kt
- [ ] T054 [US5] Track game phase and balance on each purchase in PurchaseStorage.recordPurchase() in src/main/kotlin/com/hideandseek/shop/PurchaseStorage.kt
- [ ] T055 [US5] Implement item price tier recommendations in shop.yml comments (< 300 = early, 300-500 = mid, 500+ = late) in src/main/resources/shop.yml
- [ ] T056 [US5] Add "Power Item" indicator to high-cost items (800+ coins) in shop GUI in src/main/kotlin/com/hideandseek/shop/ShopManager.kt
- [ ] T057 [US5] Sort shop items by price within categories for progressive discovery in src/main/kotlin/com/hideandseek/shop/ShopManager.kt

**Checkpoint**: Item economy creates natural progression - early/mid/late game purchases differ meaningfully

---

## Phase 8: User Story 6 - Purchase Limits and Strategic Scarcity (Priority: P3)

**Goal**: Implement per-player, per-team, and global purchase limits to prevent item spam

**Independent Test**:
1. Purchase Escape Pass (limit 1) successfully
2. Attempt to purchase second Escape Pass and verify "Limit reached: 1 per game" message
3. As Seeker, have 3 teammates purchase Tracker Compass (team limit 3)
4. Verify 4th team member sees "Team limit reached"
5. Purchase Speed Boost twice (limit 2 per player) and verify 3rd attempt blocked

### Implementation for User Story 6

- [ ] T058 [P] [US6] Add teamMaxPurchases tracking to PurchaseStorage in src/main/kotlin/com/hideandseek/shop/PurchaseStorage.kt
- [ ] T059 [P] [US6] Add globalMaxPurchases tracking to PurchaseStorage in src/main/kotlin/com/hideandseek/shop/PurchaseStorage.kt
- [ ] T060 [US6] Implement per-player purchase limit validation in PurchaseStorage.canPurchase() in src/main/kotlin/com/hideandseek/shop/PurchaseStorage.kt
- [ ] T061 [US6] Implement per-team purchase limit validation in PurchaseStorage.canPurchase() in src/main/kotlin/com/hideandseek/shop/PurchaseStorage.kt
- [ ] T062 [US6] Implement global purchase limit validation in PurchaseStorage.canPurchase() in src/main/kotlin/com/hideandseek/shop/PurchaseStorage.kt
- [ ] T063 [US6] Add cooldown tracking with ResetTrigger support in PurchaseStorage in src/main/kotlin/com/hideandseek/shop/PurchaseStorage.kt
- [ ] T064 [US6] Create PurchaseLimitException for limit violation feedback in src/main/kotlin/com/hideandseek/shop/PurchaseExceptions.kt
- [ ] T065 [US6] Display purchase limits in shop GUI item tooltips ("Limit: 1 per game", "Team limit: 3") in src/main/kotlin/com/hideandseek/shop/ShopManager.kt
- [ ] T066 [US6] Add "Out of stock" indicator for globally exhausted items in shop GUI in src/main/kotlin/com/hideandseek/shop/ShopManager.kt
- [ ] T067 [US6] Configure purchase limits for all items in shop.yml (escape-pass: 1, tracker-compass: 3 per team, etc.) in src/main/resources/shop.yml

**Checkpoint**: All purchase restrictions working - prevents dominant strategies via item spam

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T068 [P] Implement shop GUI caching for performance (< 500ms load target) in src/main/kotlin/com/hideandseek/shop/ShopGUICache.kt
- [ ] T069 [P] Implement async economy operations for purchase transactions (< 100ms target) in src/main/kotlin/com/hideandseek/shop/ShopManager.kt
- [ ] T070 [P] Create ItemStatistics data class for analytics in src/main/kotlin/com/hideandseek/shop/ItemStatistics.kt
- [ ] T071 [P] Implement CSV statistics logger (optional, for balance tuning) in src/main/kotlin/com/hideandseek/shop/StatisticsLogger.kt
- [ ] T072 [P] Add config validation on shop.yml load with descriptive error messages in src/main/kotlin/com/hideandseek/config/ShopConfig.kt
- [ ] T073 [P] Implement hot-reload support for shop config (/hs admin reload) in src/main/kotlin/com/hideandseek/commands/AdminCommand.kt
- [ ] T074 [P] Add arena-specific shop overrides support in ShopConfig in src/main/kotlin/com/hideandseek/config/ShopConfig.kt
- [ ] T075 [P] Update plugin messages file with new shop feedback strings in src/main/resources/messages.yml
- [ ] T076 Code cleanup and refactoring for shop-related classes
- [ ] T077 Run performance testing with 30 concurrent players opening shops
- [ ] T078 Validate quickstart.md implementation guide matches actual code structure

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-8)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Phase 9)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational - No dependencies on other stories
- **User Story 3 (P1)**: Can start after Foundational - Builds on US1 (camouflage system) but independently testable
- **User Story 4 (P2)**: Can start after Foundational - No dependencies on other stories (adds new items)
- **User Story 5 (P2)**: Can start after Foundational - Extends purchase tracking from US1/US3 but independently testable
- **User Story 6 (P3)**: Can start after Foundational - Extends purchase validation system but independently testable

### Within Each User Story

- Configuration files (shop.yml additions) before code that reads them
- Handler interfaces/base classes before concrete implementations
- Data models before services that use them
- Core implementation before integration with existing systems

### Parallel Opportunities

**Setup Phase**:
- T002-T005 can all run in parallel (different enum/data class files)

**Foundational Phase**:
- T006-T007 sequential (same file)
- T008-T009 sequential (same file)
- T010-T011 can run in parallel with T006-T009
- T012 can run in parallel with all above
- T013 depends on T009

**User Story 4** (most parallel opportunities):
- T033-T038 (Hider items) can all run in parallel
- T039-T048 (Seeker items) can all run in parallel
- T049-T051 (Integration) sequential after handlers complete

**Polish Phase**:
- T068-T075 can all run in parallel (different files)

---

## Parallel Example: User Story 4 - Item Variety

```bash
# Launch all Hider item handlers together:
Task: "Implement ShadowSprintHandler in src/main/kotlin/com/hideandseek/items/ShadowSprintHandler.kt"
Task: "Implement DecoyBlockHandler in src/main/kotlin/com/hideandseek/items/DecoyBlockHandler.kt"
Task: "Implement SecondChanceHandler in src/main/kotlin/com/hideandseek/items/SecondChanceHandler.kt"

# Launch all Seeker item handlers together:
Task: "Implement TrackerCompassHandler in src/main/kotlin/com/hideandseek/items/TrackerCompassHandler.kt"
Task: "Implement AreaScanHandler in src/main/kotlin/com/hideandseek/items/AreaScanHandler.kt"
Task: "Implement EagleEyeHandler in src/main/kotlin/com/hideandseek/items/EagleEyeHandler.kt"
Task: "Implement TrackerInsightHandler in src/main/kotlin/com/hideandseek/items/TrackerInsightHandler.kt"
Task: "Implement CaptureNetHandler in src/main/kotlin/com/hideandseek/items/CaptureNetHandler.kt"

# Launch all shop.yml config additions together:
Task: "Add shadow-sprint item config to shop.yml"
Task: "Add decoy-block item config to shop.yml"
Task: "Add second-chance item config to shop.yml"
Task: "Add tracker-compass item config to shop.yml"
Task: "Add area-scan item config to shop.yml"
Task: "Add eagle-eye item config to shop.yml"
Task: "Add tracker-insight item config to shop.yml"
Task: "Add capture-net item config to shop.yml"
```

---

## Implementation Strategy

### MVP First (User Stories 1-3, all P1)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (Balanced Pricing)
4. Complete Phase 4: User Story 2 (Clear Communication)
5. Complete Phase 5: User Story 3 (Camouflage Pricing)
6. **STOP and VALIDATE**: Test all P1 stories independently and together
7. Deploy/demo if ready

**Rationale**: All three P1 stories are core to the pricing system and should be delivered together as the foundational shop experience.

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Stories 1-3 (P1) → Test independently → Deploy/Demo (MVP!)
3. Add User Story 4 (P2 - Item Variety) → Test independently → Deploy/Demo
4. Add User Story 5 (P2 - Dynamic Economy) → Test independently → Deploy/Demo
5. Add User Story 6 (P3 - Purchase Limits) → Test independently → Deploy/Demo
6. Polish phase → Performance tuning and analytics
7. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (Pricing)
   - Developer B: User Story 2 (Communication)
   - Developer C: User Story 3 (Camouflage)
3. After P1 stories complete:
   - Developer A: User Story 4 (Item Variety)
   - Developer B: User Story 5 (Dynamic Economy)
   - Developer C: User Story 6 (Purchase Limits)
4. Polish phase: All developers collaborate

---

## Task Summary

**Total Tasks**: 78
- Phase 1 (Setup): 5 tasks
- Phase 2 (Foundational): 8 tasks
- Phase 3 (US1 - Pricing): 6 tasks
- Phase 4 (US2 - Communication): 7 tasks
- Phase 5 (US3 - Camouflage): 6 tasks
- Phase 6 (US4 - Item Variety): 19 tasks
- Phase 7 (US5 - Dynamic Economy): 6 tasks
- Phase 8 (US6 - Purchase Limits): 10 tasks
- Phase 9 (Polish): 11 tasks

**Parallel Opportunities**: 46 tasks marked [P] (59% of total)

**MVP Scope**: Phases 1-5 (US1-US3) = 32 tasks for complete foundational shop with camouflage pricing

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- Configuration changes (shop.yml) should be validated and tested before moving to next phase
