package com.hideandseek.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.CommandSender

/**
 * Utility for sending colored messages to players
 *
 * Handles color code translation from & codes to Component format
 */
object MessageUtil {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    /**
     * Translate & color codes to Component
     *
     * @param message Message with & color codes (e.g., "&aGreen text")
     * @return Component with colors applied
     */
    fun colorize(message: String): Component {
        return serializer.deserialize(message)
            .decoration(TextDecoration.ITALIC, false)
    }

    /**
     * Send colored message to command sender
     *
     * @param sender Receiver of the message
     * @param message Message with & color codes
     */
    fun send(sender: CommandSender, message: String) {
        sender.sendMessage(colorize(message))
    }

    /**
     * Send multiple colored messages
     *
     * @param sender Receiver of the messages
     * @param messages List of messages with & color codes
     */
    fun send(sender: CommandSender, vararg messages: String) {
        messages.forEach { send(sender, it) }
    }
}
