package com.hideandseek.config

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.io.IOException

/**
 * Manages plugin configuration files
 *
 * Loads config.yml, arenas.yml, and shop.yml
 */
class ConfigManager(private val plugin: Plugin) {

    lateinit var config: FileConfiguration
        private set

    lateinit var arenas: FileConfiguration
        private set

    lateinit var shop: FileConfiguration
        private set

    /**
     * Load all configuration files
     *
     * - Saves default resources if they don't exist
     * - Loads YAML configurations
     * - Handles errors gracefully
     */
    fun load() {
        // Load main config
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        config = plugin.config

        // Load arenas.yml
        arenas = loadOrCreate("arenas.yml")

        // Load shop.yml
        shop = loadOrCreate("shop.yml")

        plugin.logger.info("Configuration files loaded successfully")
    }

    /**
     * Load or create a configuration file
     *
     * @param fileName Name of the file (e.g., "arenas.yml")
     * @return Loaded FileConfiguration
     */
    private fun loadOrCreate(fileName: String): FileConfiguration {
        val file = File(plugin.dataFolder, fileName)

        // Create file from resources if it doesn't exist
        if (!file.exists()) {
            try {
                plugin.saveResource(fileName, false)
                plugin.logger.info("Created default $fileName")
            } catch (e: IllegalArgumentException) {
                // Resource doesn't exist in jar, create empty file
                try {
                    file.parentFile?.mkdirs()
                    file.createNewFile()
                    plugin.logger.warning("$fileName not found in resources, created empty file")
                } catch (ioException: IOException) {
                    plugin.logger.severe("Failed to create $fileName: ${ioException.message}")
                }
            }
        }

        return YamlConfiguration.loadConfiguration(file)
    }

    /**
     * Save a configuration file
     *
     * @param config FileConfiguration to save
     * @param fileName File name (e.g., "arenas.yml")
     */
    fun save(config: FileConfiguration, fileName: String) {
        val file = File(plugin.dataFolder, fileName)
        try {
            config.save(file)
            plugin.logger.info("Saved $fileName")
        } catch (e: IOException) {
            plugin.logger.severe("Failed to save $fileName: ${e.message}")
        }
    }

    /**
     * Reload all configurations
     */
    fun reload() {
        load()
        plugin.logger.info("Configuration reloaded")
    }

    /**
     * Get game preparation time in seconds
     */
    fun getPreparationTime(): Int {
        return config.getInt("game.preparation-time", 30)
    }

    /**
     * Get game seek time in seconds
     */
    fun getSeekTime(): Int {
        return config.getInt("game.seek-time", 600)
    }

    /**
     * Get minimum players required
     */
    fun getMinPlayers(): Int {
        return config.getInt("general.min-players", 2)
    }

    /**
     * Get maximum players allowed
     */
    fun getMaxPlayers(): Int {
        return config.getInt("general.max-players", 30)
    }

    /**
     * Get seeker ratio (0.23 = 23%)
     */
    fun getSeekerRatio(): Double {
        return config.getDouble("game.seeker-ratio", 0.23)
    }

    /**
     * Check if economy is enabled
     */
    fun isEconomyEnabled(): Boolean {
        return config.getBoolean("economy.enabled", true)
    }

    /**
     * Get hider win reward
     */
    fun getHiderWinReward(): Double {
        return config.getDouble("economy.rewards.hider-win", 1000.0)
    }

    /**
     * Get seeker win reward
     */
    fun getSeekerWinReward(): Double {
        return config.getDouble("economy.rewards.seeker-win", 1000.0)
    }

    /**
     * Get per-capture reward
     */
    fun getPerCaptureReward(): Double {
        return config.getDouble("economy.rewards.per-capture", 200.0)
    }

    /**
     * Get participation reward
     */
    fun getParticipationReward(): Double {
        return config.getDouble("economy.rewards.participation", 100.0)
    }
}
