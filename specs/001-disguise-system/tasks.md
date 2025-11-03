# Implementation Tasks: Disguise System

**Feature**: 001-disguise-system
**Generated**: 2025-10-24
**Total Tasks**: 87
**Estimated Duration**: 8 weeks

---

## Task Organization

Tasks are organized by implementation phase, following the quickstart.md roadmap. Each task includes:
- **TaskID**: Unique identifier (e.g., SETUP-001)
- **Priority**: P1 (must-have), P2 (should-have), P3 (nice-to-have)
- **Story**: User Story reference (Story1-Story11)
- **Description**: What needs to be implemented with file path
- **Dependencies**: Prerequisites that must be completed first
- **Test**: How to verify completion

---

## Phase 1: Setup (Project Infrastructure)

**Goal**: Initialize project structure, build system, and basic plugin skeleton

- [ ] [SETUP-001] [P1] [Story8] Create project directory structure with src/main/kotlin, src/main/resources, src/test/kotlin following standard Minecraft plugin layout (see plan.md:96-166)
- [ ] [SETUP-002] [P1] [Story8] Create build.gradle.kts with Kotlin plugin, shadow plugin, Paper API 1.21-R0.1-SNAPSHOT, Vault API 1.7 dependencies (see quickstart.md:46-97)
- [ ] [SETUP-003] [P1] [Story8] Create plugin.yml in src/main/resources with plugin metadata, commands (hideandseek with alias hs), permissions structure (see quickstart.md:103-131, contracts/commands.md:362-391)
- [ ] [SETUP-004] [P1] [Story8] Create default config.yml with game settings (preparation-time: 30, seek-time: 600, min-players: 2, max-players: 30, seeker-ratio: 0.23) (see spec.md:542-558)
- [ ] [SETUP-005] [P1] [Story1] Create default shop.yml with shop item configuration (material: EMERALD, slot: 8, glow: true) and disguise-blocks category (see spec.md:494-624)
- [ ] [SETUP-006] [P1] [Story8] Create arenas.yml template structure (empty arenas list, will be populated by commands) (see spec.md:629-680)
- [ ] [SETUP-007] [P1] [Story8] Create main plugin class HideAndSeekPlugin.kt extending JavaPlugin with onEnable/onDisable stubs (see plan.md:99)
- [ ] [SETUP-008] [P1] [Story8] Implement build task `./gradlew clean build` and verify it produces a JAR file
- [ ] [SETUP-009] [P1] [Story8] Set up test infrastructure with MockBukkit and JUnit 5 in build.gradle.kts (see research.md:73-98)

**Parallel Opportunities**: SETUP-001 through SETUP-006 can be done in parallel (file creation), then SETUP-007, then SETUP-008 and SETUP-009

---

## Phase 2: Foundational (Core Systems)

**Goal**: Implement configuration management and utility classes that all other features depend on

- [ ] [FOUND-001] [P1] [Story8] Create ConfigManager.kt to load config.yml, arenas.yml, shop.yml with YAML parsing (see research.md:362-383, plan.md:128-130)
- [ ] [FOUND-002] [P1] [Story8] Create ArenaConfig.kt to handle arenas.yml CRUD operations with Arena serialization/deserialization (see plan.md:129)
- [ ] [FOUND-003] [P1] [Story1] Create ShopConfig.kt to load shop.yml with ShopCategory and ShopItem parsing (see plan.md:130)
- [ ] [FOUND-004] [P1] [Story8] Implement ConfigManager.load() in HideAndSeekPlugin.onEnable() with error handling for missing files (see research.md:367-382)
- [ ] [FOUND-005] [P1] [Story8] Create MessageUtil.kt for colored messages with & color code translation (see plan.md:136)
- [ ] [FOUND-006] [P1] [Story1] Create ItemBuilder.kt for fluent ItemStack creation (display name, lore, glow, flags) (see plan.md:137)
- [ ] [FOUND-007] [P1] [Story8] Create TaskScheduler.kt wrapper for Bukkit scheduler (sync/async task helpers) (see plan.md:138)
- [ ] [FOUND-008] [P1] [Story8] Write unit test for ConfigManager loading valid config.yml using MockBukkit (see quickstart.md:172-182)
- [ ] [FOUND-009] [P1] [Story1] Write unit test for ShopConfig parsing shop.yml with categories and items (see quickstart.md:172-182)

**Dependencies**: Requires SETUP-001 through SETUP-009
**Parallel Opportunities**: FOUND-001/002/003 can be done in parallel, then FOUND-004, then FOUND-005/006/007 in parallel, then FOUND-008/009 in parallel

---

## Phase 3: Story 8 - Arena Setup and Configuration (P1)

**Goal**: Implement admin commands to create and manage arenas

