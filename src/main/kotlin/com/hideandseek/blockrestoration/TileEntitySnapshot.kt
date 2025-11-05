package com.hideandseek.blockrestoration

import net.kyori.adventure.text.Component
import org.bukkit.block.BlockState
import org.bukkit.block.Container
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.bukkit.inventory.ItemStack

/**
 * Snapshot of tile entity data for blocks with NBT (chests, signs, furnaces)
 *
 * @property tileEntityType Type of tile entity
 * @property data Type-specific data map
 */
data class TileEntitySnapshot(
    val tileEntityType: TileEntityType,
    val data: Map<String, Any>
)

/**
 * Types of tile entities that can be captured and restored
 */
enum class TileEntityType {
    /** Chests, barrels, shulker boxes, etc. */
    CONTAINER,

    /** Signs (front + back text) */
    SIGN,

    /** Not a tile entity */
    NONE
}

/**
 * Capture tile entity data from a BlockState
 *
 * @param blockState The block state to capture from
 * @return TileEntitySnapshot if block has tile entity data, null otherwise
 */
fun captureTileEntity(blockState: BlockState): TileEntitySnapshot? {
    return when (blockState) {
        is Container -> {
            // Capture container inventory (chest, barrel, etc.)
            TileEntitySnapshot(
                tileEntityType = TileEntityType.CONTAINER,
                data = mapOf("inventory" to blockState.inventory.contents.clone())
            )
        }
        is Sign -> {
            // Capture sign text (front and back sides for Paper 1.21.5)
            val frontLines = blockState.getSide(Side.FRONT).lines().toList()
            val backLines = blockState.getSide(Side.BACK).lines().toList()

            TileEntitySnapshot(
                tileEntityType = TileEntityType.SIGN,
                data = mapOf(
                    "front_lines" to frontLines,
                    "back_lines" to backLines
                )
            )
        }
        else -> null  // Not a tile entity or not supported
    }
}
