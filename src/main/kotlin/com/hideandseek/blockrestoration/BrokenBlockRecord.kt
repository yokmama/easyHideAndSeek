package com.hideandseek.blockrestoration

import org.bukkit.Location
import org.bukkit.block.data.BlockData

/**
 * Record of a broken block that needs restoration
 *
 * @property location Block world location (cloned to avoid chunk references)
 * @property blockData Block material, orientation, properties
 * @property tileEntityData Tile entity data (chests, signs), null for simple blocks
 * @property breakTimestamp System.currentTimeMillis() when broken
 * @property restoreTimestamp breakTimestamp + delayMillis
 * @property gameId Game session ID for cleanup
 */
data class BrokenBlockRecord(
    val location: Location,
    val blockData: BlockData,
    val tileEntityData: TileEntitySnapshot?,
    val breakTimestamp: Long,
    val restoreTimestamp: Long,
    val gameId: String
) {
    /**
     * Check if this block is ready for restoration
     *
     * @param currentTime Current system time in milliseconds
     * @return true if currentTime >= restoreTimestamp
     */
    fun isReadyForRestoration(currentTime: Long): Boolean {
        return currentTime >= restoreTimestamp
    }

    /**
     * Get remaining time until restoration in milliseconds
     *
     * @param currentTime Current system time in milliseconds
     * @return Remaining milliseconds, or 0 if ready
     */
    fun getRemainingTime(currentTime: Long): Long {
        val remaining = restoreTimestamp - currentTime
        return if (remaining > 0) remaining else 0
    }
}
