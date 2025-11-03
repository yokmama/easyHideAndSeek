package com.hideandseek.game

import com.hideandseek.arena.Arena
import com.hideandseek.config.ConfigManager
import com.hideandseek.utils.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import kotlin.math.ceil

class GameManager(
    private val plugin: Plugin,
    private val configManager: ConfigManager
) {
    var activeGame: Game? = null
        private set

    private val waitingPlayers = mutableListOf<UUID>()

    var shopManager: com.hideandseek.shop.ShopManager? = null
    var disguiseManager: com.hideandseek.disguise.DisguiseManager? = null
    var scoreboardManager: com.hideandseek.scoreboard.ScoreboardManager? = null

    fun joinGame(player: Player): Boolean {
        if (waitingPlayers.contains(player.uniqueId)) {
            MessageUtil.send(player, "&cAlready in game")
            return false
        }

        if (activeGame != null) {
            MessageUtil.send(player, "&cGame in progress. Wait for next game.")
            return false
        }

        waitingPlayers.add(player.uniqueId)
        val count = waitingPlayers.size
        MessageUtil.send(player, "&aJoined game! ($count players)")

        broadcastToWaiting("&e${player.name} joined the game ($count players)")

        // Start lobby scoreboard updates
        val minPlayers = configManager.getMinPlayers()
        scoreboardManager?.startLobbyUpdating(waitingPlayers.toList(), minPlayers)

        return true
    }

    fun leaveGame(player: Player): Boolean {
        val uuid = player.uniqueId

        if (waitingPlayers.remove(uuid)) {
            scoreboardManager?.clearScoreboard(player)
            MessageUtil.send(player, "&aLeft the game")
            broadcastToWaiting("&e${player.name} left the game (${waitingPlayers.size} players)")

            // Restart lobby scoreboard if still players waiting
            if (waitingPlayers.isNotEmpty()) {
                val minPlayers = configManager.getMinPlayers()
                scoreboardManager?.startLobbyUpdating(waitingPlayers.toList(), minPlayers)
            } else {
                scoreboardManager?.stopLobbyUpdating()
            }

            return true
        }

        val game = activeGame
        if (game != null && game.players.containsKey(uuid)) {
            val playerData = game.players[uuid]!!

            if (playerData.role == PlayerRole.HIDER && !playerData.isCaptured) {
                playerData.capture()
            }

            playerData.backup?.restore(player)
            game.players.remove(uuid)

            MessageUtil.send(player, "&aLeft the game")
            return true
        }

        MessageUtil.send(player, "&cNot in game")
        return false
    }

    fun startGame(arena: Arena): Game? {
        if (activeGame != null) {
            return null
        }

        val minPlayers = configManager.getMinPlayers()
        if (waitingPlayers.size < minPlayers) {
            return null
        }

        val players = waitingPlayers.map { uuid ->
            uuid to PlayerGameData(uuid, PlayerRole.HIDER)
        }.toMap().toMutableMap()

        val game = Game(
            arena = arena,
            phase = GamePhase.WAITING,
            players = players,
            startTime = System.currentTimeMillis(),
            phaseStartTime = System.currentTimeMillis()
        )

        activeGame = game
        waitingPlayers.clear()

        assignRoles(game)
        backupPlayers(game)
        teleportPlayers(game)
        giveShopItems(game)
        applyWorldBorder(game)

        // Stop lobby updates and start game updates
        scoreboardManager?.stopLobbyUpdating()
        scoreboardManager?.startUpdating(game)

        startPreparationPhase(game)

        return game
    }

    fun assignRoles(game: Game) {
        val playerList = game.players.keys.toList()
        val seekerRatio = configManager.getSeekerRatio()
        val seekerCount = ceil(playerList.size * seekerRatio).toInt().coerceAtLeast(1)

        val shuffled = playerList.shuffled()
        shuffled.take(seekerCount).forEach { uuid ->
            game.players[uuid]?.role = PlayerRole.SEEKER
        }
    }

    private fun backupPlayers(game: Game) {
        game.players.forEach { (uuid, playerData) ->
            Bukkit.getPlayer(uuid)?.let { player ->
                playerData.backup = PlayerBackup.create(player)
                player.inventory.clear()
            }
        }
    }

    private fun teleportPlayers(game: Game) {
        game.players.forEach { (uuid, playerData) ->
            Bukkit.getPlayer(uuid)?.let { player ->
                val spawn = when (playerData.role) {
                    PlayerRole.SEEKER -> game.arena.spawns.seeker
                    PlayerRole.HIDER -> game.arena.spawns.hider
                    else -> game.arena.spawns.hider
                }
                player.teleport(spawn)
            }
        }
    }

    private fun giveShopItems(game: Game) {
        val shopMgr = shopManager ?: return
        val shopConfig = configManager.shop.getConfigurationSection("shop")
        if (shopConfig == null) return

        val config = com.hideandseek.config.ShopConfig(shopConfig)
        val slot = config.getShopItemSlot()

        game.players.forEach { (uuid, _) ->
            Bukkit.getPlayer(uuid)?.let { player ->
                val shopItem = shopMgr.createShopItem()
                player.inventory.setItem(slot, shopItem)
            }
        }
    }

    private fun applyWorldBorder(game: Game) {
        val world = game.arena.world
        game.worldBorderBackup = WorldBorderBackup.capture(world)

        val border = world.worldBorder
        border.center = game.arena.boundaries.center
        border.size = game.arena.boundaries.size
    }

    private fun startPreparationPhase(game: Game) {
        game.phase = GamePhase.PREPARATION
        game.phaseStartTime = System.currentTimeMillis()

        game.getSeekers().forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.BLINDNESS,
                        configManager.getPreparationTime() * 20,
                        1,
                        false,
                        false
                    )
                )
            }
        }

        broadcastToGame(game, "&e===[ Game Started! ]===")
        broadcastToGame(game, "&7Preparation phase: ${configManager.getPreparationTime()} seconds")

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (activeGame == game) {
                startSeekPhase(game)
            }
        }, (configManager.getPreparationTime() * 20).toLong())
    }

    private fun startSeekPhase(game: Game) {
        game.phase = GamePhase.SEEKING
        game.phaseStartTime = System.currentTimeMillis()

        broadcastToGame(game, "&e===[ Seeking Phase Started! ]===")

        val seekTime = configManager.getSeekTime()
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (activeGame == game && game.phase == GamePhase.SEEKING) {
                val result = game.checkWinCondition(seekTime.toLong())
                if (result != null) {
                    endGame(result)
                }
            }
        }, (seekTime * 20).toLong())
    }

    fun endGame(result: GameResult) {
        val game = activeGame ?: return

        game.phase = GamePhase.ENDED

        scoreboardManager?.stopAllUpdating()
        disguiseManager?.clearAllDisguises()

        val winners = when (result) {
            GameResult.HIDER_WIN -> game.getHiders()
            GameResult.SEEKER_WIN -> game.getSeekers()
            GameResult.CANCELLED -> emptyList()
        }

        broadcastToGame(game, "&e===[ Game Over! ]===")
        broadcastToGame(game, "&aResult: ${getResultMessage(result)}")
        broadcastToGame(game, "")

        displayStats(game)

        restorePlayers(game)

        game.worldBorderBackup?.restore(game.arena.world)

        scoreboardManager?.clearAllScoreboards(game)

        activeGame = null

        broadcastToGame(game, "&7Thank you for playing!")
    }

    private fun getResultMessage(result: GameResult): String {
        return when (result) {
            GameResult.HIDER_WIN -> "Hiders Win!"
            GameResult.SEEKER_WIN -> "Seekers Win!"
            GameResult.CANCELLED -> "Game Cancelled"
        }
    }

    private fun displayStats(game: Game) {
        broadcastToGame(game, "&e--- Game Statistics ---")

        val duration = (System.currentTimeMillis() - game.startTime) / 1000
        broadcastToGame(game, "&7Duration: ${duration}s")
        broadcastToGame(game, "&7Total Captures: ${game.getCaptured().size}/${game.getHiders().size}")

        val topSeeker = game.players.values
            .filter { it.role == PlayerRole.SEEKER }
            .maxByOrNull { it.captureCount }

        if (topSeeker != null && topSeeker.captureCount > 0) {
            Bukkit.getPlayer(topSeeker.uuid)?.let { player ->
                broadcastToGame(game, "&7Top Seeker: ${player.name} (${topSeeker.captureCount} captures)")
            }
        }

        broadcastToGame(game, "")
    }

    private fun restorePlayers(game: Game) {
        game.players.forEach { (uuid, playerData) ->
            Bukkit.getPlayer(uuid)?.let { player ->
                playerData.backup?.restore(player)
            }
        }
    }

    private fun broadcastToWaiting(message: String) {
        waitingPlayers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { MessageUtil.send(it, message) }
        }
    }

    private fun broadcastToGame(game: Game, message: String) {
        game.players.keys.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { MessageUtil.send(it, message) }
        }
    }

    fun getWaitingPlayers(): List<UUID> {
        return waitingPlayers.toList()
    }

    fun isInGame(player: Player): Boolean {
        return waitingPlayers.contains(player.uniqueId) || 
               (activeGame?.players?.containsKey(player.uniqueId) == true)
    }
}
