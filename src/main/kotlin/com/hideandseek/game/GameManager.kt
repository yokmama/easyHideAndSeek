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
    var pointManager: com.hideandseek.points.PointManager? = null
    var arenaManager: com.hideandseek.arena.ArenaManager? = null

    fun joinGame(player: Player): Boolean {
        val uuid = player.uniqueId

        // Check if already in waiting list
        if (waitingPlayers.contains(uuid)) {
            MessageUtil.send(player, "&cAlready in game")
            return false
        }

        val game = activeGame

        // If game is in progress, join as Hider (mid-game join or rejoin)
        if (game != null) {
            // Check if this is a rejoin (player was in game before)
            if (game.players.containsKey(uuid)) {
                val playerData = game.players[uuid]!!

                // Restore player to game
                MessageUtil.send(player, "&aRejoined game as ${if (playerData.role == PlayerRole.SEEKER) "&cSEEKER" else "&aHIDER"}")

                // Teleport back to appropriate spawn
                val spawn = when (playerData.role) {
                    PlayerRole.SEEKER -> game.arena.spawns.seeker
                    PlayerRole.HIDER -> game.arena.spawns.hider
                    else -> game.arena.spawns.hider
                }
                player.teleport(spawn)

                // Restore inventory (shop item)
                player.inventory.clear()
                giveShopItemToPlayer(player)

                // Show role title
                when (playerData.role) {
                    PlayerRole.SEEKER -> {
                        player.showTitle(
                            net.kyori.adventure.title.Title.title(
                                MessageUtil.colorize("&c&l鬼 (SEEKER)"),
                                MessageUtil.colorize("&7再参加しました"),
                                net.kyori.adventure.title.Title.Times.times(
                                    java.time.Duration.ofMillis(500),
                                    java.time.Duration.ofMillis(2000),
                                    java.time.Duration.ofMillis(500)
                                )
                            )
                        )
                    }
                    PlayerRole.HIDER -> {
                        if (playerData.isCaptured) {
                            player.showTitle(
                                net.kyori.adventure.title.Title.title(
                                    MessageUtil.colorize("&7&l観戦モード"),
                                    MessageUtil.colorize("&7捕獲済み"),
                                    net.kyori.adventure.title.Title.Times.times(
                                        java.time.Duration.ofMillis(500),
                                        java.time.Duration.ofMillis(2000),
                                        java.time.Duration.ofMillis(500)
                                    )
                                )
                            )
                            player.gameMode = GameMode.SPECTATOR
                        } else {
                            player.showTitle(
                                net.kyori.adventure.title.Title.title(
                                    MessageUtil.colorize("&a&l隠れる側 (HIDER)"),
                                    MessageUtil.colorize("&7再参加しました"),
                                    net.kyori.adventure.title.Title.Times.times(
                                        java.time.Duration.ofMillis(500),
                                        java.time.Duration.ofMillis(2000),
                                        java.time.Duration.ofMillis(500)
                                    )
                                )
                            )
                        }
                    }
                    else -> {}
                }

                return true
            } else {
                // New player joining mid-game as Hider
                val playerData = PlayerGameData(uuid, PlayerRole.HIDER)
                playerData.backup = PlayerBackup.create(player)
                game.players[uuid] = playerData

                player.inventory.clear()
                player.teleport(game.arena.spawns.hider)
                giveShopItemToPlayer(player)

                MessageUtil.send(player, "&aJoined game as &aHIDER")
                broadcastToGame(game, "&e${player.name} joined the game as Hider!")

                // Show title
                player.showTitle(
                    net.kyori.adventure.title.Title.title(
                        MessageUtil.colorize("&a&l隠れる側 (HIDER)"),
                        MessageUtil.colorize("&7鬼から逃げろ！"),
                        net.kyori.adventure.title.Title.Times.times(
                            java.time.Duration.ofMillis(500),
                            java.time.Duration.ofMillis(2000),
                            java.time.Duration.ofMillis(500)
                        )
                    )
                )

                return true
            }
        }

        // No active game, join waiting list
        waitingPlayers.add(uuid)
        val count = waitingPlayers.size
        MessageUtil.send(player, "&aJoined game! ($count players)")

        broadcastToWaiting("&e${player.name} joined the game ($count players)")

        // Start lobby scoreboard updates
        val minPlayers = configManager.getMinPlayers()
        scoreboardManager?.startLobbyUpdating(waitingPlayers.toList(), minPlayers)

        // Check if we have enough players to auto-start
        checkAutoStart()

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

            // Restore player state but keep them in game data (for rejoin)
            playerData.backup?.restore(player)

            MessageUtil.send(player, "&aLeft the game")
            MessageUtil.send(player, "&7You can rejoin to continue playing")

            // Note: We do NOT remove from game.players - this allows rejoin
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

        // Select exactly 1 random player as seeker
        val seekerUuid = playerList.random()
        game.players[seekerUuid]?.role = PlayerRole.SEEKER
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
        game.players.forEach { (uuid, _) ->
            Bukkit.getPlayer(uuid)?.let { player ->
                giveShopItemToPlayer(player)
            }
        }
    }

    private fun giveShopItemToPlayer(player: Player) {
        val shopMgr = shopManager ?: return
        val shopConfig = configManager.shop.getConfigurationSection("shop")
        if (shopConfig == null) return

        val config = com.hideandseek.config.ShopConfig(shopConfig)
        val slot = config.getShopItemSlot()

        val shopItem = shopMgr.createShopItem()
        player.inventory.setItem(slot, shopItem)
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
                // Add blindness effect for 15 seconds
                player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.BLINDNESS,
                        15 * 20, // 15 seconds
                        1,
                        false,
                        false
                    )
                )

                // Set view distance to 0
                player.sendViewDistance = 0

                // Freeze seeker (maximum slowness and jump disable)
                player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.SLOWNESS,
                        15 * 20, // 15 seconds
                        255, // Maximum slowness
                        false,
                        false
                    )
                )

                player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.MINING_FATIGUE,
                        15 * 20, // 15 seconds
                        255,
                        false,
                        false
                    )
                )

                MessageUtil.send(player, "&cYou are the SEEKER!")
                MessageUtil.send(player, "&7You cannot move or see for 15 seconds...")

                // Show SEEKER title
                player.showTitle(
                    net.kyori.adventure.title.Title.title(
                        MessageUtil.colorize("&c&l鬼 (SEEKER)"),
                        MessageUtil.colorize("&715秒後にゲーム開始！"),
                        net.kyori.adventure.title.Title.Times.times(
                            java.time.Duration.ofMillis(500),
                            java.time.Duration.ofMillis(3000),
                            java.time.Duration.ofMillis(500)
                        )
                    )
                )
            }
        }

        // Show HIDER title to all hiders
        game.getHiders().forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                player.showTitle(
                    net.kyori.adventure.title.Title.title(
                        MessageUtil.colorize("&a&l隠れる側 (HIDER)"),
                        MessageUtil.colorize("&7鬼から逃げろ！"),
                        net.kyori.adventure.title.Title.Times.times(
                            java.time.Duration.ofMillis(500),
                            java.time.Duration.ofMillis(3000),
                            java.time.Duration.ofMillis(500)
                        )
                    )
                )
            }
        }

        broadcastToGame(game, "&e===[ Game Started! ]===")
        broadcastToGame(game, "&7Preparation phase: ${configManager.getPreparationTime()} seconds")

        // Restore seeker's view distance after 15 seconds
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (activeGame == game) {
                game.getSeekers().forEach { uuid ->
                    Bukkit.getPlayer(uuid)?.let { player ->
                        player.sendViewDistance = 10 // Reset to default
                        MessageUtil.send(player, "&aYou can now see and move!")
                    }
                }
            }
        }, 15 * 20L) // 15 seconds

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

        // Start point accumulation for hiders
        val pointsPerSecond = configManager.config.getDouble("points.points-per-second", 1.0)
        val accumulationInterval = configManager.config.getInt("points.accumulation-interval", 15)
        pointManager?.startPointAccumulation(game, pointsPerSecond, (accumulationInterval * 20).toLong())

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

        // Stop point accumulation
        pointManager?.stopPointAccumulation()

        scoreboardManager?.stopAllUpdating()
        disguiseManager?.clearAllDisguises()

        val winners = when (result) {
            GameResult.HIDER_WIN -> game.getHiders()
            GameResult.SEEKER_WIN -> game.getSeekers()
            GameResult.CANCELLED -> emptyList()
        }

        // Show result title to all players
        game.players.keys.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                val playerData = game.players[uuid]!!
                val isWinner = winners.contains(uuid)

                when {
                    result == GameResult.CANCELLED -> {
                        player.showTitle(
                            net.kyori.adventure.title.Title.title(
                                MessageUtil.colorize("&7&lゲーム中止"),
                                MessageUtil.colorize("&7Game Cancelled"),
                                net.kyori.adventure.title.Title.Times.times(
                                    java.time.Duration.ofMillis(500),
                                    java.time.Duration.ofMillis(3000),
                                    java.time.Duration.ofMillis(1000)
                                )
                            )
                        )
                    }
                    isWinner -> {
                        player.showTitle(
                            net.kyori.adventure.title.Title.title(
                                MessageUtil.colorize("&6&l★ 勝利！ ★"),
                                MessageUtil.colorize("&e${if (playerData.role == PlayerRole.SEEKER) "鬼チーム" else "ハイダーチーム"}の勝利！"),
                                net.kyori.adventure.title.Title.Times.times(
                                    java.time.Duration.ofMillis(500),
                                    java.time.Duration.ofMillis(4000),
                                    java.time.Duration.ofMillis(1000)
                                )
                            )
                        )
                        player.playSound(player.location, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
                    }
                    else -> {
                        player.showTitle(
                            net.kyori.adventure.title.Title.title(
                                MessageUtil.colorize("&c&l敗北..."),
                                MessageUtil.colorize("&7次のゲームで頑張ろう！"),
                                net.kyori.adventure.title.Title.Times.times(
                                    java.time.Duration.ofMillis(500),
                                    java.time.Duration.ofMillis(3000),
                                    java.time.Duration.ofMillis(1000)
                                )
                            )
                        )
                        player.playSound(player.location, org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
                    }
                }
            }
        }

        broadcastToGame(game, "&e===[ Game Over! ]===")
        broadcastToGame(game, "&aResult: ${getResultMessage(result)}")
        broadcastToGame(game, "")

        displayStats(game)
        displayRankings(game)

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

    private fun displayRankings(game: Game) {
        val pointMgr = pointManager ?: return

        broadcastToGame(game, "&e--- Individual Rankings ---")
        broadcastToGame(game, "")

        // Hider rankings
        broadcastToGame(game, "&a&lHider Rankings:")
        val hiderRankings = pointMgr.getRankedPlayersByRole(game, PlayerRole.HIDER)

        if (hiderRankings.isEmpty()) {
            broadcastToGame(game, "&7  No hiders")
        } else {
            hiderRankings.take(5).forEachIndexed { index, (uuid, points) ->
                val player = Bukkit.getPlayer(uuid)

                if (player != null) {
                    val medal = when (index) {
                        0 -> "&6🥇"
                        1 -> "&7🥈"
                        2 -> "&c🥉"
                        else -> "&7${index + 1}."
                    }
                    broadcastToGame(game, "  $medal &f${player.name}: &e${points} points")
                } else {
                    // Show offline players too
                    val medal = when (index) {
                        0 -> "&6🥇"
                        1 -> "&7🥈"
                        2 -> "&c🥉"
                        else -> "&7${index + 1}."
                    }
                    broadcastToGame(game, "  $medal &7[Offline]: &e${points} points")
                }
            }
        }

        broadcastToGame(game, "")

        // Seeker rankings
        broadcastToGame(game, "&c&lSeeker Rankings:")
        val seekerRankings = pointMgr.getRankedPlayersByRole(game, PlayerRole.SEEKER)

        if (seekerRankings.isEmpty()) {
            broadcastToGame(game, "&7  No seekers")
        } else {
            seekerRankings.take(5).forEachIndexed { index, (uuid, points) ->
                val player = Bukkit.getPlayer(uuid)

                if (player != null) {
                    val medal = when (index) {
                        0 -> "&6🥇"
                        1 -> "&7🥈"
                        2 -> "&c🥉"
                        else -> "&7${index + 1}."
                    }
                    broadcastToGame(game, "  $medal &f${player.name}: &e${points} points")
                } else {
                    // Show offline players too
                    val medal = when (index) {
                        0 -> "&6🥇"
                        1 -> "&7🥈"
                        2 -> "&c🥉"
                        else -> "&7${index + 1}."
                    }
                    broadcastToGame(game, "  $medal &7[Offline]: &e${points} points")
                }
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

    /**
     * Check if auto-start conditions are met and start game if so
     */
    private fun checkAutoStart() {
        // Don't auto-start if game is already in progress
        if (activeGame != null) {
            return
        }

        val minPlayers = configManager.getMinPlayers()
        if (waitingPlayers.size >= minPlayers) {
            // Select random arena
            val arena = arenaManager?.getRandomArena()
            if (arena == null) {
                broadcastToWaiting("&cNo arenas available. Cannot start game.")
                return
            }

            broadcastToWaiting("&aStarting game on arena: &e${arena.displayName}")

            // Start countdown
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                startGame(arena)
            }, 60L) // 3 second countdown
        }
    }
}
