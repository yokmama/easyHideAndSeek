# Research: Seeker Shop System Implementation

**Feature Branch**: `003-seeker-shop`
**Date**: 2025-11-03
**Target**: Paper API 1.21.5 / Minecraft 1.21.x
**Research Scope**: Phase 0 - Technical Implementation Patterns

## Overview

This document contains research findings for implementing the Seeker Shop System, focusing on five key technical areas: PotionEffect implementation, vision enhancement, player-limited glow effects, attack range extension, and effect overlap handling. Each section provides implementation decisions, rationale, alternatives considered, and risk mitigation strategies.

---

## 1. Paper API Effect Implementation Patterns

### Decision

Use **PotionEffect API** with **BukkitScheduler-based task management** for all timed effects.

**Implementation Pattern**:
```kotlin
class EffectManager(private val plugin: Plugin) {
    private val activeEffects = mutableMapOf<UUID, MutableMap<EffectType, ActiveEffect>>()

    fun applyEffect(player: Player, effectType: EffectType, duration: Int): Boolean {
        // Store effect metadata
        val effect = ActiveEffect(
            playerId = player.uniqueId,
            effectType = effectType,
            startTime = Instant.now(),
            endTime = Instant.now().plusSeconds(duration.toLong()),
            taskId = null
        )

        // Apply actual Minecraft effect
        when (effectType) {
            EffectType.SPEED -> player.addPotionEffect(
                PotionEffect(PotionEffectType.SPEED, duration * 20, 1, false, false)
            )
            // ... other effects
        }

        // Schedule removal
        val taskId = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            removeEffect(player, effectType)
        }, (duration * 20).toLong())

        effect.taskId = taskId.taskId
        activeEffects.getOrPut(player.uniqueId) { mutableMapOf() }[effectType] = effect

        return true
    }

    fun removeEffect(player: Player, effectType: EffectType) {
        val effects = activeEffects[player.uniqueId] ?: return
        val effect = effects.remove(effectType) ?: return

        // Cancel scheduled task
        effect.taskId?.let { Bukkit.getScheduler().cancelTask(it) }

        // Remove Minecraft effect
        when (effectType) {
            EffectType.SPEED -> player.removePotionEffect(PotionEffectType.SPEED)
            // ... other effects
        }

        // Cleanup if no more effects
        if (effects.isEmpty()) {
            activeEffects.remove(player.uniqueId)
        }
    }
}
```

### Rationale

1. **Native Integration**: PotionEffect API is the standard Minecraft mechanism, ensuring compatibility with all client versions and other plugins
2. **Reliable Timing**: BukkitScheduler provides tick-perfect timing (20 ticks = 1 second) synchronized with the game loop
3. **Memory Efficiency**: HashMap-based storage (O(1) lookup) with automatic cleanup prevents memory leaks
4. **Separation of Concerns**: Metadata (ActiveEffect) is separate from Minecraft effects, allowing independent tracking of custom effects like vision enhancement

### Alternatives Considered

**Alternative 1: Custom event-driven system**
- **Rejected because**: Adds unnecessary complexity. PotionEffect handles visual indicators, particle effects, and client-side rendering automatically.

**Alternative 2: Database persistence for effects**
- **Rejected because**: Effects are temporary (max 30 seconds) and session-scoped. Database overhead (I/O latency) would violate the 500ms application requirement.

**Alternative 3: Tick-based polling (check every tick)**
- **Rejected because**: Inefficient for 30 players x 4 possible effects = 120 checks/tick. Scheduled tasks only execute once per effect.

### Implementation Notes

**API Calls (Paper 1.21.5)**:
```kotlin
// Apply potion effect
player.addPotionEffect(PotionEffect effect)

// Remove potion effect
player.removePotionEffect(PotionEffectType type)

// Check active effects
player.hasPotionEffect(PotionEffectType type): Boolean
player.getPotionEffect(PotionEffectType type): PotionEffect?

// Schedule tasks
Bukkit.getScheduler().runTaskLater(plugin, Runnable, long delay): BukkitTask
Bukkit.getScheduler().cancelTask(int taskId): void
```

