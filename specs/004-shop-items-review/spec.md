# Feature Specification: Shop Items Review and Rebalance

**Feature Branch**: `004-shop-items-review`
**Created**: 2025-11-04
**Status**: Draft
**Input**: User description: "ショップのアイテムを見直す"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Balanced Pricing for Strategic Choices (Priority: P1)

Players should be able to make meaningful strategic decisions in the shop based on their budget and game situation. Current item prices may not reflect their actual value, leading to dominant strategies or unused items.

**Why this priority**: Core game balance issue that affects every match. Improper pricing makes some items mandatory purchases while others are never bought, reducing strategic depth.

**Independent Test**: Start a game with 1000 coins, verify that players can afford at least 2 different strategic item combinations, and observe purchase distribution across different items in 10+ games.

**Acceptance Scenarios**:

1. **Given** a Hider has 1000 coins at game start, **When** they open the shop, **Then** they see multiple viable item options (at least 2 items within budget) that offer different strategic benefits
2. **Given** a Hider with limited funds (50 coins), **When** they need to disguise, **Then** they can choose free high-visibility blocks (melon, pumpkin) as a budget option, or save for better camouflage
3. **Given** a Hider with 100+ coins, **When** selecting disguise blocks, **Then** they face a strategic choice: spend on effective camouflage blocks (stone, dirt) or save for utility items
4. **Given** a Seeker has earned 600 coins from 3 captures, **When** they decide on next purchase, **Then** they must choose between multiple useful items rather than having one obviously best option
5. **Given** game statistics from 20 matches, **When** reviewing purchase patterns, **Then** no single item has >60% purchase rate (indicating reasonable balance)

---

### User Story 2 - Clear Item Value Communication (Priority: P1)

Players need to understand what each item does and whether it's worth the cost before purchasing. Current shop may lack clear descriptions or benefit explanations.

**Why this priority**: Essential for new player experience and informed decision-making. Players should understand value proposition before spending limited currency.

**Independent Test**: Show shop GUI to new players and ask them to explain what each item does and estimate its value - 80%+ should accurately understand basic function.

**Acceptance Scenarios**:

1. **Given** a player opens the shop for the first time, **When** they hover over any item, **Then** they see a clear description of what it does, how long it lasts, and any usage limits
2. **Given** a player is deciding between two items, **When** they compare tooltips, **Then** they can understand the relative value and strategic tradeoffs
3. **Given** a player purchases an item with a cooldown or duration, **When** they use it, **Then** they receive clear feedback about activation, duration remaining, and cooldown status

---

### User Story 3 - Camouflage-Based Disguise Block Pricing (Priority: P1)

Disguise block pricing should reflect their strategic value based on camouflage effectiveness, not rarity. High-visibility blocks (melon, gold) should be free or cheap, while environment-blending blocks (stone, dirt, grass) should cost more, creating meaningful economic choices.

**Why this priority**: Core mechanic that affects every Hider's strategy. Inverting traditional rarity-based pricing creates unique risk-reward decisions - pay for safety or play risky for free.

**Independent Test**: Place 10 test blocks across a typical map. Have Seekers rate which blocks are hardest to spot. Price blocks inversely to their camouflage rating. Verify that 80%+ of players find the pricing fair.

**Acceptance Scenarios**:

1. **Given** a new player with 0 coins, **When** they open the disguise shop, **Then** they can always afford to disguise (free high-visibility options available)
2. **Given** a map with stone and grass terrain, **When** a Hider reviews disguise prices, **Then** stone/grass blocks cost more (100 coins) than melon/pumpkin (free)
3. **Given** a skilled Hider with map knowledge, **When** choosing a disguise location, **Then** they can use free conspicuous blocks in clever spots instead of paying for common blocks
4. **Given** a cautious Hider, **When** they have 100 coins, **Then** they can invest in high-camouflage blocks for safer hiding at the cost of other utility items

---

