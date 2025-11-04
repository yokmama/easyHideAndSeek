package com.hideandseek.items

import com.hideandseek.game.Game
import org.bukkit.entity.Player

/**
 * T010: Interface for handling shop item effects
 *
 * Each item type (Shadow Sprint, Decoy Block, etc.) implements this interface
 * to define its specific behavior
 */
interface ItemEffectHandler {
    /**
     * Check if effect can be applied to player in current game state
     *
     * @param player The player attempting to use the item
     * @param game The current game session
     * @return true if effect can be applied, false otherwise
     */
    fun canApply(player: Player, game: Game): Boolean

    /**
     * Apply effect to player
     *
     * @param player The player using the item
     * @param game The current game session
     * @param config Item configuration (duration, intensity, metadata)
     * @return Result.success if applied successfully, Result.failure with error message otherwise
     */
    fun apply(player: Player, game: Game, config: ItemConfig): Result<Unit>

    /**
     * Called when effect expires naturally (duration ends)
     *
     * @param player The player whose effect expired
     * @param game The current game session
     */
    fun onExpire(player: Player, game: Game)

    /**
     * Get tooltip lines for shop GUI
     * Dynamically generated based on item configuration
     *
     * @param config Item configuration
     * @return List of lore lines for display in shop
     */
    fun getDisplayLore(config: ItemConfig): List<String>
}