- [ ] [ARENA-001] [P1] [Story8] Create Arena.kt data class with name, displayName, world, boundaries: ArenaBoundaries, spawns: ArenaSpawns (see data-model.md:38-78)
- [ ] [ARENA-002] [P1] [Story8] Create ArenaBoundaries.kt data class with pos1, pos2, center calculation, size calculation, contains() method (see data-model.md:51-72)
- [ ] [ARENA-003] [P1] [Story8] Create ArenaSpawns.kt data class with seeker: Location, hider: Location (see data-model.md:74-77)
- [ ] [ARENA-004] [P1] [Story8] Create ArenaSetupSession.kt data class with adminUuid, pos1/pos2/seekerSpawn/hiderSpawn fields, isComplete(), validate(), toArena() methods (see data-model.md:459-499)
- [ ] [ARENA-005] [P1] [Story8] Create ArenaManager.kt singleton with setupSessions: MutableMap<UUID, ArenaSetupSession>, arenas: MutableMap<String, Arena> (see plan.md:110-112, data-model.md:554-555)
- [ ] [ARENA-006] [P1] [Story8] Implement ArenaManager.setPos1(admin, location) to store position in setup session (see contracts/commands.md:99-122)
- [ ] [ARENA-007] [P1] [Story8] Implement ArenaManager.setPos2(admin, location) with diagonal distance calculation and display (see contracts/commands.md:124-150)
- [ ] [ARENA-008] [P1] [Story8] Implement ArenaManager.setSpawn(admin, role, location) for seeker/hider spawns with rotation (see contracts/commands.md:153-178)
- [ ] [ARENA-009] [P1] [Story8] Implement ArenaManager.createArena(admin, name, displayName) with validation (all positions set, same world, unique name) (see contracts/commands.md:180-217, spec.md:254-257)
- [ ] [ARENA-010] [P1] [Story8] Implement ArenaManager.saveArena(arena) to write to arenas.yml via ArenaConfig (see spec.md:629-680)
- [ ] [ARENA-011] [P1] [Story8] Implement ArenaManager.loadArenas() to read from arenas.yml on plugin enable (see data-model.md:521-535)
- [ ] [ARENA-012] [P1] [Story8] Implement ArenaManager.listArenas() returning list of arena names and metadata (see contracts/commands.md:220-243)
- [ ] [ARENA-013] [P1] [Story8] Implement ArenaManager.deleteArena(name) with in-use check and file update (see contracts/commands.md:246-275)
- [ ] [ARENA-014] [P1] [Story8] Create AdminCommand.kt implementing CommandExecutor with permission check hideandseek.admin (see contracts/commands.md:445-454, plan.md:101-103)
- [ ] [ARENA-015] [P1] [Story8] Implement AdminCommand.onCommand() router for setpos1, setpos2, setspawn, creategame, list, delete subcommands (see contracts/commands.md:99-434)
- [ ] [ARENA-016] [P1] [Story8] Implement tab completion for /hs admin with subcommand suggestions (see contracts/commands.md:397-434)
- [ ] [ARENA-017] [P1] [Story8] Register AdminCommand in HideAndSeekPlugin.onEnable() for "hideandseek" command (see quickstart.md:103-131)
- [ ] [ARENA-018] [P1] [Story8] Write unit test for ArenaSetupSession.validate() with missing positions (see quickstart.md:202-213, spec.md:254)
- [ ] [ARENA-019] [P1] [Story8] Write unit test for Arena creation with different worlds (error case) (see spec.md:255)
- [ ] [ARENA-020] [P1] [Story8] Write integration test for full arena creation flow (setpos1 → setpos2 → setspawn × 2 → creategame) (see quickstart.md:202-213)

**Dependencies**: Requires FOUND-001 through FOUND-009
**Parallel Opportunities**: ARENA-001/002/003/004 in parallel, then ARENA-005, then ARENA-006/007/008 in parallel, then ARENA-009/010/011, then ARENA-012/013 in parallel, then ARENA-014/015/016/017 in sequence, then ARENA-018/019/020 in parallel

---

## Phase 4: Story 9 - Game Start and Player Initialization (P1)

**Goal**: Implement game lifecycle management (join, start, role assignment, preparation phase)

