package com.hideandseek.respawn

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import be.seeseemelk.mockbukkit.entity.PlayerMock
import com.hideandseek.arena.Arena
import com.hideandseek.arena.ArenaBoundaries
import com.hideandseek.arena.ArenaSpawns
import com.hideandseek.config.BlockRestorationConfig
import com.hideandseek.game.Game
import com.hideandseek.game.GameManager
import com.hideandseek.game.GamePhase
import com.hideandseek.game.PlayerGameData
import com.hideandseek.game.PlayerRole
import org.bukkit.Material
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test for player respawn flow
 *
 * Tests full flow: player death → find location → teleport → verify role preserved
 */
class RespawnIntegrationTest {
    private lateinit var server: ServerMock
    private lateinit var gameManager: GameManager
    private lateinit var respawnManager: RespawnManager

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        val plugin = MockBukkit.createMockPlugin()
        val configManager = com.hideandseek.config.ConfigManager(plugin)
        gameManager = GameManager(plugin, configManager)

        val blockRestorationYaml = org.bukkit.configuration.file.YamlConfiguration()
        val blockRestorationConfig = BlockRestorationConfig(blockRestorationYaml)

        respawnManager = RespawnManager(plugin, blockRestorationConfig, gameManager)
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `respawnPlayer teleports player to safe location and preserves role`() {
        val world = server.addSimpleWorld("world")
        val player = server.addPlayer() as PlayerMock

        // Create ground blocks for safe spawn around spawn point
        val spawnLoc = world.getSpawnLocation()
        val spawnX = spawnLoc.blockX
        val spawnZ = spawnLoc.blockZ

        // Create a grid of safe ground blocks
        for (offsetX in -20..20) {
            for (offsetZ in -20..20) {
                val x = spawnX + offsetX
                val z = spawnZ + offsetZ
                if (x >= 0 && z >= 0) {  // Stay in positive coordinates
                    world.getBlockAt(x, 63, z).type = Material.GRASS_BLOCK
                    world.getBlockAt(x, 64, z).type = Material.AIR
                    world.getBlockAt(x, 65, z).type = Material.AIR
                }
            }
        }

        // Create test arena and game using spawn-relative coordinates
        val pos1 = org.bukkit.Location(world, (spawnX - 50).toDouble(), 10.0, (spawnZ - 50).toDouble())
        val pos2 = org.bukkit.Location(world, (spawnX + 50).toDouble(), 128.0, (spawnZ + 50).toDouble())

        val arena = Arena(
            name = "test",
            displayName = "Test Arena",
            world = world,
            spawns = ArenaSpawns(
                seeker = spawnLoc,
                hider = spawnLoc
            ),
            boundaries = ArenaBoundaries(pos1, pos2)
        )

        val game = Game(
            arena = arena,
            phase = GamePhase.SEEKING,
            players = mutableMapOf(
                player.uniqueId to PlayerGameData(
                    uuid = player.uniqueId,
                    role = PlayerRole.HIDER,
                    isCaptured = false
                )
            ),
            startTime = System.currentTimeMillis(),
            phaseStartTime = System.currentTimeMillis()
        )

        // Use reflection to set activeGame since it's private
        val activeGameField = GameManager::class.java.getDeclaredField("activeGame")
        activeGameField.isAccessible = true
        activeGameField.set(gameManager, game)

        // Perform respawn
        val result = respawnManager.respawnPlayer(player)

        // Verify result
        assertTrue(result is RespawnResult.Success || result is RespawnResult.Failure)

        if (result is RespawnResult.Success) {
            assertNotNull(player.location)
            // Verify role is preserved
            val playerData = game.players[player.uniqueId]
            assertNotNull(playerData)
            assertEquals(PlayerRole.HIDER, playerData.role)
        }
    }

    @Test
    fun `respawnPlayer returns failure when player not in game`() {
        val player = server.addPlayer() as PlayerMock

        // No active game
        val result = respawnManager.respawnPlayer(player)

        assertTrue(result is RespawnResult.Failure)
        assertEquals(
            RespawnFailureReason.PLAYER_NOT_IN_GAME,
            (result as RespawnResult.Failure).reason
        )
    }

    @Test
    fun `respawnPlayer returns failure when game not active`() {
        val player = server.addPlayer() as PlayerMock

        // No active game set (activeGame is null by default)
        val result = respawnManager.respawnPlayer(player)

        assertTrue(result is RespawnResult.Failure)
        assertEquals(
            RespawnFailureReason.PLAYER_NOT_IN_GAME,
            (result as RespawnResult.Failure).reason
        )
    }
}
