package com.hideandseek.respawn

import com.hideandseek.arena.ArenaBoundaries
import com.hideandseek.config.BlockRestorationConfig
import com.hideandseek.game.GameManager
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import kotlin.random.Random

/**
 * Manager for handling player death and respawn logic
 *
 * Finds random safe ground locations within game boundaries and teleports
 * dead players while preserving their role and game state.
 */
class RespawnManager(
    private val plugin: Plugin,
    private val config: BlockRestorationConfig,
    private val gameManager: GameManager
) {

    /**
     * Handle player death and respawn in game
     *
     * @param player The player who died
     * @return RespawnResult indicating success/failure
     */
    fun respawnPlayer(player: Player): RespawnResult {
        // 1. Validate player is in active game
        val game = gameManager.activeGame ?: return RespawnResult.Failure(RespawnFailureReason.PLAYER_NOT_IN_GAME)

        val playerData = game.players[player.uniqueId]
            ?: return RespawnResult.Failure(RespawnFailureReason.PLAYER_NOT_IN_GAME)

        // 2. Get game boundaries
        val boundaries = game.arena.boundaries

        // 3. Find safe location within arena boundaries
        val centerLocation = Location(
            game.arena.world,
            boundaries.center.x,
            64.0,
            boundaries.center.z
        )

        val location = findSafeRandomLocation(
            world = game.arena.world,
            center = centerLocation,
            radius = if (config.useGameBoundaries) {
                // Use arena radius (calculate from boundaries)
                val radiusX = (boundaries.width / 2).toInt()
                val radiusZ = (boundaries.depth / 2).toInt()
                minOf(radiusX, radiusZ)
            } else {
                config.respawnSearchRadius
            },
            boundaries = if (config.useGameBoundaries) boundaries else null
        ) ?: return RespawnResult.Failure(RespawnFailureReason.NO_SAFE_LOCATION_FOUND)

        // 4. Teleport player
        val success = player.teleport(location)
        if (!success) {
            return RespawnResult.Failure(RespawnFailureReason.TELEPORT_FAILED)
        }

        if (config.debugLogging) {
            plugin.logger.info("Respawned ${player.name} at ${location.blockX}, ${location.blockY}, ${location.blockZ}")
        }

        // 5. Return success
        return RespawnResult.Success(location)
    }

    /**
     * Find a random safe spawn location within game boundaries
     *
     * Uses spiral search pattern with random offset from center
     *
     * @param world The world to search in
     * @param center The center point of search area
     * @param radius The search radius in blocks
     * @param excludeLocations Locations to avoid (for non-overlapping spawns)
     * @return Safe location or null if none found after max attempts
     */
    fun findSafeRandomLocation(
        world: World,
        center: Location,
        radius: Int = 50,
        excludeLocations: Set<Location> = emptySet(),
        boundaries: ArenaBoundaries? = null
    ): Location? {
        val maxAttempts = config.maxRespawnAttempts
        var attempts = 0

        while (attempts < maxAttempts) {
            attempts++

            // Generate random offset within radius
            val offsetX = Random.nextInt(-radius, radius + 1)
            val offsetZ = Random.nextInt(-radius, radius + 1)

            val x = center.blockX + offsetX
            val z = center.blockZ + offsetZ

            // Get highest block Y at this XZ position
            val y = world.getHighestBlockYAt(x, z)

            val location = Location(world, x.toDouble() + 0.5, y.toDouble() + 1.0, z.toDouble() + 0.5)

            // Check if within arena boundaries
            if (boundaries != null && !boundaries.contains(location)) {
                if (config.debugLogging) {
                    plugin.logger.fine("Skipping location ($x, $z) - outside arena boundaries")
                }
                continue
            }

            // Check if location is excluded
            if (excludeLocations.any { it.distanceSquared(location) < 4.0 }) {
                continue
            }

            // Validate location safety
            if (isSafeLocation(location)) {
                if (config.debugLogging) {
                    plugin.logger.info("[Respawn] Found safe location at ($x, $y, $z) within boundaries")
                }
                return location
            }
        }

        // Failed to find safe location after max attempts
        plugin.logger.warning("[Respawn] Failed to find safe location after $maxAttempts attempts")
        return null
    }

    /**
     * Validate if a location is safe for player spawn
     *
     * Checks:
     * - Y > world min height (not in void)
     * - Ground block is solid, not dangerous, and not unstable
     * - 2 blocks above are air/passable
     *
     * @param location The location to validate
     * @return true if safe, false otherwise
     */
    fun isSafeLocation(location: Location): Boolean {
        val world = location.world ?: return false

        // Check Y level (not in void)
        if (location.blockY < world.minHeight + 1) {
            return false
        }

        // Get blocks: ground (below player), feet, head
        val groundBlock = world.getBlockAt(location.blockX, location.blockY - 1, location.blockZ)
        val feetBlock = world.getBlockAt(location.blockX, location.blockY, location.blockZ)
        val headBlock = world.getBlockAt(location.blockX, location.blockY + 1, location.blockZ)

        // Ground must be solid
        if (!groundBlock.type.isSolid) {
            return false
        }

        // Ground must not be dangerous
        if (isDangerousBlock(groundBlock.type)) {
            return false
        }

        // Ground must not be unstable (leaves, glass panes, etc.)
        if (isUnstableBlock(groundBlock.type)) {
            return false
        }

        // Feet and head must be air or passable
        if (!isPassableBlock(feetBlock.type) || !isPassableBlock(headBlock.type)) {
            return false
        }

        return true
    }

    /**
     * Check if a block is unstable for spawning (leaves, glass panes, etc.)
     */
    private fun isUnstableBlock(material: Material): Boolean {
        return when {
            material.name.contains("LEAVES") -> true
            material.name.contains("GLASS_PANE") -> true
            material.name.contains("IRON_BARS") -> true
            material == Material.SCAFFOLDING -> true
            material == Material.SNOW -> true
            material == Material.POWDER_SNOW -> true
            else -> false
        }
    }

    /**
     * Check if a block type is dangerous to spawn on
     */
    private fun isDangerousBlock(material: Material): Boolean {
        return when (material) {
            Material.LAVA,
            Material.MAGMA_BLOCK,
            Material.FIRE,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.CACTUS,
            Material.SWEET_BERRY_BUSH -> true
            else -> false
        }
    }

    /**
     * Check if a block type is passable and safe for player spawn
     * Only accepts actual air blocks, not water or other fluids
     */
    private fun isPassableBlock(material: Material): Boolean {
        return when (material) {
            Material.AIR,
            Material.CAVE_AIR,
            Material.VOID_AIR -> true
            else -> false
        }
    }
}
