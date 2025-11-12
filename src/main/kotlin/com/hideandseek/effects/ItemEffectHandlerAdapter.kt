package com.hideandseek.effects

import com.hideandseek.game.GameManager
import com.hideandseek.items.ItemEffectHandler
import org.bukkit.entity.Player

/**
 * Adapter to bridge ItemEffectHandler (new interface) to EffectHandler (old interface)
 *
 * This allows new ItemEffectHandler-based handlers to be registered in the EffectManagerImpl
 * without requiring them to directly implement the old EffectHandler interface.
 */
class ItemEffectHandlerAdapter(
    private val itemHandler: ItemEffectHandler,
    private val gameManager: GameManager
) : EffectHandler {

    override fun apply(player: Player, effect: ActiveEffect) {
        player.sendMessage("§d[DEBUG] ItemEffectHandlerAdapter.apply() called for ${effect.effectType}")
        player.sendMessage("§d[DEBUG] Handler class: ${itemHandler.javaClass.simpleName}")

        val game = gameManager.activeGame
        if (game == null) {
            player.sendMessage("§c[DEBUG] No active game found!")
            return
        }
        player.sendMessage("§d[DEBUG] Active game found: ${game.arena.name}")

        // Check if handler can apply
        if (!itemHandler.canApply(player, game)) {
            player.sendMessage("§c[DEBUG] Handler.canApply() returned false!")
            return
        }
        player.sendMessage("§a[DEBUG] Handler.canApply() returned true")

        // Convert ActiveEffect to ItemConfig
        val config = com.hideandseek.items.ItemConfig(
            duration = ((effect.endTime.epochSecond - effect.startTime.epochSecond).toInt()),
            intensity = effect.intensity,
            metadata = effect.metadata
        )
        player.sendMessage("§d[DEBUG] ItemConfig created: duration=${config.duration}, intensity=${config.intensity}")

        // Apply through ItemEffectHandler
        player.sendMessage("§d[DEBUG] Calling itemHandler.apply()...")
        val result = itemHandler.apply(player, game, config)
        player.sendMessage("§d[DEBUG] itemHandler.apply() result: success=${result.isSuccess}, failure=${result.isFailure}")
        if (result.isFailure) {
            player.sendMessage("§c[DEBUG] Error: ${result.exceptionOrNull()?.message}")
        }
    }

    override fun remove(player: Player, effect: ActiveEffect) {
        val game = gameManager.activeGame ?: return
        itemHandler.onExpire(player, game)
    }
}
