# Feature Specification: Disguise System

**Feature Branch**: `001-disguise-system`
**Created**: 2025-10-23
**Status**: Draft
**Input**: User description: "Implement the disguise system that allows hiders to transform into blocks"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Shop Access and Block Selection (Priority: P1)

A hider player wants to hide from seekers by transforming into a block. They have a special shop item in their inventory that cannot be dropped or moved. When they right-click this item in the air, a shop GUI opens showing categories. They navigate to the disguise blocks category, select a specific block type, and instantly become disguised as that block at their current position.

**Why this priority**: This is the core mechanic of the disguise system. Without this, hiders cannot hide effectively, and the game cannot function. The shop interface is the gateway to all disguise functionality.

**Independent Test**: Can be fully tested by having a single hider player right-click the shop item, navigate through the category system, select a block, and verify they become invisible with a block placed at their feet. Delivers immediate value by enabling the basic hiding mechanic.

**Acceptance Scenarios**:

1. **Given** a hider is in an active game, **When** they check their inventory, **Then** they see a special shop item that cannot be dropped or moved to other slots
2. **Given** a hider has the shop item, **When** they right-click it in the air (not targeting a block), **Then** a shop GUI opens displaying available categories
3. **Given** the shop GUI is showing categories, **When** the hider clicks on the "Disguise Blocks" category, **Then** the GUI displays all available block types for disguise
4. **Given** the disguise blocks are displayed, **When** the hider clicks on a specific block type (e.g., stone, dirt, wood), **Then** the block is placed at their feet position and they become invisible
5. **Given** a hider is standing in an open area, **When** they select a block from the shop, **Then** the block appears at their exact foot location and they can no longer see their own body
6. **Given** a hider successfully disguises, **When** another player looks at the disguised location, **Then** they see only the block, not the player model
7. **Given** a hider tries to drop the shop item, **When** they press the drop key, **Then** nothing happens and the item stays in their inventory
8. **Given** a hider tries to move the shop item to another slot, **When** they attempt to drag it, **Then** the item remains in its original slot

---

### User Story 2 - Disguise Maintenance and Movement Detection (Priority: P1)

While disguised as a block, a hider needs to remain completely still. If they move even slightly, their disguise should be removed immediately to maintain game balance.

**Why this priority**: This is critical for game balance. Without movement detection, hiders could move while disguised, making the game unfair and breaking the core mechanic.

**Independent Test**: Can be tested by having a disguised player attempt to move in any direction. The system should immediately remove the disguise and make the player visible again.

**Acceptance Scenarios**:

1. **Given** a hider is disguised as a block, **When** they press movement keys (WASD) to move in any direction, **Then** the disguise is immediately removed, the block disappears, and they become visible
2. **Given** a hider is disguised as a block, **When** they attempt to jump (press spacebar), **Then** the disguise is removed before the jump completes
3. **Given** a disguised hider's block is in place, **When** they are knocked back by external forces (e.g., explosions, other players), **Then** the disguise remains intact and the player stays invisible
4. **Given** a hider's disguise is removed by player-initiated movement, **When** the removal occurs, **Then** they receive a notification indicating their disguise was broken

---

### User Story 3 - Re-disguising and Location Changes (Priority: P2)

After a hider's disguise is removed (by movement or choice), they want to disguise again at a new location. They should be able to open the shop and select any block type again without restriction.

**Why this priority**: This enables dynamic gameplay where hiders can relocate and hide multiple times during a match. While important, the basic disguise mechanic (P1) must work first.

**Independent Test**: Can be tested by having a player disguise, move to break the disguise, then disguise again at a new location. Verifies the system allows multiple disguise cycles.

**Acceptance Scenarios**:

1. **Given** a hider's disguise was removed by movement, **When** they stop at a new location and open the shop, **Then** they can select a block and disguise again
2. **Given** a hider is disguised at location A, **When** they manually break their disguise (by moving), move to location B, and disguise again, **Then** the old block at location A is removed and a new block appears at location B
3. **Given** a hider has disguised multiple times, **When** they disguise at their current location, **Then** no blocks from previous disguises remain in the world
4. **Given** a hider wants to change their disguise without moving, **When** they open the shop and select a different block type while already disguised, **Then** the existing block changes to the new type at the same location

---

### User Story 4 - Disguise Detection by Seekers (Priority: P1)

A seeker wants to capture hiders by attacking suspicious blocks. When they attack a disguised block, the hider should be captured immediately.

**Why this priority**: This is the core counter-mechanic to disguising. Without this, seekers cannot capture disguised hiders, breaking the game entirely.

**Independent Test**: Can be tested by having a seeker attack a disguised hider's block. The hider should be captured, sent to spectator mode, and the block should disappear.

**Acceptance Scenarios**:

1. **Given** a hider is disguised as a block, **When** a seeker attacks (left-clicks) that block, **Then** the hider is captured, the block is removed, and the hider enters spectator mode
2. **Given** a seeker attacks a disguised block, **When** the capture occurs, **Then** no damage is dealt to either player, only the capture mechanic triggers
3. **Given** a hider is captured by block attack, **When** the capture completes, **Then** the seeker receives a capture reward and all players see a capture announcement
4. **Given** a seeker attacks a regular world block (not a disguise), **When** the attack occurs, **Then** nothing happens (no capture, no block breaking)

---

### User Story 5 - Collision Handling for Disguise Blocks (Priority: P2)

