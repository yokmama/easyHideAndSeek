package com.hideandseek.blockrestoration

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import org.bukkit.Material
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Integration test for block restoration flow
 *
 * Note: Full integration with BlockRestorationManager will be tested
 * after implementation (T013-T021)
 */
class BlockRestorationIntegrationTest {
    private lateinit var server: ServerMock

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `BrokenBlockRecord can be created from block state`() {
        val world = server.addSimpleWorld("world")
        val block = world.getBlockAt(0, 64, 0)
        block.type = Material.STONE

        val location = block.location.clone()
        val blockData = block.blockData.clone()
        val tileEntityData = captureTileEntity(block.state)

        val record = BrokenBlockRecord(
            location = location,
            blockData = blockData,
            tileEntityData = tileEntityData,
            breakTimestamp = System.currentTimeMillis(),
            restoreTimestamp = System.currentTimeMillis() + 5000,
            gameId = "test-game"
        )

        assertNotNull(record)
        assertEquals(Material.STONE, record.blockData.material)
        assertEquals("test-game", record.gameId)
    }

    @Test
    fun `Tile entity data is captured for chest`() {
        val world = server.addSimpleWorld("world")
        val block = world.getBlockAt(0, 64, 0)
        block.type = Material.CHEST

        val tileEntityData = captureTileEntity(block.state)

        assertNotNull(tileEntityData)
        assertEquals(TileEntityType.CONTAINER, tileEntityData.tileEntityType)
    }
}
