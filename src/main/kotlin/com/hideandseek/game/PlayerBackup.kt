package com.hideandseek.game

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scoreboard.Scoreboard

data class PlayerBackup(
    val inventory: Array<ItemStack?>,
    val armor: Array<ItemStack?>,
    val location: Location,
    val gameMode: GameMode,
    val scoreboard: Scoreboard?
) {
    fun restore(player: Player) {
        player.inventory.contents = inventory.clone()
        player.inventory.armorContents = armor.clone()
        player.gameMode = gameMode

        // Safe teleport
        if (location.world != null && location.isChunkLoaded) {
            player.teleport(location)
        } else {
            val fallback = location.world?.spawnLocation ?: player.world.spawnLocation
            player.teleport(fallback)
        }

        if (scoreboard != null) {
            player.scoreboard = scoreboard
        }
    }

    companion object {
        fun create(player: Player): PlayerBackup {
            return PlayerBackup(
                inventory = player.inventory.contents.clone(),
                armor = player.inventory.armorContents.clone(),
                location = player.location.clone(),
                gameMode = player.gameMode,
                scoreboard = player.scoreboard
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerBackup

        if (!inventory.contentEquals(other.inventory)) return false
        if (!armor.contentEquals(other.armor)) return false
        if (location != other.location) return false
        if (gameMode != other.gameMode) return false
        if (scoreboard != other.scoreboard) return false

        return true
    }

    override fun hashCode(): Int {
        var result = inventory.contentHashCode()
        result = 31 * result + armor.contentHashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + gameMode.hashCode()
        result = 31 * result + (scoreboard?.hashCode() ?: 0)
        return result
    }
}