When a hider is disguised, both the block and the invisible player should have collision. Other players should not be able to walk through the block or the hidden player's hitbox.

**Why this priority**: This prevents seekers from easily identifying disguises by walking through blocks. It's important for realism but the core disguise mechanic must work first.

**Independent Test**: Can be tested by having a seeker or another hider attempt to walk through a disguised block. They should collide with it as if it were a real block.

**Acceptance Scenarios**:

1. **Given** a hider is disguised as a block, **When** another player walks toward the block, **Then** they collide with it and cannot pass through
2. **Given** a hider is disguised, **When** another player attempts to stand in the same location, **Then** they are blocked by the invisible player's hitbox
3. **Given** multiple blocks exist in a small area (including disguise blocks), **When** a player navigates the area, **Then** disguise blocks feel identical to normal blocks in terms of collision

---

### User Story 6 - Visual and Audio Feedback (Priority: P3)

When a hider disguises or undisguises, they should receive clear visual and audio feedback to confirm the action. This helps players understand the system state.

**Why this priority**: While helpful for user experience, the core mechanics (P1-P2) must work first. This is polish that improves clarity but isn't essential for functionality.

**Independent Test**: Can be tested by observing particle effects and sounds when disguising and undisguising. Delivers improved user experience without affecting core mechanics.

**Acceptance Scenarios**:

1. **Given** a hider selects a block from the shop, **When** they become disguised, **Then** particles appear at their location and a confirmation sound plays
2. **Given** a hider is disguised, **When** they move and break the disguise, **Then** particles appear and a different sound plays to indicate disguise removal
3. **Given** a hider successfully disguises, **When** the action completes, **Then** they see a confirmation message in their action bar or chat
4. **Given** a disguise is broken automatically (by movement), **When** this occurs, **Then** the player receives a notification explaining why the disguise was removed

---

### User Story 7 - HUD Display and Game Status Visualization (Priority: P2)

Players need to see real-time game information through various HUD elements. The sidebar scoreboard shows persistent game state (time, player counts, role, disguise status), the title display announces major events (game start, phase changes, game end), and the action bar provides immediate feedback for quick actions (disguise changes, time warnings).

**Why this priority**: While not essential for basic gameplay, clear HUD information significantly improves player experience and reduces confusion. Players need to understand the game state, their role, and remaining time. This should be implemented after core mechanics (P1) but before polish features (P3).

**Independent Test**: Can be tested by observing scoreboard updates during different game phases, verifying title animations for events, and checking action bar messages during disguise actions. Delivers value by keeping players informed without affecting core gameplay.

**Acceptance Scenarios**:

#### Sidebar Scoreboard

1. **Given** a hider is in an active game during the seek phase, **When** they look at the sidebar, **Then** they see: game title, current time/total time, seeker count, remaining hider count, captured count, their role, their money, disguise status, and escape item status
2. **Given** a seeker is in an active game, **When** they look at the sidebar, **Then** they see: game title, current time/total time, seeker count, remaining hider count, their capture count, their role, and their money
3. **Given** a hider becomes disguised, **When** the disguise activates, **Then** the sidebar updates to show "偽装: [ブロック名]" within 1 second
4. **Given** a hider's disguise is removed, **When** the removal occurs, **Then** the sidebar updates to show "偽装: なし" within 1 second
5. **Given** a hider is captured, **When** they enter spectator mode, **Then** the sidebar shows spectator-specific information (remaining players, time left)
6. **Given** the game is in preparation phase, **When** players check the sidebar, **Then** they see "準備中" phase indicator and countdown timer

#### Title Display

7. **Given** the game starts, **When** players are assigned roles, **Then** they see a title animation announcing their role ("あなたはハイダーです" or "あなたはシーカーです") with subtitle showing objective
8. **Given** the preparation phase ends, **When** the seek phase begins, **Then** all players see a title "探索開始！" with seekers' blindness effect removed
9. **Given** a hider is captured, **When** the capture occurs, **Then** the captured player sees a title "捕獲されました！" and other players see an announcement
10. **Given** the game ends, **When** time expires or all hiders are captured, **Then** all players see a title announcing the result ("ハイダーの勝利！" or "シーカーの勝利！")
11. **Given** 1 minute remains in the game, **When** the timer hits 1:00, **Then** all players see a title warning "残り1分！"
12. **Given** 10 seconds remain in the game, **When** the timer hits 0:10, **Then** all players see a countdown title (10, 9, 8...)

#### Action Bar

13. **Given** a hider disguises as a block, **When** the disguise activates, **Then** they see an action bar message "偽装: [ブロック名] | 動くと解除されます"
14. **Given** a hider is disguised, **When** each second passes, **Then** the action bar shows "偽装中: [ブロック名] | 残り時間: [時間]"
15. **Given** a hider's disguise breaks, **When** they move, **Then** the action bar immediately shows "偽装解除！ 再度ショップから選択できます"
16. **Given** a seeker is searching, **When** each second passes, **Then** the action bar shows "残りハイダー: [数] | あなたの捕獲数: [数]"
17. **Given** less than 30 seconds remain, **When** time is critical, **Then** the action bar shows time in red color with warning

---

### User Story 8 - Arena Setup and Configuration (Priority: P1)

An administrator needs to create and configure game arenas before players can play. They select two corner positions to define the play area boundaries, set spawn points for seekers and hiders, and create the arena with a unique name. The system validates the configuration and saves it for future games.

