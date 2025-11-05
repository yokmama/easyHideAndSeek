package com.hideandseek.i18n

import org.bukkit.entity.Player
import java.util.*

/**
 * Manages player language preferences with persistence and caching.
 *
 * This interface provides methods to get, set, and manage player language preferences.
 * Preferences are:
 * - Cached in memory for <1ms access time
 * - Persisted to languages.yml for survival across server restarts
 * - Auto-detected from client locale on first join (if enabled)
 *
 * Performance requirement: getLocale() must complete in <1ms for cached values.
 *
 * Thread safety: All implementations must be thread-safe for concurrent access.
 */
interface LanguagePreferenceManager {

    /**
     * Gets the locale for a player.
     *
     * Resolution order:
     * 1. Check in-memory cache (UUID -> Locale)
     * 2. If not cached, check languages.yml
     * 3. If not found and auto-detect enabled, use player.locale()
     * 4. Otherwise, use default language from config
     *
     * Performance: <1ms for cached values (guaranteed)
     *
     * @param player The player whose locale to retrieve
     * @return The player's preferred locale (never null)
     */
    fun getLocale(player: Player): Locale

    /**
     * Sets the locale for a player.
     *
     * Actions performed:
     * 1. Update in-memory cache immediately
     * 2. Optionally save to languages.yml (synchronously or asynchronously)
     * 3. Clear ResourceBundle cache for this player if needed
     *
     * @param player The player whose locale to set
     * @param locale The locale to set (must be in available languages)
     * @param saveImmediately If true, saves to file synchronously; if false, queues async save
     * @throws IllegalArgumentException If locale is not in available languages
     */
    fun setLocale(player: Player, locale: Locale, saveImmediately: Boolean = true)

    /**
     * Gets the list of available languages on this server.
     *
     * This list is read from languages.yml configuration:
     * ```yaml
     * settings:
     *   available-languages:
     *     - "ja_JP"
     *     - "en_US"
     * ```
     *
     * Performance: Cached value, <1ms access time
     *
     * @return List of supported locales (never empty)
     */
    fun getAvailableLanguages(): List<Locale>

    /**
     * Gets the default language for new players.
     *
     * This is read from languages.yml configuration:
     * ```yaml
     * settings:
     *   default-language: "ja_JP"
     * ```
     *
     * Performance: Cached value, <1ms access time
     *
     * @return The default locale (never null)
     */
    fun getDefaultLanguage(): Locale

    /**
     * Checks if auto-detection of client locale is enabled.
     *
     * When enabled, new players automatically use their Minecraft client's
     * language setting (via Paper's player.locale()) if it's in available languages.
     *
     * This is read from languages.yml configuration:
     * ```yaml
     * settings:
     *   auto-detect-client-locale: true
     * ```
     *
     * @return True if auto-detection is enabled
     */
    fun isAutoDetectEnabled(): Boolean

    /**
     * Removes a player's language preference from cache and persistence.
     *
     * Used when a player leaves the server or resets their preference.
     * The preference is removed from:
     * - In-memory cache
     * - languages.yml file
     *
     * Next time the player joins, they will get auto-detected or default language.
     *
     * @param player The player whose preference to remove
     */
    fun removePreference(player: Player)

    /**
     * Saves all pending language preferences to languages.yml.
     *
     * This is called:
     * - On plugin disable
     * - Periodically by auto-save task (if implemented)
     * - Manually via admin command
     *
     * Implementation note: Should be synchronous to ensure data safety on shutdown.
     */
    fun saveAll()

    /**
     * Reloads language preferences and configuration from languages.yml.
     *
     * This is called:
     * - On plugin enable
     * - After admin uses /hs admin reload
     *
     * Actions performed:
     * 1. Clear in-memory cache
     * 2. Reload configuration (default language, auto-detect setting, available languages)
     * 3. Reload player preferences from file
     */
    fun reload()

    /**
     * Gets the total number of players with saved language preferences.
     *
     * This counts entries in the languages.yml file, not in-memory cache.
     *
     * @return Number of players with saved preferences
     */
    fun getPreferenceCount(): Int
}