### User Story 4 - Expanded Item Variety for Role Identity (Priority: P2)

Each role (Hider/Seeker) should have diverse item options that support different playstyles and adapt to various game situations. Currently limited item selection may feel repetitive.

**Why this priority**: Enhances replayability and allows players to develop personal strategies. Different items enable different playstyles within the same role.

**Independent Test**: Add 2-3 new items per role and verify that experienced players use different item combinations across multiple games based on their preferred strategy.

**Acceptance Scenarios**:

1. **Given** a Hider who prefers aggressive movement, **When** they review shop options, **Then** they find items that support a mobile playstyle (speed, stamina, quick disguise changes)
2. **Given** a Hider who prefers stationary hiding, **When** they review shop options, **Then** they find items that support defensive play (better disguises, escape tools, decoys)
3. **Given** a Seeker who focuses on area control, **When** they review shop options, **Then** they find items for territorial coverage (area scans, trap placement, vision tools)
4. **Given** a Seeker who focuses on rapid pursuit, **When** they review shop options, **Then** they find items for chase efficiency (speed boosts, tracking tools, cooldown reduction)

---

### User Story 5 - Dynamic Economy Throughout Match (Priority: P2)

The shop should create interesting decisions at different game phases. Early-game purchases should differ from late-game purchases as the economy evolves.

**Why this priority**: Creates dynamic gameplay where strategy evolves. Prevents matches from feeling identical in pacing and decisions.

**Independent Test**: Track purchase timings across a match - verify that items are bought at different time points, not all at game start or all at same time.

**Acceptance Scenarios**:

1. **Given** game start (0-2 minutes), **When** players make purchases, **Then** they primarily buy low-cost utility items (60%+ of purchases under 300 coins)
2. **Given** mid-game (3-6 minutes), **When** Seekers have earned capture rewards, **Then** they invest in higher-cost tactical items (avg purchase price increases to 350+ coins)
3. **Given** late-game (7-10 minutes), **When** few Hiders remain, **Then** both sides make strategic all-in purchases with accumulated wealth
4. **Given** a player accumulates 1500+ coins mid-game, **When** they open shop, **Then** they see high-value "power items" that can swing the game but require significant investment

---

### User Story 6 - Purchase Limits and Strategic Scarcity (Priority: P3)

Some powerful items should have purchase limits or cooldowns to prevent spam and maintain balance. Players should feel their purchases matter individually.

**Why this priority**: Prevents degenerative strategies where one item dominates. Creates meaningful resource management decisions.

**Independent Test**: Implement purchase limits on 2-3 strong items and verify that players cannot buy unlimited copies, encouraging diverse purchases.

**Acceptance Scenarios**:

1. **Given** a Hider purchases the Escape Pass item, **When** they try to buy another, **Then** they see "Limit reached: 1 per game" message and cannot purchase
2. **Given** a Seeker uses Area Scan (30s cooldown), **When** they try to buy another immediately, **Then** they can purchase but using it shows "Cooldown: Xs remaining"
3. **Given** a player attempts to buy a third Speed Boost, **When** they already own 2, **Then** they see "Maximum 2 per player" restriction
4. **Given** global item limits (team-wide caps), **When** a teammate buys the last available Tracker Compass, **Then** other teammates see it marked as "Out of stock"

---

### Edge Cases

- What happens when a player doesn't have enough money for any remaining unbought items?
- How does the shop handle simultaneous purchases of limited-stock items by multiple players?
- What happens to unused items and their value when the game ends?
- How are item effects handled if a player leaves mid-game after purchasing?
- What happens when a Hider uses Escape Pass at the exact moment they're captured?
- How does the shop handle price changes or item additions mid-game (via config reload)?
- What happens if a player tries to use an item during the preparation phase?
- How are consumable vs permanent vs one-time-use items distinguished in inventory?

## Requirements *(mandatory)*

### Functional Requirements

**Item Catalog Management**