**Why this priority**: Without arenas, games cannot be played. This is a prerequisite for all gameplay, making it P1 priority.

**Independent Test**: Can be tested by an admin using position selection commands, setting spawn points, creating an arena, and verifying the configuration is saved correctly. Delivers value by enabling game setup.

**Acceptance Scenarios**:

1. **Given** an admin is standing at a desired corner of the play area, **When** they execute `/hs admin setpos1`, **Then** the first corner position is stored and they receive confirmation
2. **Given** an admin has set pos1, **When** they move to the opposite corner and execute `/hs admin setpos2`, **Then** the second corner position is stored and they see the area volume
3. **Given** pos1 and pos2 are set, **When** the admin stands at the desired seeker spawn location and executes `/hs admin setspawn seeker`, **Then** the seeker spawn point is stored with coordinates and rotation
4. **Given** spawn points are configured, **When** the admin stands at the hider spawn location and executes `/hs admin setspawn hider`, **Then** the hider spawn point is stored
5. **Given** all positions are configured, **When** the admin executes `/hs admin creategame <name>`, **Then** an arena is created in arenas.yml with all settings and the admin receives confirmation
6. **Given** an arena already exists with the same name, **When** the admin tries to create it again, **Then** they receive an error message asking to delete the old one first or use a different name
7. **Given** pos1 and pos2 are set in different worlds, **When** the admin tries to create the arena, **Then** they receive an error that positions must be in the same world
8. **Given** an admin wants to view all arenas, **When** they execute `/hs admin list`, **Then** they see a list of all configured arenas with names, worlds, and player counts
9. **Given** an admin wants to remove an arena, **When** they execute `/hs admin delete <name>`, **Then** the arena is removed from arenas.yml after confirmation

---

### User Story 9 - Game Start and Player Initialization (Priority: P1)

An administrator starts a game on a configured arena. The system prepares players by backing up and clearing their inventories, assigning roles (23% seekers, 77% hiders) randomly, teleporting them to spawn points, giving shop items to hiders, applying blindness to seekers, and starting the preparation phase countdown.

**Why this priority**: This is the core game initialization flow. Without this, games cannot begin, making it P1 priority.

**Independent Test**: Can be tested by starting a game with multiple players and verifying role assignment, teleportation, inventory management, and phase initialization all work correctly. Delivers immediate value by enabling gameplay.

**Acceptance Scenarios**:

1. **Given** players have joined with `/hs join`, **When** an admin executes `/hs start <arena-name>`, **Then** the game initialization begins
2. **Given** less than 2 players have joined, **When** admin tries to start the game, **Then** they receive an error "最小2人のプレイヤーが必要です"
3. **Given** the specified arena doesn't exist, **When** admin tries to start it, **Then** they receive an error listing available arenas
4. **Given** game initialization starts, **When** roles are assigned, **Then** approximately 23% of players become seekers and 77% become hiders (with minimum 1 seeker)
5. **Given** roles are assigned, **When** players are prepared, **Then** their current inventory is backed up, cleared, and their location is saved
6. **Given** players are prepared, **When** teleportation occurs, **Then** seekers go to seeker spawn, hiders go to hider spawn, maintaining their view direction
7. **Given** players are teleported, **When** items are distributed, **Then** hiders receive the shop item in the configured slot and seekers receive nothing special
8. **Given** the preparation phase starts, **When** it begins, **Then** seekers receive blindness effect and all players see a title announcing their role and a 30-second countdown
9. **Given** the preparation phase is active, **When** time progresses, **Then** the scoreboard shows "準備中" and the remaining preparation time
10. **Given** the preparation phase ends, **When** 30 seconds elapse, **Then** seekers' blindness is removed, the seek phase begins, and all players see "探索開始！" title
11. **Given** game starts, **When** the arena world's WorldBorder is checked, **Then** the original WorldBorder settings are backed up before applying arena boundaries
12. **Given** arena boundaries are configured, **When** game starts, **Then** WorldBorder is set to the arena's center and size calculated from pos1/pos2

---

### User Story 10 - Game End and Cleanup (Priority: P1)

When a game ends (all hiders captured or time expires), the system determines the winner, displays results, distributes rewards, cleans up all game state (disguise blocks, shop items, effects, WorldBorder), restores player inventories, and teleports players back to their original locations.

**Why this priority**: Proper game cleanup is essential to prevent bugs, memory leaks, and item loss. Without this, players would be stuck in game state forever, making it P1 priority.

**Independent Test**: Can be tested by ending a game through both win conditions and verifying all cleanup steps complete successfully, all players return to normal state, and no game artifacts remain. Delivers value by properly concluding gameplay.

**Acceptance Scenarios**:

1. **Given** all hiders are captured, **When** the last hider is caught, **Then** the game immediately ends with "シーカーの勝利！" result
2. **Given** the seek phase timer reaches 0:00, **When** time expires, **Then** the game ends with "ハイダーの勝利！" if any hiders remain
3. **Given** the game ends, **When** results are determined, **Then** all players see a title announcing the winner for 5 seconds
4. **Given** results are displayed, **When** rewards are calculated, **Then** winners receive the victory reward, seekers receive capture bonuses, and all players receive participation rewards via Vault
5. **Given** rewards are distributed, **When** cleanup begins, **Then** all disguise blocks are removed from the world (set to air)
6. **Given** cleanup is in progress, **When** shop items are removed, **Then** all shop items are removed from all player inventories
7. **Given** cleanup continues, **When** effects are cleared, **Then** all potion effects (invisibility, blindness, etc.) are removed from all players
8. **Given** cleanup continues, **When** scoreboard is restored, **Then** each player's previous scoreboard (backed up at game start) is restored
9. **Given** cleanup continues, **When** WorldBorder is restored, **Then** the world's WorldBorder returns to its original center, size, and settings from before the game
10. **Given** cleanup continues, **When** inventories are restored, **Then** each player's backed-up inventory is restored to them exactly as it was before the game
11. **Given** all cleanup is complete, **When** players are released, **Then** they are teleported back to their original location (saved at game start)
12. **Given** an admin wants to force-end a game, **When** they execute `/hs stop <arena-name>`, **Then** the game ends immediately and full cleanup occurs with no winner declared
13. **Given** a player disconnects during game, **When** disconnect occurs, **Then** their disguise block (if any) is removed, they are counted as eliminated, and their backed-up state is saved for restoration on reconnect
14. **Given** the server crashes during a game, **When** the server restarts, **Then** no game artifacts remain (disguise blocks, shop items) and WorldBorder is at default values

---

### User Story 11 - Game Boundary Enforcement (Priority: P1)

During an active game, players must stay within the arena boundaries defined by WorldBorder. When players approach or touch the boundary, they see visual warnings and receive damage, preventing them from leaving the play area. This ensures fair gameplay and prevents players from exploiting areas outside the intended game zone.

**Why this priority**: Without boundary enforcement, players could escape the arena, hide outside the intended area, or abuse the game mechanics. This is essential for fair gameplay, making it P1 priority.

**Independent Test**: Can be tested by having a player approach and touch the arena boundary during a game and verifying WorldBorder effects (visual warning, damage, pushback) work correctly. Delivers value by maintaining game integrity.

**Acceptance Scenarios**:

1. **Given** a game is active, **When** a player is within the arena boundaries, **Then** they can move freely without any boundary effects
2. **Given** a player approaches the WorldBorder, **When** they get close (within 5 blocks), **Then** they see a red vignette effect warning them of the boundary
3. **Given** a player touches the WorldBorder, **When** they try to pass through, **Then** they are pushed back into the arena and cannot exit
4. **Given** a player is at the WorldBorder, **When** they remain there, **Then** they receive periodic damage (Minecraft default WorldBorder damage)
5. **Given** a player receives WorldBorder damage, **When** damage is taken, **Then** they also see a warning message "ゲームエリアの外に出ることはできません！"
6. **Given** a disguised hider is at the boundary, **When** they receive WorldBorder damage, **Then** their disguise is NOT automatically removed (external force, not player movement)
7. **Given** the game ends, **When** WorldBorder is restored, **Then** boundary enforcement stops and players can move freely again

---

### Edge Cases

#### Arena and Game Lifecycle Edge Cases

- What happens when an admin tries to set pos2 before pos1?
  - The system should allow it and just store pos2; both positions can be set in any order
- What happens when an admin tries to create an arena without setting all required positions?
  - The system should validate and show an error listing missing positions (pos1, pos2, seeker spawn, hider spawn)
- What happens when the calculated arena size is too small (e.g., 5x5 blocks)?
  - The system should warn the admin but allow creation (configurable minimum size)
- What happens when a player disconnects during the preparation phase?
  - They are removed from the game, and if too few players remain (< 2), the game is cancelled
- What happens when a player disconnects during the seek phase?
  - Hiders: counted as captured; Seekers: remain in seeker count but become inactive
- What happens when all seekers disconnect?
  - The game ends immediately with hiders winning
- What happens when WorldBorder restoration fails (original settings lost)?
  - Use default fallback values (center: world spawn, size: 29999984 blocks - Minecraft default)
- What happens when inventory restoration fails (backup data corrupted)?
  - Log the error and give the player an empty inventory rather than crashing
- What happens when a player joins multiple times?
  - Prevent duplicate joins; show message "すでに参加しています"
- What happens when an admin starts a game that's already running?
  - Show error message "このアリーナは既にゲーム実行中です"
- What happens when an arena's world is unloaded or deleted?
  - Show error when trying to start the game; allow admin to delete the invalid arena
- What happens when a player is teleported back to their original location but that location is now invalid (void, lava, etc.)?
  - Teleport to world spawn as a safe fallback

#### HUD System Edge Cases

- What happens when scoreboard updates conflict with other plugins' scoreboards?
  - The game should take priority during active games, and restore previous scoreboard when game ends
- What happens when a player's name is too long for scoreboard display?
  - Names should be truncated with "..." to fit within scoreboard width limits
- What happens when multiple title messages are sent rapidly (e.g., capture during countdown)?
  - Title messages should queue or the more important message (game result) should override less important ones
- What happens when the scoreboard has too many lines for the screen?
  - The design should ensure scoreboard stays within 15 lines maximum
- What happens when action bar messages are sent every tick?
  - Updates should be throttled to once per second to avoid flickering

#### Shop System Edge Cases

- What happens when a player tries to drop the shop item?
  - The drop action should be cancelled and the item remains in inventory
- What happens when a player tries to move the shop item to another inventory slot?
  - The move action should be cancelled and the item stays in its designated slot
- What happens when a player right-clicks the shop item while targeting a block?
  - Nothing should happen; the shop only opens when right-clicking in the air