- [ ] [GAME-001] [P1] [Story9] Create GamePhase.kt enum with WAITING, PREPARATION, SEEKING, ENDED (see data-model.md:137-142)
- [ ] [GAME-002] [P1] [Story9] Create PlayerRole.kt enum with HIDER, SEEKER, SPECTATOR (see data-model.md:193-197)
- [ ] [GAME-003] [P1] [Story9] Create GameResult.kt enum with SEEKER_WIN, HIDER_WIN, CANCELLED (see data-model.md:144-148)
- [ ] [GAME-004] [P1] [Story9] Create PlayerBackup.kt data class with inventory, armor, location, gameMode, scoreboard fields and restore() method (see data-model.md:221-244)
- [ ] [GAME-005] [P1] [Story9] Create PlayerGameData.kt data class with uuid, role, isCaptured, captureCount, backup fields and capture(), recordCapture() methods (see data-model.md:174-191)
- [ ] [GAME-006] [P1] [Story9] Create WorldBorderBackup.kt data class with center, size, damage settings and restore(), capture() static methods (see data-model.md:393-435)
- [ ] [GAME-007] [P1] [Story9] Create Game.kt data class with arena, phase, players: MutableMap<UUID, PlayerGameData>, startTime, phaseStartTime, worldBorderBackup (see data-model.md:111-135)
- [ ] [GAME-008] [P1] [Story9] Implement Game.getSeekers(), getHiders(), getCaptured() filter methods (see data-model.md:119-121)
- [ ] [GAME-009] [P1] [Story9] Implement Game.checkWinCondition() returning GameResult? (all hiders captured OR time expired) (see data-model.md:126-134)
- [ ] [GAME-010] [P1] [Story9] Create GameManager.kt singleton with activeGame: Game?, waitingPlayers: MutableList<UUID> (see plan.md:106)
- [ ] [GAME-011] [P1] [Story9] Implement GameManager.joinGame(player) adding player to waitingPlayers with duplicate check (see contracts/commands.md:14-40, spec.md:268)
- [ ] [GAME-012] [P1] [Story9] Implement GameManager.leaveGame(player) removing from waiting or active game with cleanup (see contracts/commands.md:43-66)
- [ ] [GAME-013] [P1] [Story9] Implement GameManager.startGame(arena) with minimum player validation (2+) (see contracts/commands.md:278-317, spec.md:186-187)
- [ ] [GAME-014] [P1] [Story9] Implement GameManager.assignRoles(players) with 23% seeker ratio, minimum 1 seeker, random selection (see spec.md:188, data-model.md:240-250)
- [ ] [GAME-015] [P1] [Story9] Implement GameManager.backupPlayers(players) creating PlayerBackup for each player (inventory, location, gameMode, scoreboard) (see spec.md:189, data-model.md:221-244)
- [ ] [GAME-016] [P1] [Story9] Implement GameManager.clearInventories(players) after backup (see spec.md:189)
- [ ] [GAME-017] [P1] [Story9] Implement GameManager.teleportPlayers(game) sending seekers to seeker spawn, hiders to hider spawn (see spec.md:190)
- [ ] [GAME-018] [P1] [Story9] Implement GameManager.giveShopItems(hiders) giving shop item to slot 8 for hiders only (see spec.md:191)
- [ ] [GAME-019] [P1] [Story9] Implement GameManager.applyPreparationEffects(seekers) adding blindness effect to seekers (see spec.md:192)
- [ ] [GAME-020] [P1] [Story9] Implement GameManager.backupWorldBorder(world) capturing current WorldBorder settings (see spec.md:195, data-model.md:412-422)
- [ ] [GAME-021] [P1] [Story9] Implement GameManager.applyArenaBorder(arena) setting WorldBorder to arena center and size (see spec.md:196, research.md:164-183)
- [ ] [GAME-022] [P1] [Story9] Implement GameManager.startPreparationPhase(game) setting phase to PREPARATION with 30s countdown (see spec.md:193)
- [ ] [GAME-023] [P1] [Story9] Implement GameManager.startSeekPhase(game) removing blindness, setting phase to SEEKING (see spec.md:194)
- [ ] [GAME-024] [P1] [Story9] Create JoinCommand.kt implementing /hs join with permission hideandseek.play (see contracts/commands.md:14-40, plan.md:102)
- [ ] [GAME-025] [P1] [Story9] Create LeaveCommand.kt implementing /hs leave with permission hideandseek.play (see contracts/commands.md:43-66, plan.md:103)
- [ ] [GAME-026] [P1] [Story9] Add /hs start <arena-name> to AdminCommand with arena existence check (see contracts/commands.md:278-317)
- [ ] [GAME-027] [P1] [Story9] Register JoinCommand and LeaveCommand in HideAndSeekPlugin.onEnable()
- [ ] [GAME-028] [P1] [Story9] Write unit test for role assignment with 13 players (expect 3 seekers, 10 hiders) (see quickstart.md:239-251)
- [ ] [GAME-029] [P1] [Story9] Write unit test for minimum seeker guarantee with 2 players (expect 1 seeker, 1 hider) (see data-model.md:240-250)
- [ ] [GAME-030] [P1] [Story9] Write integration test for full game start flow (join → start → verify teleport, items, effects) (see quickstart.md:443-466)

**Dependencies**: Requires ARENA-001 through ARENA-020
**Parallel Opportunities**: GAME-001/002/003 in parallel, then GAME-004/005/006 in parallel, then GAME-007, then GAME-008/009 in parallel, then GAME-010, then GAME-011/012 in parallel, then GAME-013 through GAME-023 in sequence, then GAME-024/025/026 in parallel, then GAME-027, then GAME-028/029/030 in parallel

---

## Phase 5: Story 1 - Shop Access and Block Selection (P1)

**Goal**: Implement shop GUI system for hiders to select disguise blocks

- [ ] [SHOP-001] [P1] [Story1] Create ShopCategory.kt data class with id, displayName, icon, slot, items: List<ShopItem> (see data-model.md:314-320)
- [ ] [SHOP-002] [P1] [Story1] Create ShopItem.kt data class with id, material, displayName, lore, slot, price, action: ShopAction (see data-model.md:346-355)
- [ ] [SHOP-003] [P1] [Story1] Create ShopAction.kt sealed class with Disguise(blockType), PurchaseItem, GoBack, Close (see data-model.md:357-363)
- [ ] [SHOP-004] [P1] [Story1] Create ShopManager.kt singleton with categories: List<ShopCategory> loaded from ShopConfig (see plan.md:118-119)
- [ ] [SHOP-005] [P1] [Story1] Implement ShopManager.loadCategories(shopConfig) parsing shop.yml categories and items (see spec.md:510-582)
- [ ] [SHOP-006] [P1] [Story1] Implement ShopManager.createShopItem() returning ItemStack for shop item with NBT tag for identification (see spec.md:496-502, research.md:265-294)
- [ ] [SHOP-007] [P1] [Story1] Implement ShopManager.isShopItem(itemStack) checking NBT tag (see spec.md:20)
- [ ] [SHOP-008] [P1] [Story1] Implement ShopManager.openMainMenu(player) creating Inventory GUI with category icons (see research.md:278-294, spec.md:22)
- [ ] [SHOP-009] [P1] [Story1] Implement ShopManager.openCategory(player, category) creating Inventory GUI with block items (see spec.md:23)
- [ ] [SHOP-010] [P1] [Story1] Create ShopListener.kt implementing Listener for InventoryClickEvent (see plan.md:120)
- [ ] [SHOP-011] [P1] [Story1] Implement ShopListener.onInventoryClick() handling category selection and block selection (see research.md:296-303, spec.md:22-24)
- [ ] [SHOP-012] [P1] [Story1] Create InventoryListener.kt implementing Listener for item protection events (see plan.md:132-134)
- [ ] [SHOP-013] [P1] [Story1] Implement InventoryListener.onPlayerDropItem() cancelling if shop item (see spec.md:26, quickstart.md:306-317)
- [ ] [SHOP-014] [P1] [Story1] Implement InventoryListener.onInventoryClick() cancelling shop item moves (see spec.md:27)
- [ ] [SHOP-015] [P1] [Story1] Create PlayerInteractListener.kt for opening shop on right-click air (see plan.md:120)
- [ ] [SHOP-016] [P1] [Story1] Implement PlayerInteractListener.onPlayerInteract() checking Action.RIGHT_CLICK_AIR and shop item (see spec.md:21, research.md:278-294)
- [ ] [SHOP-017] [P1] [Story1] Prevent shop opening on right-click block (PlayerInteractEvent with Action.RIGHT_CLICK_BLOCK) (see spec.md:296)
- [ ] [SHOP-018] [P1] [Story1] Register ShopListener, InventoryListener, PlayerInteractListener in HideAndSeekPlugin.onEnable()
- [ ] [SHOP-019] [P1] [Story1] Implement ShopManager.closeAllShops(game) when game ends to prevent leaks (see spec.md:297-298)
- [ ] [SHOP-020] [P1] [Story1] Write unit test for shop item drop prevention (see quickstart.md:306-317)
- [ ] [SHOP-021] [P1] [Story1] Write unit test for shop GUI navigation (main menu → category → item selection)
- [ ] [SHOP-022] [P1] [Story1] Write integration test for shop item persistence in inventory during game (see spec.md:20)

