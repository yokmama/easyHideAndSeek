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

    // ========== Auto-Restart Configuration ==========

    /**
     * Check if auto-restart is enabled
     */
    fun isAutoRestartEnabled(): Boolean {
        return config.getBoolean("auto-restart.enabled", true)
    }

    /**
     * Get post-game countdown time in seconds
     */
    fun getPostGameCountdown(): Int {
        return config.getInt("auto-restart.post-game-countdown", 20).coerceIn(5, 300)
    }

    /**
     * Check if force-join-all-players is enabled
     */
    fun isForceJoinAllPlayers(): Boolean {
        return config.getBoolean("auto-restart.force-join-all-players", true)
    }

    /**
     * Get broadcast interval for countdown messages in seconds
     */
    fun getBroadcastInterval(): Int {
        return config.getInt("auto-restart.broadcast-interval", 5)
    }

    // ========== Spectator Configuration ==========

    /**
     * Check if spectator mode is enabled
     */
    fun isSpectatorEnabled(): Boolean {
        return config.getBoolean("spectator.enabled", true)
    }

    /**
     * Check if spectators can join during game as hiders
     */
    fun isAllowJoinDuringGame(): Boolean {
        return config.getBoolean("spectator.allow-join-during-game", true)
    }

    /**
     * Check if spectator state should persist
     */
    fun isPersistSpectatorState(): Boolean {
        return config.getBoolean("spectator.persist-state", true)
    }

    /**
     * Get spectator flight speed multiplier
     */
    fun getSpectatorFlightSpeed(): Double {
        return config.getDouble("spectator.flight-speed", 1.5)
    }

    /**
     * Check if seeker count should be shown on spectator scoreboard
     */
    fun isShowSeekerCount(): Boolean {
        return config.getBoolean("spectator.scoreboard.show-seeker-count", true)
    }

    /**
     * Check if hider count should be shown on spectator scoreboard
     */
    fun isShowHiderCount(): Boolean {
        return config.getBoolean("spectator.scoreboard.show-hider-count", true)
    }

    /**
     * Get number of top players to show on spectator scoreboard
     */
    fun getShowTopPlayers(): Int {
        return config.getInt("spectator.scoreboard.show-top-players", 3).coerceIn(0, 10)
    }

    /**
     * Check if team lists should be shown on spectator scoreboard
     */
    fun isShowTeamLists(): Boolean {
        return config.getBoolean("spectator.scoreboard.show-team-lists", true)
    }

    /**
     * Get spectator scoreboard update interval in seconds
     */
    fun getScoreboardUpdateInterval(): Int {
        return config.getInt("spectator.scoreboard.update-interval", 2).coerceAtLeast(1)
    }

    // ========== Capture Configuration ==========

    /**
     * Get capture mode: "SPECTATOR" or "INFECTION"
     *
     * SPECTATOR: Captured hiders become spectators (original mode)
     * INFECTION: Captured hiders become seekers (infection mode)
     */
    fun getCaptureMode(): CaptureMode {
        val modeString = config.getString("capture.mode", "INFECTION")?.uppercase() ?: "INFECTION"
        return try {
            CaptureMode.valueOf(modeString)
        } catch (e: IllegalArgumentException) {
            plugin.logger.warning("Invalid capture mode '$modeString', using INFECTION")
            CaptureMode.INFECTION
        }
    }
}

/**
 * Enum for capture mode configuration
 */
enum class CaptureMode {
    SPECTATOR,  // Captured hiders become spectators
    INFECTION   // Captured hiders become seekers (infection mode)
}