- What happens when a player opens the shop GUI and the game ends?
  - The GUI should be automatically closed and the shop item removed from inventory
- What happens when shop.yml is missing or has invalid configuration?
  - The system should use default values (basic blocks) and log an error message
- What happens when a player has the shop GUI open and disconnects?
  - The GUI state should be cleaned up to prevent memory leaks

#### Disguise System Edge Cases

- What happens when a hider tries to disguise in a location where a block already exists (not air)?
  - The disguise should be prevented with an error message indicating the location is obstructed
- What happens when a hider tries to disguise while falling or in mid-air?
  - The disguise should be prevented until the player lands on solid ground
- What happens when two hiders try to disguise at the exact same block position?
  - Only the first hider's disguise should succeed; the second should receive an error that the location is occupied
- What happens when a disguised block is affected by game events (explosions, block updates, etc.)?
  - The disguise should be removed if the block position becomes invalid or affected by world changes
- What happens when a hider disguises at the edge of the game boundary?
  - The disguise should work normally as long as the position is within valid game bounds
- What happens when the game ends while a player is disguised?
  - All disguise blocks should be automatically removed and players made visible for the results phase
- What happens if a seeker accidentally attacks a natural block thinking it's a disguise?
  - Nothing should happen; blocks should not break, and no capture occurs
- What happens when a hider disconnects while disguised?
  - The disguise block should be removed immediately upon disconnect
- What happens when a hider is in water or lava and tries to disguise?
  - The disguise should be prevented with an error message indicating the player cannot disguise while in liquid
- What happens when a disguised hider is pushed by another player or affected by knockback/explosions?
  - The disguise should remain intact; only player-initiated movement (WASD keys, jumping) should break the disguise

## Requirements *(mandatory)*

### Functional Requirements

#### Arena Management Requirements

- **FR-001**: System MUST provide commands for admins to set position 1 (`/hs admin setpos1`) and position 2 (`/hs admin setpos2`) at their current location
- **FR-002**: System MUST provide commands to set seeker spawn (`/hs admin setspawn seeker`) and hider spawn (`/hs admin setspawn hider`) with location and rotation
- **FR-003**: System MUST provide command to create arena (`/hs admin creategame <name>`) that saves all configured positions to arenas.yml
- **FR-004**: System MUST validate that pos1 and pos2 are in the same world before creating arena
- **FR-005**: System MUST validate that all required positions (pos1, pos2, seeker spawn, hider spawn) are set before creating arena
- **FR-006**: System MUST calculate arena center and size from pos1/pos2 coordinates automatically
- **FR-007**: System MUST prevent creating duplicate arena names
- **FR-008**: System MUST provide command to list all arenas (`/hs admin list`)
- **FR-009**: System MUST provide command to delete arenas (`/hs admin delete <name>`)
- **FR-010**: System MUST store arena configuration in arenas.yml with world name, boundaries, spawn points

#### Game Lifecycle Requirements

- **FR-011**: System MUST provide command for players to join game (`/hs join`)
- **FR-012**: System MUST maintain a list of players who have joined and are waiting for game start
- **FR-013**: System MUST provide command for admins to start game (`/hs start <arena-name>`)
- **FR-014**: System MUST validate minimum 2 players before allowing game start
- **FR-015**: System MUST assign roles randomly with 23% seekers and 77% hiders (minimum 1 seeker)
- **FR-016**: System MUST backup each player's inventory, location, and scoreboard state before game starts
- **FR-017**: System MUST clear player inventories after backup
- **FR-018**: System MUST teleport seekers to seeker spawn and hiders to hider spawn
- **FR-019**: System MUST give hiders the shop item in configured slot after teleportation
- **FR-020**: System MUST apply blindness effect to seekers during preparation phase (30 seconds default)
- **FR-021**: System MUST backup the world's current WorldBorder settings before game starts
- **FR-022**: System MUST set WorldBorder to arena boundaries (center and size) when game starts
- **FR-023**: System MUST start preparation phase countdown (30 seconds default, configurable)
- **FR-024**: System MUST remove seeker blindness and start seek phase when preparation ends
- **FR-025**: System MUST track game state (waiting, preparation, seeking, ended)
- **FR-026**: System MUST detect win conditions (all hiders captured OR time expires)
- **FR-027**: System MUST end game immediately when win condition is met
- **FR-028**: System MUST display result title to all players for 5 seconds
- **FR-029**: System MUST calculate and distribute rewards via Vault (victory reward, capture bonuses, participation reward)
- **FR-030**: System MUST remove all disguise blocks from world when game ends
- **FR-031**: System MUST remove all shop items from player inventories when game ends
- **FR-032**: System MUST clear all potion effects from players when game ends
- **FR-033**: System MUST restore each player's backed-up scoreboard when game ends
- **FR-034**: System MUST restore WorldBorder to original settings when game ends
- **FR-035**: System MUST restore each player's backed-up inventory when game ends
- **FR-036**: System MUST teleport players back to their original location when game ends
- **FR-037**: System MUST provide command for admins to force-end game (`/hs stop <arena-name>`)
- **FR-038**: System MUST handle player disconnects during game (remove disguise, count as eliminated, save backup data)
- **FR-039**: System MUST prevent duplicate joins (same player joining twice)
- **FR-040**: System MUST prevent starting game on arena that's already running
- **FR-041**: System MUST use fallback values if WorldBorder or inventory restoration fails