- **FR-001**: System MUST support distinct item catalogs for Hiders and Seekers with role-specific items
- **FR-002**: System MUST display items in shop GUI with name, description, price, duration/effect, and purchase limits
- **FR-003**: System MUST allow configuration of item properties (price, duration, cooldown, purchase limits) without code changes
- **FR-004**: System MUST support item categories (Offense, Defense, Utility, Movement) for organization

**Pricing and Economy**

- **FR-005**: System MUST validate sufficient player funds before allowing purchase
- **FR-006**: System MUST deduct item cost from player balance immediately upon successful purchase
- **FR-007**: System MUST support different pricing models (flat cost, scaling cost based on purchases, dynamic pricing based on game state)
- **FR-008**: System MUST display player's current balance in shop GUI

**Purchase Restrictions**

- **FR-009**: System MUST enforce per-player purchase limits for specified items (e.g., "1 Escape Pass per game")
- **FR-010**: System MUST enforce global/team purchase limits for specified items (e.g., "3 Tracker Compasses total per team")
- **FR-011**: System MUST enforce cooldowns between uses of the same item type
- **FR-012**: System MUST prevent purchases during game phases where shop is disabled (before game start, after game end)

**Item Activation and Effects**

- **FR-013**: System MUST support passive items (automatic effect on purchase, like Escape Pass)
- **FR-014**: System MUST support active items (player must activate via right-click or command)
- **FR-015**: System MUST support duration-based items that expire after specified time
- **FR-016**: System MUST support consumable items that disappear after one use
- **FR-017**: System MUST provide clear visual/audio feedback when items activate, when effects expire, and when cooldowns complete

**Hider-Specific Items**

- **FR-018**: Disguise block pricing MUST be based on camouflage effectiveness (high visibility = low/free cost, high camouflage = higher cost)
- **FR-019**: Escape Pass MUST trigger automatically when Hider takes capture damage and provide invulnerability window
- **FR-020**: System MUST support Hider mobility items (speed boosts, teleportation, stamina)
- **FR-021**: System MUST support Hider defensive items (better disguises, decoys, shields)

**Seeker-Specific Items**

- **FR-022**: Tracker Compass MUST point toward nearest Hider (disguised or visible) within detection range
- **FR-023**: Area Scan MUST reveal number of Hiders within specified radius and provide directional hints
- **FR-024**: Speed Boost MUST increase Seeker movement speed by configurable percentage for configurable duration
- **FR-025**: System MUST support Seeker detection items (reveal effects, tracking tools, vision enhancements)

**Inventory and State Management**

- **FR-026**: System MUST track purchased items per player for current game session
- **FR-027**: System MUST clear player inventories and item effects when game ends
- **FR-028**: System MUST persist which items have been purchased to enforce purchase limits
- **FR-029**: System MUST handle item effects when player disconnects (remove effects, no refunds)

**Balance and Tuning**

- **FR-030**: System MUST log purchase statistics (item ID, buyer role, purchase time, usage rate) for balance analysis
- **FR-031**: System MUST support A/B testing by enabling/disabling specific items via configuration
- **FR-032**: System MUST allow administrators to reload shop configuration without restarting server

### Key Entities *(include if feature involves data)*

