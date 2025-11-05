package com.hideandseek.i18n

import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import java.time.Duration

/**
 * Manages localized message delivery to players using Adventure Components.
 *
 * This interface provides methods to send messages in a player's preferred language.
 * All messages are:
 * - Loaded from messages_{locale}.properties files
 * - Formatted with MessageFormat for placeholder substitution
 * - Converted to Adventure Components with color code support
 * - Delivered via appropriate channels (chat, action bar, title, etc.)
 *
 * Performance requirement: getMessage() must complete in <1ms for cached values.
 *
 * Thread safety: All implementations must be thread-safe for concurrent access.
 */
interface MessageManager {

    /**
     * Gets a localized message as an Adventure Component.
     *
     * Resolution process:
     * 1. Get player's locale from LanguagePreferenceManager
     * 2. Load ResourceBundle for that locale (with UTF8Control)
     * 3. Get message template from bundle using key
     * 4. Format with MessageFormat using provided arguments
     * 5. Convert & color codes to § codes
     * 6. Parse as Adventure Component
     *
     * Performance: <1ms for cached ResourceBundles and MessageFormats
     *
     * @param player The player whose language to use
     * @param key The message key (e.g., "game.start", "player.join.success")
     * @param args Optional arguments for MessageFormat placeholders ({0}, {1}, etc.)
     * @return A formatted Component with color codes applied
     * @throws MissingResourceException If the key is not found in any properties file
     */
    fun getMessage(player: Player, key: String, vararg args: Any): Component

    /**
     * Gets a raw localized message string without Component conversion.
     *
     * Useful for:
     * - Logging purposes
     * - Non-player contexts (console messages)
     * - Custom Component building
     *
     * @param player The player whose language to use (or null for default language)
     * @param key The message key
     * @param args Optional arguments for MessageFormat placeholders
     * @return A formatted string with & color codes (not converted to §)
     */
    fun getRawMessage(player: Player?, key: String, vararg args: Any): String

    /**
     * Sends a localized chat message to a player.
     *
     * Equivalent to:
     * ```
     * player.sendMessage(getMessage(player, key, *args))
     * ```
     *
     * @param player The player to send the message to
     * @param key The message key
     * @param args Optional arguments for placeholders
     */
    fun send(player: Player, key: String, vararg args: Any)

    /**
     * Sends a localized action bar message to a player.
     *
     * Action bar messages appear above the hotbar and are useful for:
     * - Real-time status updates (time remaining, disguise status)
     * - Non-intrusive notifications
     * - Temporary information display
     *
     * @param player The player to send the action bar to
     * @param key The message key
     * @param args Optional arguments for placeholders
     */
    fun sendActionBar(player: Player, key: String, vararg args: Any)

    /**
     * Sends a localized title and optional subtitle to a player.
     *
     * Titles are used for:
     * - Important announcements (game start, game end)
     * - Role assignments (you are a hider/seeker)
     * - Major events (player captured)
     *
     * @param player The player to send the title to
     * @param titleKey The message key for the main title
     * @param subtitleKey Optional message key for the subtitle (null for no subtitle)
     * @param fadeIn Duration for fade-in animation (default: 0.5s)
     * @param stay Duration to display the title (default: 3s)
     * @param fadeOut Duration for fade-out animation (default: 1s)
     * @param args Optional arguments for placeholders (used for both title and subtitle)
     */
    fun sendTitle(
        player: Player,
        titleKey: String,
        subtitleKey: String? = null,
        fadeIn: Duration = Duration.ofMillis(500),
        stay: Duration = Duration.ofSeconds(3),
        fadeOut: Duration = Duration.ofSeconds(1),
        vararg args: Any
    )

    /**
     * Broadcasts a localized message to multiple players.
     *
     * Each player receives the message in their own language.
     * This is used for:
     * - Game-wide announcements
     * - Capture notifications
     * - Phase transitions
     *
     * Performance: Parallelized for large player lists
     *
     * @param players The players to broadcast to
     * @param key The message key
     * @param args Optional arguments for placeholders
     */
    fun broadcast(players: Collection<Player>, key: String, vararg args: Any)

    /**
     * Broadcasts a localized action bar to multiple players.
     *
     * Each player receives the action bar in their own language.
     *
     * @param players The players to broadcast to
     * @param key The message key
     * @param args Optional arguments for placeholders
     */
    fun broadcastActionBar(players: Collection<Player>, key: String, vararg args: Any)

    /**
     * Broadcasts a localized title to multiple players.
     *
     * Each player receives the title in their own language.
     *
     * @param players The players to broadcast to
     * @param titleKey The message key for the main title
     * @param subtitleKey Optional message key for the subtitle
     * @param fadeIn Duration for fade-in animation
     * @param stay Duration to display the title
     * @param fadeOut Duration for fade-out animation
     * @param args Optional arguments for placeholders
     */
    fun broadcastTitle(
        players: Collection<Player>,
        titleKey: String,
        subtitleKey: String? = null,
        fadeIn: Duration = Duration.ofMillis(500),
        stay: Duration = Duration.ofSeconds(3),
        fadeOut: Duration = Duration.ofSeconds(1),
        vararg args: Any
    )

    /**
     * Clears the ResourceBundle cache for all locales.
     *
     * This is called:
     * - After /hs admin reload
     * - After languages are changed
     * - After .properties files are modified
     *
     * Performance note: Next message lookup will be slower (10-50ms) until cache warms up.
     */
    fun clearCache()

    /**
     * Checks if a message key exists in the properties files.
     *
     * Useful for:
     * - Validation during development
     * - Fallback logic
     * - Debug commands
     *
     * @param key The message key to check
     * @param locale Optional specific locale to check (null = check default)
     * @return True if the key exists in the properties file
     */
    fun hasKey(key: String, locale: java.util.Locale? = null): Boolean
}