#### WorldBorder Boundary Requirements

- **FR-042**: System MUST use Minecraft WorldBorder API for arena boundaries
- **FR-043**: WorldBorder MUST display red vignette warning effect when players approach (within 5 blocks)
- **FR-044**: WorldBorder MUST push players back when they touch the boundary
- **FR-045**: WorldBorder MUST deal periodic damage to players at the boundary (Minecraft default behavior)
- **FR-046**: System MUST display warning message "ゲームエリアの外に出ることはできません！" when players take WorldBorder damage
- **FR-047**: WorldBorder damage MUST NOT automatically remove disguises (external force, not player movement)

#### HUD System Requirements

- **FR-048**: System MUST display a sidebar scoreboard to all players during active games
- **FR-049**: Sidebar scoreboard MUST show game title, current time/total time, and phase indicator
- **FR-050**: Sidebar scoreboard MUST show role-specific information (hider: disguise status and escape item; seeker: capture count)
- **FR-051**: Sidebar scoreboard MUST show player counts (seeker count, remaining hider count, captured count)
- **FR-052**: Sidebar scoreboard MUST update within 1 second when game state changes (disguise, capture, time)
- **FR-053**: System MUST display title messages for major events (game start, phase change, capture, game end, time warnings)
- **FR-054**: System MUST show role assignment title when game starts ("あなたはハイダーです" or "あなたはシーカーです")
- **FR-055**: System MUST show phase transition title when preparation ends ("探索開始！")
- **FR-056**: System MUST show game result title when game ends (winner announcement)
- **FR-057**: System MUST show time warning titles at 1 minute and 10-second countdown
- **FR-058**: System MUST display action bar messages for immediate feedback (disguise status, time remaining, capture count)
- **FR-059**: Action bar MUST update at least once per second during active gameplay
- **FR-060**: System MUST use color coding in action bar for urgency (red for warnings, green for success)
- **FR-061**: System MUST restore previous scoreboard state when game ends or player leaves
- **FR-062**: System MUST limit scoreboard to maximum 15 lines to prevent overflow
- **FR-063**: System MUST truncate long player names to fit scoreboard width constraints

#### Shop System Requirements

- **FR-064**: System MUST provide a special shop item to all hider players when the game starts
- **FR-065**: Shop item MUST be non-droppable and non-movable (locked to its inventory slot)
- **FR-066**: System MUST open the shop GUI when a hider right-clicks the shop item in the air (PlayerInteractEvent with Action.RIGHT_CLICK_AIR)
- **FR-067**: System MUST NOT open the shop GUI when the shop item is right-clicked on a block
- **FR-068**: Shop GUI MUST display a category selection screen as the initial view
- **FR-069**: System MUST provide a "Disguise Blocks" category in the shop
- **FR-070**: System MUST display all available block types when the "Disguise Blocks" category is selected
- **FR-071**: System MUST load shop structure and items from shop.yml configuration file
- **FR-072**: System MUST prevent the shop item from being removed from player inventory during the game
- **FR-073**: System MUST remove the shop item when the game ends or the player leaves the game

#### Disguise Mechanics Requirements

- **FR-074**: System MUST display a selection of block types in the disguise blocks category (configurable via shop.yml)
- **FR-028**: Hiders MUST be able to select any block type from the shop to initiate disguise at their current position
- **FR-029**: System MUST place the selected block at the player's feet location (the block their feet occupy) when disguise is activated
- **FR-030**: System MUST apply complete invisibility effect to the hider when disguised (no player model visible to any player)
- **FR-031**: System MUST remove disguise immediately when the disguised player initiates movement input (WASD keys or jump), but MUST NOT remove disguise from external forces (knockback, explosions, etc.)
- **FR-032**: System MUST detect left-click interactions (PlayerInteractEvent) by seekers on disguise blocks and trigger capture
- **FR-033**: System MUST differentiate between disguise blocks and natural world blocks when processing attacks
- **FR-034**: System MUST maintain collision detection for both the disguise block and the invisible player's hitbox
- **FR-035**: System MUST prevent multiple players from disguising at the same block position
- **FR-036**: System MUST remove disguise blocks when the player undisguises (by movement or capture)
- **FR-037**: System MUST allow players to re-disguise an unlimited number of times during a game
- **FR-038**: System MUST remove all active disguise blocks when the game ends
- **FR-039**: System MUST remove a player's disguise block when they disconnect
- **FR-040**: System MUST prevent disguise activation when the target block position is obstructed, invalid, or when the player is in liquid (water or lava)
- **FR-041**: System MUST track which blocks in the world are disguise blocks versus natural blocks
- **FR-042**: System MUST provide visual feedback (particles) when disguise is activated and deactivated
- **FR-043**: System MUST provide audio feedback (sounds) when disguise is activated and deactivated
- **FR-044**: System MUST send notifications to players confirming disguise activation and explaining removal reasons
- **FR-045**: System MUST ensure disguise blocks do not persist after server restart or world reload

### Key Entities

