package com.hideandseek.listeners

import com.hideandseek.blockrestoration.BlockRestorationManager
import com.hideandseek.game.GameManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.plugin.Plugin

/**
 * Listener for block break events during gameplay
 *
 * Tracks broken blocks for automatic restoration when:
 * - Player is in an active game
 * - Block is not a disguise block
 * - Restoration is enabled in config
 */
class BlockBreakListener(
    private val gameManager: GameManager,
    private val blockRestorationManager: BlockRestorationManager,
    private val plugin: Plugin
) : Listener {

    /**
     * Handle block break events
     *
     * Priority HIGH: We need to cancel before other plugins process
     * We handle the block breaking ourselves to prevent item drops
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block

        plugin.logger.info("[BlockBreak] Player ${player.name} broke block at ${block.location}")

        // Check if player is in an active game
        val game = gameManager.activeGame
        if (game == null) {
            plugin.logger.info("[BlockBreak] No active game - allowing normal break")
            return
        }

        plugin.logger.info("[BlockBreak] Active game found: ${game.id}")

        // Check if this player is part of the game
        if (!game.players.containsKey(player.uniqueId)) {
            plugin.logger.info("[BlockBreak] Player ${player.name} not in game - allowing normal break")
            return
        }

        plugin.logger.info("[BlockBreak] Player ${player.name} is in game - processing restoration")

        // Cancel the event to prevent item drops
        event.isCancelled = true

        // Track the broken block for restoration BEFORE changing it
        // DisguiseManager check is handled internally in trackBrokenBlock()
        val tracked = blockRestorationManager.trackBrokenBlock(block, game.id)

        plugin.logger.info("[BlockBreak] Block tracked: $tracked")

        if (tracked) {
            // Manually break the block (set to air) without dropping items
            block.type = org.bukkit.Material.AIR

            // Optional: Play break sound and particles
            block.world.playEffect(block.location, org.bukkit.Effect.STEP_SOUND, block.type)

            plugin.logger.info("[BlockBreak] Block set to AIR, will restore in 5 seconds")
        } else {
            plugin.logger.warning("[BlockBreak] Block was NOT tracked (might be disguise block)")
        }
    }
}