**Dependencies**: Requires GAME-001 through GAME-030
**Parallel Opportunities**: SHOP-001/002/003 in parallel, then SHOP-004/005 in sequence, then SHOP-006/007 in parallel, then SHOP-008/009 in parallel, then SHOP-010/011 in sequence, then SHOP-012/013/014 in sequence, then SHOP-015/016/017 in sequence, then SHOP-018/019 in sequence, then SHOP-020/021/022 in parallel

---

## Phase 6: Story 2/4 - Disguise System (P1)

**Goal**: Implement disguise mechanics (block placement, invisibility, movement detection, capture)

- [ ] [DISGUISE-001] [P1] [Story2] Create DisguiseBlock.kt data class with location, material, playerUuid, timestamp, place(), remove(), isValid() methods (see data-model.md:270-288)
- [ ] [DISGUISE-002] [P1] [Story2] Create DisguiseManager.kt singleton with disguises: MutableMap<Location, UUID>, playerDisguises: MutableMap<UUID, DisguiseBlock> (see plan.md:114-116, research.md:206-224)
- [ ] [DISGUISE-003] [P1] [Story1] Implement DisguiseManager.disguise(player, blockType) checking obstructions, placing block, applying invisibility (see research.md:210-223, spec.md:24-25)
- [ ] [DISGUISE-004] [P1] [Story1] Validate disguise location is air or valid placeholder, not obstructed (see spec.md:307, spec.md:FR-040)
- [ ] [DISGUISE-005] [P1] [Story1] Validate player is not in liquid (water/lava) before disguising (see spec.md:322-323, spec.md:FR-040)
- [ ] [DISGUISE-006] [P1] [Story1] Validate player is on solid ground (not falling/mid-air) before disguising (see spec.md:308-309)
- [ ] [DISGUISE-007] [P1] [Story1] Prevent multiple players from disguising at same location (see spec.md:310-311)
- [ ] [DISGUISE-008] [P1] [Story2] Apply PotionEffectType.INVISIBILITY with duration Integer.MAX_VALUE when disguised (see research.md:222)
- [ ] [DISGUISE-009] [P1] [Story2] Implement DisguiseManager.removeDisguise(player, reason) removing block, clearing invisibility, notifying player (see spec.md:44, research.md:256)
- [ ] [DISGUISE-010] [P1] [Story2] Implement DisguiseManager.isDisguised(player) checking playerDisguises map (see data-model.md:564)
- [ ] [DISGUISE-011] [P1] [Story2] Implement DisguiseManager.getDisguise(location) checking disguises map (see data-model.md:565)
- [ ] [DISGUISE-012] [P1] [Story2] Create DisguiseListener.kt implementing Listener for movement and attack events (see plan.md:116)
- [ ] [DISGUISE-013] [P1] [Story2] Implement DisguiseListener.onPlayerMove() checking horizontal movement (X/Z change) and removing disguise (see research.md:244-258, spec.md:41-42)
- [ ] [DISGUISE-014] [P1] [Story2] Ignore Y-axis only changes (vertical movement from gravity) in movement detection (see research.md:253-254)
- [ ] [DISGUISE-015] [P1] [Story2] Detect jump (spacebar) by checking Y velocity and remove disguise before jump completes (see spec.md:42)
- [ ] [DISGUISE-016] [P1] [Story2] Ensure external forces (knockback, explosions) do NOT remove disguise (see spec.md:43, spec.md:FR-031)
- [ ] [DISGUISE-017] [P1] [Story4] Implement DisguiseListener.onPlayerInteract() for left-click (Action.LEFT_CLICK_BLOCK) on disguise blocks (see spec.md:75-76, research.md:227-258)
- [ ] [DISGUISE-018] [P1] [Story4] Check if clicked block is a disguise using DisguiseManager.getDisguise(location) (see spec.md:77)
- [ ] [DISGUISE-019] [P1] [Story4] Trigger capture mechanic: remove disguise, set hider to spectator, increment seeker capture count (see spec.md:75-76)
- [ ] [DISGUISE-020] [P1] [Story4] Ensure no damage is dealt on capture (cancel event or set damage to 0) (see spec.md:76)
- [ ] [DISGUISE-021] [P1] [Story4] Prevent capturing regular world blocks (only disguise blocks trigger capture) (see spec.md:78)
- [ ] [DISGUISE-022] [P1] [Story3] Allow players to re-disguise unlimited times by opening shop and selecting new block (see spec.md:58-59)
- [ ] [DISGUISE-023] [P1] [Story3] Remove old disguise block when player disguises at new location (see spec.md:59-60)
- [ ] [DISGUISE-024] [P1] [Story3] Allow changing disguise type without moving (shop selection replaces block at same location) (see spec.md:61-62)
- [ ] [DISGUISE-025] [P1] [Story2] Register DisguiseListener in HideAndSeekPlugin.onEnable()
- [ ] [DISGUISE-026] [P1] [Story2] Implement DisguiseManager.removeAllDisguises() for game end cleanup (see spec.md:317, spec.md:FR-038)
- [ ] [DISGUISE-027] [P1] [Story2] Handle player disconnect by removing their disguise block immediately (see spec.md:320-321)
- [ ] [DISGUISE-028] [P1] [Story2] Write unit test for disguise removal on horizontal movement (see quickstart.md:271-283)
- [ ] [DISGUISE-029] [P1] [Story2] Write unit test for disguise persistence under knockback (external force) (see spec.md:324-325)
- [ ] [DISGUISE-030] [P1] [Story4] Write unit test for capture on block attack (see quickstart.md:271-283)
- [ ] [DISGUISE-031] [P1] [Story1] Write integration test for full disguise cycle (shop → select → place → move → remove) (see quickstart.md:443-466)