- **Scoreboard Display**: The sidebar scoreboard shown to players. Contains: title, lines of text with dynamic placeholders, update frequency, player-specific data
- **Scoreboard Line**: A single line of text in the scoreboard. Contains: line number, text content with placeholders (e.g., {time}, {hiders}, {role}), color codes
- **Title Message**: A large center-screen message. Contains: main title text, subtitle text, fade-in/stay/fade-out timings, priority level
- **Action Bar Message**: A text message above the hotbar. Contains: message text, color formatting, timestamp of last update
- **HUD State**: Tracks the current HUD state for each player. Contains: active scoreboard, previous scoreboard (for restoration), last action bar message, pending title messages
- **Shop Item**: A special inventory item that opens the shop GUI. Properties: non-droppable, non-movable, always present in hider's inventory during game, item type and display configurable via shop.yml
- **Shop Category**: A grouping of related shop items. Contains: category name, display icon, list of items in the category. Examples: "Disguise Blocks", "Power-ups" (future)
- **Shop GUI State**: Tracks the current view state of the shop for each player. Contains: current category (or main menu), player UUID, timestamp of last interaction
- **Disguise Block Entry**: Configuration entry in shop.yml defining an available disguise block. Contains: block type/material, display name, icon, slot position in GUI, price (currently free for disguise blocks)
- **Disguise Block Instance**: Represents a placed block that conceals a hider player in the game world. Contains: block location, block type/material, associated player UUID, creation timestamp
- **Hider Player State**: Tracks whether a hider is currently disguised, their disguise location, and their selected block type
- **Movement Event**: Represents a player's movement that can break disguise. Contains: player, old position, new position, movement distance, movement cause (player input vs external force)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Hiders can successfully disguise as blocks within 3 seconds of opening the shop and selecting a block type
- **SC-002**: Disguise removal occurs within 0.5 seconds of detecting player-initiated movement input
- **SC-003**: Seekers can successfully capture disguised hiders by attacking the disguise block with 100% reliability
- **SC-004**: System correctly differentiates between disguise blocks and natural blocks in 100% of attack cases (no false captures or missed captures)
- **SC-005**: No disguise blocks remain in the world after game conclusion or player disconnect (100% cleanup rate)
- **SC-006**: Players can re-disguise an unlimited number of times during a match without performance degradation
- **SC-007**: Multiple disguised players in close proximity (within 10 blocks) do not cause collision detection issues or performance drops
- **SC-008**: Visual and audio feedback for disguise actions occurs within 0.3 seconds of the action
- **SC-009**: Players correctly understand when and why their disguise is removed (measured by reduction in confusion-related questions)
- **SC-010**: System prevents invalid disguise attempts (obstructed locations, mid-air, occupied positions, liquid environments) with clear error messages in 100% of cases
- **SC-011**: Shop item remains non-droppable and non-movable throughout the entire game session (100% reliability)
- **SC-012**: Shop GUI opens within 0.5 seconds of right-clicking the shop item in the air
- **SC-013**: Scoreboard updates reflect game state changes within 1 second (disguise changes, captures, time updates)
- **SC-014**: Title messages display for major events with appropriate timing (visible long enough to read but not obstructive)
- **SC-015**: Action bar messages update at least once per second during active gameplay without flickering
- **SC-016**: Players can clearly identify their role, current status, and remaining time by glancing at HUD elements
- **SC-017**: Scoreboard remains within 15 lines and is readable on all screen resolutions
- **SC-018**: Previous scoreboard state is correctly restored when player leaves game (100% restoration rate)
- **SC-019**: Arena creation completes successfully with all positions validated and saved to arenas.yml (100% success rate for valid inputs)
- **SC-020**: Game starts within 3 seconds of admin executing start command (assuming valid arena and sufficient players)
- **SC-021**: Role assignment distributes players with 23% ± 5% as seekers (with minimum 1 seeker guaranteed)
- **SC-022**: Player inventory, location, and WorldBorder backup occurs without data loss (100% backup success rate)
- **SC-023**: Game cleanup removes all artifacts (disguise blocks, shop items, effects) with 100% completeness
- **SC-024**: WorldBorder and inventory restoration occurs with 100% success rate (or falls back to safe defaults)
- **SC-025**: Players are teleported back to original location or safe fallback within 2 seconds of game end
- **SC-026**: WorldBorder boundary enforcement prevents players from leaving arena in 100% of attempts
- **SC-027**: WorldBorder damage and warning messages display correctly when players touch boundary

## Configuration Structure *(reference)*

### shop.yml Structure

This configuration file defines the shop system structure, including the shop item properties, categories, and available items.

