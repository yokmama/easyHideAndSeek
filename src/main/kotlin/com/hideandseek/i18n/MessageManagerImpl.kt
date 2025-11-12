package com.hideandseek.i18n

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.text.MessageFormat
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of MessageManager with 2-layer caching for optimal performance.
 *
 * Caching architecture:
 * - Layer 1: ResourceBundle cache (built-in to Java, per-locale)
 * - Layer 2: MessageFormat cache (custom, per-locale per-key)
 *
 * Performance characteristics:
 * - First message lookup: ~10-50ms (loads ResourceBundle + creates MessageFormat)
 * - Cached message lookup: <1ms (cache hit on both layers)
 * - Color code conversion: ~0.1ms (Adventure serializer)
 *
 * Thread safety: All caches use ConcurrentHashMap for thread-safe access.
 */
class MessageManagerImpl(
    private val plugin: Plugin,
    private val languagePreferenceManager: LanguagePreferenceManager
) : MessageManager {

    companion object {
        private const val MESSAGES_BASE_NAME = "messages"
        private val LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand()
        private val UTF8_CONTROL = UTF8Control()
    }

    // Layer 2 cache: Locale -> (MessageKey -> MessageFormat)
    private val messageFormatCache = ConcurrentHashMap<Locale, ConcurrentHashMap<String, MessageFormat>>()

    override fun getMessage(player: Player, key: String, vararg args: Any): Component {
        val rawMessage = getRawMessage(player, key, *args)
        return LEGACY_SERIALIZER.deserialize(rawMessage)
    }

    override fun getRawMessage(player: Player?, key: String, vararg args: Any): String {
        val locale = player?.let { languagePreferenceManager.getLocale(it) }
            ?: languagePreferenceManager.getDefaultLanguage()

        return try {
            val messageFormat = getMessageFormat(locale, key)

            if (args.isEmpty()) {
                // No placeholders, return as-is
                messageFormat.toPattern()
            } else {
                // Format with arguments
                messageFormat.format(args)
            }
        } catch (e: MissingResourceException) {
            // Key not found - return error message
            plugin.logger.warning("Missing translation key: $key for locale: ${locale.toLanguageTag()}")
            getRawMessage(null, "system.translation.missing", key)
        } catch (e: Exception) {
            // Other errors (formatting, etc.)
            plugin.logger.severe("Error formatting message '$key' for locale ${locale.toLanguageTag()}: ${e.message}")
            "[Error: $key]"
        }
    }

    override fun send(player: Player, key: String, vararg args: Any) {
        player.sendMessage(getMessage(player, key, *args))
    }

    override fun sendActionBar(player: Player, key: String, vararg args: Any) {
        player.sendActionBar(getMessage(player, key, *args))
    }

    override fun sendTitle(
        player: Player,
        titleKey: String,
        subtitleKey: String?,
        fadeIn: Duration,
        stay: Duration,
        fadeOut: Duration,
        vararg args: Any
    ) {
        val title = getMessage(player, titleKey, *args)
        val subtitle = subtitleKey?.let { getMessage(player, it, *args) } ?: Component.empty()

        val times = Title.Times.times(fadeIn, stay, fadeOut)
        player.showTitle(Title.title(title, subtitle, times))
    }

    override fun broadcast(players: Collection<Player>, key: String, vararg args: Any) {
        players.forEach { player ->
            send(player, key, *args)
        }
    }

    override fun broadcastActionBar(players: Collection<Player>, key: String, vararg args: Any) {
        players.forEach { player ->
            sendActionBar(player, key, *args)
        }
    }

    override fun broadcastTitle(
        players: Collection<Player>,
        titleKey: String,
        subtitleKey: String?,
        fadeIn: Duration,
        stay: Duration,
        fadeOut: Duration,
        vararg args: Any
    ) {
        players.forEach { player ->
            sendTitle(player, titleKey, subtitleKey, fadeIn, stay, fadeOut, *args)
        }
    }

    override fun clearCache() {
        // Clear Layer 2 cache
        messageFormatCache.clear()

        // Clear Layer 1 cache (ResourceBundle)
        ResourceBundle.clearCache()

        plugin.logger.info("Message cache cleared")
    }

    override fun hasKey(key: String, locale: Locale?): Boolean {
        val targetLocale = locale ?: languagePreferenceManager.getDefaultLanguage()

        return try {
            val bundle = ResourceBundle.getBundle(MESSAGES_BASE_NAME, targetLocale, UTF8_CONTROL)
            bundle.containsKey(key)
        } catch (e: MissingResourceException) {
            false
        }
    }

    override fun send(sender: CommandSender, key: String, vararg args: Any) {
        if (sender is Player) {
            send(sender, key, *args)
        } else {
            // Console or other CommandSender - use default language
            val rawMessage = getRawMessage(null, key, *args)
            val component = LEGACY_SERIALIZER.deserialize(rawMessage)
            sender.sendMessage(component)
        }
    }

    override fun reload() {
        clearCache()
        // Clear ResourceBundle cache (Layer 1)
        ResourceBundle.clearCache()
        plugin.logger.info("MessageManager reloaded - all translation caches cleared")
    }

    override fun getAvailableLanguages(): List<String> {
        return languagePreferenceManager.getAvailableLanguages().map { it.toLanguageTag() }
    }

    /**
     * Gets a MessageFormat for a specific locale and key.
     *
     * This method implements the 2-layer caching:
     * 1. Check Layer 2 cache (MessageFormat cache)
     * 2. If miss, load from Layer 1 (ResourceBundle) and create MessageFormat
     * 3. Store in Layer 2 cache for future use
     *
     * Performance: <1ms for cache hits, ~10-50ms for cache misses
     *
     * @param locale The locale to use
     * @param key The message key
     * @return A MessageFormat ready for formatting
     * @throws MissingResourceException If the key is not found
     */
    private fun getMessageFormat(locale: Locale, key: String): MessageFormat {
        // Get or create locale-specific cache
        val localeCache = messageFormatCache.computeIfAbsent(locale) {
            ConcurrentHashMap()
        }

        // Get or create MessageFormat for this key
        return localeCache.computeIfAbsent(key) { messageKey ->
            val bundle = ResourceBundle.getBundle(MESSAGES_BASE_NAME, locale, UTF8_CONTROL)
            val pattern = bundle.getString(messageKey)
            MessageFormat(pattern, locale)
        }
    }
}
