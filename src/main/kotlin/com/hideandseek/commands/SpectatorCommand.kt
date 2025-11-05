package com.hideandseek.commands

import com.hideandseek.game.GameManager
import com.hideandseek.game.GamePhase
import com.hideandseek.spectator.SpectatorManager
import com.hideandseek.utils.MessageUtil
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Handles /hs spectator on|off command
 */
class SpectatorCommand(
    private val spectatorManager: SpectatorManager,
    private val gameManager: GameManager
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // Only players can use this command
        if (sender !is Player) {
            sender.sendMessage("This command can only be used by players")
            return true
        }

        // Check permission
        if (!sender.hasPermission("hideandseek.spectate")) {
            MessageUtil.send(sender, "&cこのコマンドを使用する権限がありません")
            return true
        }

        // Validate arguments
        if (args.isEmpty()) {
            MessageUtil.send(sender, "&c使用方法: /hs spectator <on|off>")
            return true
        }

        val mode = args[0].lowercase()
        when (mode) {
            "on" -> handleSpectatorOn(sender)
            "off" -> handleSpectatorOff(sender)
            else -> {
                MessageUtil.send(sender, "&c使用方法: /hs spectator <on|off>")
            }
        }

        return true
    }

    /**
     * Handle spectator mode ON
     */
    private fun handleSpectatorOn(player: Player) {
        val game = gameManager.activeGame
        val currentPhase = game?.phase

        // Check if already spectator
        if (spectatorManager.isSpectator(player.uniqueId)) {
            MessageUtil.send(player, "&e既に観戦モードです")
            return
        }

        // Check game phase
        when (currentPhase) {
            null, GamePhase.WAITING, GamePhase.POST_GAME -> {
                // Pre-game: Enable spectator for next game
                spectatorManager.toggleSpectator(player, true)
                MessageUtil.send(player, "&a観戦モードを有効にしました。次のゲームから観戦者として参加します")
            }
            GamePhase.ENDED -> {
                MessageUtil.send(player, "&cゲームが終了しました。次のゲームをお待ちください")
            }
            GamePhase.PREPARATION, GamePhase.SEEKING -> {
                // Mid-game: Check for admin permission
                if (player.hasPermission("hideandseek.admin") || player.isOp) {
                    // Admin can force spectator mode during game
                    spectatorManager.toggleSpectator(player, true)
                    spectatorManager.applySpectatorMode(player)

                    // Remove from active game if in game
                    val wasHider = game.players[player.uniqueId]?.role == com.hideandseek.game.PlayerRole.HIDER
                    game.players.remove(player.uniqueId)

                    MessageUtil.send(player, "&a観戦モードに切り替えました")
                    gameManager.broadcastToActiveGame("&7${player.name} が観戦モードになりました")

                    // Check win condition if a hider left
                    if (wasHider) {
                        val remainingHiders = game.getHiders()
                        if (remainingHiders.isEmpty()) {
                            gameManager.endGame(com.hideandseek.game.GameResult.SEEKER_WIN)
                        }
                    }
                } else {
                    MessageUtil.send(player, "&cゲーム中は観戦モードに変更できません")
                }
            }
        }
    }

    /**
     * Handle spectator mode OFF
     */
    private fun handleSpectatorOff(player: Player) {
        val game = gameManager.activeGame
        val currentPhase = game?.phase

        // Check if not spectator
        if (!spectatorManager.isSpectator(player.uniqueId)) {
            MessageUtil.send(player, "&e既に通常モードです")
            return
        }

        // Check game phase
        when (currentPhase) {
            null, GamePhase.WAITING, GamePhase.POST_GAME -> {
                // Pre-game: Disable spectator for next game
                spectatorManager.toggleSpectator(player, false)
                MessageUtil.send(player, "&a観戦モードを無効にしました。次のゲームから通常プレイヤーとして参加します")
            }
            GamePhase.ENDED -> {
                spectatorManager.toggleSpectator(player, false)
                MessageUtil.send(player, "&a観戦モードを無効にしました。次のゲームから通常プレイヤーとして参加します")
            }
            GamePhase.PREPARATION, GamePhase.SEEKING -> {
                // Mid-game join as hider
                if (game != null && joinGameAsHider(player, game)) {
                    spectatorManager.toggleSpectator(player, false)
                    spectatorManager.removeSpectatorMode(player)
                    MessageUtil.send(player, "&a逃走者として参加しました")
                } else {
                    MessageUtil.send(player, "&cゲームへの参加に失敗しました")
                }
            }
        }
    }

    /**
     * Join game as hider during SEEKING phase
     *
     * @param player Player to join
     * @param game Active game
     * @return true if successfully joined
     */
    private fun joinGameAsHider(player: Player, game: com.hideandseek.game.Game): Boolean {
        // Check if all hiders are captured
        if (game.getHiders().isEmpty() && game.getCaptured().isNotEmpty()) {
            MessageUtil.send(player, "&c全ての逃走者が捕まっています。次のゲームをお待ちください")
            return false
        }

        // Add to game
        game.players[player.uniqueId] = com.hideandseek.game.PlayerGameData(
            uuid = player.uniqueId,
            role = com.hideandseek.game.PlayerRole.HIDER,
            isCaptured = false
        )

        // Get random spawn location
        val spawnLocation = gameManager.getRandomSpawnLocation(game.arena)

        // Teleport and setup
        player.teleport(spawnLocation)
        player.gameMode = org.bukkit.GameMode.ADVENTURE

        // Give shop item
        gameManager.giveShopItemToPlayer(player)

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (args.size == 1) {
            return listOf("on", "off").filter { it.startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}
