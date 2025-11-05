package com.hideandseek.config

import org.bukkit.configuration.ConfigurationSection

/**
 * Configuration for block restoration system
 * Loads settings from block-restoration.yml
 */
class BlockRestorationConfig(private val section: ConfigurationSection) {

    // Core Settings
    val enabled: Boolean
        get() = section.getBoolean("block-restoration.enabled", true)

    val restorationDelaySeconds: Int
        get() = section.getInt("block-restoration.restoration-delay-seconds", 5)

    val maxRestorationsPerTick: Int
        get() = section.getInt("block-restoration.max-restorations-per-tick", 10)

    val onlyDuringActiveGame: Boolean
        get() = section.getBoolean("block-restoration.only-during-active-game", true)

    val applyDuringPreparation: Boolean
        get() = section.getBoolean("block-restoration.apply-during-preparation", true)

    // Performance
    val enableTileEntityRestoration: Boolean
        get() = section.getBoolean("block-restoration.enable-tile-entity-restoration", true)

    // Debug
    val debugLogging: Boolean
        get() = section.getBoolean("block-restoration.debug-logging", false)

    val logRestorationStats: Boolean
        get() = section.getBoolean("block-restoration.log-restoration-stats", false)

    // Random Respawn Settings
    val randomRespawnEnabled: Boolean
        get() = section.getBoolean("random-respawn.enabled", true)

    val respawnSearchRadius: Int
        get() = section.getInt("random-respawn.search-radius", 50)

    val maxRespawnAttempts: Int
        get() = section.getInt("random-respawn.max-attempts", 50)

    val useGameBoundaries: Boolean
        get() = section.getBoolean("random-respawn.use-game-boundaries", true)

    /**
     * Get restoration delay in milliseconds
     */
    fun getRestorationDelayMs(): Long = restorationDelaySeconds * 1000L
}