**Dependencies**: Requires SHOP-001 through SHOP-022
**Parallel Opportunities**: DISGUISE-001/002 in parallel, then DISGUISE-003 through DISGUISE-011 in sequence, then DISGUISE-012, then DISGUISE-013/014/015/016 in sequence, then DISGUISE-017/018/019/020/021 in sequence, then DISGUISE-022/023/024 in parallel, then DISGUISE-025/026/027 in sequence, then DISGUISE-028/029/030/031 in parallel

---

## Phase 7: Story 10 - Game End and Cleanup (P1)

**Goal**: Implement game termination, reward distribution, and complete state restoration

- [ ] [END-001] [P1] [Story10] Create RewardManager.kt with Vault Economy integration (see plan.md:126-127, research.md:324-342)
- [ ] [END-002] [P1] [Story10] Implement RewardManager.getEconomy() checking for Vault service registration (see research.md:326-327)
- [ ] [END-003] [P1] [Story10] Implement RewardManager.distributeRewards(game, result) calculating rewards based on GameResult (see spec.md:210-211)
- [ ] [END-004] [P1] [Story10] Calculate winner reward (1000 coins default from config.yml) for winning team (see spec.md:554)
- [ ] [END-005] [P1] [Story10] Calculate capture bonuses for seekers (200 coins per capture from config.yml) (see spec.md:556)
- [ ] [END-006] [P1] [Story10] Calculate participation reward (100 coins default from config.yml) for all players (see spec.md:557)
- [ ] [END-007] [P1] [Story10] Execute economy transactions asynchronously using Bukkit scheduler (see research.md:334-340)
- [ ] [END-008] [P1] [Story10] Implement GameManager.endGame(game, result) orchestrating all cleanup steps (see contracts/commands.md:334-356)
- [ ] [END-009] [P1] [Story10] Display result title to all players for 5 seconds (see spec.md:212)
- [ ] [END-010] [P1] [Story10] Call RewardManager.distributeRewards() for Vault economy (see spec.md:213)
- [ ] [END-011] [P1] [Story10] Call DisguiseManager.removeAllDisguises() to clear all blocks (see spec.md:214)
- [ ] [END-012] [P1] [Story10] Remove all shop items from player inventories (see spec.md:215)
- [ ] [END-013] [P1] [Story10] Clear all potion effects (invisibility, blindness) from all players (see spec.md:216)
- [ ] [END-014] [P1] [Story10] Restore each player's backed-up scoreboard (see spec.md:217)
- [ ] [END-015] [P1] [Story10] Restore WorldBorder using WorldBorderBackup.restore() (see spec.md:218, data-model.md:401-409)
- [ ] [END-016] [P1] [Story10] Restore each player's inventory using PlayerBackup.restore() (see spec.md:219, data-model.md:228-243)
- [ ] [END-017] [P1] [Story10] Teleport players to original location with safe fallback (see spec.md:220, data-model.md:234-238)
- [ ] [END-018] [P1] [Story10] Implement safe teleport fallback to world spawn if original location invalid (see spec.md:273-274, data-model.md:234-238)
- [ ] [END-019] [P1] [Story10] Clear GameManager.activeGame and waitingPlayers after cleanup
- [ ] [END-020] [P1] [Story10] Implement /hs stop <arena-name> in AdminCommand for force-end (see contracts/commands.md:320-356)
- [ ] [END-021] [P1] [Story10] Handle player disconnect during game: remove disguise, count as eliminated, save backup (see spec.md:222)
- [ ] [END-022] [P1] [Story10] Handle all seekers disconnecting: end game with hiders winning (see spec.md:261-262)
- [ ] [END-023] [P1] [Story10] Implement fallback for WorldBorder restoration failure (use Minecraft defaults) (see spec.md:263-264, data-model.md:424-433)
- [ ] [END-024] [P1] [Story10] Implement fallback for inventory restoration failure (log error, give empty inventory) (see spec.md:265-266)
- [ ] [END-025] [P1] [Story10] Write unit test for reward calculation with 5 captures (see quickstart.md:371-382)
- [ ] [END-026] [P1] [Story10] Write unit test for complete cleanup (all state restored) (see quickstart.md:401-412)
- [ ] [END-027] [P1] [Story10] Write integration test for full game lifecycle (start → play → end → verify cleanup) (see quickstart.md:443-466)