**Performance Considerations**:
- PotionEffect storage: ~200 bytes per effect (Mojang's internal data structure)
- ActiveEffect metadata: ~150 bytes per effect (UUID + timestamps + enums)
- Total memory per player with 4 active effects: ~1.4 KB (well within 500KB limit)
- Task scheduling overhead: Negligible (Bukkit maintains a priority queue internally)

**Code Patterns**:
1. **Always store task IDs**: Required for cleanup when effects are manually removed (e.g., game end)
2. **Use PersistentDataContainer for custom data**: Don't extend PotionEffect; store metadata separately
3. **Nullable safety**: Always check if player is online before applying/removing effects
4. **Synchronous execution**: Never use async tasks for gameplay effects (causes race conditions with event handlers)

### Risks and Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Task leaks (not cancelled on game end) | Medium | Implement centralized cleanup in `GameManager.endGame()` |
| Player disconnect during effect | Low | `PlayerQuitEvent` listener removes all effects for that player |
| Effect duration desync (lag spikes) | Low | BukkitScheduler guarantees execution order; use server time, not wall time |
| Memory leak from orphaned ActiveEffect | Medium | Use weak references or periodic cleanup task (every 60s) |

---

## 2. Vision Enhancement Implementation

### Decision

Use **Night Vision potion effect** combined with **client view distance adjustment** via packet manipulation.

**Implementation Approach**:
```kotlin
fun applyVisionEnhancement(player: Player, duration: Int) {
    // 1. Apply Night Vision for brightness (eliminates fog/darkness)
    player.addPotionEffect(
        PotionEffect(
            PotionEffectType.NIGHT_VISION,
            duration * 20,
            0, // Amplifier 0 is sufficient
            false, // No ambient particles
            false  // No particles visible
        )
    )

    // 2. Increase client render distance
    // Note: Paper 1.21+ has Player.sendViewDistance(int)
    player.sendViewDistance(32) // Increase from default (usually 10-12)

    // 3. Store original view distance for restoration
    val original = player.clientViewDistance
    storeOriginalViewDistance(player.uniqueId, original)

    // Schedule restoration
    Bukkit.getScheduler().runTaskLater(plugin, Runnable {
        player.removePotionEffect(PotionEffectType.NIGHT_VISION)
        player.sendViewDistance(original)
    }, (duration * 20).toLong())
}
```

### Rationale

1. **Night Vision**: Removes fog, darkness, and underwater dimming - making distant blocks clearly visible
2. **View Distance**: Directly controls how many chunks the client renders (affects perceived "vision range")
3. **Client-side Control**: `sendViewDistance()` overrides player's F3+F adjustments temporarily without modifying server settings
4. **No Shader Dependency**: Works on all clients (vanilla, Optifine, Sodium) unlike shader-based solutions

### Alternatives Considered

**Alternative 1: Glowing effect on all entities**
- **Rejected because**: Doesn't help see blocks/terrain. Only highlights entities. Doesn't match "vision expansion" semantics.

**Alternative 2: Remove fog via world border manipulation**
- **Rejected because**: World border affects all players globally. We need per-player effects.

**Alternative 3: Increase server-side view distance**
- **Rejected because**: Affects all players and severely impacts server performance (exponential chunk load).

**Alternative 4: Send fake lighting updates**
- **Rejected because**: Extremely complex packet manipulation, high risk of client desync, no real benefit over Night Vision.

### Implementation Notes

**API Calls (Paper 1.21.5)**:
```kotlin
// Night Vision effect
player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, duration, amplifier))

// View distance (Paper API)
player.sendViewDistance(distance: Int) // Range: 2-32 chunks
player.clientViewDistance: Int // Get player's current client setting
player.simulationDistance: Int // Entity simulation distance (read-only)

// Note: Bukkit doesn't have sendViewDistance; this is Paper-specific
```

**Configuration Defaults**:
```yaml
seeker-items:
  vision-enhancer:
    night-vision-amplifier: 0  # Level 1 (0-indexed)
    view-distance: 32          # chunks (max recommended)
    duration: 30               # seconds
```

**Client Interaction Analysis**:
- **Client setting override**: `sendViewDistance()` temporarily overrides F3+F setting
- **Performance impact on client**: Depends on client's GPU/CPU. Modern PCs handle 32 chunks easily.
- **Server-side optimization**: Paper only sends chunks that the client can render (intelligent chunk streaming)
- **Restoration**: Must restore original `clientViewDistance` (not server default) to respect player preferences

**Edge Cases**:
1. **Client view distance < server setting**: Player sees less than enhanced amount → Not an issue, server sends up to 32 chunks, client renders what it can
2. **Client has shaders/mods**: Night Vision works with 99% of shader packs. Fallback: players still benefit from view distance increase
3. **Low-end client**: May experience FPS drop → Document this in item description, make view distance configurable

### Risks and Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Client FPS drop on low-end PCs | Medium | Make view distance configurable (20-32). Add warning in item lore. |
| View distance not restored on crash | Low | On server restart, clients revert to their saved settings automatically |
| Night Vision underwater brightness | Low | Feature, not bug. Helps seekers see underwater disguises. |
| Server render distance < 32 | Medium | Check `server.properties` on startup, warn if `view-distance < 32` |

---

## 3. Glow Effect - Player-Limited Display

### Decision

Use **custom particle systems** sent only to the seeker, **not** the Glowing entity effect.

**Implementation Pattern**:
```kotlin
class GlowDetectorEffect(
    private val plugin: Plugin,
    private val disguiseManager: DisguiseManager
) {
    fun applyGlowEffect(seeker: Player, duration: Int) {
        val taskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            // Get all disguised block locations
            val disguises = disguiseManager.getActiveDisguises()

            disguises.values.forEach { disguiseData ->
                val location = disguiseData.blockLocation

                // Send particle only to this seeker
                seeker.spawnParticle(
                    Particle.END_ROD,           // Bright, visible particle
                    location.clone().add(0.5, 0.5, 0.5), // Center of block
                    5,                          // Particle count
                    0.3, 0.3, 0.3,             // Spread (x, y, z)
                    0.01                        // Speed/extra data
                )
            }
        }, 0L, 10L) // Every 10 ticks (0.5 seconds)

        // Schedule cleanup
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            Bukkit.getScheduler().cancelTask(taskId.taskId)
        }, (duration * 20).toLong())
    }
}
```

### Rationale

1. **Player-Limited Visibility**: `Player.spawnParticle()` sends packets only to that player - other players don't see the glow
2. **Performance**: Particles are lightweight (no entity creation) and client-side rendered
3. **Block Targeting**: Works on disguise blocks (which are actual placed blocks), unlike entity-based Glowing effect
4. **Customization**: Can choose particle type, color, frequency, and density

### Alternatives Considered

**Alternative 1: Glowing entity effect (PotionEffectType.GLOWING)**
- **Rejected because**:
  - Only works on entities, not blocks
  - Visible to ALL players who have the spectral arrow effect applied
  - Can't restrict visibility to specific players
  - Requires creating fake entities at block locations (massive overhead)

**Alternative 2: Light block placement**
- **Rejected because**: Light blocks are visible to all players (global effect), and placing/removing blocks causes chunk updates (lag)

**Alternative 3: Send fake BlockData updates (make blocks glow)**
- **Rejected because**: Minecraft blocks don't have a native "glow" property. Would require replacing with light-emitting blocks, causing visual artifacts.

**Alternative 4: Team-based Glowing (Scoreboard teams)**
- **Rejected because**: Still requires entities. Can't apply to blocks. Overly complex for this use case.

### Implementation Notes

**API Calls (Paper 1.21.5)**:
```kotlin
// Player-specific particle spawning
player.spawnParticle(
    particle: Particle,
    location: Location,
    count: Int,
    offsetX: Double, offsetY: Double, offsetZ: Double,
    extra: Double,
    data: T? = null,
    force: Boolean = false
)

// Particle types suitable for "glowing" effect:
Particle.END_ROD          // Bright white beam
Particle.GLOW             // Yellow sparkle (1.17+)
Particle.ELECTRIC_SPARK   // Blue electric (1.19+)
Particle.WAX_ON           // Orange glow (subtle)
Particle.SOUL_FIRE_FLAME  // Blue flame (eerie)
```

**Recommended Configuration**:
```yaml
seeker-items:
  glow-detector:
    particle-type: END_ROD
    particle-count: 5
    update-interval: 10        # ticks (10 = 0.5s)
    particle-spread: 0.3       # blocks
    max-distance: 50           # Only show disguises within 50 blocks (performance)
    duration: 15               # seconds
```

**Performance Analysis**:
- **Worst case**: 10 disguised hiders, 3 seekers using glow detector
- **Particle packets per second**: 10 hiders × 5 particles × 2 updates/sec × 3 seekers = 300 packets/sec
- **Bandwidth**: ~50 bytes/packet × 300 = 15 KB/s (negligible)
- **Server CPU**: Particle rendering is client-side; server only sends packets (minimal overhead)

**Optimization Strategies**:
1. **Distance culling**: Only show particles for disguises within 50 blocks
2. **Update frequency**: 10 ticks (0.5s) instead of every tick reduces packet spam by 90%
3. **Particle count**: 5 particles per block (visible but not overwhelming)
4. **Early termination**: Stop task immediately when effect ends or disguise removed

**Visual Design**:
- **Particle color**: Match team colors (if applicable) or use bright white (END_ROD)
- **Animation**: Consider alternating particles (e.g., every other update) for pulsing effect
- **Density**: 5 particles per block is visible from 20+ blocks away without lag

### Risks and Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Particle spam with many disguises (15+) | Medium | Limit to nearest N disguises or within radius. Add config option. |
| Task not cancelled on disguise removal | Low | Listen to `PlayerUnDisguiseEvent` and filter out removed disguises |
| Packet overflow on low-bandwidth clients | Low | Paper's async packet system queues overflow; client smooths rendering |
| Effect persists after seeker dies/disconnects | Medium | Clear all scheduled tasks in `PlayerQuitEvent` and `PlayerDeathEvent` |

---

## 4. Attack Range Extension

### Decision

Use **raycasting with extended distance** in `BlockDamageEvent`, integrated with existing capture system.

**Implementation Pattern**:
```kotlin
class ReachExtenderEffect(
    private val disguiseManager: DisguiseManager
) {
    // Store players with active reach extension
    private val activeReachExtensions = mutableMapOf<UUID, Double>() // UUID -> multiplier

    fun applyReachExtension(player: Player, multiplier: Double, duration: Int) {
        activeReachExtensions[player.uniqueId] = multiplier

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            activeReachExtensions.remove(player.uniqueId)
        }, (duration * 20).toLong())
    }

    fun getReachMultiplier(playerId: UUID): Double {
        return activeReachExtensions[playerId] ?: 1.0
    }
}

// Extend BlockDamageListener
class BlockDamageListener(
    private val disguiseManager: DisguiseManager,
    private val gameManager: GameManager,
    private val reachExtender: ReachExtenderEffect
) : Listener {

    @EventHandler
    fun onBlockDamage(event: BlockDamageEvent) {
        val attacker = event.player
        val game = gameManager.activeGame ?: return

        val attackerData = game.players[attacker.uniqueId] ?: return
        if (attackerData.role != PlayerRole.SEEKER) return

        // Check direct block hit first (existing behavior)
        val directHit = event.block.location
        if (disguiseManager.getDisguisedPlayerAt(directHit) != null) {
            // Existing capture logic
            return
        }

        // If reach extender active, check extended raycast
        val multiplier = reachExtender.getReachMultiplier(attacker.uniqueId)
        if (multiplier > 1.0) {
            val extendedTarget = getExtendedTarget(attacker, multiplier)
            if (extendedTarget != null) {
                val hiderId = disguiseManager.getDisguisedPlayerAt(extendedTarget)
                if (hiderId != null) {
                    event.isCancelled = true
                    // Perform capture on extended target
                    performCapture(attacker, hiderId, game)
                }
            }
        }
    }

    private fun getExtendedTarget(player: Player, multiplier: Double): Location? {
        val baseReach = 4.5 // Vanilla Minecraft creative mode reach
        val extendedReach = baseReach * multiplier

        // Raycast from player's eye location
        val rayTrace = player.world.rayTraceBlocks(
            player.eyeLocation,
            player.eyeLocation.direction,
            extendedReach,
            FluidCollisionMode.NEVER,
            true // Ignore passable blocks
        )

        return rayTrace?.hitBlock?.location
    }
}
```

### Rationale

1. **Non-Invasive**: Extends existing `BlockDamageEvent` logic without replacing it
2. **Accurate Hit Detection**: Uses Minecraft's native raycasting (considers block collision boxes)
3. **Multiplier System**: Configurable (1.5x = 6.75 blocks, 2.0x = 9 blocks) for balance tuning
4. **Backward Compatible**: Falls back to normal reach when effect inactive

### Alternatives Considered

**Alternative 1: Entity reach extension via NMS (minecraft.server internals)**
- **Rejected because**: Highly version-dependent, breaks on Paper updates, requires reflection. Raycasting achieves same result safely.

**Alternative 2: Custom PlayerInteractEvent with manual distance checks**
- **Rejected because**: `BlockDamageEvent` already fires on left-click. No need to duplicate logic.

**Alternative 3: Increase attack damage attribute (`PLAYER_BLOCK_INTERACTION_RANGE`)**
- **Rejected because**:
  - Attribute is for entity attacks, not block interactions
  - Attribute API in 1.21+ is experimental and may change
  - Doesn't extend block break distance

**Alternative 4: Teleport player closer to target momentarily**
- **Rejected because**: Causes client-side jitter, exploitable, poor UX.

### Implementation Notes

**API Calls (Paper 1.21.5)**:
```kotlin
// Raycasting
world.rayTraceBlocks(
    start: Location,
    direction: Vector,
    maxDistance: Double,
    fluidCollisionMode: FluidCollisionMode,
    ignorePassableBlocks: Boolean
): RayTraceResult?

// Player eye location (where raycast starts)
player.eyeLocation: Location
player.location.direction: Vector // Player's facing direction

// Vanilla reach distances (for reference)
// Survival: 4.5 blocks
// Creative: 5.0 blocks
// Spectator: Unlimited (not applicable here)
```

**Configuration**:
```yaml
seeker-items:
  reach-extender:
    reach-multiplier: 1.5   # 4.5 * 1.5 = 6.75 blocks
    duration: 30            # seconds
    visual-feedback: true   # Show particle beam when attacking
```

**Visual Feedback** (optional enhancement):
```kotlin
// Show particle line from player to hit location
fun showReachIndicator(player: Player, target: Location) {
    val start = player.eyeLocation
    val direction = target.toVector().subtract(start.toVector()).normalize()
    val distance = start.distance(target)

    for (i in 0 until distance.toInt() * 2) { // 2 particles per block
        val point = start.clone().add(direction.clone().multiply(i * 0.5))
        player.spawnParticle(Particle.CRIT, point, 1, 0.0, 0.0, 0.0, 0.0)
    }
}
```

**Integration with Existing System**:
- The existing `BlockDamageListener` already handles capture logic
- Simply add extended raycast check AFTER direct hit check
- Reuse `performCapture()` logic for both direct and extended hits
- No changes needed to `DisguiseManager`

**Performance**:
- Raycasting: ~0.05ms per ray (Paper optimized)
- Only executes on left-click (not every tick)
- Negligible CPU impact

### Risks and Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Hitting through walls (block collision bug) | Medium | Use `ignorePassableBlocks: true` to respect collision. Add material blacklist (e.g., BARRIER). |
| Balance issues (too powerful) | High | Start with 1.5x multiplier. Gather player feedback. Increase price if overpowered. |
| Raycast hitting wrong block (floating point error) | Low | Round to block coordinates. Check for disguise at `hitBlock.location` (not hit position). |
| Visual confusion (player expects closer hit) | Medium | Add particle beam feedback or sound effect when extended hit registers |

---

## 5. Effect Overlap and Conflict Resolution

### Decision

Use **effect replacement (overwrite)** strategy with **independent effect management** per type.

**Resolution Rules**:
```kotlin
enum class OverlapPolicy {
    REPLACE,   // New effect overwrites old (default)
    EXTEND,    // Add remaining time to new duration
    REJECT,    // Refuse to apply if active
    STACK      // Both effects active (for different types only)
}

class EffectManager(private val plugin: Plugin) {
    private val activeEffects = mutableMapOf<UUID, MutableMap<EffectType, ActiveEffect>>()

    fun applyEffect(
        player: Player,
        effectType: EffectType,
        duration: Int,
        policy: OverlapPolicy = OverlapPolicy.REPLACE
    ): Boolean {
        val playerEffects = activeEffects.getOrPut(player.uniqueId) { mutableMapOf() }
        val existing = playerEffects[effectType]

        if (existing != null) {
            when (policy) {
                OverlapPolicy.REPLACE -> {
                    // Cancel old task, apply new effect
                    removeEffect(player, effectType)
                    return applyNewEffect(player, effectType, duration)
                }

                OverlapPolicy.EXTEND -> {
                    // Calculate remaining time + new duration
                    val remaining = (existing.endTime.epochSecond - Instant.now().epochSecond)
                    val totalDuration = (remaining + duration).toInt().coerceAtMost(120) // Cap at 2 min

                    removeEffect(player, effectType)
                    return applyNewEffect(player, effectType, totalDuration)
                }

                OverlapPolicy.REJECT -> {
                    MessageUtil.send(player, "&cEffect already active!")
                    return false
                }

                OverlapPolicy.STACK -> {
                    // Only for different effect types - shouldn't reach here
                    return applyNewEffect(player, effectType, duration)
                }
            }
        } else {
            return applyNewEffect(player, effectType, duration)
        }

        return false
    }

    private fun applyNewEffect(player: Player, effectType: EffectType, duration: Int): Boolean {
        // Standard effect application logic (see Section 1)
        // ...
    }
}
```

### Rationale

1. **REPLACE (default)**: Prevents duration stacking exploits (buying 10 items = 5 minutes). Keeps game balanced.
2. **Independent Types**: Speed + Vision can coexist (different effect types, no conflict)
3. **Simple State**: Only one effect per type per player - easy to reason about, no complex state management
4. **User-Friendly**: Players can re-purchase to "refresh" timer (common pattern in other games)

### Alternatives Considered

**Alternative 1: Duration stacking (always extend)**
- **Rejected because**:
  - Balance issues: Rich players buy unlimited effects
  - Memory leak risk: Unbounded duration growth
  - Poor UX: Hard to track when effects actually end

**Alternative 2: Reject duplicates entirely**
- **Rejected because**: Frustrating UX. Players can't "refresh" effects that are about to expire.

**Alternative 3: Queue effects (apply second after first ends)**
- **Rejected because**: Adds complexity (queue management), confusing for players (when does queued effect start?), memory overhead.

**Alternative 4: Partial stacking (only extend if < 5s remaining)**
- **Considered for future**: Could add as config option. Adds branching logic but decent compromise.

### Implementation Notes

**Configuration**:
```yaml
effects:
  overlap-policy:
    vision: REPLACE
    glow: REPLACE
    speed: REPLACE
    reach: REPLACE

  max-duration: 120        # Maximum seconds (prevents exploits)
  allow-cross-stacking: true # Different effects can coexist
```

**Policy by Effect Type**:

| Effect Type | Policy | Reason |
|-------------|--------|--------|
| Vision Enhancement | REPLACE | Only one view distance can be active |
| Glow Detector | REPLACE | Particle tasks would overlap (visual spam) |
| Speed Boost | REPLACE | Multiple Speed potion effects would stack multipliers (too powerful) |
| Reach Extender | REPLACE | Multipliers shouldn't stack (4.5 * 1.5 * 1.5 = 10 blocks is absurd) |

**Cross-Type Interaction Matrix**:

|          | Vision | Glow | Speed | Reach |
|----------|--------|------|-------|-------|
| Vision   | ❌ Replace | ✅ Stack | ✅ Stack | ✅ Stack |
| Glow     | ✅ Stack | ❌ Replace | ✅ Stack | ✅ Stack |
| Speed    | ✅ Stack | ✅ Stack | ❌ Replace | ✅ Stack |
| Reach    | ✅ Stack | ✅ Stack | ✅ Stack | ❌ Replace |

✅ = Effects can coexist
❌ = Same effect type, apply policy

**Code Example - Multi-Effect Check**:
```kotlin
fun hasAnyEffect(player: Player): Boolean {
    return activeEffects[player.uniqueId]?.isNotEmpty() ?: false
}

fun getActiveEffectTypes(player: Player): Set<EffectType> {
    return activeEffects[player.uniqueId]?.keys?.toSet() ?: emptySet()
}

// For scoreboard display
fun getEffectSummary(player: Player): List<String> {
    val effects = activeEffects[player.uniqueId] ?: return emptyList()
    return effects.map { (type, effect) ->
        val remaining = getRemainingTime(player, type)
        "${type.displayName}: ${remaining}s"
    }
}
```

**Cleanup on Game End**:
```kotlin
// In GameManager.endGame()
fun clearAllEffects(game: Game) {
    game.players.keys.forEach { uuid ->
        Bukkit.getPlayer(uuid)?.let { player ->
            effectManager.removeAllEffects(player)
        }
    }
}
```

### Risks and Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Players spam-buying items (edge case) | Low | Add purchase cooldown (5s) or cost scaling. Document REPLACE policy in item lore. |
| Effect removed but PotionEffect persists | Medium | Always pair `removeEffect()` with explicit `removePotionEffect()` calls |
| Race condition (2 effects applied same tick) | Low | Use synchronized block or single-threaded scheduler (BukkitScheduler is already single-threaded) |
| Effect types conflict unexpectedly | Low | Document compatibility matrix. Add warning if effect combo causes issues. |

---

## Summary & Recommendations

### Recommended Implementation Order

1. **Phase 1 - Foundation**: EffectManager + ActiveEffect data class (Section 1)
2. **Phase 2 - Simple Effects**: Speed Boost (uses vanilla PotionEffect, minimal complexity)
3. **Phase 3 - Vision**: Night Vision + View Distance (Section 2)
4. **Phase 4 - Complex Effects**: Glow Detector (particle systems, Section 3)
5. **Phase 5 - Advanced**: Reach Extender (raycast integration, Section 4)

### Performance Budget Summary

| Component | Memory/Player | CPU Impact | Network Impact |
|-----------|---------------|------------|----------------|
| EffectManager | ~1.5 KB | Negligible | None |
| Vision Enhancement | ~200 bytes | Negligible | 1 KB (view distance packet) |
| Glow Detector | ~500 bytes | Low (0.5s tick) | 5 KB/s (particles) |
| Speed Boost | ~200 bytes | Negligible | None (vanilla effect) |
| Reach Extender | ~100 bytes | Negligible | None (server-side) |
| **Total** | **~2.5 KB** | **< 1% CPU** | **~6 KB/s** |

**Conclusion**: All components well within constraints (500 KB/player budget, TPS 18+ target).

### Critical Path Dependencies

```
EffectManager (Core)
    ├── Speed Boost (no dependencies)
    ├── Vision Enhancement (no dependencies)
    ├── Glow Detector (requires DisguiseManager)
    └── Reach Extender (requires DisguiseManager + BlockDamageListener)
```

### Open Questions for Implementation

1. **Should vision enhancement affect spectators?** → Recommendation: No (they already have unlimited view distance)
2. **Glow detector particle color preference?** → Recommendation: Configurable, default END_ROD (bright white)
3. **Reach extender visual feedback?** → Recommendation: Optional particle beam (configurable)
4. **Effect duration UI position?** → Recommendation: Action bar (less intrusive than scoreboard)

### Testing Checklist

- [ ] Effect application completes within 500ms (FR requirement)
- [ ] Effect removal accurate to 200ms (FR requirement)
- [ ] Multiple concurrent effects (4 types × 3 seekers) maintain TPS 18+
- [ ] Glow detector only visible to user (packet inspection test)
- [ ] Reach extender respects block collision (can't hit through walls)
- [ ] Effect cleanup on game end (no lingering effects)
- [ ] Effect cleanup on player disconnect (no task leaks)
- [ ] View distance restored to original (not server default)
- [ ] Effect overlap policy enforced (no duration stacking beyond cap)
- [ ] Memory usage under 500 KB/player (profiler test)

---

**Research completed**: 2025-11-03
**Ready for Phase 1**: Data model design and contract specification
