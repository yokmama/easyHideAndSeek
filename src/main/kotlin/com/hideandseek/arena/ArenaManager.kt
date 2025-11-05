package com.hideandseek.arena

import com.hideandseek.config.ArenaConfig
import com.hideandseek.config.ConfigManager
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.UUID

/**
 * Manages arenas and arena setup sessions
 *
 * Handles CRUD operations for arenas and position selection workflow
 */
class ArenaManager(
    private val plugin: Plugin,
    private val configManager: ConfigManager
) {
    private val setupSessions = mutableMapOf<UUID, ArenaSetupSession>()
    private val arenas = mutableMapOf<String, Arena>()
    private lateinit var arenaConfig: ArenaConfig

    /**
     * Load arenas from configuration
     */
    fun loadArenas() {
        arenaConfig = ArenaConfig(configManager.arenas)
        arenas.clear()
        arenas.putAll(arenaConfig.loadArenas())
        plugin.logger.info("Loaded ${arenas.size} arena(s)")
    }

    /**
     * Get arena by name
     *
     * @param name Arena name
     * @return Arena or null if not found
     */
    fun getArena(name: String): Arena? {
        return arenas[name]
    }

    /**
     * Get all arena names
     *
     * @return List of arena names
     */
    fun getArenaNames(): List<String> {
        return arenas.keys.toList()
    }

    /**
     * Get all arenas
     *
     * @return Map of arena name to Arena
     */
    fun getAllArenas(): Map<String, Arena> {
        return arenas.toMap()
    }

    /**
     * Set position 1 for admin's setup session
     *
     * @param admin Admin player
     * @param location Position to set
     */
    fun setPos1(admin: Player, location: Location) {
        val session = setupSessions.getOrPut(admin.uniqueId) {
            ArenaSetupSession(admin.uniqueId)
        }
        session.pos1 = location
    }

    /**
     * Set position 2 for admin's setup session
     *
     * @param admin Admin player
     * @param location Position to set
     */
    fun setPos2(admin: Player, location: Location) {
        val session = setupSessions.getOrPut(admin.uniqueId) {
            ArenaSetupSession(admin.uniqueId)
        }
        session.pos2 = location
    }

    /**
     * Get admin's current setup session
     *
     * @param admin Admin player
     * @return Setup session or null if none exists
     */
    fun getSetupSession(admin: Player): ArenaSetupSession? {
        return setupSessions[admin.uniqueId]
    }

    /**
     * Create arena from admin's setup session
     *
     * @param admin Admin player
     * @param name Arena unique name
     * @param displayName Arena display name (optional, defaults to name)
     * @return Created Arena
     * @throws IllegalStateException if session is incomplete or arena name exists
     */
    fun createArena(admin: Player, name: String, displayName: String = name): Arena {
        val session = setupSessions[admin.uniqueId]
            ?: throw IllegalStateException("No setup session found. Use /hs admin setpos1 first.")

        // Validate arena name doesn't exist
        if (arenas.containsKey(name)) {
            throw IllegalStateException("Arena '$name' already exists. Delete it first or use a different name.")
        }

        // Create arena from session
        val arena = session.toArena(name, displayName)

        // Save to memory and config
        arenas[name] = arena
        arenaConfig.saveArena(arena)
        configManager.save(configManager.arenas, "arenas.yml")

        // Clear setup session
        setupSessions.remove(admin.uniqueId)

        plugin.logger.info("Created arena: $name")
        return arena
    }

    /**
     * Delete an arena
     *
     * @param name Arena name
     * @return True if deleted, false if not found
     * @throws IllegalStateException if arena is in use
     */
    fun deleteArena(name: String): Boolean {
        val arena = arenas[name] ?: return false

        // TODO: Check if arena is in use by active game

        // Remove from memory and config
        arenas.remove(name)
        arenaConfig.deleteArena(name)
        configManager.save(configManager.arenas, "arenas.yml")

        plugin.logger.info("Deleted arena: $name")
        return true
    }

    /**
     * Check if an arena exists
     *
     * @param name Arena name
     * @return True if exists
     */
    fun arenaExists(name: String): Boolean {
        return arenas.containsKey(name)
    }

    /**
     * Get a random arena
     *
     * @return Random arena or null if no arenas exist
     */
    fun getRandomArena(): Arena? {
        if (arenas.isEmpty()) {
            return null
        }
        return arenas.values.random()
    }
}
