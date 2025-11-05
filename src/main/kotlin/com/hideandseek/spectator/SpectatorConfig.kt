package com.hideandseek.spectator

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.UUID

/**
 * Manages spectator state persistence via YAML file
 */
class SpectatorConfig(private val plugin: Plugin) {

    private val file: File = File(plugin.dataFolder, "spectators.yml")
    private lateinit var config: FileConfiguration

    /**
     * Load spectator states from file
     */
    fun load() {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
        config = YamlConfiguration.loadConfiguration(file)
    }

    /**
     * Save spectator states to file
     */
    fun save() {
        config.save(file)
    }

    /**
     * Check if a player is in spectator mode
     *
     * @param uuid Player UUID
     * @return true if spectator mode is enabled, false otherwise
     */
    fun isSpectator(uuid: UUID): Boolean {
        return config.getBoolean("spectators.$uuid.enabled", false)
    }

    /**
     * Set spectator mode state for a player
     *
     * @param uuid Player UUID
     * @param enabled Whether spectator mode should be enabled
     */
    fun setSpectator(uuid: UUID, enabled: Boolean) {
        config.set("spectators.$uuid.enabled", enabled)
        config.set("spectators.$uuid.last-updated", System.currentTimeMillis())
        save()
    }

    /**
     * Get the last updated timestamp for a player's spectator state
     *
     * @param uuid Player UUID
     * @return Timestamp in milliseconds, or 0 if not found
     */
    fun getLastUpdated(uuid: UUID): Long {
        return config.getLong("spectators.$uuid.last-updated", 0L)
    }

    /**
     * Remove spectator state for a player
     *
     * @param uuid Player UUID
     */
    fun removeSpectator(uuid: UUID) {
        config.set("spectators.$uuid", null)
        save()
    }

    /**
     * Get all spectator UUIDs
     *
     * @return Set of UUIDs with spectator mode enabled
     */
    fun getAllSpectators(): Set<UUID> {
        val spectators = config.getConfigurationSection("spectators") ?: return emptySet()
        return spectators.getKeys(false)
            .mapNotNull {
                try {
                    UUID.fromString(it)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            .filter { isSpectator(it) }
            .toSet()
    }
}
