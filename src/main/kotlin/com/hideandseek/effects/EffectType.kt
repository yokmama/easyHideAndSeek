package com.hideandseek.effects

import org.bukkit.Material

/**
 * Types of effects available in the seeker shop system
 *
 * @property displayName User-facing name (Japanese)
 * @property defaultDuration Default duration in seconds
 * @property defaultIntensity Default intensity multiplier
 * @property icon Material icon for shop GUI
 */
enum class EffectType(
    val displayName: String,
    val defaultDuration: Int,
    val defaultIntensity: Double,
    val icon: Material
) {
    /**
     * Vision enhancement - Night Vision + extended view distance (32 chunks)
     * Duration: 30 seconds
     */
    VISION(
        displayName = "視界拡大",
        defaultDuration = 30,
        defaultIntensity = 1.0,
        icon = Material.ENDER_EYE
    ),

    /**
     * Disguise block glow detection - Shows particles around disguised blocks
     * Duration: 15 seconds (powerful effect, shorter duration)
     */
    GLOW(
        displayName = "偽装ブロック発光",
        defaultDuration = 15,
        defaultIntensity = 1.0,
        icon = Material.GLOWSTONE_DUST
    ),

    /**
     * Speed boost - Speed II potion effect
     * Duration: 30 seconds
     */
    SPEED(
        displayName = "スピードブースト",
        defaultDuration = 30,
        defaultIntensity = 1.0,
        icon = Material.SUGAR
    ),

    /**
     * Attack reach extension - Extends attack range by multiplier
     * Duration: 30 seconds
     * Intensity: 1.5x multiplier (4.5 blocks → 6.75 blocks)
     */
    REACH(
        displayName = "攻撃範囲拡大",
        defaultDuration = 30,
        defaultIntensity = 1.5,
        icon = Material.STICK
    );

    /**
     * Get configuration key for this effect type
     */
    fun getConfigKey(): String = this.name.lowercase()
}
