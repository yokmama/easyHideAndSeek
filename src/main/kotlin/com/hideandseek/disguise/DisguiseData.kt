package com.hideandseek.disguise

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import java.util.UUID

data class DisguiseData(
    val playerId: UUID,
    val blockLocation: Location,
    val blockType: Material,
    val originalBlockData: BlockData,
    val disguiseTime: Long = System.currentTimeMillis()
)
