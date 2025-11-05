package com.hideandseek.respawn

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import com.hideandseek.config.BlockRestorationConfig
import com.hideandseek.game.GameManager
import org.bukkit.Material
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafeLocationValidatorTest {
    private lateinit var server: ServerMock
    private lateinit var respawnManager: RespawnManager

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        val plugin = MockBukkit.createMockPlugin()
        val configManager = com.hideandseek.config.ConfigManager(plugin)
        val gameManager = GameManager(plugin, configManager)

        val blockRestorationYaml = org.bukkit.configuration.file.YamlConfiguration()
        val blockRestorationConfig = BlockRestorationConfig(blockRestorationYaml)

        respawnManager = RespawnManager(plugin, blockRestorationConfig, gameManager)
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `isSafeLocation returns true for valid ground block`() {
        val world = server.addSimpleWorld("world")
        val location = world.getBlockAt(0, 64, 0).location

        // Set ground block to solid
        world.getBlockAt(0, 63, 0).type = Material.STONE
        world.getBlockAt(0, 64, 0).type = Material.AIR
        world.getBlockAt(0, 65, 0).type = Material.AIR

        assertTrue(respawnManager.isSafeLocation(location))
    }

    @Test
    fun `isSafeLocation returns false for lava ground`() {
        val world = server.addSimpleWorld("world")
        val location = world.getBlockAt(0, 64, 0).location

        // Set ground block to lava
        world.getBlockAt(0, 63, 0).type = Material.LAVA
        world.getBlockAt(0, 64, 0).type = Material.AIR
        world.getBlockAt(0, 65, 0).type = Material.AIR

        assertFalse(respawnManager.isSafeLocation(location))
    }

    @Test
    fun `isSafeLocation returns false for void (Y less than 0)`() {
        val world = server.addSimpleWorld("world")
        // Create a location manually with Y=-1 since getBlockAt may not support negative Y
        val location = org.bukkit.Location(world, 0.0, -1.0, 0.0)

        assertFalse(respawnManager.isSafeLocation(location))
    }

    @Test
    fun `isSafeLocation returns false for obstructed space above`() {
        val world = server.addSimpleWorld("world")
        val location = world.getBlockAt(0, 64, 0).location

        // Set ground block to solid but block above is obstructed
        world.getBlockAt(0, 63, 0).type = Material.STONE
        world.getBlockAt(0, 64, 0).type = Material.STONE  // Obstructed
        world.getBlockAt(0, 65, 0).type = Material.AIR

        assertFalse(respawnManager.isSafeLocation(location))
    }

    @Test
    fun `isSafeLocation returns false for non-solid ground`() {
        val world = server.addSimpleWorld("world")
        val location = world.getBlockAt(0, 64, 0).location

        // Set ground block to air
        world.getBlockAt(0, 63, 0).type = Material.AIR
        world.getBlockAt(0, 64, 0).type = Material.AIR
        world.getBlockAt(0, 65, 0).type = Material.AIR

        assertFalse(respawnManager.isSafeLocation(location))
    }
}
