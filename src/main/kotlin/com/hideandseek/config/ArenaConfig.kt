package com.hideandseek.config

import com.hideandseek.arena.Arena
import com.hideandseek.arena.ArenaBoundaries
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration

/**
 * Handles arena serialization to/from arenas.yml
 */
class ArenaConfig(private val config: FileConfiguration) {

    /**
     * Load all arenas from configuration
     *
     * @return Map of arena name to Arena object
     */
    fun loadArenas(): Map<String, Arena> {
        val arenas = mutableMapOf<String, Arena>()
        val arenasSection = config.getConfigurationSection("arenas") ?: return arenas

        for (key in arenasSection.getKeys(false)) {
            val arenaSection = arenasSection.getConfigurationSection(key) ?: continue
            try {
                val arena = deserializeArena(key, arenaSection)
                arenas[key] = arena
            } catch (e: Exception) {
                println("Failed to load arena '$key': ${e.message}")
            }
        }

        return arenas
    }

    /**
     * Save an arena to configuration
     *
     * @param arena Arena to save
     */
    fun saveArena(arena: Arena) {
        val path = "arenas.${arena.name}"

        config.set("$path.display-name", arena.displayName)
        config.set("$path.world", arena.world.name)

        // Save boundaries
        serializeLocation("$path.boundaries.pos1", arena.boundaries.pos1)
        serializeLocation("$path.boundaries.pos2", arena.boundaries.pos2)

        // Auto-calculated values
        val center = arena.boundaries.center
        config.set("$path.boundaries.center.x", center.x)
        config.set("$path.boundaries.center.y", center.y)
        config.set("$path.boundaries.center.z", center.z)
        config.set("$path.boundaries.size", arena.boundaries.size)
    }

    /**
     * Delete an arena from configuration
     *
     * @param name Arena name
     */
    fun deleteArena(name: String) {
        config.set("arenas.$name", null)
    }

    /**
     * Deserialize arena from configuration section
     */
    private fun deserializeArena(name: String, section: ConfigurationSection): Arena {
        val displayName = section.getString("display-name") ?: name
        val worldName = section.getString("world")
            ?: throw IllegalArgumentException("Arena '$name' missing world")
        val world = Bukkit.getWorld(worldName)
            ?: throw IllegalArgumentException("World '$worldName' not found for arena '$name'")

        val boundariesSection = section.getConfigurationSection("boundaries")
            ?: throw IllegalArgumentException("Arena '$name' missing boundaries")

        val pos1 = deserializeLocation(boundariesSection, "pos1", world)
        val pos2 = deserializeLocation(boundariesSection, "pos2", world)
        val boundaries = ArenaBoundaries(pos1, pos2)

        return Arena(name, displayName, world, boundaries)
    }

    /**
     * Serialize location to config
     */
    private fun serializeLocation(path: String, loc: Location) {
        config.set("$path.x", loc.x)
        config.set("$path.y", loc.y)
        config.set("$path.z", loc.z)
    }

    /**
     * Deserialize location from config
     */
    private fun deserializeLocation(section: ConfigurationSection, key: String, world: org.bukkit.World): Location {
        val locSection = section.getConfigurationSection(key)
            ?: throw IllegalArgumentException("Missing location section: $key")

        return Location(
            world,
            locSection.getDouble("x"),
            locSection.getDouble("y"),
            locSection.getDouble("z")
        )
    }
}
