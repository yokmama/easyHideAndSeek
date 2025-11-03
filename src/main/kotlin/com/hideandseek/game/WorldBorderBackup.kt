package com.hideandseek.game

import org.bukkit.Location
import org.bukkit.World

data class WorldBorderBackup(
    val center: Location,
    val size: Double,
    val damageAmount: Double,
    val damageBuffer: Double,
    val warningDistance: Int,
    val warningTime: Int
) {
    fun restore(world: World) {
        val border = world.worldBorder
        border.center = center
        border.size = size
        border.damageAmount = damageAmount
        border.damageBuffer = damageBuffer
        border.warningDistance = warningDistance
        border.warningTime = warningTime
    }

    companion object {
        fun capture(world: World): WorldBorderBackup {
            val border = world.worldBorder
            return WorldBorderBackup(
                border.center,
                border.size,
                border.damageAmount,
                border.damageBuffer,
                border.warningDistance,
                border.warningTime
            )
        }

        fun defaultBackup(world: World): WorldBorderBackup {
            return WorldBorderBackup(
                world.spawnLocation,
                29999984.0,
                0.2,
                5.0,
                5,
                15
            )
        }
    }
}