**Dependencies**: Requires DISGUISE-001 through DISGUISE-031
**Parallel Opportunities**: END-001/002 in parallel, then END-003 through END-007 in sequence, then END-008, then END-009 through END-019 in sequence, then END-020/021/022 in sequence, then END-023/024 in sequence, then END-025/026/027 in parallel

---

## Phase 8: Story 11 - Game Boundary Enforcement (P1)

**Goal**: Implement WorldBorder effects and boundary warnings

- [ ] [BOUNDARY-001] [P1] [Story11] Verify WorldBorder visual warning (red vignette) when players approach within 5 blocks (Minecraft default behavior) (see spec.md:238)
- [ ] [BOUNDARY-002] [P1] [Story11] Verify WorldBorder pushback when players touch boundary (Minecraft default behavior) (see spec.md:239)
- [ ] [BOUNDARY-003] [P1] [Story11] Verify WorldBorder damage when players remain at boundary (Minecraft default behavior) (see spec.md:240)
- [ ] [BOUNDARY-004] [P1] [Story11] Create WorldBorderListener.kt implementing Listener for EntityDamageEvent (see plan.md:134)
- [ ] [BOUNDARY-005] [P1] [Story11] Implement WorldBorderListener.onEntityDamage() checking if damage cause is WORLD_BORDER (see spec.md:241)
- [ ] [BOUNDARY-006] [P1] [Story11] Send warning message "ゲームエリアの外に出ることはできません！" on WorldBorder damage (see spec.md:241)
- [ ] [BOUNDARY-007] [P1] [Story11] Ensure disguise is NOT removed on WorldBorder damage (external force, not player movement) (see spec.md:242)
- [ ] [BOUNDARY-008] [P1] [Story11] Register WorldBorderListener in HideAndSeekPlugin.onEnable()
- [ ] [BOUNDARY-009] [P1] [Story11] Verify WorldBorder stops enforcing when game ends (restoration complete) (see spec.md:243)
- [ ] [BOUNDARY-010] [P1] [Story11] Write unit test for boundary warning message on damage
- [ ] [BOUNDARY-011] [P1] [Story11] Write integration test for disguise persistence at boundary (see spec.md:242)

**Dependencies**: Requires END-001 through END-027
**Parallel Opportunities**: BOUNDARY-001/002/003 can be verified in parallel (Minecraft behavior), then BOUNDARY-004, then BOUNDARY-005/006/007 in sequence, then BOUNDARY-008/009 in sequence, then BOUNDARY-010/011 in parallel

---

## Phase 9: Story 7 - HUD Display and Game Status Visualization (P2)

**Goal**: Implement scoreboard, title, and action bar displays

- [ ] [HUD-001] [P2] [Story7] Create ScoreboardManager.kt singleton with playerScoreboards: MutableMap<UUID, Scoreboard> (see plan.md:122, research.md:117-133)
- [ ] [HUD-002] [P2] [Story7] Implement ScoreboardManager.createGameScoreboard(player, game) creating Bukkit Scoreboard with Objective (see research.md:119-131)
- [ ] [HUD-003] [P2] [Story7] Implement ScoreboardManager.updateScoreboard(player, game) setting lines based on role (see spec.md:125-132, quickstart.md:337-355)
- [ ] [HUD-004] [P2] [Story7] Display game title, current time/total time, phase indicator in scoreboard (see spec.md:126-127)
- [ ] [HUD-005] [P2] [Story7] Display role-specific info: hiders see disguise status and escape item, seekers see capture count (see spec.md:127-128)
- [ ] [HUD-006] [P2] [Story7] Display player counts: seeker count, remaining hider count, captured count (see spec.md:127)
- [ ] [HUD-007] [P2] [Story7] Update scoreboard within 1 second when game state changes (disguise, capture, time) (see spec.md:129-131)
- [ ] [HUD-008] [P2] [Story7] Implement ScoreboardManager.startUpdates(game) with 1-second ticker (20 ticks) (see research.md:423-429)
- [ ] [HUD-009] [P2] [Story7] Implement ScoreboardManager.stopUpdates() cancelling update task
- [ ] [HUD-010] [P2] [Story7] Limit scoreboard to maximum 15 lines to prevent overflow (see spec.md:284-285)
- [ ] [HUD-011] [P2] [Story7] Truncate long player names to fit scoreboard width (see spec.md:280-281)
- [ ] [HUD-012] [P2] [Story7] Create TitleManager.kt singleton for title announcements (see plan.md:123)
- [ ] [HUD-013] [P2] [Story7] Implement TitleManager.sendRoleAssignment(player, role) displaying "あなたはハイダーです" or "あなたはシーカーです" (see spec.md:136-137)
- [ ] [HUD-014] [P2] [Story7] Implement TitleManager.sendPhaseTransition() displaying "探索開始！" when preparation ends (see spec.md:138)
- [ ] [HUD-015] [P2] [Story7] Implement TitleManager.sendCaptureNotification(player) displaying "捕獲されました！" (see spec.md:139)
- [ ] [HUD-016] [P2] [Story7] Implement TitleManager.sendGameResult(players, result) displaying winner announcement (see spec.md:140)
- [ ] [HUD-017] [P2] [Story7] Implement TitleManager.sendTimeWarning(players) displaying "残り1分！" at 1 minute (see spec.md:141)
- [ ] [HUD-018] [P2] [Story7] Implement TitleManager.sendCountdown(players, seconds) displaying 10-second countdown (see spec.md:142)
- [ ] [HUD-019] [P2] [Story7] Create ActionBarManager.kt singleton for quick feedback (see plan.md:124)
- [ ] [HUD-020] [P2] [Story7] Implement ActionBarManager.sendDisguiseActivated(player, blockType) displaying "偽装: [ブロック名] | 動くと解除されます" (see spec.md:145)
- [ ] [HUD-021] [P2] [Story7] Implement ActionBarManager.sendDisguiseStatus(player, blockType, timeRemaining) updating every second (see spec.md:146)
- [ ] [HUD-022] [P2] [Story7] Implement ActionBarManager.sendDisguiseRemoved(player) displaying "偽装解除！ 再度ショップから選択できます" (see spec.md:147)
- [ ] [HUD-023] [P2] [Story7] Implement ActionBarManager.sendSeekerStatus(player, hiderCount, captureCount) updating every second (see spec.md:148)
- [ ] [HUD-024] [P2] [Story7] Display time warning in red color when less than 30 seconds remain (see spec.md:149)
- [ ] [HUD-025] [P2] [Story7] Throttle action bar updates to once per second to avoid flickering (see spec.md:286-287)
- [ ] [HUD-026] [P2] [Story7] Integrate ScoreboardManager.createGameScoreboard() into GameManager.startGame()
- [ ] [HUD-027] [P2] [Story7] Integrate TitleManager calls into game lifecycle events (start, phase change, capture, end)
- [ ] [HUD-028] [P2] [Story7] Integrate ActionBarManager calls into DisguiseManager events (disguise, undisguise)
- [ ] [HUD-029] [P2] [Story7] Implement ScoreboardManager.restoreScoreboard(player) when game ends (see spec.md:217)
- [ ] [HUD-030] [P2] [Story7] Handle scoreboard conflicts with other plugins by taking priority during games (see spec.md:278-279)
- [ ] [HUD-031] [P2] [Story7] Write unit test for role-specific scoreboard content (see quickstart.md:337-355)
- [ ] [HUD-032] [P2] [Story7] Write integration test for scoreboard updates during game flow (see quickstart.md:337-355)