```yaml
# Shop Item Configuration
shop-item:
  material: EMERALD                    # The item type for the shop item
  display-name: "&a&lショップ"          # Display name (supports color codes)
  lore:                                # Item description
    - "&7右クリックでショップを開く"
  slot: 8                              # Inventory slot (0-8 for hotbar)
  glow: true                           # Whether the item should have enchantment glow

# Shop GUI Configuration
shop-gui:
  title: "&8ショップメニュー"            # Main shop GUI title
  size: 54                             # Inventory size (must be multiple of 9)

# Category Configuration
categories:
  # Disguise Blocks Category
  disguise-blocks:
    display-name: "&e偽装ブロック"      # Category display name
    icon: GRASS_BLOCK                  # Icon shown in category selection
    slot: 10                           # Slot in the main menu
    description:                       # Description lore
      - "&7ブロックに変身して隠れよう"

    # Items in this category
    items:
      stone:
        material: STONE
        display-name: "&7石"
        slot: 10
        price: 0                       # Free for disguise blocks
        lore:
          - "&7このブロックに変身します"
          - "&a無料"

      dirt:
        material: DIRT
        display-name: "&7土"
        slot: 11
        price: 0
        lore:
          - "&7このブロックに変身します"
          - "&a無料"

      grass_block:
        material: GRASS_BLOCK
        display-name: "&a草ブロック"
        slot: 12
        price: 0
        lore:
          - "&7このブロックに変身します"
          - "&a無料"

      oak_log:
        material: OAK_LOG
        display-name: "&6オークの原木"
        slot: 13
        price: 0
        lore:
          - "&7このブロックに変身します"
          - "&a無料"

      cobblestone:
        material: COBBLESTONE
        display-name: "&7丸石"
        slot: 14
        price: 0
        lore:
          - "&7このブロックに変身します"
          - "&a無料"

      sand:
        material: SAND
        display-name: "&e砂"
        slot: 15
        price: 0
        lore:
          - "&7このブロックに変身します"
          - "&a無料"

      gravel:
        material: GRAVEL
        display-name: "&7砂利"
        slot: 16
        price: 0
        lore:
          - "&7このブロックに変身します"
          - "&a無料"

# Navigation Items
navigation:
  back-button:
    material: ARROW
    display-name: "&c戻る"
    slot: 45                           # Bottom-left corner

  close-button:
    material: BARRIER
    display-name: "&c閉じる"
    slot: 49                           # Bottom-center

# Messages
messages:
  shop-opened: "&aショップを開きました"
  disguise-activated: "&a{block}に変身しました！"
  disguise-failed-obstructed: "&cその場所には変身できません（障害物）"
  disguise-failed-liquid: "&c液体の中では変身できません"
  disguise-failed-occupied: "&cその場所は既に使用されています"
  disguise-removed: "&c変身が解除されました"
  item-cannot-drop: "&cこのアイテムは捨てられません"
  item-cannot-move: "&cこのアイテムは移動できません"
```

### Configuration Notes

- **shop-item.slot**: Inventory slot where the shop item appears (0-8 for hotbar, 0 is leftmost)
- **categories.*.slot**: Position in the main shop menu GUI
- **items.*.slot**: Position within the category GUI
- **price**: Currently 0 (free) for disguise blocks, but structure supports paid items for future features
- **Color codes**: Use & for Minecraft color codes (&a = green, &c = red, &e = yellow, etc.)
- **GUI size**: Must be a multiple of 9 (9, 18, 27, 36, 45, 54)

### Default Values (if shop.yml is missing)

If shop.yml is not found, the system will use these defaults:
- Shop item: EMERALD in slot 8
- Available blocks: STONE, DIRT, GRASS_BLOCK, OAK_LOG, COBBLESTONE, SAND, GRAVEL
- All blocks free (price: 0)
- Simple GUI with no categories (direct block selection)

### arenas.yml Structure

This configuration file stores all created arenas with their boundaries and spawn points.

```yaml
# Arena Configuration
arenas:
  # Arena name (used in commands)
  main-arena:
    display-name: "メインアリーナ"         # Display name shown to players
    world: "world"                      # World name where arena exists

    # Boundaries (defined by setpos1 and setpos2)
    boundaries:
      pos1:
        x: 0
        y: 64
        z: 0
      pos2:
        x: 100
        y: 100
        z: 100
      # Auto-calculated values
      center:
        x: 50.0                          # Calculated center X
        y: 82.0                          # Calculated center Y (not used for WorldBorder)
        z: 50.0                          # Calculated center Z
      size: 100.0                        # WorldBorder size (diagonal distance from pos1 to pos2)

    # Spawn points (defined by setspawn commands)
    spawn-seeker:
      x: 50.0
      y: 64.0
      z: 50.0
      yaw: 0.0                           # View direction (horizontal)
      pitch: 0.0                         # View direction (vertical)

    spawn-hider:
      x: 50.0
      y: 64.0
      z: 80.0
      yaw: 180.0
      pitch: 0.0

  # Additional arenas can be added
  small-arena:
    display-name: "小さいアリーナ"
    world: "world"
    boundaries:
      pos1: {x: -50, y: 60, z: -50}
      pos2: {x: 50, y: 80, z: 50}
      center: {x: 0.0, y: 70.0, z: 0.0}
      size: 100.0
    spawn-seeker: {x: 0.0, y: 64.0, z: 0.0, yaw: 0.0, pitch: 0.0}
    spawn-hider: {x: 0.0, y: 64.0, z: 30.0, yaw: 180.0, pitch: 0.0}
```

### Arena Configuration Notes

- **world**: Must be a valid loaded world name
- **pos1 & pos2**: Define the rectangular play area boundaries
- **center**: Auto-calculated as midpoint between pos1 and pos2 (X and Z used for WorldBorder)
- **size**: Auto-calculated as the diagonal distance, used for WorldBorder radius
- **yaw**: 0 = south, 90 = west, 180 = north, 270 = east
- **pitch**: 0 = straight ahead, negative = looking up, positive = looking down

### Admin Commands for Arena Management

```
/hs admin setpos1               - Set first corner at current location
/hs admin setpos2               - Set second corner at current location
/hs admin setspawn seeker       - Set seeker spawn at current location
/hs admin setspawn hider        - Set hider spawn at current location
/hs admin creategame <name>     - Create arena with configured positions
/hs admin list                  - List all configured arenas
/hs admin delete <name>         - Delete an arena
/hs join                        - Join the next game
/hs start <arena-name>          - Start game on specified arena (admin only)
/hs stop <arena-name>           - Force-end active game (admin only)
```