- **ShopItem**: Represents a purchasable item with properties (id, name, description, price, role, category, purchaseLimit, cooldown, duration, effects)
- **ItemCategory**: Organizational grouping (OFFENSE, DEFENSE, UTILITY, MOVEMENT) for shop UI presentation
- **PlayerPurchaseRecord**: Tracks what items a player has bought in current game (playerId, itemId, purchaseTime, quantity, activated)
- **ItemEffect**: Active effect applied to player (itemId, playerId, effectType, startTime, duration, magnitude)
- **PurchaseLimit**: Restriction rules (itemId, limitType: PER_PLAYER/PER_TEAM/GLOBAL, maxQuantity, cooldownSeconds)
- **ShopConfiguration**: All shop settings loaded from config (items list, pricing rules, restrictions, enabled/disabled items)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: At least 70% of items have purchase rate between 10% and 60% per game (no dead items, no mandatory items)
- **SC-002**: Players make an average of 2.5+ shop purchases per game (up from baseline of 1-2)
- **SC-003**: No single item accounts for more than 40% of total spending in analyzed games
- **SC-004**: Players can understand item function from tooltip with 85%+ accuracy (tested via user comprehension surveys)
- **SC-005**: Shop UI loads and displays all items in under 500ms for 30 concurrent players
- **SC-006**: Purchase transactions complete in under 100ms (from click to confirmation)
- **SC-007**: Item purchase distribution shows at least 60% of available items being bought across 20-game sample
- **SC-008**: Early-game purchases (0-3min) differ from late-game purchases (7-10min) by at least 40% in item selection
- **SC-009**: Zero economy exploits or item duplication bugs detected in 100+ test games
- **SC-010**: 80% of players report shop items feel "balanced" or "mostly balanced" in post-game surveys

## Item Suggestions *(informational - for reference)*

**Note**: These are example items for context. Final item list and balance will be determined during planning phase.

### Revised Hider Items (Examples)

**Disguise Blocks (Pricing by Camouflage Effectiveness)**:
- **High Visibility (Free)**: Melon, Pumpkin, Sponge, Gold Block, Diamond Block, Glowstone, Sea Lantern
  - These blocks stand out and are easy to spot, so no cost
- **Medium Visibility (50 coins)**: Oak Log, Brick, Sandstone, Gravel, Glass
  - Somewhat noticeable but can work in certain areas
- **Low Visibility (100 coins)**: Stone, Cobblestone, Dirt, Grass Block, Andesite
  - Common environment blocks that blend in easily - most effective camouflage

**Other Hider Items**:
1. **Escape Pass** (600 coins, limit 1) - Auto-trigger invulnerability for 5 seconds when hit (price increased from 500)
2. **Shadow Sprint** (350 coins) - 20 seconds of increased movement speed, can be used while visible
3. **Decoy Block** (400 coins, limit 2) - Place a fake disguise block at another location to confuse Seekers
4. **Second Chance** (800 coins, limit 1) - If captured, respawn at random location with 10 seconds immunity (rare, expensive, game-changing)

### Revised Seeker Items (Examples)

1. **Tracker Compass** (250 coins) - Points to nearest Hider for 30 seconds (price increased from 200)
2. **Speed Boost** (300 coins) - 30 seconds movement speed increase (existing)
3. **Area Scan** (450 coins) - Reveals Hider count in 12-block radius with directional indicators (price decreased from 500, radius increased)
4. **Eagle Eye** (500 coins, 20s duration) - Highlights disguised blocks within 15 blocks with particle effects
5. **Tracker's Insight** (200 coins, consumable) - Shows time since each Hider last moved (encourages checking stationary disguises)
6. **Capture Net** (600 coins, limit 2) - Next capture within 10 seconds grants 2x reward coins

## Assumptions *(informational)*

1. Players earn coins through game participation, captures (Seekers), and survival time (Hiders)
2. Average starting balance is 100-200 coins at game start
3. Typical Seeker earns 200 coins per capture
4. Average game session involves 10-15 total players
5. Shop is accessible throughout entire game via command or hotkey
6. Item effects stack multiplicatively (2 speed boosts = 2x individual effect)
7. Players understand basic shop mechanics from tutorial or in-game hints
8. Server configuration allows custom item additions by administrators
9. Shop uses standard Minecraft inventory GUI for display
10. Economy system (Vault) is already integrated and functional
11. Disguise block camouflage effectiveness is relative to typical map environments (stone/grass-heavy terrain)
12. Map environments are varied enough that both high-visibility and low-visibility blocks have situational uses
13. Players can visually distinguish block types at normal viewing distances (5-15 blocks)