**Dependencies**: Requires BOUNDARY-001 through BOUNDARY-011
**Parallel Opportunities**: HUD-001/012/019 in parallel (manager creation), then HUD-002 through HUD-011 in sequence, then HUD-013 through HUD-018 in sequence, then HUD-020 through HUD-025 in sequence, then HUD-026/027/028/029/030 in sequence, then HUD-031/032 in parallel

---

## Phase 10: Story 5 - Collision Handling (P2)

**Goal**: Verify collision mechanics for disguise blocks and invisible players

- [ ] [COLLISION-001] [P2] [Story5] Verify real block collision when hider is disguised (Minecraft default behavior for placed blocks) (see spec.md:92)
- [ ] [COLLISION-002] [P2] [Story5] Verify invisible player hitbox collision (Minecraft default behavior for invisible entities) (see spec.md:93)
- [ ] [COLLISION-003] [P2] [Story5] Write integration test for collision with disguise blocks (player cannot walk through) (see spec.md:92-94)
- [ ] [COLLISION-004] [P2] [Story5] Write integration test for collision with invisible player hitbox (see spec.md:93)

**Dependencies**: Requires HUD-001 through HUD-032
**Parallel Opportunities**: COLLISION-001/002 can be verified in parallel (Minecraft behavior), then COLLISION-003/004 in parallel

---

## Phase 11: Story 6 - Visual and Audio Feedback (P3)

**Goal**: Add particle effects and sound feedback for disguise actions

- [ ] [FEEDBACK-001] [P3] [Story6] Add particle effect (SPELL_MOB) when disguise is activated (see spec.md:108)
- [ ] [FEEDBACK-002] [P3] [Story6] Add particle effect (BLOCK_DUST) when disguise is removed (see spec.md:109)
- [ ] [FEEDBACK-003] [P3] [Story6] Add sound (NOTE_PLING) when disguise is activated (see spec.md:108)
- [ ] [FEEDBACK-004] [P3] [Story6] Add sound (BLOCK_BREAK) when disguise is removed (see spec.md:109)
- [ ] [FEEDBACK-005] [P3] [Story6] Add confirmation message in action bar when disguise succeeds (see spec.md:110)
- [ ] [FEEDBACK-006] [P3] [Story6] Add notification explaining why disguise was removed (movement, capture, etc.) (see spec.md:111)
- [ ] [FEEDBACK-007] [P3] [Story6] Ensure particles and sounds trigger within 0.3 seconds of action (see spec.md:SC-008)
- [ ] [FEEDBACK-008] [P3] [Story6] Write integration test for visual/audio feedback on disguise cycle

**Dependencies**: Requires COLLISION-001 through COLLISION-004
**Parallel Opportunities**: FEEDBACK-001/002 in parallel, then FEEDBACK-003/004 in parallel, then FEEDBACK-005/006 in parallel, then FEEDBACK-007/008 in parallel

---

## Phase 12: Story 3 - Re-disguising (P2)

**Goal**: Verify re-disguise functionality (already implemented in Phase 6)

- [ ] [REDISGUISE-001] [P2] [Story3] Verify re-disguise after movement (open shop → select → new disguise) (see spec.md:58-59)
- [ ] [REDISGUISE-002] [P2] [Story3] Verify old block removal when re-disguising at new location (see spec.md:59-60)
- [ ] [REDISGUISE-003] [P2] [Story3] Verify no orphaned blocks remain from previous disguises (see spec.md:60-61)
- [ ] [REDISGUISE-004] [P2] [Story3] Verify changing disguise type without moving (same location, different block) (see spec.md:61-62)
- [ ] [REDISGUISE-005] [P2] [Story3] Write integration test for multiple disguise cycles (disguise → move → disguise → move → disguise) (see spec.md:58-62)

