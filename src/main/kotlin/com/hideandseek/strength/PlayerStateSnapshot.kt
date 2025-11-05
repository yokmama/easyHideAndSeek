package com.hideandseek.strength

import org.bukkit.Location
import java.util.UUID

/**
 * Represents a snapshot of a player's state when they were a hider.
 *
 * This class is currently optional and reserved for future enhancements.
 * In the current implementation, we restore economic points directly from
 * PlayerGameData.hiderPoints without using snapshots.
 *
 * Future use cases:
 * - Restoring inventory contents
 * - Restoring potion effects
 * - Restoring specific spawn location
 * - Restoring other custom player state
 *
 * @property playerId The unique identifier of the player
 * @property economicPoints The economic points the player had as a hider
 * @property location The location where the player was captured (optional, may respawn randomly)
 * @property timestamp The time when this snapshot was created (milliseconds since epoch)
 */
data class PlayerStateSnapshot(
    val playerId: UUID,
    val economicPoints: Int,
    val location: Location?,
    val timestamp: Long = System.currentTimeMillis()
)
