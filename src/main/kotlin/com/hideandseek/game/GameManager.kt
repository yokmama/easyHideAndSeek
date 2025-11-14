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
    var messageManager: com.hideandseek.i18n.MessageManager? = null
    var pointManager: com.hideandseek.points.PointManager? = null
    var arenaManager: com.hideandseek.arena.ArenaManager? = null
    var gameScoreboard: com.hideandseek.scoreboard.GameScoreboard? = null
    var localizedScoreboard: com.hideandseek.scoreboard.LocalizedScoreboardManager? = null
    var seekerStrengthManager: com.hideandseek.strength.SeekerStrengthManager? = null
    var blockRestorationManager: com.hideandseek.blockrestoration.BlockRestorationManager? = null

    fun joinGame(player: Player): Boolean {
        val uuid = player.uniqueId

        // Check if player is spectator
        val spectatorManager = (plugin as? com.hideandseek.HideAndSeekPlugin)?.spectatorManager
        if (spectatorManager?.isSpectator(uuid) == true) {
            messageManager?.send(player, "game.spectator.enabled_warning")
            messageManager?.send(player, "game.spectator.wait_list_notice")
            // Still allow to join waiting list for spectating
        }

        // Check if already in waiting list
        if (waitingPlayers.contains(uuid)) {
            messageManager?.send(player, "player.already_in_game")
            return false
        }

        val game = activeGame

        // If game is in progress, join as Hider (mid-game join or rejoin)
        if (game != null) {
            // Check if this is a rejoin (player was in game before)
            if (game.players.containsKey(uuid)) {
                val playerData = game.players[uuid]!!

                // Restore player to game
                val roleName = if (playerData.role == PlayerRole.SEEKER) {
                    messageManager?.getMessage(player, "ui.scoreboard.role.seeker") ?: "&cSeeker"
                } else {
                    messageManager?.getMessage(player, "ui.scoreboard.role.hider") ?: "&aHider"
                }
                messageManager?.send(player, "player.join.rejoin", roleName)

                // Teleport back to random spawn
                val spawn = getRandomSpawnLocation(game.arena)
                player.teleport(spawn)

                // Restore scoreboard
                localizedScoreboard?.addPlayer(player, game)

                // Restore inventory (shop item)
                player.inventory.clear()
                giveShopItemToPlayer(player)

                // Show role title
                when (playerData.role) {
                    PlayerRole.SEEKER -> {
                        messageManager?.sendTitle(
                            player,
                            "game.role.title.seeker",
                            "game.role.subtitle.rejoined"
                        )
                    }
                    PlayerRole.HIDER -> {
                        if (playerData.isCaptured) {
                            messageManager?.sendTitle(
                                player,
                                "game.role.title.spectator",
                                "game.role.subtitle.spectator"
                            )
                            player.gameMode = GameMode.SPECTATOR
                        } else {
                            messageManager?.sendTitle(
                                player,
                                "game.role.title.hider",
                                "game.role.subtitle.rejoined"
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

                // Add to scoreboard
                localizedScoreboard?.addPlayer(player, game)

                messageManager?.send(player, "player.join.mid_game")
                messageManager?.broadcast(game.players.keys.mapNotNull { Bukkit.getPlayer(it) }, "player.join.mid_game_broadcast", player.name)

                // Show title
                messageManager?.sendTitle(player, "game.role.title.hider", "game.role.subtitle.hider_midjoin")

                return true
            }
        }

        // No active game, join waiting list
        waitingPlayers.add(uuid)
        val count = waitingPlayers.size
        messageManager?.send(player, "player.join.waiting", count.toString())

        messageManager?.broadcast(
            waitingPlayers.mapNotNull { Bukkit.getPlayer(it) }.filter { it.uniqueId != uuid },
            "player.join.waiting_broadcast",
            player.name,
            count.toString()
        )

        // Check if max players reached (auto-start)
        checkAutoStart()

        return true
    }

    fun leaveGame(player: Player): Boolean {
        val uuid = player.uniqueId

        if (waitingPlayers.remove(uuid)) {
            messageManager?.send(player, "player.leave")
            messageManager?.broadcast(
                waitingPlayers.mapNotNull { Bukkit.getPlayer(it) },
                "player.leave.broadcast",
                player.name,
                waitingPlayers.size.toString()
            )
            return true
        }

        val game = activeGame
        if (game != null && game.players.containsKey(uuid)) {
            val playerData = game.players[uuid]!!

            // Restore player state but keep them in game data (for rejoin)
            playerData.backup?.restore(player)

            messageManager?.send(player, "player.leave")
            messageManager?.send(player, "player.leave.can_rejoin")

            // Note: We do NOT remove from game.players - this allows rejoin
            return true
        }

        messageManager?.send(player, "error.not_in_game")
        return false
    }

    fun startGame(arena: Arena): Game? {
        if (activeGame != null) {
            return null
        }

        // Filter out spectators from waiting players
        val spectatorManager = (plugin as? com.hideandseek.HideAndSeekPlugin)?.spectatorManager

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

        // Apply spectator mode to spectators who are in waiting list
        spectatorPlayers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                messageManager?.send(player, "game.spectator.start_notice")
                messageManager?.send(player, "game.spectator.start_how_to_join")

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
                messageManager?.broadcast(
                    waitingPlayers.mapNotNull { Bukkit.getPlayer(it) },
                    "error.min_players_spectators_only"
                )
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
        clearPlayerInventories(game)
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

        // Select exactly 1 random player as seeker
        val seekerUuid = playerList.random()
        game.players[seekerUuid]?.role = PlayerRole.SEEKER
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

        // Assign players to teams
        game.players.forEach { (uuid, playerData) ->
            Bukkit.getPlayer(uuid)?.let { player ->
                // Make sure player is using main scoreboard
                if (player.scoreboard != mainScoreboard) {
                    player.scoreboard = mainScoreboard
                }

                val playerName = player.name
                when (playerData.role) {
                    PlayerRole.SEEKER -> seekerTeam.addEntry(playerName)
                    PlayerRole.HIDER -> hiderTeam.addEntry(playerName)
                    PlayerRole.SPECTATOR -> spectatorTeam.addEntry(playerName)
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
            }
        }
    }

    /**
     * Clean up teams after game ends
     * Removes all players from teams and unregisters the teams
     */
    private fun cleanupTeams(game: Game) {
        val mainScoreboard = Bukkit.getScoreboardManager().mainScoreboard

        // Get all team names
        val teamNames = listOf("hs_seekers", "hs_hiders", "hs_spectators")

        teamNames.forEach { teamName ->
            mainScoreboard.getTeam(teamName)?.let { team ->
                // Remove all entries from team
                team.entries.toList().forEach { entry ->
                    team.removeEntry(entry)
                }

                // Unregister team
                team.unregister()
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

                    // Verify the spawn location one more time before returning
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
     * Returns Y coordinate where player should spawn (feet position), or null if not safe
     */
    private fun findSafeGroundY(world: org.bukkit.World, x: Int, z: Int): Int? {
        // Use Minecraft's built-in highest block detection
        // This gives us the topmost solid block, which is what we want for surface spawning
        val highestY = world.getHighestBlockYAt(x, z)

        // Check a range around the highest block to ensure it's truly safe
        // Sometimes getHighestBlockYAt can return leaves or other non-solid blocks
        for (yOffset in 0..5) {
            val y = highestY - yOffset
            if (y < world.minHeight) continue

            val groundBlock = world.getBlockAt(x, y, z)
            val feetBlock = world.getBlockAt(x, y + 1, z)
            val headBlock = world.getBlockAt(x, y + 2, z)

            // Check if this is a safe spawn location:
            // 1. Ground must be solid and not dangerous
            // 2. Player's feet and head positions must be passable (air, cave_air, void_air)
            // 3. Ground must not be a problematic block (leaves, glass panes, etc.)
            if (groundBlock.type.isSolid &&
                !isDangerousBlock(groundBlock.type) &&
                isSafeAir(feetBlock.type) &&
                isSafeAir(headBlock.type) &&
                !isUnstableBlock(groundBlock.type)) {
                return y + 1 // Return Y position for player's feet (one block above ground)
            }
        }

        plugin.logger.warning("[Safe Ground] No safe ground found at ($x, $z) near highest block Y=$highestY")
        return null
    }

    /**
     * Check if a block is unstable for spawning (leaves, glass panes, etc.)
     */
    private fun isUnstableBlock(material: org.bukkit.Material): Boolean {
        return when {
            material.name.contains("LEAVES") -> true
            material.name.contains("GLASS_PANE") -> true
            material.name.contains("IRON_BARS") -> true
            material == org.bukkit.Material.SCAFFOLDING -> true
            material == org.bukkit.Material.SNOW -> true
            material == org.bukkit.Material.POWDER_SNOW -> true
            else -> false
        }
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

    /**
     * Clear all player inventories at game start
     */
    private fun clearPlayerInventories(game: Game) {
        game.players.forEach { (uuid, _) ->
            Bukkit.getPlayer(uuid)?.let { player ->
                player.inventory.clear()
                player.inventory.armorContents = arrayOfNulls(4)
                player.inventory.setItemInOffHand(null)
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

                messageManager?.send(captured, "game.spectator.captured_spectator", capturer.name)
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

                // Give shop item
                giveShopItemToPlayer(captured)

                messageManager?.send(captured, "game.spectator.captured_infection", capturer.name)
                messageManager?.send(capturer, "game.capture.success_infection", captured.name)
            }
        }
    }

    /**
     * Revert a seeker back to a hider.
     *
     * This is called when a weaker seeker attacks a stronger seeker.
     * The attacker is forced back to hider role with:
     * - Strength points reset to 0
     * - Economic points restored to hider points
     * - All seeker effects cleared
     * - Respawn to random safe location
     * - Team updated (RED → GREEN)
     *
     * @param seeker The seeker to revert to hider
     * @param seekerData The player game data of the seeker
     * @param game The active game
     */
    fun revertSeekerToHider(seeker: Player, seekerData: PlayerGameData, game: Game) {
        // 1. Role change (SEEKER → HIDER)
        seekerData.role = PlayerRole.HIDER
        seekerData.isCaptured = false

        // 2. Reset strength points
        val previousStrength = seekerStrengthManager?.getStrength(seeker.uniqueId) ?: 0
        seekerStrengthManager?.resetStrength(seeker.uniqueId)

        // 3. Restore economic points (hider points)
        val hiderPoints = seekerData.hiderPoints
        pointManager?.setPoints(seeker.uniqueId, hiderPoints)

        // 4. Clear all seeker effects
        val effectManager = (plugin as? com.hideandseek.HideAndSeekPlugin)?.effectManager
        effectManager?.removeAllEffects(seeker.uniqueId)

        // 5. Team update (RED → GREEN)
        updateTeamForReversion(seeker, game)

        // 6. Respawn to random safe location
        val spawnLocation = getRandomSpawnLocation(game.arena)
        seeker.teleport(spawnLocation)

        // 7. Clear inventory and give shop item
        seeker.inventory.clear()
        giveShopItemToPlayer(seeker)

        // 8. Notifications and feedback
        messageManager?.send(seeker, "game.seeker.reverted")
        messageManager?.broadcast(
            game.players.keys.mapNotNull { Bukkit.getPlayer(it) }.filter { it.uniqueId != seeker.uniqueId },
            "game.seeker.pk.broadcast_reversion",
            seeker.name,
            "" // Second parameter is the victim name, but not used in this broadcast
        )

        // Play sound and particles
        seeker.playSound(seeker.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
        seeker.world.spawnParticle(
            org.bukkit.Particle.ENCHANT,
            seeker.location,
            50,
            0.5, 1.0, 0.5
        )
    }

    /**
     * Update team assignment when a seeker reverts to hider.
     *
     * Moves the player from SEEKER team (RED) to HIDER team (GREEN).
     *
     * @param player The player whose team to update
     * @param game The active game
     */
    private fun updateTeamForReversion(player: Player, game: Game) {
        val mainScoreboard = Bukkit.getScoreboardManager().mainScoreboard
        val seekerTeam = mainScoreboard.getTeam("hs_seekers")
        val hiderTeam = mainScoreboard.getTeam("hs_hiders")

        seekerTeam?.removeEntry(player.name)
        hiderTeam?.addEntry(player.name)

        // Update team in all players' individual scoreboards
        game.players.keys.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { otherPlayer ->
                val playerScoreboard = otherPlayer.scoreboard
                if (playerScoreboard != mainScoreboard) {
                    val otherSeekerTeam = playerScoreboard.getTeam("hs_seekers")
                    val otherHiderTeam = playerScoreboard.getTeam("hs_hiders")

                    otherSeekerTeam?.removeEntry(player.name)
                    otherHiderTeam?.addEntry(player.name)
                }
            }
        }
    }

    private fun applyWorldBorder(game: Game) {
        val world = game.arena.world
        game.worldBorderBackup = WorldBorderBackup.capture(world)

        val worldBorder = world.worldBorder
        val boundaries = game.arena.boundaries
        val center = boundaries.center

        // WorldBorder is a SQUARE (not circle!)
        // Use the larger dimension to ensure the entire rectangular area fits
        val size = kotlin.math.max(boundaries.width, boundaries.depth)

        // Set world border to match arena boundaries
        worldBorder.center = center
        worldBorder.size = size
        worldBorder.warningDistance = 5  // Show warning 5 blocks from border
        worldBorder.damageAmount = 0.2   // Damage per second outside border
        worldBorder.damageBuffer = 0.0   // No buffer, immediate damage
    }

    private fun startPreparationPhase(game: Game) {
        game.phase = GamePhase.PREPARATION
        game.phaseStartTime = System.currentTimeMillis()

        // Start localized scoreboard updates
        localizedScoreboard?.startUpdating(game)

        val msgMgr = messageManager
        if (msgMgr != null) {
            // Get all players to broadcast to
            val allPlayers = game.players.keys.mapNotNull { Bukkit.getPlayer(it) }

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

                    msgMgr.send(player, "game.seeker.you_are_seeker")
                    msgMgr.send(player, "game.seeker.cannot_move")

                    // Show SEEKER title
                    msgMgr.sendTitle(
                        player,
                        "game.role.title.seeker",
                        "game.role.subtitle.seeker",
                        java.time.Duration.ofMillis(500),
                        java.time.Duration.ofMillis(3000),
                        java.time.Duration.ofMillis(500)
                    )
                }
            }

            // Show HIDER title to all hiders
            game.getHiders().forEach { uuid ->
                Bukkit.getPlayer(uuid)?.let { player ->
                    msgMgr.sendTitle(
                        player,
                        "game.role.title.hider",
                        "game.role.subtitle.hider",
                        java.time.Duration.ofMillis(500),
                        java.time.Duration.ofMillis(3000),
                        java.time.Duration.ofMillis(500)
                    )
                }
            }

            msgMgr.broadcast(allPlayers, "game.phase.preparation.broadcast")
            msgMgr.broadcast(allPlayers, "game.phase.preparation.duration", configManager.getPreparationTime())
        }

        // Restore seeker's view distance after 15 seconds
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (activeGame == game) {
                val msgMgr = messageManager
                game.getSeekers().forEach { uuid ->
                    Bukkit.getPlayer(uuid)?.let { player ->
                        player.sendViewDistance = 10 // Reset to default
                        msgMgr?.send(player, "game.seeker.can_move")
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

        val msgMgr = messageManager
        val allPlayers = game.players.keys.mapNotNull { Bukkit.getPlayer(it) }

        msgMgr?.broadcast(allPlayers, "game.phase.seeking.broadcast")

        // Apply darkness effect to all seekers (松明より少し先まで見える程度)
        game.getSeekers().forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                // Apply permanent darkness effect (lasts until removed)
                player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.DARKNESS,
                        Int.MAX_VALUE, // Permanent duration
                        0, // Level 0 for moderate darkness
                        false,
                        false,
                        false
                    )
                )
                msgMgr?.send(player, "game.seeker.vision_restricted")
            }
        }

        // Start point accumulation for hiders
        val pointsPerSecond = configManager.config.getDouble("points.points-per-second", 1.0)
        val accumulationInterval = configManager.config.getInt("points.accumulation-interval", 15)
        pointManager?.startPointAccumulation(game, pointsPerSecond, (accumulationInterval * 20).toLong())

        // Start periodic win condition check (every second)
        val seekTime = configManager.getSeekTime()
        val warnedTimes = mutableSetOf<Int>() // Track which warnings have been sent
        val checkTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, {
            if (activeGame == game && game.phase == GamePhase.SEEKING) {
                val elapsed = (System.currentTimeMillis() - game.phaseStartTime) / 1000
                val remaining = seekTime - elapsed.toInt()

                // Send warnings at specific times
                val msgMgr = messageManager
                val allPlayers = game.players.keys.mapNotNull { Bukkit.getPlayer(it) }

                when (remaining) {
                    60 -> {
                        if (!warnedTimes.contains(60)) {
                            msgMgr?.broadcast(allPlayers, "game.time.warning_1min")
                            game.players.keys.forEach { uuid ->
                                Bukkit.getPlayer(uuid)?.playSound(
                                    Bukkit.getPlayer(uuid)!!.location,
                                    org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING,
                                    1.0f, 1.0f
                                )
                            }
                            warnedTimes.add(60)
                        }
                    }
                    30 -> {
                        if (!warnedTimes.contains(30)) {
                            msgMgr?.broadcast(allPlayers, "game.time.warning_30sec")
                            game.players.keys.forEach { uuid ->
                                Bukkit.getPlayer(uuid)?.playSound(
                                    Bukkit.getPlayer(uuid)!!.location,
                                    org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING,
                                    1.0f, 1.2f
                                )
                            }
                            warnedTimes.add(30)
                        }
                    }
                    10 -> {
                        if (!warnedTimes.contains(10)) {
                            msgMgr?.broadcast(allPlayers, "game.time.warning_10sec")
                            game.players.keys.forEach { uuid ->
                                Bukkit.getPlayer(uuid)?.playSound(
                                    Bukkit.getPlayer(uuid)!!.location,
                                    org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING,
                                    1.0f, 1.5f
                                )
                            }
                            warnedTimes.add(10)
                        }
                    }
                }

                // Check win condition
                val result = game.checkWinCondition(seekTime.toLong() * 1000) // Convert to milliseconds
                if (result != null) {
                    endGame(result)
                }
            } else {
                // Game ended or changed, cancel this task
                game.winConditionCheckTaskId?.let { taskId ->
                    Bukkit.getScheduler().cancelTask(taskId)
                    game.winConditionCheckTaskId = null
                }
            }
        }, 20L, 20L) // Check every second (20 ticks)

        game.winConditionCheckTaskId = checkTaskId
    }

    fun endGame(result: GameResult) {
        val game = activeGame ?: return

        // Prevent multiple calls to endGame
        if (game.phase == GamePhase.ENDED || game.phase == GamePhase.POST_GAME) {
            return
        }

        game.phase = GamePhase.ENDED

        // Stop scoreboard updates
        localizedScoreboard?.stopUpdating()

        // Stop point accumulation
        pointManager?.stopPointAccumulation()

        // Cancel win condition check task
        game.winConditionCheckTaskId?.let { taskId ->
            Bukkit.getScheduler().cancelTask(taskId)
            game.winConditionCheckTaskId = null
        }

        // Cancel night skip task
        game.nightSkipTaskId?.let { taskId ->
            Bukkit.getScheduler().cancelTask(taskId)
            game.nightSkipTaskId = null
        }

        // Clear block restoration tracking for this game
        blockRestorationManager?.let { manager ->
            val clearedCount = manager.clearGameBlocks(game.id)
            if (clearedCount > 0) {
                plugin.logger.info("[Game] Cleared $clearedCount tracked blocks for game ${game.id}")
            }
        }

        disguiseManager?.clearAllDisguises()

        val winners = when (result) {
            GameResult.HIDER_WIN -> game.getHiders()
            GameResult.SEEKER_WIN -> game.getSeekers()
            GameResult.CANCELLED -> emptyList()
        }

        val msgMgr = messageManager
        val allPlayers = game.players.keys.mapNotNull { Bukkit.getPlayer(it) }

        // Show game end title to all players
        if (msgMgr != null) {
            game.players.keys.forEach { uuid ->
                Bukkit.getPlayer(uuid)?.let { player ->
                    when (result) {
                        GameResult.CANCELLED -> {
                            msgMgr.sendTitle(
                                player,
                                "game.result.title.cancelled",
                                "game.result.subtitle.cancelled",
                                java.time.Duration.ofMillis(500),
                                java.time.Duration.ofMillis(3000),
                                java.time.Duration.ofMillis(1000)
                            )
                        }
                        GameResult.SEEKER_WIN -> {
                            msgMgr.sendTitle(
                                player,
                                "game.result.title.ended",
                                "game.result.subtitle.all_captured",
                                java.time.Duration.ofMillis(500),
                                java.time.Duration.ofMillis(3000),
                                java.time.Duration.ofMillis(1000)
                            )
                            player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
                        }
                        GameResult.HIDER_WIN -> {
                            msgMgr.sendTitle(
                                player,
                                "game.result.title.ended",
                                "game.result.subtitle.time_up",
                                java.time.Duration.ofMillis(500),
                                java.time.Duration.ofMillis(3000),
                                java.time.Duration.ofMillis(1000)
                            )
                            player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
                        }
                    }
                }
            }

            msgMgr.broadcast(allPlayers, "game.result.broadcast_header")
            val resultKey = when (result) {
                GameResult.HIDER_WIN -> "game.result.message.hiders_win"
                GameResult.SEEKER_WIN -> "game.result.message.seekers_win"
                GameResult.CANCELLED -> "game.result.message.cancelled"
            }
            val resultMessage = msgMgr.getRawMessage(null, resultKey)
            msgMgr.broadcast(allPlayers, "game.result.broadcast_result", resultMessage)
        } else {
            // Fallback
            game.players.keys.forEach { uuid ->
                Bukkit.getPlayer(uuid)?.let { player ->
                    when (result) {
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

            val resultMessageKey = when (result) {
                GameResult.HIDER_WIN -> "game.result.message.hiders_win"
                GameResult.SEEKER_WIN -> "game.result.message.seekers_win"
                GameResult.CANCELLED -> "game.result.message.cancelled"
            }
            msgMgr?.broadcast(allPlayers, "game.result.broadcast_header")
            allPlayers.forEach { player ->
                val resultMessage = msgMgr?.getMessage(player, resultMessageKey) ?: getResultMessage(result)
                msgMgr?.send(player, "game.result.broadcast_result", resultMessage)
            }
        }

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

        msgMgr?.broadcast(allPlayers, "game.result.thanks")
    }

    private fun getResultMessage(result: GameResult): String {
        return when (result) {
            GameResult.HIDER_WIN -> "人間の勝利！"
            GameResult.SEEKER_WIN -> "鬼の勝利！"
            GameResult.CANCELLED -> "ゲーム中止"
        }
    }

    private fun displayStats(game: Game) {
        val msgMgr = messageManager
        val allPlayers = game.players.keys.mapNotNull { Bukkit.getPlayer(it) }

        msgMgr?.broadcast(allPlayers, "game.stats.title")

        val duration = (System.currentTimeMillis() - game.startTime) / 1000
        msgMgr?.broadcast(allPlayers, "game.stats.duration", duration.toString())
        msgMgr?.broadcast(allPlayers, "game.stats.total_captures", game.getCaptured().size.toString(), game.getHiders().size.toString())

        val topSeeker = game.players.values
            .filter { it.role == PlayerRole.SEEKER }
            .maxByOrNull { it.captureCount }

        if (topSeeker != null && topSeeker.captureCount > 0) {
            Bukkit.getPlayer(topSeeker.uuid)?.let { player ->
                msgMgr?.broadcast(allPlayers, "game.stats.top_seeker", player.name, topSeeker.captureCount.toString())
            }
        }
    }

    private fun displayRankings(game: Game) {
        val pointMgr = pointManager ?: return

        val msgMgr = messageManager
        val allPlayers = game.players.keys.mapNotNull { Bukkit.getPlayer(it) }

        msgMgr?.broadcast(allPlayers, "game.rankings.title")
        msgMgr?.broadcast(allPlayers, "game.rankings.subtitle")

        // Human rankings - based on points earned as Human
        msgMgr?.broadcast(allPlayers, "game.rankings.hider_title")
        val hiderRankings = pointMgr.getRankedPlayersByRolePoints(game, PlayerRole.HIDER)

        if (hiderRankings.isEmpty()) {
            allPlayers.forEach { player ->
                msgMgr?.send(player, "game.rankings.no_hiders")
            }
        } else {
            hiderRankings.take(5).forEachIndexed { index, (uuid, points) ->
                val player = Bukkit.getPlayer(uuid)

                val medal = when (index) {
                    0 -> "&6🥇"
                    1 -> "&7🥈"
                    2 -> "&c🥉"
                    else -> "&7${index + 1}."
                }

                if (player != null) {
                    msgMgr?.broadcast(allPlayers, "game.rankings.entry_online", medal, player.name, points.toString())
                } else {
                    msgMgr?.broadcast(allPlayers, "game.rankings.entry_offline", medal, points.toString())
                }
            }
        }

        // Demon rankings - based on points earned as Demon
        msgMgr?.broadcast(allPlayers, "game.rankings.seeker_title")
        val seekerRankings = pointMgr.getRankedPlayersByRolePoints(game, PlayerRole.SEEKER)

        if (seekerRankings.isEmpty()) {
            allPlayers.forEach { player ->
                msgMgr?.send(player, "game.rankings.no_seekers")
            }
        } else {
            seekerRankings.take(5).forEachIndexed { index, (uuid, points) ->
                val player = Bukkit.getPlayer(uuid)

                val medal = when (index) {
                    0 -> "&6🥇"
                    1 -> "&7🥈"
                    2 -> "&c🥉"
                    else -> "&7${index + 1}."
                }

                if (player != null) {
                    msgMgr?.broadcast(allPlayers, "game.rankings.entry_online", medal, player.name, points.toString())
                } else {
                    msgMgr?.broadcast(allPlayers, "game.rankings.entry_offline", medal, points.toString())
                }
            }
        }
    }

    private fun restorePlayers(game: Game) {
        game.players.forEach { (uuid, playerData) ->
            Bukkit.getPlayer(uuid)?.let { player ->
                playerData.backup?.restore(player)

                // Clear player's scoreboard
                localizedScoreboard?.removePlayer(player)
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
     * Auto-starts when max-players is reached
     */
    private fun checkAutoStart() {
        // Don't auto-start if game is already in progress
        if (activeGame != null) {
            return
        }

        val maxPlayers = configManager.getMaxPlayers()
        if (waitingPlayers.size >= maxPlayers) {
            // Select random arena
            val arena = arenaManager?.getRandomArena()
            if (arena == null) {
                broadcastToWaiting("&cNo arenas available. Cannot start game.")
                return
            }

            messageManager?.broadcast(
                waitingPlayers.mapNotNull { Bukkit.getPlayer(it) },
                "game.auto_start.max_players",
                maxPlayers.toString()
            )

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
        val arena = arenaManager?.getRandomArena()
        if (arena != null) {
            startGameCountdown(arena, 3)
        } else {
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
        var remaining = seconds

        val countdownTask = object : Runnable {
            override fun run() {
                if (remaining <= 0) {
                    // Start the game
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
                val msgMgr = messageManager
                val allPlayers = game.players.keys.mapNotNull { Bukkit.getPlayer(it) }
                msgMgr?.broadcast(allPlayers, "game.auto_restart.countdown", remaining.toString())
            }
        }, 0L, 20L) // Run every second (20 ticks)

        game.autoRestartTaskId = countdownTaskId
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
        }
    }

    /**
     * Transition from POST_GAME to WAITING mode
     * Checks player count and starts new game if requirements met
     *
     * @param game The game to transition
     */
    private fun transitionToWaiting(game: Game) {
        // Get previous players before clearing
        val previousPlayers = game.players.keys.toList()

        // Broadcast to players before destroying game
        val msgMgr = messageManager
        val allPlayers = game.players.keys.mapNotNull { Bukkit.getPlayer(it) }
        msgMgr?.broadcast(allPlayers, "game.auto_restart.ended")

        // Get active players (excluding spectators if available)
        val activePlayers = previousPlayers.mapNotNull { Bukkit.getPlayer(it) }
            .filter { it.isOnline }
            .filterNot { player ->
                // Filter out spectators if spectatorManager is available
                val spectatorManager = (plugin as? com.hideandseek.HideAndSeekPlugin)?.spectatorManager
                spectatorManager?.isSpectator(player.uniqueId) ?: false
            }

        val minPlayers = configManager.getMinPlayers()
        val maxPlayers = configManager.getMaxPlayers()

        // IMPORTANT: Clear active game to create a fresh one
        activeGame = null

        // Add available players to waiting list
        waitingPlayers.clear()
        activePlayers.forEach { waitingPlayers.add(it.uniqueId) }

        // Check if we have enough players
        if (activePlayers.size < minPlayers) {
            msgMgr?.broadcast(Bukkit.getOnlinePlayers().toList(), "game.auto_restart.waiting_players", activePlayers.size.toString(), minPlayers.toString())
            msgMgr?.broadcast(Bukkit.getOnlinePlayers().toList(), "game.auto_restart.waiting_message")
        } else if (activePlayers.size >= maxPlayers) {
            // Only auto-start if max players is reached
            msgMgr?.broadcast(Bukkit.getOnlinePlayers().toList(), "game.auto_start.max_players", maxPlayers.toString())
            tryStartGame()
        } else {
            // Between min and max: wait for admin to start or more players to join
            msgMgr?.broadcast(Bukkit.getOnlinePlayers().toList(), "game.auto_restart.ready_to_start", activePlayers.size.toString(), minPlayers.toString(), maxPlayers.toString())
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
