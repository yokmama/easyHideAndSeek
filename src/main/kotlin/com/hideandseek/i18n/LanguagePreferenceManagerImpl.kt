package com.hideandseek.i18n

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of LanguagePreferenceManager with in-memory caching and YAML persistence.
 *
 * Architecture:
 * - In-memory cache: ConcurrentHashMap<UUID, Locale> for <1ms access
 * - Persistence: languages.yml in plugin data folder
 * - Thread-safe: All operations use concurrent data structures
 *
 * Performance characteristics:
 * - getLocale(): <1ms (cached)
 * - setLocale(): <1ms (in-memory) + async file save
 * - reload(): ~10-50ms depending on file size
 */
class LanguagePreferenceManagerImpl(private val plugin: Plugin) : LanguagePreferenceManager {

    private val languagesFile: File = File(plugin.dataFolder, "languages.yml")
    private var config: FileConfiguration = YamlConfiguration()

    // In-memory cache: UUID -> Locale
    private val preferenceCache = ConcurrentHashMap<UUID, Locale>()

    // Cached configuration values (reloaded on reload())
    private var defaultLanguage: Locale = Locale.JAPANESE
    private var autoDetectEnabled: Boolean = true
    private var availableLanguages: List<Locale> = listOf()

    init {
        reload()
    }

    override fun getLocale(player: Player): Locale {
        val uuid = player.uniqueId

        // 1. Check in-memory cache (fastest path)
        preferenceCache[uuid]?.let { return it }

        // 2. Check languages.yml (slower, but still fast)
        val savedLanguage = config.getString("players.$uuid")
        if (savedLanguage != null) {
            val locale = parseLocale(savedLanguage)
            if (locale != null && availableLanguages.contains(locale)) {
                preferenceCache[uuid] = locale
                return locale
            }
        }

        // 3. Auto-detect from client if enabled
        if (autoDetectEnabled) {
            val clientLocale = parseLocale(player.locale().toString())
            if (clientLocale != null && availableLanguages.contains(clientLocale)) {
                // Cache and save this detected preference
                setLocale(player, clientLocale, saveImmediately = false)
                return clientLocale
            }
        }

        // 4. Fall back to default language
        val defaultLoc = defaultLanguage
        preferenceCache[uuid] = defaultLoc
        return defaultLoc
    }

    override fun setLocale(player: Player, locale: Locale, saveImmediately: Boolean) {
        require(availableLanguages.contains(locale)) {
            "Language ${locale.toLanguageTag()} is not available. Available: ${availableLanguages.joinToString { it.toLanguageTag() }}"
        }

        val uuid = player.uniqueId

        // Update in-memory cache immediately
        preferenceCache[uuid] = locale

        // Update config
        config.set("players.$uuid", locale.toLanguageTag())

        // Save to file
        if (saveImmediately) {
            saveConfig()
        } else {
            // Queue async save
            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                saveConfig()
            })
        }
    }

    override fun getAvailableLanguages(): List<Locale> {
        return availableLanguages
    }

    override fun getDefaultLanguage(): Locale {
        return defaultLanguage
    }

    override fun isAutoDetectEnabled(): Boolean {
        return autoDetectEnabled
    }

    override fun removePreference(player: Player) {
        val uuid = player.uniqueId

        // Remove from cache
        preferenceCache.remove(uuid)

        // Remove from config
        config.set("players.$uuid", null)

        // Save to file asynchronously
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            saveConfig()
        })
    }

    override fun saveAll() {
        saveConfig()
    }

    override fun reload() {
        // Load or create languages.yml
        if (!languagesFile.exists()) {
            plugin.saveResource("languages.yml", false)
        }

        config = YamlConfiguration.loadConfiguration(languagesFile)

        // Load settings
        defaultLanguage = parseLocale(config.getString("settings.default-language", "ja_JP")) ?: Locale.JAPANESE
        autoDetectEnabled = config.getBoolean("settings.auto-detect-client-locale", true)

        // Load available languages
        val langStrings = config.getStringList("settings.available-languages")
        availableLanguages = langStrings.mapNotNull { parseLocale(it) }.ifEmpty {
            listOf(Locale.JAPANESE, Locale.US)
        }

        // Clear and reload preference cache
        preferenceCache.clear()
        val playersSection = config.getConfigurationSection("players")
        playersSection?.getKeys(false)?.forEach { uuidString ->
            val uuid = try {
                UUID.fromString(uuidString)
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Invalid UUID in languages.yml: $uuidString")
                return@forEach
            }

            val localeString = config.getString("players.$uuidString")
            val locale = parseLocale(localeString)
            if (locale != null && availableLanguages.contains(locale)) {
                preferenceCache[uuid] = locale
            }
        }

        plugin.logger.info("Loaded ${preferenceCache.size} language preferences")
    }

    override fun getPreferenceCount(): Int {
        return config.getConfigurationSection("players")?.getKeys(false)?.size ?: 0
    }

    /**
     * Saves the configuration to languages.yml.
     *
     * This method is synchronized to prevent concurrent file writes.
     */
    @Synchronized
    private fun saveConfig() {
        try {
            config.save(languagesFile)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save languages.yml: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Parses a locale string (e.g., "ja_JP", "en-US") into a Locale object.
     *
     * Handles both underscore and hyphen separators.
     *
     * @param localeString The locale string to parse (e.g., "ja_JP" or "en-US")
     * @return A Locale object, or null if parsing fails
     */
    private fun parseLocale(localeString: String?): Locale? {
        if (localeString == null || localeString.isBlank()) {
            return null
        }

        return try {
            // Use Locale.forLanguageTag() for modern locale parsing
            // Convert underscore format to hyphen format (BCP 47)
            val normalized = localeString.replace("_", "-")
            Locale.forLanguageTag(normalized)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to parse locale: $localeString")
            null
        }
    }
}
