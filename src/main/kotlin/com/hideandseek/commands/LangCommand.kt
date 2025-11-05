package com.hideandseek.commands

import com.hideandseek.i18n.LanguagePreferenceManager
import com.hideandseek.i18n.MessageManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.*

/**
 * Language command for players to manage their language preferences.
 *
 * Subcommands:
 * - /hs lang - Show current language
 * - /hs lang list - List available languages
 * - /hs lang <language> - Set language (e.g., "ja_JP", "en_US")
 *
 * Examples:
 * - /hs lang
 * - /hs lang list
 * - /hs lang ja_JP
 * - /hs lang en_US
 */
class LangCommand(
    private val languagePreferenceManager: LanguagePreferenceManager,
    private val messageManager: MessageManager
) : CommandExecutor, TabCompleter {

    companion object {
        private const val PERMISSION = "hideandseek.lang"
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // Only players can use this command
        if (sender !is Player) {
            messageManager.send(sender as? Player ?: return true, "error.player_only")
            return true
        }

        // Check permission
        if (!sender.hasPermission(PERMISSION)) {
            messageManager.send(sender, "error.no_permission")
            return true
        }

        when {
            // No arguments - show current language
            args.isEmpty() -> {
                showCurrentLanguage(sender)
            }

            // "list" subcommand - show available languages
            args[0].equals("list", ignoreCase = true) -> {
                showAvailableLanguages(sender)
            }

            // Language code argument - set language
            else -> {
                setLanguage(sender, args[0])
            }
        }

        return true
    }

    /**
     * Shows the player's current language.
     */
    private fun showCurrentLanguage(player: Player) {
        val currentLocale = languagePreferenceManager.getLocale(player)
        val displayName = getLanguageDisplayName(currentLocale)
        messageManager.send(player, "command.lang.current", displayName)
    }

    /**
     * Shows the list of available languages.
     */
    private fun showAvailableLanguages(player: Player) {
        val availableLanguages = languagePreferenceManager.getAvailableLanguages()
        val languageNames = availableLanguages.joinToString(", ") { locale ->
            getLanguageDisplayName(locale)
        }
        messageManager.send(player, "command.lang.list", languageNames)
    }

    /**
     * Sets the player's language preference.
     */
    private fun setLanguage(player: Player, languageCode: String) {
        val availableLanguages = languagePreferenceManager.getAvailableLanguages()

        // Try to parse the language code
        val targetLocale = parseLocale(languageCode)

        if (targetLocale == null) {
            messageManager.send(player, "command.lang.invalid", languageCode)
            showAvailableLanguages(player)
            return
        }

        // Check if the language is available
        if (!availableLanguages.contains(targetLocale)) {
            messageManager.send(player, "command.lang.invalid", languageCode)
            showAvailableLanguages(player)
            return
        }

        // Set the language
        try {
            languagePreferenceManager.setLocale(player, targetLocale, saveImmediately = true)
            val displayName = getLanguageDisplayName(targetLocale)
            messageManager.send(player, "command.lang.set", displayName)
        } catch (e: IllegalArgumentException) {
            messageManager.send(player, "command.lang.invalid", languageCode)
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String>? {
        if (sender !is Player || !sender.hasPermission(PERMISSION)) {
            return emptyList()
        }

        return when (args.size) {
            // First argument: "list" or language codes
            1 -> {
                val suggestions = mutableListOf("list")
                suggestions.addAll(
                    languagePreferenceManager.getAvailableLanguages()
                        .map { it.toLanguageTag().replace("-", "_") }
                )
                suggestions.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            // No further arguments
            else -> emptyList()
        }
    }

    /**
     * Parses a locale string (e.g., "ja_JP", "en_US", "ja-JP") into a Locale object.
     */
    private fun parseLocale(localeString: String): Locale? {
        return try {
            // Convert underscore format to hyphen format (BCP 47)
            val normalized = localeString.replace("_", "-")
            val locale = Locale.forLanguageTag(normalized)

            // Check if the locale was actually parsed (empty language means parsing failed)
            if (locale.language.isEmpty()) null else locale
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets a human-readable display name for a locale.
     *
     * Examples:
     * - ja_JP -> "日本語 (ja_JP)"
     * - en_US -> "English (en_US)"
     */
    private fun getLanguageDisplayName(locale: Locale): String {
        val nativeName = locale.getDisplayLanguage(locale).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }
        val tag = locale.toLanguageTag().replace("-", "_")
        return "$nativeName ($tag)"
    }
}
