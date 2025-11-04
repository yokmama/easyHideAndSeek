package com.hideandseek.items

import com.hideandseek.disguise.DisguiseManager
import com.hideandseek.effects.ActiveEffect
import com.hideandseek.effects.EffectHandler
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.UUID

/**
 * Effect handler for GLOW effect
 * Shows particles around disguised blocks, visible only to the seeker
 */
class GlowDetectorHandler(
    private val plugin: Plugin,
    private val disguiseManager: DisguiseManager
) : EffectHandler {

    // Store glow task IDs separately since we can't modify ActiveEffect after creation
    private val glowTasks = mutableMapOf<UUID, Int>()

    override fun apply(player: Player, effect: ActiveEffect) {
        // Get configuration values
        val glowUpdateInterval = plugin.config.getInt("effects.performance.glow-update-interval", 10)
        val glowMaxDistance = plugin.config.getInt("effects.performance.glow-max-distance", 50)

        // Start repeating task to show glow particles
        val glowTaskId = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            spawnGlowParticles(player, glowMaxDistance)
        }, 0L, glowUpdateInterval.toLong()).taskId

        // Store glow task ID in a separate map (since we can't modify effect after creation)
        glowTasks[player.uniqueId] = glowTaskId

        // Visual and audio feedback
        player.playSound(player.location, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f)
        player.spawnParticle(
            Particle.GLOW,
            player.location.add(0.0, 1.0, 0.0),
            10,
            0.5, 0.5, 0.5,
            0.05
        )

        player.sendMessage("§e✨ 偽装ブロック検出が有効になりました（${effect.getTotalDuration()}秒間）")
    }

    override fun remove(player: Player, effect: ActiveEffect) {
        // Cancel the glow particle task
        val glowTaskId = glowTasks.remove(player.uniqueId)
        if (glowTaskId != null) {
            plugin.server.scheduler.cancelTask(glowTaskId)
        }

        player.sendMessage("§7偽装ブロック検出の効果が切れました")
    }

    /**
     * Spawn glow particles around all disguised blocks within range
     */
    private fun spawnGlowParticles(player: Player, maxDistance: Int) {
        if (!player.isOnline) return

        val playerLoc = player.location
        val disguises = disguiseManager.getActiveDisguises()

        disguises.values.forEach { disguiseData ->
            val location = disguiseData.blockLocation

            // Distance culling - only show particles for nearby disguises
            if (location.world == playerLoc.world && location.distance(playerLoc) <= maxDistance) {
                // Spawn particles at the center of the block
                val centerLoc = location.clone().add(0.5, 0.5, 0.5)

                // Use END_ROD particles for a glowing effect
                // Only this player can see them (player-specific particles)
                player.spawnParticle(
                    Particle.END_ROD,
                    centerLoc,
                    5,              // 5 particles
                    0.3, 0.3, 0.3,  // Spread
                    0.02            // Speed
                )
            }
        }
    }
}
