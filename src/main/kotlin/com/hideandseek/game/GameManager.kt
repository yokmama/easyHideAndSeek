package com.hideandseek.game

import com.hideandseek.arena.Arena
import com.hideandseek.config.ConfigManager
import com.hideandseek.utils.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scoreboard.Team
import java.util.UUID
import kotlin.math.ceil

class GameManager(
    val plugin: Plugin,
    val configManager: ConfigManager
) {
    var activeGame: Game? = null
        private set

    private val waitingPlayers = mutableListOf<UUID>()

    var shopManager: com.hideandseek.shop.ShopManager? = null
    var disguiseManager: com.hideandseek.disguise.DisguiseManager? = null
    var pointManager: com.hideandseek.points.PointManager? = null
    var arenaManager: com.hideandseek.arena.ArenaManager? = null
    var gameScoreboard: com.hideandseek.scoreboard.GameScoreboard? = null

    fun joinGame(player: Player): Boolean {
        val uuid = player.uniqueId

        // Check if player is spectator
        val spectatorManager = (plugin as? com.hideandseek.HideAndSeekPlugin)?.spectatorManager
        if (spectatorManager?.isSpectator(uuid) == true) {
            MessageUtil.send(player, "&e観戦モードが有効です。ゲームに参加するには &c/hs spectator off &eを実行してください。")
            MessageUtil.send(player, "&7待機リストには表示されますが、ゲーム開始時に自動的に観戦者になります。")
            // Still allow to join waiting list for spectating
        }

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
                MessageUtil.send(player, "&aRejoined game as ${if (playerData.role == PlayerRole.SEEKER) "&c鬼" else "&a人"}")

                // Teleport back to random spawn
                val spawn = getRandomSpawnLocation(game.arena)
                player.teleport(spawn)

                // Restore inventory (shop item)
                player.inventory.clear()
                giveShopItemToPlayer(player)

                // Show role title
                when (playerData.role) {
                    PlayerRole.SEEKER -> {
                        player.showTitle(
                            net.kyori.adventure.title.Title.title(
                                MessageUtil.colorize("&c&l鬼"),
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
                                    MessageUtil.colorize("&7鬼化済み"),
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
                                    MessageUtil.colorize("&a&l人"),
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
                player.teleport(getRandomSpawnLocation(game.arena))
                giveShopItemToPlayer(player)

                MessageUtil.send(player, "&aJoined game as &a人")
                broadcastToGame(game, "&e${player.name} がゲームに参加しました（人）")

                // Show title
                player.showTitle(
                    net.kyori.adventure.title.Title.title(
                        MessageUtil.colorize("&a&l人"),
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

        // Check if we have enough players to auto-start
        checkAutoStart()

        return true
    }

    fun leaveGame(player: Player): Boolean {
        val uuid = player.uniqueId

        if (waitingPlayers.remove(uuid)) {
            MessageUtil.send(player, "&aLeft the game")
            broadcastToWaiting("&e${player.name} left the game (${waitingPlayers.size} players)")
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

        // Filter out spectators from waiting players
        val spectatorManager = (plugin as? com.hideandseek.HideAndSeekPlugin)?.spectatorManager

        plugin.logger.info("[DEBUG] Waiting players: ${waitingPlayers.size}")
        waitingPlayers.forEach { uuid ->
            val playerName = Bukkit.getPlayer(uuid)?.name ?: "Unknown"
            val isSpectator = spectatorManager?.isSpectator(uuid) ?: false
            plugin.logger.info("[DEBUG] - $playerName (isSpectator: $isSpectator)")
        }

        val spectatorPlayers = mutableListOf<UUID>()
        val activePlayers = if (configManager.isForceJoinAllPlayers()) {
            // Force join all non-spectators
            waitingPlayers.filter { uuid ->
                val isSpectator = spectatorManager?.isSpectator(uuid) ?: false
                if (isSpectator) {
                    spectatorPlayers.add(uuid)
                }
                !isSpectator
            }
        } else {
            // Use all waiting players
            waitingPlayers.toList()
        }

        plugin.logger.info("[DEBUG] Active players (after filtering spectators): ${activePlayers.size}")

        // Apply spectator mode to spectators who are in waiting list
        spectatorPlayers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                MessageUtil.send(player, "&e観戦モードのため、ゲームには参加せず観戦します。")
                MessageUtil.send(player, "&7参加するには &c/hs spectator off &7を実行してください。")

                // Apply spectator game mode
                spectatorManager?.applySpectatorMode(player)

                // Teleport to arena center to spectate
                player.teleport(arena.boundaries.center.apply { y = arena.boundaries.center.y + 10 })
            }
        }

        val minPlayers = configManager.getMinPlayers()
        if (activePlayers.size < minPlayers) {
            // Check if there are only spectators
            if (waitingPlayers.isNotEmpty() && activePlayers.isEmpty()) {
                broadcastToWaiting("&c参加プレイヤーが不足しています。観戦者のみではゲームを開始できません。")
            }
            return null
        }

        val players = activePlayers.map { uuid ->
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
        setupWorldTime(game)

        // Setup teams
        setupTeams(game)

        startPreparationPhase(game)

        // Apply spectator mode to spectators
        applySpectatorModeToSpectators(game)

        return game
    }

    /**
     * Apply spectator mode to all spectators at game start
     */
    private fun applySpectatorModeToSpectators(game: Game) {
        val spectatorManager = (plugin as? com.hideandseek.HideAndSeekPlugin)?.spectatorManager ?: return

        // Get all online players who are spectators but not in game
        Bukkit.getOnlinePlayers().forEach { player ->
            if (spectatorManager.isSpectator(player.uniqueId) && !game.players.containsKey(player.uniqueId)) {
                spectatorManager.applySpectatorMode(player)

                // Send message
                MessageUtil.send(player, "&e観戦モードでゲームを観戦しています")
            }
        }
    }

    fun assignRoles(game: Game) {
        val playerList = game.players.keys.toList()

        plugin.logger.info("[DEBUG] Assigning roles for ${playerList.size} players")

        // Select exactly 1 random player as seeker
        val seekerUuid = playerList.random()
        game.players[seekerUuid]?.role = PlayerRole.SEEKER

        // Log role assignments
        game.players.forEach { (uuid, data) ->
            val playerName = Bukkit.getPlayer(uuid)?.name ?: "Unknown"
            plugin.logger.info("[DEBUG] Assigned role - Player: $playerName, Role: ${data.role}")
        }
    }

    /**
     * Setup scoreboard teams for Seekers, Hiders, and Spectators
     * - Seekers: Red color, NameTag always visible
     * - Hiders: Green color, NameTag hidden
     * - Spectators: Gray color, NameTag always visible
     */
    private fun setupTeams(game: Game) {
        val mainScoreboard = Bukkit.getScoreboardManager().mainScoreboard

        // Unregister old teams if they exist
        mainScoreboard.getTeam("hs_seekers")?.unregister()
        mainScoreboard.getTeam("hs_hiders")?.unregister()
        mainScoreboard.getTeam("hs_spectators")?.unregister()

        // Setup teams on main scoreboard
        val seekerTeam = mainScoreboard.registerNewTeam("hs_seekers")
        @Suppress("DEPRECATION")
        seekerTeam.setColor(org.bukkit.ChatColor.RED)
        @Suppress("DEPRECATION")
        seekerTeam.prefix = "§c"
        @Suppress("DEPRECATION")
        seekerTeam.suffix = ""
        seekerTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)

        val hiderTeam = mainScoreboard.registerNewTeam("hs_hiders")
        @Suppress("DEPRECATION")
        hiderTeam.setColor(org.bukkit.ChatColor.GREEN)
        @Suppress("DEPRECATION")
        hiderTeam.prefix = "§a"
        @Suppress("DEPRECATION")
        hiderTeam.suffix = ""
        hiderTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER)

        val spectatorTeam = mainScoreboard.registerNewTeam("hs_spectators")
        @Suppress("DEPRECATION")
        spectatorTeam.setColor(org.bukkit.ChatColor.GRAY)
        @Suppress("DEPRECATION")
        spectatorTeam.prefix = "§7"
        @Suppress("DEPRECATION")
        spectatorTeam.suffix = ""
        spectatorTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)

        plugin.logger.info("[Team Setup] Created teams - 鬼: RED, 人: GREEN, 観戦者: GRAY")

        // Assign players to teams
        game.players.forEach { (uuid, playerData) ->
            Bukkit.getPlayer(uuid)?.let { player ->
                // Make sure player is using main scoreboard
                if (player.scoreboard != mainScoreboard) {
                    plugin.logger.info("[Team] Setting ${player.name} to use main scoreboard")
                    player.scoreboard = mainScoreboard
                }

                val playerName = player.name
                when (playerData.role) {
                    PlayerRole.SEEKER -> {
                        seekerTeam.addEntry(playerName)
                        plugin.logger.info("[Team] Added ${playerName} to 鬼 team (RED)")
                    }
                    PlayerRole.HIDER -> {
                        hiderTeam.addEntry(playerName)
                        plugin.logger.info("[Team] Added ${playerName} to 人 team (GREEN)")
                    }
                    PlayerRole.SPECTATOR -> {
                        spectatorTeam.addEntry(playerName)
                        plugin.logger.info("[Team] Added ${playerName} to SPECTATOR team (GRAY)")
                    }
                }
            }
        }

        // Add spectators who are not in game
        val spectatorManager = (plugin as? com.hideandseek.HideAndSeekPlugin)?.spectatorManager
        Bukkit.getOnlinePlayers().forEach { player ->
            if (spectatorManager?.isSpectator(player.uniqueId) == true && !game.players.containsKey(player.uniqueId)) {
                if (player.scoreboard != mainScoreboard) {
                    player.scoreboard = mainScoreboard
                }
                spectatorTeam.addEntry(player.name)
                plugin.logger.info("[Team] Added ${player.name} (non-game spectator) to SPECTATOR team (GRAY)")
            }
        }
    }

    /**
     * Clean up teams after game ends
     * Removes all players from teams and unregisters the teams
     */
    private fun cleanupTeams(game: Game) {
        val mainScoreboard = Bukkit.getScoreboardManager().mainScoreboard

        plugin.logger.info("[Team Cleanup] Cleaning up teams for game")

        // Get all team names
        val teamNames = listOf("hs_seekers", "hs_hiders", "hs_spectators")

        teamNames.forEach { teamName ->
            mainScoreboard.getTeam(teamName)?.let { team ->
                // Remove all entries from team
                team.entries.toList().forEach { entry ->
                    team.removeEntry(entry)
                    plugin.logger.info("[Team Cleanup] Removed $entry from team $teamName")
                }

                // Unregister team
                team.unregister()
                plugin.logger.info("[Team Cleanup] Unregistered team $teamName")
            }
        }

        // Reset all game players to main scoreboard (in case they have custom ones)
        game.players.keys.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                if (player.scoreboard != mainScoreboard) {
                    player.scoreboard = mainScoreboard
                }
            }
        }

        plugin.logger.info("[Team Cleanup] Team cleanup complete")
    }

    /**
     * Setup world time to day and start night skip task
     */
    private fun setupWorldTime(game: Game) {
        val world = game.arena.world

        // Set time to day (1000 = morning, 6000 = noon)
        world.time = 1000

        // Start night skip task - check every 10 seconds (200 ticks)
        val taskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            // If it's night time (12542-23459), set to day
            // Night starts at 12542 ticks, ends at 23459 ticks
            if (world.time >= 12542 || world.time <= 23459) {
                val isNight = world.time in 12542..23999
                if (isNight) {
                    world.time = 1000 // Set to morning
                }
            }
        }, 200L, 200L).taskId

        // Store task ID for cleanup
        game.nightSkipTaskId = taskId
    }

    private fun backupPlayers(game: Game) {
        game.players.forEach { (uuid, playerData) ->
            Bukkit.getPlayer(uuid)?.let { player ->
                playerData.backup = PlayerBackup.create(player)

                // If player is dead, respawn them first
                if (player.isDead) {
                    player.spigot().respawn()
                }

                player.inventory.clear()
                player.gameMode = GameMode.SURVIVAL

                // Clear all potion effects
                player.activePotionEffects.forEach { effect ->
                    player.removePotionEffect(effect.type)
                }

                // Reset health and hunger
                player.health = player.maxHealth
                player.foodLevel = 20
                player.saturation = 20f
            }
        }
    }

    private fun teleportPlayers(game: Game) {
        val usedLocations = mutableSetOf<Pair<Int, Int>>() // Track used X,Z coordinates
        val minDistance = 5 // Minimum distance between players in blocks

        game.players.forEach { (uuid, _) ->
            Bukkit.getPlayer(uuid)?.let { player ->
                val spawn = getRandomSpawnLocation(game.arena, usedLocations, minDistance)

                // Calculate distance from center for debugging
                val center = game.arena.boundaries.center
                val distance = spawn.distance(center)
                val radius = game.arena.boundaries.size / 2.0

                plugin.logger.info("[Spawn] ${player.name} at (${spawn.blockX}, ${spawn.blockY}, ${spawn.blockZ}), distance from center: ${distance.toInt()}/${radius.toInt()}")

                player.teleport(spawn)
                usedLocations.add(Pair(spawn.blockX, spawn.blockZ))
            }
        }
    }

    /**
     * Get random spawn location within arena boundaries, avoiding already used locations
     *
     * @param arena Arena to spawn in
     * @param usedLocations Set of already used X,Z coordinates
     * @param minDistance Minimum distance from other spawn points
     * @return Random location within arena center area
     */
    fun getRandomSpawnLocation(
        arena: Arena,
        usedLocations: Set<Pair<Int, Int>> = emptySet(),
        minDistance: Int = 5
    ): Location {
        val center = arena.boundaries.center
        val diameter = arena.boundaries.size.toDouble()
        val radius = diameter / 2.0
        // Use 50% of radius (= diameter/4) for spawn area
        val maxOffset = radius * 0.5
        val world = arena.world

        plugin.logger.info("[Spawn] Arena diameter: $diameter, radius: $radius, spawn range: $maxOffset")

        var attempts = 0
        val maxAttempts = 50

        while (attempts < maxAttempts) {
            // Generate random offset from center
            val offsetX = (Math.random() * maxOffset * 2) - maxOffset
            val offsetZ = (Math.random() * maxOffset * 2) - maxOffset

            val spawnX = (center.blockX + offsetX).toInt()
            val spawnZ = (center.blockZ + offsetZ).toInt()

            // Create test location to check distance from center
            val testLocation = Location(world, spawnX.toDouble() + 0.5, center.y, spawnZ.toDouble() + 0.5)
            val distanceFromCenter = testLocation.distance(center)

            // Check if location is within spawn area (50% of radius)
            if (distanceFromCenter > maxOffset) {
                attempts++
                continue
            }

            // Check if location is far enough from all used locations
            val isFarEnough = usedLocations.all { (usedX, usedZ) ->
                val distance = kotlin.math.sqrt(
                    ((spawnX - usedX) * (spawnX - usedX) + (spawnZ - usedZ) * (spawnZ - usedZ)).toDouble()
                )
                distance >= minDistance
            }

            if (isFarEnough) {
                // Find safe Y coordinate (solid ground, not water)
                val safeY = findSafeGroundY(world, spawnX, spawnZ)
                if (safeY != null) {
                    val spawnLocation = Location(world, spawnX.toDouble() + 0.5, safeY.toDouble(), spawnZ.toDouble() + 0.5, 0f, 0f)
                    plugin.logger.info("[Spawn Check] Location at (${spawnX}, ${safeY}, ${spawnZ}), distance from center: ${distanceFromCenter.toInt()}/${radius.toInt()} (${(distanceFromCenter/radius*100).toInt()}%)")
                    return spawnLocation
                }
            }

            attempts++
        }

        // Fallback: spawn at center
        plugin.logger.warning("[Spawn] Could not find safe spawn location after $maxAttempts attempts, using center")
        val safeY = findSafeGroundY(world, center.blockX, center.blockZ)
            ?: (world.getHighestBlockYAt(center.blockX, center.blockZ) + 1)
        return Location(world, center.blockX.toDouble() + 0.5, safeY.toDouble(), center.blockZ.toDouble() + 0.5, 0f, 0f)
    }

    /**
     * Find safe ground Y coordinate at given X,Z position
     * Returns Y coordinate above solid ground, or null if not safe
     */
    private fun findSafeGroundY(world: org.bukkit.World, x: Int, z: Int): Int? {
        // Start from world height and search down for solid ground
        val maxY = world.maxHeight
        val minY = world.minHeight

        for (y in maxY downTo minY) {
            val groundBlock = world.getBlockAt(x, y, z)
            val aboveBlock = world.getBlockAt(x, y + 1, z)
            val aboveBlock2 = world.getBlockAt(x, y + 2, z)

            // Check if this is solid ground with safe blocks above
            if (groundBlock.type.isSolid &&
                !isDangerousBlock(groundBlock.type) &&
                isSafeAir(aboveBlock.type) &&
                isSafeAir(aboveBlock2.type)) {

                plugin.logger.info("[Safe Ground] Found at ($x, ${y + 1}, $z): ground=${groundBlock.type}, above=${aboveBlock.type}")
                return y + 1 // Return position above the solid block
            }
        }

        plugin.logger.warning("[Safe Ground] No safe ground found at ($x, $z)")
        return null
    }

    /**
     * Check if a block type is dangerous to spawn on
     */
    private fun isDangerousBlock(material: org.bukkit.Material): Boolean {
        return when (material) {
            org.bukkit.Material.LAVA,
            org.bukkit.Material.MAGMA_BLOCK,
            org.bukkit.Material.FIRE,
            org.bukkit.Material.CAMPFIRE,
            org.bukkit.Material.SOUL_CAMPFIRE,
            org.bukkit.Material.CACTUS,
            org.bukkit.Material.SWEET_BERRY_BUSH -> true
            else -> false
        }
    }

    /**
     * Check if a block type is safe air (not water or lava)
     */
    private fun isSafeAir(material: org.bukkit.Material): Boolean {
        return when (material) {
            org.bukkit.Material.AIR,
            org.bukkit.Material.CAVE_AIR,
            org.bukkit.Material.VOID_AIR -> true
            else -> false
        }
    }

    /**
     * Check if a block type is passable (air or non-solid)
     * @deprecated Use isSafeAir instead for spawn location checking
     */
    @Deprecated("Use isSafeAir for spawn checking")
    private fun isPassableBlock(material: org.bukkit.Material): Boolean {
        return material.isAir || !material.isSolid
    }

    private fun giveShopItems(game: Game) {
        game.players.forEach { (uuid, _) ->
            Bukkit.getPlayer(uuid)?.let { player ->
                giveShopItemToPlayer(player)
            }
        }
    }

    fun giveShopItemToPlayer(player: Player) {
        val shopMgr = shopManager ?: return
        val shopConfig = configManager.shop.getConfigurationSection("shop")
        if (shopConfig == null) return

        val config = com.hideandseek.config.ShopConfig(shopConfig)
        val slot = config.getShopItemSlot()

        val shopItem = shopMgr.createShopItem()
        player.inventory.setItem(slot, shopItem)
    }

    /**
     * Handle player capture based on configured mode
     *
     * @param capturer The seeker who captured
     * @param captured The hider who was captured
     */
    fun handleCapture(capturer: Player, captured: Player) {
        val game = activeGame ?: return
        val capturedData = game.players[captured.uniqueId] ?: return

        when (configManager.getCaptureMode()) {
            com.hideandseek.config.CaptureMode.SPECTATOR -> {
                // Original mode: become spectator
                capturedData.role = PlayerRole.SPECTATOR
                captured.gameMode = GameMode.SPECTATOR

                // Update team in main scoreboard
                val mainScoreboard = Bukkit.getScoreboardManager().mainScoreboard
                val mainSpectatorTeam = mainScoreboard.getTeam("hs_spectators")
                val mainHiderTeam = mainScoreboard.getTeam("hs_hiders")

                mainHiderTeam?.removeEntry(captured.name)
                mainSpectatorTeam?.addEntry(captured.name)

                plugin.logger.info("[Team] ${captured.name} 鬼化 - moved from 人 to 観戦者 team (main scoreboard)")

                // Update team in all players' individual scoreboards
                game.players.keys.forEach { uuid ->
                    Bukkit.getPlayer(uuid)?.let { player ->
                        val playerScoreboard = player.scoreboard
                        if (playerScoreboard != mainScoreboard) {
                            val spectatorTeam = playerScoreboard.getTeam("hs_spectators")
                            val hiderTeam = playerScoreboard.getTeam("hs_hiders")

                            hiderTeam?.removeEntry(captured.name)
                            spectatorTeam?.addEntry(captured.name)
                        }
                    }
                }

                plugin.logger.info("[Team] ${captured.name} team updated in all individual scoreboards")

                MessageUtil.send(captured, "&cYou were captured by ${capturer.name}! &7観戦モードになりました")
            }
            com.hideandseek.config.CaptureMode.INFECTION -> {
                // Infection mode: become seeker
                capturedData.role = PlayerRole.SEEKER
                capturedData.isCaptured = false  // No longer captured, now a seeker

                // Update team in main scoreboard
                val mainScoreboard = Bukkit.getScoreboardManager().mainScoreboard
                val mainSeekerTeam = mainScoreboard.getTeam("hs_seekers")
                val mainHiderTeam = mainScoreboard.getTeam("hs_hiders")

                mainHiderTeam?.removeEntry(captured.name)
                mainSeekerTeam?.addEntry(captured.name)

                plugin.logger.info("[Team] ${captured.name} 鬼化 - moved from 人 to 鬼 team (main scoreboard)")

                // Update team in all players' individual scoreboards
                game.players.keys.forEach { uuid ->
                    Bukkit.getPlayer(uuid)?.let { player ->
                        val playerScoreboard = player.scoreboard
                        if (playerScoreboard != mainScoreboard) {
                            val seekerTeam = playerScoreboard.getTeam("hs_seekers")
                            val hiderTeam = playerScoreboard.getTeam("hs_hiders")

                            hiderTeam?.removeEntry(captured.name)
                            seekerTeam?.addEntry(captured.name)
                        }
                    }
                }

                plugin.logger.info("[Team] ${captured.name} team updated in all individual scoreboards")

                // Give shop item
                giveShopItemToPlayer(captured)

                MessageUtil.send(captured, "&cYou were captured by ${capturer.name}! &e鬼になりました！")
                MessageUtil.send(capturer, "&a${captured.name} を捕まえました！ &e${captured.name}が鬼になりました！")
            }
        }
    }

    private fun applyWorldBorder(game: Game) {
        val world = game.arena.world
        game.worldBorderBackup = WorldBorderBackup.capture(world)

        val worldBorder = world.worldBorder
        val boundaries = game.arena.boundaries
        val center = boundaries.center
        val diameter = boundaries.size.toDouble()

        // Set world border to match arena boundaries
        worldBorder.center = center
        worldBorder.size = diameter
        worldBorder.warningDistance = 5  // Show warning 5 blocks from border
        worldBorder.damageAmount = 0.2   // Damage per second outside border
        worldBorder.damageBuffer = 0.0   // No buffer, immediate damage

        plugin.logger.info("[WorldBorder] Set border at (${center.blockX}, ${center.blockZ}) with diameter $diameter")
    }

    private fun startPreparationPhase(game: Game) {
        game.phase = GamePhase.PREPARATION
        game.phaseStartTime = System.currentTimeMillis()

        // Start scoreboard updates
        gameScoreboard?.startUpdating(game)

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

                MessageUtil.send(player, "&cあなたは鬼です！")
                MessageUtil.send(player, "&7You cannot move or see for 15 seconds...")

                // Show SEEKER title
                player.showTitle(
                    net.kyori.adventure.title.Title.title(
                        MessageUtil.colorize("&c&l鬼"),
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
                        MessageUtil.colorize("&a&l人"),
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
                val result = game.checkWinCondition(seekTime.toLong() * 1000) // Convert to milliseconds
                if (result != null) {
                    endGame(result)
                }
            }
        }, (seekTime * 20).toLong())
    }

    fun endGame(result: GameResult) {
        val game = activeGame ?: return

        game.phase = GamePhase.ENDED

        // Stop scoreboard updates
        gameScoreboard?.stopUpdating()

        // Stop point accumulation
        pointManager?.stopPointAccumulation()

        // Cancel night skip task
        game.nightSkipTaskId?.let { taskId ->
            Bukkit.getScheduler().cancelTask(taskId)
            game.nightSkipTaskId = null
        }

        disguiseManager?.clearAllDisguises()

        val winners = when (result) {
            GameResult.HIDER_WIN -> game.getHiders()
            GameResult.SEEKER_WIN -> game.getSeekers()
            GameResult.CANCELLED -> emptyList()
        }

        // Show game end title to all players
        game.players.keys.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                val resultTitle = when (result) {
                    GameResult.CANCELLED -> {
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
                    GameResult.SEEKER_WIN -> {
                        player.showTitle(
                            net.kyori.adventure.title.Title.title(
                                MessageUtil.colorize("&e&lゲーム終了"),
                                MessageUtil.colorize("&c全員鬼化！"),
                                net.kyori.adventure.title.Title.Times.times(
                                    java.time.Duration.ofMillis(500),
                                    java.time.Duration.ofMillis(3000),
                                    java.time.Duration.ofMillis(1000)
                                )
                            )
                        )
                        player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
                    }
                    GameResult.HIDER_WIN -> {
                        player.showTitle(
                            net.kyori.adventure.title.Title.title(
                                MessageUtil.colorize("&e&lゲーム終了"),
                                MessageUtil.colorize("&a時間切れ！"),
                                net.kyori.adventure.title.Title.Times.times(
                                    java.time.Duration.ofMillis(500),
                                    java.time.Duration.ofMillis(3000),
                                    java.time.Duration.ofMillis(1000)
                                )
                            )
                        )
                        player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
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

        // Clean up teams before transitioning
        cleanupTeams(game)

        // Check if auto-restart is enabled
        if (configManager.isAutoRestartEnabled() && result != GameResult.CANCELLED) {
            game.phase = GamePhase.POST_GAME
            game.phaseStartTime = System.currentTimeMillis()
            activeGame = game  // Keep game active for POST_GAME phase
            startAutoRestartTimer(game)
        } else {
            activeGame = null
        }

        broadcastToGame(game, "&7Thank you for playing!")
    }

    private fun getResultMessage(result: GameResult): String {
        return when (result) {
            GameResult.HIDER_WIN -> "人間の勝利！"
            GameResult.SEEKER_WIN -> "鬼の勝利！"
            GameResult.CANCELLED -> "ゲーム中止"
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
                broadcastToGame(game, "&7最多鬼化: ${player.name} (${topSeeker.captureCount}人鬼化)")
            }
        }

        broadcastToGame(game, "")
    }

    private fun displayRankings(game: Game) {
        val pointMgr = pointManager ?: return

        // Debug: Log all player points
        plugin.logger.info("[Rankings] === Player Points Debug ===")
        game.players.values.forEach { playerData ->
            val player = Bukkit.getPlayer(playerData.uuid)
            plugin.logger.info("[Rankings] ${player?.name ?: playerData.uuid}: 人=${playerData.hiderPoints}, 鬼=${playerData.seekerPoints}, Total=${pointMgr.getPoints(playerData.uuid)}")
        }

        broadcastToGame(game, "&e--- Individual Rankings ---")
        broadcastToGame(game, "&7(Players can appear in both rankings)")
        broadcastToGame(game, "")

        // Human rankings - based on points earned as Human
        broadcastToGame(game, "&a&l人間ランキング:")
        val hiderRankings = pointMgr.getRankedPlayersByRolePoints(game, PlayerRole.HIDER)

        if (hiderRankings.isEmpty()) {
            broadcastToGame(game, "&7  人間なし")
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

        // Demon rankings - based on points earned as Demon
        broadcastToGame(game, "&c&l鬼ランキング:")
        val seekerRankings = pointMgr.getRankedPlayersByRolePoints(game, PlayerRole.SEEKER)

        if (seekerRankings.isEmpty()) {
            broadcastToGame(game, "&7  鬼なし")
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

                // Clear player's scoreboard
                gameScoreboard?.removePlayer(player)
            }
        }

        // Remove players from teams
        cleanupTeams(game)
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

    /**
     * Public wrapper for broadcasting to active game players
     */
    fun broadcastToActiveGame(message: String) {
        activeGame?.let { game ->
            broadcastToGame(game, message)
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

            // Start countdown with title
            startGameCountdown(arena, 3)
        }
    }

    /**
     * Broadcast message to all online players
     */
    private fun broadcastToServer(message: String) {
        Bukkit.getOnlinePlayers().forEach { player ->
            MessageUtil.send(player, message)
        }
    }

    /**
     * Try to start game with current waiting players
     */
    private fun tryStartGame() {
        plugin.logger.info("[AutoRestart] tryStartGame() called with ${waitingPlayers.size} waiting players")
        val arena = arenaManager?.getRandomArena()
        if (arena != null) {
            plugin.logger.info("[AutoRestart] Arena selected: ${arena.displayName}, starting countdown")
            startGameCountdown(arena, 3)
        } else {
            plugin.logger.warning("[AutoRestart] No arenas available!")
            broadcastToWaiting("&cNo arenas available. Cannot start game.")
        }
    }

    /**
     * Start game countdown with title display
     *
     * @param arena Arena to start game in
     * @param seconds Countdown duration in seconds
     */
    private fun startGameCountdown(arena: Arena, seconds: Int) {
        plugin.logger.info("[AutoRestart] Starting countdown from $seconds seconds")
        var remaining = seconds

        val countdownTask = object : Runnable {
            override fun run() {
                if (remaining <= 0) {
                    // Start the game
                    plugin.logger.info("[AutoRestart] Countdown finished, calling startGame()")
                    startGame(arena)
                    return
                }

                // Show title to all waiting players
                waitingPlayers.forEach { uuid ->
                    Bukkit.getPlayer(uuid)?.let { player ->
                        val color = when (remaining) {
                            1 -> "&c"
                            2 -> "&e"
                            else -> "&a"
                        }

                        player.showTitle(
                            net.kyori.adventure.title.Title.title(
                                MessageUtil.colorize("${color}&l$remaining"),
                                MessageUtil.colorize("&7ゲーム開始まで..."),
                                net.kyori.adventure.title.Title.Times.times(
                                    java.time.Duration.ofMillis(0),
                                    java.time.Duration.ofMillis(1000),
                                    java.time.Duration.ofMillis(250)
                                )
                            )
                        )
                    }
                }

                remaining--
                Bukkit.getScheduler().runTaskLater(plugin, this, 20L)
            }
        }

        // Start the countdown
        Bukkit.getScheduler().runTask(plugin, countdownTask)
    }

    // ========== Auto-Restart Functions ==========

    /**
     * Start auto-restart countdown timer
     *
     * @param game The game to restart
     */
    fun startAutoRestartTimer(game: Game) {
        if (!configManager.isAutoRestartEnabled()) return

        val countdownSeconds = configManager.getPostGameCountdown()
        val broadcastInterval = configManager.getBroadcastInterval()

        game.autoRestartStartTime = System.currentTimeMillis()

        // Schedule repeating task for countdown messages
        val countdownTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, {
            val elapsed = (System.currentTimeMillis() - game.autoRestartStartTime) / 1000
            val remaining = countdownSeconds - elapsed.toInt()

            if (remaining <= 0) {
                // Timer完了 - WAITINGモードに移行
                Bukkit.getScheduler().cancelTask(game.autoRestartTaskId ?: return@scheduleSyncRepeatingTask)
                game.autoRestartTaskId = null
                transitionToWaiting(game)
            } else if (remaining % broadcastInterval == 0 || remaining <= 5) {
                // Broadcast countdown message
                broadcastToGame(game, "&e次のゲームまで &b$remaining &e秒...")
            }
        }, 0L, 20L) // Run every second (20 ticks)

        game.autoRestartTaskId = countdownTaskId

        plugin.logger.info("Auto-restart timer started: ${countdownSeconds}s countdown")
    }

    /**
     * Cancel auto-restart timer
     *
     * @param game The game whose timer to cancel
     */
    fun cancelAutoRestartTimer(game: Game) {
        game.autoRestartTaskId?.let { taskId ->
            Bukkit.getScheduler().cancelTask(taskId)
            game.autoRestartTaskId = null
            plugin.logger.info("Auto-restart timer cancelled")
        }
    }

    /**
     * Transition from POST_GAME to WAITING mode
     * Checks player count and starts new game if requirements met
     *
     * @param game The game to transition
     */
    private fun transitionToWaiting(game: Game) {
        plugin.logger.info("[AutoRestart] Transitioning to WAITING phase")

        // Get previous players before clearing
        val previousPlayers = game.players.keys.toList()

        // Broadcast to players before destroying game
        broadcastToGame(game, "&aゲームが終了しました。新しいゲームの準備中...")

        // Get active players (excluding spectators if available)
        val activePlayers = previousPlayers.mapNotNull { Bukkit.getPlayer(it) }
            .filter { it.isOnline }
            .filterNot { player ->
                // Filter out spectators if spectatorManager is available
                val spectatorManager = (plugin as? com.hideandseek.HideAndSeekPlugin)?.spectatorManager
                spectatorManager?.isSpectator(player.uniqueId) ?: false
            }

        val minPlayers = configManager.getMinPlayers()

        plugin.logger.info("[AutoRestart] Active players: ${activePlayers.size}, Min required: $minPlayers")
        activePlayers.forEach { player ->
            plugin.logger.info("[AutoRestart]   - ${player.name}")
        }

        // IMPORTANT: Clear active game to create a fresh one
        activeGame = null
        plugin.logger.info("[AutoRestart] Cleared activeGame, will create new Game object")

        if (activePlayers.size >= minPlayers) {
            broadcastToServer("&a規定人数に達しました！ゲームを開始します...")
            plugin.logger.info("[AutoRestart] Starting new game with ${activePlayers.size} players")

            // Add players back to waiting list
            waitingPlayers.clear()
            activePlayers.forEach { waitingPlayers.add(it.uniqueId) }

            // Start game with countdown (will create NEW game object)
            tryStartGame()
        } else {
            broadcastToServer("&e参加プレイヤーが不足しています（${activePlayers.size}/${minPlayers}人）")
            broadcastToServer("&e規定人数に達するまで待機します...")
            plugin.logger.info("[AutoRestart] Insufficient players, waiting for more")

            // Add available players to waiting list
            waitingPlayers.clear()
            activePlayers.forEach { waitingPlayers.add(it.uniqueId) }
        }
    }

    /**
     * Get count of non-spectator players
     *
     * @return Number of active players excluding spectators
     */
    fun getActivePlayerCount(): Int {
        val spectatorManager = (plugin as? com.hideandseek.HideAndSeekPlugin)?.spectatorManager
        return waitingPlayers.count { uuid ->
            val isSpectator = spectatorManager?.isSpectator(uuid) ?: false
            !isSpectator
        }
    }
}
