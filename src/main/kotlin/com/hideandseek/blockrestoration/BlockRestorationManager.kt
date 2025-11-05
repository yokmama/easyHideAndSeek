package com.hideandseek.blockrestoration

import com.hideandseek.config.BlockRestorationConfig
import com.hideandseek.disguise.DisguiseManager
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.block.Container
import org.bukkit.block.Sign
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager for tracking and restoring broken blocks automatically
 *
 * This manager tracks blocks broken during gameplay and restores them after
 * a configured delay. It excludes disguise blocks and integrates with the
 * game lifecycle.
 */
class BlockRestorationManager(
    private val plugin: Plugin,
    private val config: BlockRestorationConfig,
    private val disguiseManager: DisguiseManager
) {
    private val trackedBlocks = ConcurrentHashMap<Location, BrokenBlockRecord>()
    private var restorationTask: BukkitTask? = null

    /**
     * Track a broken block for automatic restoration
     *
     * @param block The block that was broken
     * @param gameId The current game session ID
     * @return true if tracking started, false if block is disguise or already tracked
     * @throws IllegalArgumentException if gameId is empty
     */
    fun trackBrokenBlock(block: Block, gameId: String): Boolean {
        require(gameId.isNotEmpty()) { "gameId must not be empty" }

        // Check if this is a disguise block
        if (disguiseManager.getDisguisedPlayerAt(block.location) != null) {
            return false
        }

        val location = block.location.clone()
        val blockData = block.blockData.clone()
        val tileEntityData = captureTileEntity(block.state)

        val currentTime = System.currentTimeMillis()
        val restoreTime = currentTime + config.getRestorationDelayMs()

        val record = BrokenBlockRecord(
            location = location,
            blockData = blockData,
            tileEntityData = tileEntityData,
            breakTimestamp = currentTime,
            restoreTimestamp = restoreTime,
            gameId = gameId
        )

        // Use putIfAbsent for thread-safe insert-if-not-exists
        val existingRecord = trackedBlocks.putIfAbsent(location, record)
        return existingRecord == null
    }

    /**
     * Manually restore a specific block immediately (bypasses timer)
     *
     * @param location The location of the block to restore
     * @return true if block was restored, false if not tracked
     * @throws IllegalStateException if called from async thread
     */
    fun restoreBlockNow(location: Location): Boolean {
        check(plugin.server.isPrimaryThread) { "restoreBlockNow must be called from main thread" }

        val record = trackedBlocks.remove(location) ?: return false
        return performRestoration(record)
    }

    /**
     * Clear all tracked blocks for a specific game session
     *
     * @param gameId The game session ID
     * @return Number of blocks cleared
     */
    fun clearGameBlocks(gameId: String): Int {
        require(gameId.isNotEmpty()) { "gameId must not be empty" }

        var count = 0
        val iterator = trackedBlocks.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.gameId == gameId) {
                iterator.remove()
                count++
            }
        }
        return count
    }

    /**
     * Clear all tracked blocks (for plugin disable)
     *
     * @return Number of blocks cleared
     */
    fun clearAll(): Int {
        val count = trackedBlocks.size
        trackedBlocks.clear()
        return count
    }

    /**
     * Get the count of currently tracked blocks
     *
     * @return Number of blocks awaiting restoration
     */
    fun getTrackedBlockCount(): Int = trackedBlocks.size

    /**
     * Get all tracked blocks for a specific game
     *
     * @param gameId The game session ID
     * @return List of BrokenBlockRecord instances
     */
    fun getGameBlocks(gameId: String): List<BrokenBlockRecord> {
        require(gameId.isNotEmpty()) { "gameId must not be empty" }
        return trackedBlocks.values.filter { it.gameId == gameId }
    }

    /**
     * Check if a specific location is tracked for restoration
     *
     * @param location The location to check
     * @return true if location is tracked
     */
    fun isTracked(location: Location): Boolean = trackedBlocks.containsKey(location)

    /**
     * Start the restoration task (called in onEnable)
     *
     * @return BukkitTask instance (for cancellation)
     * @throws IllegalStateException if task already running
     */
    fun start(): BukkitTask {
        check(restorationTask == null) { "Restoration task is already running" }

        restorationTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            processRestorations()
        }, 1L, 1L) // Run every tick (50ms)

        return restorationTask!!
    }

    /**
     * Stop the restoration task (called in onDisable)
     */
    fun stop() {
        restorationTask?.cancel()
        restorationTask = null
    }

    /**
     * Process pending restorations (called by scheduled task)
     * Restores up to maxRestorationsPerTick blocks per tick
     */
    private fun processRestorations() {
        val currentTime = System.currentTimeMillis()
        var restoredCount = 0
        val maxPerTick = config.maxRestorationsPerTick

        val iterator = trackedBlocks.entries.iterator()
        while (iterator.hasNext() && restoredCount < maxPerTick) {
            val entry = iterator.next()
            val record = entry.value

            if (record.isReadyForRestoration(currentTime)) {
                // Check if chunk is loaded
                if (!record.location.isChunkLoaded) {
                    continue
                }

                // Check if disguise block appeared at this location
                if (disguiseManager.getDisguisedPlayerAt(record.location) != null) {
                    // Skip restoration if disguise is at this location
                    continue
                }

                // Perform restoration
                if (performRestoration(record)) {
                    iterator.remove()
                    restoredCount++

                    if (config.debugLogging) {
                        plugin.logger.info("Restored block at ${record.location} (game: ${record.gameId})")
                    }
                }
            }
        }

        if (config.logRestorationStats && restoredCount > 0) {
            plugin.logger.info("Restored $restoredCount blocks this tick (${trackedBlocks.size} remaining)")
        }
    }

    /**
     * Perform the actual block restoration
     *
     * @param record The block record to restore
     * @return true if restoration succeeded, false otherwise
     */
    private fun performRestoration(record: BrokenBlockRecord): Boolean {
        return try {
            val block = record.location.block

            // Restore block data
            block.setBlockData(record.blockData, false)

            // Restore tile entity data if present
            if (record.tileEntityData != null && config.enableTileEntityRestoration) {
                restoreTileEntity(block.state, record.tileEntityData)
            }

            true
        } catch (e: Exception) {
            plugin.logger.warning("Failed to restore block at ${record.location}: ${e.message}")
            false
        }
    }

    /**
     * Restore tile entity data to a block state
     *
     * @param blockState The block state to restore to
     * @param snapshot The tile entity snapshot to restore from
     */
    private fun restoreTileEntity(blockState: BlockState, snapshot: TileEntitySnapshot) {
        try {
            when (snapshot.tileEntityType) {
                TileEntityType.CONTAINER -> {
                    if (blockState is Container) {
                        @Suppress("UNCHECKED_CAST")
                        val contents = snapshot.data["inventory"] as? Array<ItemStack?>
                        if (contents != null) {
                            blockState.inventory.contents = contents
                            blockState.update(true, false)
                        }
                    }
                }
                TileEntityType.SIGN -> {
                    if (blockState is Sign) {
                        @Suppress("UNCHECKED_CAST")
                        val frontLines = snapshot.data["front_lines"] as? List<*>
                        @Suppress("UNCHECKED_CAST")
                        val backLines = snapshot.data["back_lines"] as? List<*>

                        // Note: Sign line restoration would require proper Component handling
                        // For now, we just update the state
                        blockState.update(true, false)
                    }
                }
                TileEntityType.NONE -> {
                    // No tile entity data to restore
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to restore tile entity at ${blockState.location}: ${e.message}")
        }
    }
}
