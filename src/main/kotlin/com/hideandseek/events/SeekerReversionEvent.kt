package com.hideandseek.events

import com.hideandseek.game.Game
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Event fired when a seeker is reverted to a hider.
 *
 * This event is triggered when a weaker seeker attacks a stronger seeker,
 * resulting in the attacker being forced back to hider role.
 *
 * @property player The player who is being reverted to hider
 * @property game The active game
 * @property previousStrength The strength points the player had before reversion
 * @property restoredPoints The economic points restored to the player (hider points)
 */
class SeekerReversionEvent(
    val player: Player,
    val game: Game,
    val previousStrength: Int,
    val restoredPoints: Int
) : Event() {

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