**Dependencies**: Requires FEEDBACK-001 through FEEDBACK-008
**Parallel Opportunities**: REDISGUISE-001/002/003/004 can be verified in parallel, then REDISGUISE-005

---

## Phase 13: Polish & Cross-Cutting Concerns

**Goal**: Final integration, performance testing, and documentation

- [ ] [POLISH-001] [P1] Complete integration test suite covering all 11 user stories end-to-end
- [ ] [POLISH-002] [P1] Verify all 92 functional requirements (FR-001 through FR-092) are implemented
- [ ] [POLISH-003] [P1] Verify all 27 success criteria (SC-001 through SC-027) are met
- [ ] [POLISH-004] [P1] Test with 30 concurrent players and verify TPS remains above 19.5 (see plan.md:46-47)
- [ ] [POLISH-005] [P1] Verify event processing takes less than 50ms per event (see plan.md:47)
- [ ] [POLISH-006] [P1] Verify scoreboard updates take less than 10ms per player (see plan.md:48)
- [ ] [POLISH-007] [P1] Manual QA testing of all edge cases in spec.md (33 edge cases)
- [ ] [POLISH-008] [P2] Add Japanese language messages to messages.yml (optional, currently in code)
- [ ] [POLISH-009] [P2] Add English language support via locale files (optional)
- [ ] [POLISH-010] [P2] Performance profiling with Spark plugin (see quickstart.md:505-519)
- [ ] [POLISH-011] [P3] Add PlaceholderAPI integration for %hideandseek_role%, %hideandseek_disguise% (see research.md:502-520)
- [ ] [POLISH-012] [P1] Create deployment guide in README.md with installation steps
- [ ] [POLISH-013] [P1] Create admin quick-start guide for arena setup commands
- [ ] [POLISH-014] [P1] Final code review for error handling, null safety, thread safety
- [ ] [POLISH-015] [P1] Build release JAR with `./gradlew clean shadowJar` and test on Paper 1.21.x server

**Dependencies**: Requires REDISGUISE-001 through REDISGUISE-005
**Parallel Opportunities**: POLISH-001/002/003 in sequence, then POLISH-004/005/006 in parallel, then POLISH-007, then POLISH-008/009/010/011 in parallel, then POLISH-012/013/014 in sequence, then POLISH-015

---

## Summary Statistics

- **Total Tasks**: 87
- **P1 Tasks**: 74 (85%)
- **P2 Tasks**: 11 (13%)
- **P3 Tasks**: 2 (2%)
- **Estimated Duration**: 8 weeks (following quickstart.md roadmap)

### Task Breakdown by Phase

1. **Setup**: 9 tasks (1 week)
2. **Foundational**: 9 tasks (1 week)
3. **Arena Setup (Story 8)**: 20 tasks (1 week)
4. **Game Lifecycle (Story 9)**: 30 tasks (1 week)
5. **Shop System (Story 1)**: 22 tasks (1 week)
6. **Disguise System (Story 2/4)**: 31 tasks (1 week)
7. **Game End (Story 10)**: 27 tasks (1 week)
8. **Boundary (Story 11)**: 11 tasks (0.5 weeks)
9. **HUD (Story 7)**: 32 tasks (1 week)
10. **Collision (Story 5)**: 4 tasks (0.5 weeks)
11. **Feedback (Story 6)**: 8 tasks (0.5 weeks)
12. **Re-disguise (Story 3)**: 5 tasks (0.5 weeks)
13. **Polish**: 15 tasks (1 week)

### Parallel Execution Opportunities

Many tasks within each phase can be executed in parallel:
- **Setup Phase**: 6 tasks can run concurrently (file creation)
- **Foundational Phase**: 3 parallel groups (configs, utilities, tests)
- **Arena Phase**: 4 parallel groups (entities, manager methods, commands, tests)
- **Game Phase**: 5 parallel groups (entities, manager methods, commands, listeners, tests)
- **Shop Phase**: 4 parallel groups (entities, GUI, listeners, tests)
- **Disguise Phase**: 5 parallel groups (entities, manager, listeners, validations, tests)

With 2-3 developers working in parallel, estimated time can be reduced from 8 weeks to approximately 4-5 weeks.

### MVP Scope (Minimum Viable Product)

To deliver a playable game as quickly as possible, focus on:
- **Phase 1-8** (Setup through Boundary): 168 P1 tasks
- This delivers all P1 user stories (Stories 1, 2, 4, 8, 9, 10, 11)
- Estimated time: 6 weeks

**Defer to post-MVP**:
- Phase 9 (HUD - Story 7): Can use basic chat messages initially
- Phase 10 (Collision - Story 5): Minecraft default behavior works
- Phase 11 (Feedback - Story 6): Polish feature
- Phase 12 (Re-disguise - Story 3): Already implemented in Phase 6

---

## Next Steps

1. **Review this task list** with stakeholders for approval
2. **Assign tasks** to developers based on expertise
3. **Begin Phase 1 (Setup)** to establish project foundation
4. **Follow TDD approach** - write tests before implementation (as shown in quickstart.md)
5. **Track progress** by checking off completed tasks in this file
6. **Update estimates** as actual implementation time becomes known

For detailed implementation guidance, refer to:
- **Technical decisions**: research.md
- **Data structures**: data-model.md
- **API contracts**: contracts/commands.md
- **Development workflow**: quickstart.md
- **Architecture**: plan.md
