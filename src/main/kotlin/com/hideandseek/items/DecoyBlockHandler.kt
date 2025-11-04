package com.hideandseek.items

import com.hideandseek.game.Game
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.util.EulerAngle
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * T034: Handler for Decoy Block effect
 *
 * Places a fake disguise block at a distance to mislead Seekers
 * The decoy appears as a disguised block but has no player inside
 * Duration: Until destroyed or game ends (600 seconds default)
 */
class DecoyBlockHandler : ItemEffectHandler {

    companion object {
        // Track active decoys: decoyId -> ArmorStand
        private val activeDecoys = ConcurrentHashMap<UUID, ArmorStand>()

        /**
         * Clean up a specific decoy
         */
        fun removeDecoy(decoyId: UUID) {
            activeDecoys.remove(decoyId)?.let { armorStand ->
                armorStand.remove()
            }
        }

        /**
         * Clean up all decoys for a player
         */
        fun removePlayerDecoys(playerId: UUID) {
            activeDecoys.entries.removeIf { (decoyId, armorStand) ->
                val metadata = armorStand.getMetadata("ownerId")
                if (metadata.isNotEmpty() && metadata[0].asString() == playerId.toString()) {
                    armorStand.remove()
                    true
                } else {
                    false
                }
            }
        }

        /**
         * Clean up all decoys
         */
        fun removeAllDecoys() {
            activeDecoys.values.forEach { it.remove() }
            activeDecoys.clear()
        }
    }

    override fun canApply(player: Player, game: Game): Boolean {
        val playerData = game.players[player.uniqueId] ?: return false

        // Must be a Hider
        if (playerData.role.name != "HIDER") {
            return false
        }

        // Cannot use while captured
        if (playerData.isCaptured) {
            return false
        }

        // Check if player already has max decoys (limit: 2)
        val existingDecoys = activeDecoys.values.count { armorStand ->
            val metadata = armorStand.getMetadata("ownerId")
            metadata.isNotEmpty() && metadata[0].asString() == player.uniqueId.toString()
        }

        if (existingDecoys >= 2) {
            player.sendMessage("§cデコイの最大数に達しています（最大2個）")
            return false
        }

        return true
    }

    override fun apply(player: Player, game: Game, config: ItemConfig): Result<Unit> {
        return try {
            // Get target location (5 blocks in front of player)
            val direction = player.location.direction.normalize().multiply(5)
            val targetLocation = player.location.clone().add(direction)

            // Adjust to ground level
            targetLocation.y = targetLocation.world.getHighestBlockYAt(targetLocation.blockX, targetLocation.blockZ).toDouble()

            // Get decoy block type from metadata or use random common block
            val blockType = config.getMetadata<Material>("blockType")
                ?: listOf(Material.STONE, Material.DIRT, Material.COBBLESTONE).random()

            // Create armor stand as decoy
            val armorStand = targetLocation.world.spawn(targetLocation, ArmorStand::class.java) { stand ->
                stand.isVisible = false
                stand.isSmall = true
                stand.setGravity(false)
                stand.isMarker = true
                stand.isInvulnerable = true
                stand.setAI(false)
                stand.customName = "§7偽装ブロック"
                stand.isCustomNameVisible = false

                // Store owner ID
                stand.setMetadata("ownerId", org.bukkit.metadata.FixedMetadataValue(
                    player.server.pluginManager.getPlugin("EasyHideAndSeek")!!,
                    player.uniqueId.toString()
                ))
                stand.setMetadata("isDecoy", org.bukkit.metadata.FixedMetadataValue(
                    player.server.pluginManager.getPlugin("EasyHideAndSeek")!!,
                    true
                ))
            }

            // Place the actual block at the location
            targetLocation.block.type = blockType

            // Store decoy ID in metadata for cleanup
            val decoyId = UUID.randomUUID()
            activeDecoys[decoyId] = armorStand

            // Schedule removal after duration
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                player.server.pluginManager.getPlugin("EasyHideAndSeek")!!,
                Runnable {
                    removeDecoy(decoyId)
                    targetLocation.block.type = Material.AIR
                },
                (config.duration * 20).toLong()
            )

            // Visual feedback
            player.sendMessage("§a✦ デコイブロックを設置しました")
            player.sendMessage("§7場所: X:${targetLocation.blockX}, Y:${targetLocation.blockY}, Z:${targetLocation.blockZ}")
            player.playSound(player.location, org.bukkit.Sound.BLOCK_CHEST_OPEN, 1.0f, 0.8f)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun onExpire(player: Player, game: Game) {
        player.sendMessage("§7デコイブロックが消滅しました")
    }

    override fun getDisplayLore(config: ItemConfig): List<String> {
        return listOf(
            "§7偽の偽装ブロックを設置",
            "§7シーカーを欺くためのダミー",
            "",
            "§b効果: §f偽装ブロックを5ブロック先に設置",
            "§b持続時間: §f${config.duration / 60}分間",
            "§b制限: §f最大2個まで",
            "",
            "§7シーカーが攻撃しても捕獲されません"
        )
    }
}
