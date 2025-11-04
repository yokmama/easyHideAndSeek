# API Contract: Item Effects Specification

**Feature**: Seeker Shop System
**Component**: Individual Item Effect Implementations
**Created**: 2025-11-03
**Status**: Design Phase

## Overview

This document specifies the detailed implementation requirements for each of the four seeker shop items: Vision Enhancer, Glow Detector, Speed Boost, and Reach Extender. Each specification includes activation conditions, effect application logic, cleanup procedures, edge cases, and code examples.

---

## Table of Contents

1. [Vision Enhancer](#1-vision-enhancer)
2. [Glow Detector](#2-glow-detector)
3. [Speed Boost](#3-speed-boost)
4. [Reach Extender](#4-reach-extender)
5. [Common Patterns](#5-common-patterns)
6. [Testing Requirements](#6-testing-requirements)

---

## 1. Vision Enhancer

### Overview

**Item ID**: `vision-enhancer`
**Effect Type**: `EffectType.VISION`
**Duration**: 30 seconds (configurable)
**Intensity**: 1.0 (view distance multiplier)

**Purpose**: Enhances the seeker's visual range by applying Night Vision effect and temporarily increasing client view distance, making it easier to spot distant hiders and see in dark areas.

### Technical Approach

**Mechanism**:
1. Apply Night Vision potion effect (eliminates darkness/fog)
2. Increase client view distance to 32 chunks
3. Store original view distance for restoration
4. Schedule automatic restoration after duration

**Minecraft APIs Used**:
- `PotionEffect(PotionEffectType.NIGHT_VISION, duration, amplifier)`
- `Player.sendViewDistance(int)` (Paper API 1.21+)
- `Player.getClientViewDistance()` (for restoration)

### Activation Conditions

**Preconditions**:
- Player must be SEEKER role
- Game must be in SEEKING phase
- Player must be online
- Player must be in active game
- No validation errors (duration, intensity)

**Validation**:
```kotlin
override fun canActivate(player: Player, context: EffectContext): Result<Unit> {
    // Check role
    val gameData = context.gameManager.getPlayerGameData(player.uniqueId)
        ?: return Result.failure(IllegalStateException("Player not in game"))

    if (gameData.role != PlayerRole.SEEKER) {
        return Result.failure(IllegalStateException("Only seekers can use this item"))
    }

    // Check phase
    val game = context.gameManager.getActiveGame()
        ?: return Result.failure(IllegalStateException("No active game"))

    if (game.phase != GamePhase.SEEKING) {
        return Result.failure(IllegalStateException("Only usable during seek phase"))
    }

    // Check player online
    if (!player.isOnline) {
        return Result.failure(IllegalStateException("Player offline"))
    }

    return Result.success(Unit)
}
```

### Effect Application

**Implementation**:
```kotlin
package com.hideandseek.items

import com.hideandseek.effects.EffectType
import com.hideandseek.effects.ActiveEffect
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.time.Instant

class VisionEnhancerItem(
    private val plugin: Plugin,
    private val config: VisionConfig
) : SeekerEffectItem {

    // Storage for original view distances
    private val originalViewDistances = mutableMapOf<UUID, Int>()

    override fun applyEffect(
        player: Player,
        duration: Int,
        intensity: Double
    ): Result<ActiveEffect> {

        // 1. Store original view distance
        val originalDistance = player.clientViewDistance
        originalViewDistances[player.uniqueId] = originalDistance

        // 2. Apply Night Vision
        val nightVision = PotionEffect(
            PotionEffectType.NIGHT_VISION,
            duration * 20,  // Convert to ticks
            0,              // Level 1 (0-indexed)
            false,          // No ambient particles
            false,          // No visible particles
            true            // Show icon
        )
        player.addPotionEffect(nightVision)

        // 3. Increase view distance
        val newDistance = (config.viewDistance * intensity).toInt().coerceIn(2, 32)
        player.sendViewDistance(newDistance)

        // 4. Create effect metadata
        val effect = ActiveEffect(
            playerId = player.uniqueId,
            effectType = EffectType.VISION,
            startTime = Instant.now(),
            endTime = Instant.now().plusSeconds(duration.toLong()),
            intensity = intensity,
            metadata = mutableMapOf(
                "originalViewDistance" to originalDistance,
                "newViewDistance" to newDistance
            )
        )

        // 5. Schedule cleanup
        val taskId = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            removeEffect(player, effect)
        }, (duration * 20).toLong())

        effect.taskId = taskId.taskId

        // 6. Visual feedback
        playActivationFeedback(player)

        return Result.success(effect)
    }

    override fun removeEffect(player: Player, effect: ActiveEffect) {
        // 1. Remove Night Vision
        player.removePotionEffect(PotionEffectType.NIGHT_VISION)

        // 2. Restore original view distance
        val originalDistance = originalViewDistances.remove(player.uniqueId)
            ?: player.clientViewDistance
        player.sendViewDistance(originalDistance)

        // 3. Cancel scheduled task if still pending
        effect.taskId?.let { Bukkit.getScheduler().cancelTask(it) }

        // 4. Notify player
        player.sendActionBar("${ChatColor.YELLOW}Vision enhancement expired")
        player.playSound(player.location, Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 1.0f)
    }

    private fun playActivationFeedback(player: Player) {
        player.sendActionBar("${ChatColor.GREEN}${ChatColor.BOLD}Vision Enhanced!")
        player.playSound(player.location, Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.2f)
        player.spawnParticle(
            Particle.END_ROD,
            player.eyeLocation,
            20,
            0.3, 0.3, 0.3,
            0.1
        )
    }
}

data class VisionConfig(
    val viewDistance: Int = 32,        // Target view distance
    val amplifier: Int = 0,            // Night Vision amplifier
    val enableParticles: Boolean = true,
    val enableSound: Boolean = true
)
```

### Cleanup Procedures

**Automatic Cleanup** (after duration expires):
1. Remove Night Vision potion effect
2. Restore original client view distance
3. Cancel scheduled cleanup task
4. Remove from original view distance storage
5. Send expiration notification

**Manual Cleanup** (game end, player quit):
```kotlin
fun cleanupPlayer(playerId: UUID) {
    originalViewDistances.remove(playerId)
    Bukkit.getPlayer(playerId)?.let { player ->
        player.removePotionEffect(PotionEffectType.NIGHT_VISION)
        // Note: View distance resets automatically on disconnect
    }
}
```

### Edge Cases

| Case | Behavior | Handling |
|------|----------|----------|
| Server view distance < 32 | Limited by server setting | Warn in logs, use server max |
| Client FPS drop | Performance issue for player | Configurable, warn in item lore |
| Player disconnects during effect | View distance auto-restores | Clear storage entry |
| Effect replaced/extended | Re-store original distance | Don't overwrite first stored value |
| Player enters water | Night Vision works underwater | Feature, not bug |
| Server restart during effect | Effect lost | Acceptable, non-persistent |
| View distance already at 32 | No change visible | Still apply Night Vision |
| Client settings override | Client may render less | Server sends up to 32, client decides |

**Edge Case Implementation**:
```kotlin
override fun applyEffect(player: Player, duration: Int, intensity: Double): Result<ActiveEffect> {
    // Check server view distance
    val serverViewDistance = player.world.viewDistance
    if (serverViewDistance < config.viewDistance) {
        plugin.logger.warning(
            "Server view distance ($serverViewDistance) is less than configured " +
            "vision enhancement ($config.viewDistance). Enhancement may not be fully effective."
        )
    }

    // Only store original distance on first application
    if (!originalViewDistances.containsKey(player.uniqueId)) {
        originalViewDistances[player.uniqueId] = player.clientViewDistance
    }

    // ... rest of application logic
}
```

### Configuration

```yaml
seeker-items:
  vision-enhancer:
    material: ENDER_PEARL
    display-name: "&b&lVision Enhancer"
    price: 300
    effect-type: VISION
    effect-duration: 30
    effect-intensity: 1.0

    # Vision-specific settings
    settings:
      view-distance: 32           # Chunks (2-32)
      night-vision-amplifier: 0   # Potion level (0-indexed)
      warn-on-low-fps: true       # Warn if view distance may cause lag

    lore:
      - "&7Enhances your vision for &e30 seconds"
      - "&7See farther and through darkness"
      - "&cMay cause FPS drop on low-end PCs"
      - ""
      - "&e&lPrice: &f300 coins"
```

---

## 2. Glow Detector

### Overview

**Item ID**: `glow-detector`
**Effect Type**: `EffectType.GLOW`
**Duration**: 15 seconds (configurable)
**Intensity**: 1.0 (particle frequency multiplier)

**Purpose**: Makes disguised blocks visible by spawning particle effects around them. Particles are visible ONLY to the player who activated the item, providing a tactical advantage without revealing hider locations to other players.

### Technical Approach

**Mechanism**:
1. Query `DisguiseManager` for all active disguise locations
2. Start repeating task to spawn particles at those locations
3. Send particles only to the activating player
4. Update particle locations when disguises change
5. Stop task and clear particles after duration

**Minecraft APIs Used**:
- `Player.spawnParticle(Particle, Location, count, ...)` (player-specific)
- `BukkitScheduler.runTaskTimer()` (repeating task)
- `DisguiseManager.getActiveDisguises()` (custom API)

### Activation Conditions

**Preconditions**:
- Same as Vision Enhancer (SEEKER role, SEEKING phase, online, in-game)
- At least one disguised hider must exist (optional warning if none)

**Validation**:
```kotlin
override fun canActivate(player: Player, context: EffectContext): Result<Unit> {
    // Standard validation (role, phase, online)
    val baseValidation = super.canActivate(player, context)
    if (baseValidation.isFailure) return baseValidation

    // Optional: Warn if no disguised hiders
    val disguises = context.disguiseManager.getActiveDisguises()
    if (disguises.isEmpty()) {
        player.sendMessage("${ChatColor.YELLOW}Warning: No disguised hiders detected")
    }

    return Result.success(Unit)
}
```

### Effect Application

**Implementation**:
```kotlin
package com.hideandseek.items

import com.hideandseek.effects.EffectType
import com.hideandseek.effects.ActiveEffect
import com.hideandseek.disguise.DisguiseManager
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.time.Instant

class GlowDetectorItem(
    private val plugin: Plugin,
    private val disguiseManager: DisguiseManager,
    private val config: GlowConfig
) : SeekerEffectItem {

    // Storage for active particle tasks
    private val particleTasks = mutableMapOf<UUID, BukkitTask>()

    override fun applyEffect(
        player: Player,
        duration: Int,
        intensity: Double
    ): Result<ActiveEffect> {

        // 1. Create effect object
        val effect = ActiveEffect(
            playerId = player.uniqueId,
            effectType = EffectType.GLOW,
            startTime = Instant.now(),
            endTime = Instant.now().plusSeconds(duration.toLong()),
            intensity = intensity
        )

        // 2. Start particle task
        val particleTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            spawnGlowParticles(player, intensity)
        }, 0L, config.updateInterval.toLong())

        particleTasks[player.uniqueId] = particleTask
        effect.metadata["particleTaskId"] = particleTask.taskId

        // 3. Schedule cleanup
        val cleanupTask = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            removeEffect(player, effect)
        }, (duration * 20).toLong())

        effect.taskId = cleanupTask.taskId

        // 4. Visual feedback
        playActivationFeedback(player, duration)

        return Result.success(effect)
    }

    private fun spawnGlowParticles(player: Player, intensity: Double) {
        // Get all active disguises
        val disguises = disguiseManager.getActiveDisguises()

        // Apply distance filtering
        val playerLoc = player.location
        val maxDistance = config.maxDistance

        disguises.values.forEach { disguiseData ->
            val blockLoc = disguiseData.blockLocation

            // Distance check (performance optimization)
            if (blockLoc.world == playerLoc.world &&
                blockLoc.distance(playerLoc) <= maxDistance) {

                // Calculate particle count based on intensity
                val particleCount = (config.particleCount * intensity).toInt()

                // Spawn particles at center of block
                val centerLoc = blockLoc.clone().add(0.5, 0.5, 0.5)

                player.spawnParticle(
                    config.particleType,
                    centerLoc,
                    particleCount,
                    0.3, 0.3, 0.3,  // Spread (x, y, z)
                    0.01             // Speed/extra data
                )
            }
        }
    }

    override fun removeEffect(player: Player, effect: ActiveEffect) {
        // 1. Cancel particle task
        particleTasks.remove(player.uniqueId)?.cancel()

        // 2. Cancel cleanup task
        effect.taskId?.let { Bukkit.getScheduler().cancelTask(it) }

        // 3. Notify player
        player.sendActionBar("${ChatColor.YELLOW}Glow detector deactivated")
        player.playSound(player.location, Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 0.8f)
    }

    private fun playActivationFeedback(player: Player, duration: Int) {
        player.sendActionBar(
            "${ChatColor.GOLD}${ChatColor.BOLD}Glow Detector Active! " +
            "${ChatColor.YELLOW}(${duration}s)"
        )
        player.playSound(player.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.7f, 1.5f)
        player.spawnParticle(
            Particle.GLOW,
            player.eyeLocation,
            30,
            0.5, 0.5, 0.5,
            0.1
        )
    }

    // Listen for disguise changes to update particles
    @EventHandler
    fun onDisguiseChange(event: PlayerUnDisguiseEvent) {
        // Particles will automatically stop showing for removed disguises
        // on next update cycle (no action needed)
    }
}

data class GlowConfig(
    val particleType: Particle = Particle.END_ROD,
    val particleCount: Int = 5,
    val updateInterval: Int = 10,        // Ticks (10 = 0.5s)
    val maxDistance: Double = 50.0,      // Blocks
    val enableSound: Boolean = true
)
```

### Cleanup Procedures

**Automatic Cleanup**:
1. Cancel repeating particle task
2. Cancel scheduled cleanup task
3. Remove from particle task storage
4. Send expiration notification

**Manual Cleanup**:
```kotlin
fun cleanupPlayer(playerId: UUID) {
    particleTasks.remove(playerId)?.cancel()
    // Particles disappear automatically (client-side rendering)
}
```

### Edge Cases

| Case | Behavior | Handling |
|------|----------|----------|
| No disguised hiders | Particles not spawned | Warn player on activation |
| Hider undisguises during effect | Stop showing particles for that block | Listen to `PlayerUnDisguiseEvent` |
| Too many disguises (15+) | Particle spam | Limit to nearest N or within radius |
| Player moves far from disguise | Continue showing if within max distance | Distance filtering per update |
| Multiple seekers use glow detector | Each sees own particles | Player-specific spawning |
| Disguise block changes type | Show particles at new location | Update automatically on next cycle |
| Server lag (TPS drop) | Reduce particle frequency | Configurable update interval |
| Hider moves (undisguises/redisguises) | Particles update to new location | Automatic via `getActiveDisguises()` |

**Edge Case Implementation**:
```kotlin
private fun spawnGlowParticles(player: Player, intensity: Double) {
    val disguises = disguiseManager.getActiveDisguises()

    // Limit to prevent spam
    if (disguises.size > config.maxDisguisesToShow) {
        // Show only nearest disguises
        val nearest = disguises.values
            .sortedBy { it.blockLocation.distance(player.location) }
            .take(config.maxDisguisesToShow)

        nearest.forEach { disguiseData ->
            spawnParticleAt(player, disguiseData.blockLocation, intensity)
        }
    } else {
        disguises.values.forEach { disguiseData ->
            spawnParticleAt(player, disguiseData.blockLocation, intensity)
        }
    }
}
```

### Configuration

```yaml
seeker-items:
  glow-detector:
    material: GLOWSTONE_DUST
    display-name: "&e&lGlow Detector"
    price: 500
    effect-type: GLOW
    effect-duration: 15
    effect-intensity: 1.0

    # Glow-specific settings
    settings:
      particle-type: END_ROD          # Particle type
      particle-count: 5               # Particles per block
      update-interval: 10             # Ticks between updates
      max-distance: 50                # Max render distance
      max-disguises-shown: 20         # Limit to prevent spam

    lore:
      - "&7Makes disguised blocks &eglow &7for &e15 seconds"
      - "&7Only you can see the glow effect"
      - "&7Range: &e50 blocks"
      - ""
      - "&e&lPrice: &f500 coins"
```

---

## 3. Speed Boost

### Overview

**Item ID**: `speed-boost`
**Effect Type**: `EffectType.SPEED`
**Duration**: 30 seconds (configurable)
**Intensity**: 1.0 (amplifier level, 0-indexed)

**Purpose**: Increases the seeker's movement speed, allowing faster traversal of the map and better pursuit of visible hiders.

### Technical Approach

**Mechanism**:
1. Apply Speed potion effect at configured amplifier level
2. Schedule automatic removal after duration
3. Remove effect on cleanup

**Minecraft APIs Used**:
- `PotionEffect(PotionEffectType.SPEED, duration, amplifier)`
- `Player.addPotionEffect(PotionEffect)`
- `Player.removePotionEffect(PotionEffectType)`

### Activation Conditions

**Preconditions**:
- Same as Vision Enhancer (SEEKER role, SEEKING phase, online, in-game)

**Validation**:
```kotlin
override fun canActivate(player: Player, context: EffectContext): Result<Unit> {
    return super.canActivate(player, context)
}
```

### Effect Application

**Implementation**:
```kotlin
package com.hideandseek.items

import com.hideandseek.effects.EffectType
import com.hideandseek.effects.ActiveEffect
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.time.Instant

class SpeedBoostItem(
    private val plugin: Plugin,
    private val config: SpeedConfig
) : SeekerEffectItem {

    override fun applyEffect(
        player: Player,
        duration: Int,
        intensity: Double
    ): Result<ActiveEffect> {

        // 1. Calculate amplifier (intensity 1.0 = level 2 = amplifier 1)
        val amplifier = (intensity.toInt()).coerceIn(0, 2)

        // 2. Create and apply potion effect
        val speedEffect = PotionEffect(
            PotionEffectType.SPEED,
            duration * 20,  // Convert to ticks
            amplifier,      // 0 = Speed I, 1 = Speed II, 2 = Speed III
            false,          // No ambient particles
            false,          // No visible particles
            true            // Show icon
        )
        player.addPotionEffect(speedEffect)

        // 3. Create effect metadata
        val effect = ActiveEffect(
            playerId = player.uniqueId,
            effectType = EffectType.SPEED,
            startTime = Instant.now(),
            endTime = Instant.now().plusSeconds(duration.toLong()),
            intensity = intensity,
            metadata = mutableMapOf("amplifier" to amplifier)
        )

        // 4. Schedule cleanup
        val taskId = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            removeEffect(player, effect)
        }, (duration * 20).toLong())

        effect.taskId = taskId.taskId

        // 5. Visual feedback
        playActivationFeedback(player, amplifier)

        return Result.success(effect)
    }

    override fun removeEffect(player: Player, effect: ActiveEffect) {
        // 1. Remove potion effect
        player.removePotionEffect(PotionEffectType.SPEED)

        // 2. Cancel scheduled task
        effect.taskId?.let { Bukkit.getScheduler().cancelTask(it) }

        // 3. Notify player
        player.sendActionBar("${ChatColor.YELLOW}Speed boost expired")
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 0.8f)
    }

    private fun playActivationFeedback(player: Player, amplifier: Int) {
        val level = amplifier + 1
        player.sendActionBar(
            "${ChatColor.GREEN}${ChatColor.BOLD}Speed Boost! " +
            "${ChatColor.GRAY}(Level $level)"
        )
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f)
        player.spawnParticle(
            Particle.CRIT,
            player.location,
            30,
            0.5, 0.5, 0.5,
            0.5
        )
    }
}

data class SpeedConfig(
    val defaultAmplifier: Int = 1,     // Speed II (0-indexed)
    val enableParticles: Boolean = true,
    val enableSound: Boolean = true
)
```

### Cleanup Procedures

**Automatic Cleanup**:
1. Remove Speed potion effect
2. Cancel scheduled cleanup task
3. Send expiration notification

**Manual Cleanup**:
```kotlin
fun cleanupPlayer(playerId: UUID) {
    Bukkit.getPlayer(playerId)?.removePotionEffect(PotionEffectType.SPEED)
}
```

### Edge Cases

| Case | Behavior | Handling |
|------|----------|----------|
| Player already has Speed from other source | Effects stack (additive) | Document in lore, may be very fast |
| Player in water/cobweb | Speed reduced by environment | Normal Minecraft behavior |
| Effect replaced/extended | Remove old, apply new | Standard overlap policy |
| Player rides vehicle | Speed doesn't affect vehicles | Document in lore |
| Amplifier too high (>2) | Clamp to 2 (Speed III) | Prevent excessive speed |
| Negative amplifier | Clamp to 0 (Speed I) | Validation |

**Edge Case Implementation**:
```kotlin
override fun applyEffect(player: Player, duration: Int, intensity: Double): Result<ActiveEffect> {
    // Warn if player already has speed
    if (player.hasPotionEffect(PotionEffectType.SPEED)) {
        player.sendMessage(
            "${ChatColor.YELLOW}Note: You already have a speed effect. " +
            "Effects will stack!"
        )
    }

    // Clamp amplifier
    val amplifier = (intensity.toInt()).coerceIn(0, 2)

    // ... rest of application logic
}
```

### Configuration

```yaml
seeker-items:
  speed-boost:
    material: SUGAR
    display-name: "&a&lSpeed Boost"
    price: 200
    effect-type: SPEED
    effect-duration: 30
    effect-intensity: 1.0

    # Speed-specific settings
    settings:
      amplifier: 1              # 0=Speed I, 1=Speed II, 2=Speed III
      stack-warning: true       # Warn if player already has speed

    lore:
      - "&7Increases movement speed for &e30 seconds"
      - "&7Level: &eSpeed II"
      - "&cDoes not affect vehicles"
      - ""
      - "&e&lPrice: &f200 coins"
```

---

## 4. Reach Extender

### Overview

**Item ID**: `reach-extender`
**Effect Type**: `EffectType.REACH`
**Duration**: 30 seconds (configurable)
**Intensity**: 1.5 (reach multiplier, 1.0 = normal, 1.5 = 50% increase)

**Purpose**: Extends the seeker's attack range, allowing them to hit disguised blocks from farther away without needing to get close.

### Technical Approach

**Mechanism**:
1. Store reach multiplier in effect metadata
2. Extend `BlockDamageListener` to check for active reach extension
3. Perform raycast with extended distance when attacking
4. Trigger capture if raycast hits disguised block within extended range
5. Remove multiplier storage on cleanup

**Minecraft APIs Used**:
- `World.rayTraceBlocks(start, direction, distance, ...)` (extended raycasting)
- `Player.getEyeLocation()` (raycast start point)
- `Location.getDirection()` (raycast direction)

### Activation Conditions

**Preconditions**:
- Same as Vision Enhancer (SEEKER role, SEEKING phase, online, in-game)

**Validation**:
```kotlin
override fun canActivate(player: Player, context: EffectContext): Result<Unit> {
    return super.canActivate(player, context)
}
```

### Effect Application

**Implementation**:
```kotlin
package com.hideandseek.items

import com.hideandseek.effects.EffectType
import com.hideandseek.effects.ActiveEffect
import org.bukkit.entity.Player
import java.time.Instant

class ReachExtenderItem(
    private val plugin: Plugin,
    private val config: ReachConfig
) : SeekerEffectItem {

    // Storage for active reach multipliers
    private val activeMultipliers = mutableMapOf<UUID, Double>()

    override fun applyEffect(
        player: Player,
        duration: Int,
        intensity: Double
    ): Result<ActiveEffect> {

        // 1. Validate intensity (must be >= 1.0)
        val multiplier = intensity.coerceIn(1.0, 2.0)

        // 2. Store multiplier
        activeMultipliers[player.uniqueId] = multiplier

        // 3. Create effect metadata
        val effect = ActiveEffect(
            playerId = player.uniqueId,
            effectType = EffectType.REACH,
            startTime = Instant.now(),
            endTime = Instant.now().plusSeconds(duration.toLong()),
            intensity = multiplier,
            metadata = mutableMapOf("multiplier" to multiplier)
        )

        // 4. Schedule cleanup
        val taskId = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            removeEffect(player, effect)
        }, (duration * 20).toLong())

        effect.taskId = taskId.taskId

        // 5. Visual feedback
        playActivationFeedback(player, multiplier)

        return Result.success(effect)
    }

    override fun removeEffect(player: Player, effect: ActiveEffect) {
        // 1. Remove multiplier storage
        activeMultipliers.remove(player.uniqueId)

        // 2. Cancel scheduled task
        effect.taskId?.let { Bukkit.getScheduler().cancelTask(it) }

        // 3. Notify player
        player.sendActionBar("${ChatColor.YELLOW}Reach extension expired")
        player.playSound(player.location, Sound.BLOCK_ANVIL_LAND, 0.3f, 1.5f)
    }

    /**
     * Get active reach multiplier for a player.
     * Called by BlockDamageListener.
     */
    fun getReachMultiplier(playerId: UUID): Double {
        return activeMultipliers[playerId] ?: 1.0
    }

    private fun playActivationFeedback(player: Player, multiplier: Double) {
        val percentage = ((multiplier - 1.0) * 100).toInt()
        player.sendActionBar(
            "${ChatColor.GOLD}${ChatColor.BOLD}Reach Extended! " +
            "${ChatColor.YELLOW}(+$percentage%)"
        )
        player.playSound(player.location, Sound.BLOCK_ANVIL_USE, 0.7f, 1.0f)
        player.spawnParticle(
            Particle.CRIT,
            player.location.add(player.location.direction.multiply(2)),
            20,
            0.3, 0.3, 0.3,
            0.2
        )
    }
}

data class ReachConfig(
    val baseReach: Double = 4.5,        // Vanilla creative reach
    val maxMultiplier: Double = 2.0,    // Max 2x reach
    val showVisualBeam: Boolean = true, // Show particle beam on hit
    val enableSound: Boolean = true
)
```

### Integration with BlockDamageListener

**Extended Listener**:
```kotlin
package com.hideandseek.listeners

import com.hideandseek.items.ReachExtenderItem
import com.hideandseek.disguise.DisguiseManager
import com.hideandseek.game.GameManager
import org.bukkit.FluidCollisionMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDamageEvent

class BlockDamageListener(
    private val disguiseManager: DisguiseManager,
    private val gameManager: GameManager,
    private val reachExtender: ReachExtenderItem
) : Listener {

    @EventHandler
    fun onBlockDamage(event: BlockDamageEvent) {
        val attacker = event.player
        val game = gameManager.activeGame ?: return

        val attackerData = game.players[attacker.uniqueId] ?: return
        if (attackerData.role != PlayerRole.SEEKER) return

        // Check direct hit first (existing behavior)
        val directHit = event.block.location
        if (disguiseManager.getDisguisedPlayerAt(directHit) != null) {
            // Existing capture logic
            return
        }

        // Check for extended reach
        val multiplier = reachExtender.getReachMultiplier(attacker.uniqueId)
        if (multiplier > 1.0) {
            val extendedTarget = performExtendedRaycast(attacker, multiplier)
            if (extendedTarget != null) {
                val hiderId = disguiseManager.getDisguisedPlayerAt(extendedTarget)
                if (hiderId != null) {
                    event.isCancelled = true
                    performCapture(attacker, hiderId, game)
                    showReachHitFeedback(attacker, extendedTarget)
                }
            }
        }
    }

    private fun performExtendedRaycast(
        player: Player,
        multiplier: Double
    ): Location? {
        val baseReach = 4.5 // Vanilla creative reach
        val extendedReach = baseReach * multiplier

        val rayTrace = player.world.rayTraceBlocks(
            player.eyeLocation,
            player.eyeLocation.direction,
            extendedReach,
            FluidCollisionMode.NEVER,
            true // Ignore passable blocks
        )

        return rayTrace?.hitBlock?.location
    }

    private fun showReachHitFeedback(player: Player, target: Location) {
        // Draw particle line from player to target
        val start = player.eyeLocation
        val direction = target.toVector().subtract(start.toVector()).normalize()
        val distance = start.distance(target)

        for (i in 0 until (distance * 2).toInt()) {
            val point = start.clone().add(direction.clone().multiply(i * 0.5))
            player.spawnParticle(Particle.CRIT, point, 1, 0.0, 0.0, 0.0, 0.0)
        }

        player.playSound(player.location, Sound.ENTITY_ARROW_HIT, 1.0f, 1.2f)
    }
}
```

### Cleanup Procedures

**Automatic Cleanup**:
1. Remove multiplier from storage
2. Cancel scheduled cleanup task
3. Send expiration notification

**Manual Cleanup**:
```kotlin
fun cleanupPlayer(playerId: UUID) {
    activeMultipliers.remove(playerId)
}
```

### Edge Cases

| Case | Behavior | Handling |
|------|----------|----------|
| Hitting through walls | Should NOT work | Raycast respects block collision |
| Hitting non-disguise blocks | No effect | Only trigger on disguised blocks |
| Multiple seekers with reach extender | Independent multipliers | Per-player storage |
| Raycast hits wrong block | Check disguise at exact hit location | Use `hitBlock.location` |
| Player too far (beyond extended reach) | No hit registered | Normal behavior |
| Balance issues (too powerful) | Adjust price or multiplier | Configuration |
| Visual confusion | Show particle beam on hit | Optional visual feedback |
| Hitting while moving | Works normally | Raycast is instant |

**Edge Case Implementation**:
```kotlin
private fun performExtendedRaycast(player: Player, multiplier: Double): Location? {
    val baseReach = config.baseReach
    val extendedReach = baseReach * multiplier

    val rayTrace = player.world.rayTraceBlocks(
        player.eyeLocation,
        player.eyeLocation.direction,
        extendedReach,
        FluidCollisionMode.NEVER,
        true // Respect block collision
    )

    val hitBlock = rayTrace?.hitBlock ?: return null

    // Blacklist certain blocks (e.g., barriers, air)
    if (hitBlock.type.isAir || hitBlock.type == Material.BARRIER) {
        return null
    }

    return hitBlock.location
}
```

### Configuration

```yaml
seeker-items:
  reach-extender:
    material: STICK
    display-name: "&6&lReach Extender"
    price: 250
    effect-type: REACH
    effect-duration: 30
    effect-intensity: 1.5

    # Reach-specific settings
    settings:
      base-reach: 4.5           # Vanilla creative reach
      max-multiplier: 2.0       # Max 2x (9 blocks)
      show-hit-beam: true       # Particle line on hit
      respect-collision: true   # Can't hit through walls

    lore:
      - "&7Extends attack range by &e50% &7for &e30 seconds"
      - "&7Hit disguised blocks from farther away"
      - "&cCannot hit through walls"
      - ""
      - "&e&lPrice: &f250 coins"
```

---

## 5. Common Patterns

### Base Effect Item Interface

All item implementations follow this common interface:

```kotlin
package com.hideandseek.items

import com.hideandseek.effects.ActiveEffect
import com.hideandseek.effects.EffectType
import org.bukkit.entity.Player

interface SeekerEffectItem {

    /**
     * Get the effect type this item provides.
     */
    val effectType: EffectType

    /**
     * Check if this item can be activated for a player.
     */
    fun canActivate(player: Player, context: EffectContext): Result<Unit>

    /**
     * Apply the effect to a player.
     */
    fun applyEffect(
        player: Player,
        duration: Int,
        intensity: Double
    ): Result<ActiveEffect>

    /**
     * Remove the effect from a player.
     */
    fun removeEffect(player: Player, effect: ActiveEffect)

    /**
     * Clean up resources for a player (on disconnect/game end).
     */
    fun cleanupPlayer(playerId: UUID)
}

data class EffectContext(
    val gameManager: GameManager,
    val disguiseManager: DisguiseManager,
    val effectManager: EffectManager
)
```

### Standard Validation Pattern

```kotlin
abstract class BaseSeekerEffectItem : SeekerEffectItem {

    override fun canActivate(player: Player, context: EffectContext): Result<Unit> {
        // Check online
        if (!player.isOnline) {
            return Result.failure(IllegalStateException("Player offline"))
        }

        // Check in game
        val gameData = context.gameManager.getPlayerGameData(player.uniqueId)
            ?: return Result.failure(IllegalStateException("Player not in game"))

        // Check role
        if (gameData.role != PlayerRole.SEEKER) {
            return Result.failure(IllegalStateException("Only seekers can use this"))
        }

        // Check phase
        val game = context.gameManager.getActiveGame()
            ?: return Result.failure(IllegalStateException("No active game"))

        if (game.phase != GamePhase.SEEKING) {
            return Result.failure(IllegalStateException("Only usable during seek phase"))
        }

        return Result.success(Unit)
    }
}
```

### Feedback Pattern

All items should provide consistent feedback:

```kotlin
private fun playActivationFeedback(
    player: Player,
    effectType: EffectType,
    duration: Int
) {
    // Action bar message
    player.sendActionBar(
        "${ChatColor.GREEN}${ChatColor.BOLD}${effectType.displayName} Activated! " +
        "${ChatColor.YELLOW}(${duration}s)"
    )

    // Sound effect
    val sound = when (effectType) {
        EffectType.VISION -> Sound.BLOCK_BEACON_ACTIVATE
        EffectType.GLOW -> Sound.BLOCK_ENCHANTMENT_TABLE_USE
        EffectType.SPEED -> Sound.ENTITY_PLAYER_LEVELUP
        EffectType.REACH -> Sound.BLOCK_ANVIL_USE
    }
    player.playSound(player.location, sound, 0.7f, 1.2f)

    // Particles
    val particle = when (effectType) {
        EffectType.VISION -> Particle.END_ROD
        EffectType.GLOW -> Particle.GLOW
        EffectType.SPEED -> Particle.CRIT
        EffectType.REACH -> Particle.CRIT
    }
    player.spawnParticle(particle, player.eyeLocation, 20, 0.3, 0.3, 0.3, 0.1)
}

private fun playExpirationFeedback(player: Player, effectType: EffectType) {
    player.sendActionBar("${ChatColor.YELLOW}${effectType.displayName} expired")
    player.playSound(player.location, Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 1.0f)
}
```

---

## 6. Testing Requirements

### Unit Tests

Each item implementation must have comprehensive unit tests:

```kotlin
class VisionEnhancerItemTest {

    private lateinit var item: VisionEnhancerItem
    private lateinit var mockPlayer: Player
    private lateinit var mockPlugin: Plugin

    @BeforeEach
    fun setup() {
        mockPlugin = mockk()
        mockPlayer = mockk()
        item = VisionEnhancerItem(mockPlugin, VisionConfig())
    }

    @Test
    fun `applyEffect should apply Night Vision`() {
        every { mockPlayer.uniqueId } returns UUID.randomUUID()
        every { mockPlayer.addPotionEffect(any()) } returns true
        every { mockPlayer.clientViewDistance } returns 10
        every { mockPlayer.sendViewDistance(any()) } just Runs

        val result = item.applyEffect(mockPlayer, 30, 1.0)

        assertTrue(result.isSuccess)
        verify { mockPlayer.addPotionEffect(match { it.type == PotionEffectType.NIGHT_VISION }) }
        verify { mockPlayer.sendViewDistance(32) }
    }

    @Test
    fun `removeEffect should restore original view distance`() {
        val originalDistance = 12
        every { mockPlayer.uniqueId } returns UUID.randomUUID()
        every { mockPlayer.clientViewDistance } returns originalDistance
        every { mockPlayer.sendViewDistance(any()) } just Runs
        every { mockPlayer.removePotionEffect(any()) } just Runs

        // Apply then remove
        val effect = item.applyEffect(mockPlayer, 30, 1.0).getOrThrow()
        item.removeEffect(mockPlayer, effect)

        verify { mockPlayer.sendViewDistance(originalDistance) }
        verify { mockPlayer.removePotionEffect(PotionEffectType.NIGHT_VISION) }
    }
}
```

### Integration Tests

Test item integration with EffectManager:

```kotlin
@Test
fun `full effect lifecycle should work correctly`() {
    val effectManager = EffectManagerImpl(plugin)
    val visionItem = VisionEnhancerItem(plugin, VisionConfig())
    val player = createTestPlayer()

    // Apply effect via item
    val result = visionItem.applyEffect(player, 5, 1.0)
    assertTrue(result.isSuccess)

    // Verify effect is tracked
    assertTrue(effectManager.hasEffect(player, EffectType.VISION))
    assertEquals(5L, effectManager.getRemainingTime(player, EffectType.VISION))

    // Wait for expiration
    Thread.sleep(6000)

    // Verify auto-cleanup
    assertFalse(effectManager.hasEffect(player, EffectType.VISION))
}
```

### Edge Case Tests

Test all documented edge cases:

```kotlin
@Test
fun `glow detector should handle no disguised hiders`() {
    val glowItem = GlowDetectorItem(plugin, emptyDisguiseManager, GlowConfig())
    val player = createTestPlayer()

    // Should still activate but show warning
    val result = glowItem.applyEffect(player, 15, 1.0)
    assertTrue(result.isSuccess)
    verify { player.sendMessage(match { it.contains("No disguised hiders") }) }
}

@Test
fun `reach extender should not hit through walls`() {
    val reachItem = ReachExtenderItem(plugin, ReachConfig())
    val player = createTestPlayer()

    // Apply reach extension
    reachItem.applyEffect(player, 30, 1.5)

    // Raycast should return null when hitting wall
    val behindWall = createLocationBehindWall()
    val raycast = performRaycast(player, 6.75) // 1.5x reach

    assertNull(raycast) // Should not penetrate wall
}
```

---

## Summary

This specification provides complete implementation details for all four seeker shop items:

1. **Vision Enhancer**: Night Vision + View Distance (30s, 300 coins)
2. **Glow Detector**: Particle highlighting of disguises (15s, 500 coins)
3. **Speed Boost**: Movement speed increase (30s, 200 coins)
4. **Reach Extender**: Extended attack range (30s, 250 coins)

Each item includes:
- Full Kotlin implementation
- Activation conditions and validation
- Effect application and cleanup logic
- Edge case handling
- Configuration examples
- Testing requirements

**Implementation Status**: Ready for development
**Next Steps**: Implement each item class following these specifications
