package com.hideandseek.items

/**
 * T011: Configuration data for item effects
 *
 * Contains all parameters needed to apply an item effect
 */
data class ItemConfig(
    /**
     * Effect duration in seconds
     */
    val duration: Int,

    /**
     * Effect intensity/magnitude (e.g., speed multiplier, scan radius)
     */
    val intensity: Double,

    /**
     * Additional effect-specific metadata
     * Examples:
     * - Tracker Compass: targetHiderId (UUID)
     * - Decoy Block: decoyLocation (Location)
     * - Area Scan: hiderCount (Int), directions (List<String>)
     */
    val metadata: Map<String, Any> = emptyMap()
) {
    companion object {
        /**
         * Create config with just duration and default intensity
         */
        fun withDuration(seconds: Int): ItemConfig {
            return ItemConfig(
                duration = seconds,
                intensity = 1.0,
                metadata = emptyMap()
            )
        }

        /**
         * Create config with duration and intensity
         */
        fun withDurationAndIntensity(seconds: Int, intensity: Double): ItemConfig {
            return ItemConfig(
                duration = seconds,
                intensity = intensity,
                metadata = emptyMap()
            )
        }

        /**
         * Create config with metadata
         */
        fun withMetadata(seconds: Int, intensity: Double, metadata: Map<String, Any>): ItemConfig {
            return ItemConfig(
                duration = seconds,
                intensity = intensity,
                metadata = metadata
            )
        }
    }

    /**
     * Get metadata value by key with type safety
     */
    inline fun <reified T> getMetadata(key: String): T? {
        return metadata[key] as? T
    }

    /**
     * Get metadata value with default
     */
    inline fun <reified T> getMetadataOrDefault(key: String, default: T): T {
        return (metadata[key] as? T) ?: default
    }
}
