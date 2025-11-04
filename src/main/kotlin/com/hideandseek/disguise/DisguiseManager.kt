package com.hideandseek.disguise

import com.hideandseek.game.GameManager
import com.hideandseek.game.PlayerRole
import com.hideandseek.utils.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

class DisguiseManager(
    private val plugin: Plugin,
    private val gameManager: GameManager
) {
    private val disguises = mutableMapOf<UUID, DisguiseData>()
    private val blockLocations = mutableMapOf<Location, UUID>()

    fun disguise(player: Player, blockType: Material): Boolean {
        val game = gameManager.activeGame
        if (game == null) {
            MessageUtil.send(player, "&cNo active game")
            return false
        }

        val playerData = game.players[player.uniqueId]
        if (playerData == null) {
            MessageUtil.send(player, "&cYou are not in this game")
            return false
        }

        if (playerData.role != PlayerRole.HIDER) {
            MessageUtil.send(player, "&cOnly hiders can disguise")
            return false
        }

        if (playerData.isCaptured) {
            MessageUtil.send(player, "&cYou have been captured")
            return false
        }

        if (isDisguised(player.uniqueId)) {
            MessageUtil.send(player, "&cYou are already disguised")
            return false
        }

        val footLocation = player.location.block.location

        if (blockLocations.containsKey(footLocation)) {
            MessageUtil.send(player, "&cSomeone is already disguised here")
            return false
        }

        val block = footLocation.block
        val originalBlockData = block.blockData.clone()

        block.type = blockType

        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.INVISIBILITY,
                Int.MAX_VALUE,
                1,
                false,
                false
            )
        )

        val disguiseData = DisguiseData(
            playerId = player.uniqueId,
            blockLocation = footLocation,
            blockType = blockType,
            originalBlockData = originalBlockData
        )

        disguises[player.uniqueId] = disguiseData
        blockLocations[footLocation] = player.uniqueId

        MessageUtil.send(player, "&aDisguised as ${blockType.name}")
        return true
    }

    fun undisguise(player: Player, reason: String = "manual"): Boolean {
        val disguiseData = disguises[player.uniqueId] ?: return false

        val block = disguiseData.blockLocation.block
        block.blockData = disguiseData.originalBlockData

        player.removePotionEffect(PotionEffectType.INVISIBILITY)

        disguises.remove(player.uniqueId)
        blockLocations.remove(disguiseData.blockLocation)

        when (reason) {
            "movement" -> MessageUtil.send(player, "&cDisguise removed - you moved!")
            "capture" -> MessageUtil.send(player, "&cDisguise removed - you were captured!")
            "manual" -> MessageUtil.send(player, "&aDisguise removed")
            "game_end" -> {} // Silent
        }

        return true
    }

    fun undisguiseByLocation(location: Location): Player? {
        val playerId = blockLocations[location] ?: return null
        val player = Bukkit.getPlayer(playerId) ?: return null
        
        undisguise(player, "capture")
        return player
    }

    fun isDisguised(playerId: UUID): Boolean {
        return disguises.containsKey(playerId)
    }

    fun getDisguiseLocation(playerId: UUID): Location? {
        return disguises[playerId]?.blockLocation
    }

    fun getDisguisedPlayerAt(location: Location): UUID? {
        return blockLocations[location]
    }

    fun clearAllDisguises() {
        disguises.keys.toList().forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let { player ->
                undisguise(player, "game_end")
            }
        }
        disguises.clear()
        blockLocations.clear()
    }

    fun getActiveDisguises(): Map<UUID, DisguiseData> {
        return disguises.toMap()
    }
}
