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
    ),

    // T012: NEW EFFECT TYPES FOR EXPANDED ITEM VARIETY

    // --- HIDER ITEMS ---

    /**
     * Shadow Sprint - Speed boost for Hiders
     * Duration: 20 seconds
     * Intensity: 1.5x speed
     */
    SHADOW_SPRINT(
        displayName = "影の疾走",
        defaultDuration = 20,
        defaultIntensity = 1.5,
        icon = Material.FEATHER
    ),

    /**
     * Decoy Block - Places fake disguise block at another location
     * Duration: Until destroyed or game ends
     * Intensity: N/A (1.0)
     */
    DECOY_BLOCK(
        displayName = "デコイブロック",
        defaultDuration = 600,  // 10 minutes (until game ends)
        defaultIntensity = 1.0,
        icon = Material.ARMOR_STAND
    ),

    /**
     * Second Chance - Respawn on capture with immunity
     * Duration: 10 seconds immunity after respawn
     * Intensity: N/A (1.0)
     */
    SECOND_CHANCE(
        displayName = "セカンドチャンス",
        defaultDuration = 10,
        defaultIntensity = 1.0,
        icon = Material.TOTEM_OF_UNDYING
    ),

    // --- SEEKER ITEMS ---

    /**
     * Tracker Compass - Points to nearest Hider
     * Duration: 30 seconds
     * Intensity: N/A (1.0)
     */
    TRACKER_COMPASS(
        displayName = "追跡コンパス",
        defaultDuration = 30,
        defaultIntensity = 1.0,
        icon = Material.COMPASS
    ),

    /**
     * Area Scan - Reveals Hider count in radius
     * Duration: Instant (5 seconds display)
     * Intensity: Scan radius in blocks
     */
    AREA_SCAN(
        displayName = "エリアスキャン",
        defaultDuration = 5,
        defaultIntensity = 12.0,  // 12 block radius
        icon = Material.ECHO_SHARD
    ),

    /**
     * Eagle Eye - Highlights disguised blocks with particles
     * Duration: 20 seconds
     * Intensity: Detection radius in blocks
     */
    EAGLE_EYE(
        displayName = "鷹の目",
        defaultDuration = 20,
        defaultIntensity = 15.0,  // 15 block radius
        icon = Material.ENDER_EYE
    ),

    /**
     * Tracker Insight - Shows time since Hiders last moved
     * Duration: Instant (10 seconds display)
     * Intensity: N/A (1.0)
     */
    TRACKER_INSIGHT(
        displayName = "追跡の洞察",
        defaultDuration = 10,
        defaultIntensity = 1.0,
        icon = Material.CLOCK
    ),

    /**
     * Capture Net - Next capture grants 2x reward
     * Duration: 10 seconds (window to capture)
     * Intensity: 2.0x reward multiplier
     */
    CAPTURE_NET(
        displayName = "捕獲ネット",
        defaultDuration = 10,
        defaultIntensity = 2.0,
        icon = Material.FISHING_ROD
    );

    /**
     * Get configuration key for this effect type
     */
    fun getConfigKey(): String = this.name.lowercase()
}
