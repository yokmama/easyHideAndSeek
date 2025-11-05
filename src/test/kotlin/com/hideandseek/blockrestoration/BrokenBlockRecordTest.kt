package com.hideandseek.blockrestoration

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import org.bukkit.Material
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrokenBlockRecordTest {
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
    fun `isReadyForRestoration returns false before restore time`() {
        val world = server.addSimpleWorld("world")
        val location = world.getBlockAt(0, 64, 0).location
        val blockData = server.createBlockData(Material.STONE)

        val record = BrokenBlockRecord(
            location = location,
            blockData = blockData,
            tileEntityData = null,
            breakTimestamp = System.currentTimeMillis(),
            restoreTimestamp = System.currentTimeMillis() + 5000,  // 5 seconds in future
            gameId = "test-game"
        )

        assertFalse(record.isReadyForRestoration(System.currentTimeMillis()))
    }

    @Test
    fun `isReadyForRestoration returns true after restore time`() {
        val world = server.addSimpleWorld("world")
        val location = world.getBlockAt(0, 64, 0).location
        val blockData = server.createBlockData(Material.STONE)

        val record = BrokenBlockRecord(
            location = location,
            blockData = blockData,
            tileEntityData = null,
            breakTimestamp = System.currentTimeMillis() - 10000,  // 10 seconds ago
            restoreTimestamp = System.currentTimeMillis() - 5000,  // 5 seconds ago
            gameId = "test-game"
        )

        assertTrue(record.isReadyForRestoration(System.currentTimeMillis()))
    }

    @Test
    fun `getRemainingTime returns 0 when ready`() {
        val world = server.addSimpleWorld("world")
        val location = world.getBlockAt(0, 64, 0).location
        val blockData = server.createBlockData(Material.STONE)

        val record = BrokenBlockRecord(
            location = location,
            blockData = blockData,
            tileEntityData = null,
            breakTimestamp = System.currentTimeMillis() - 10000,
            restoreTimestamp = System.currentTimeMillis() - 5000,
            gameId = "test-game"
        )

        val remaining = record.getRemainingTime(System.currentTimeMillis())
        assertTrue(remaining == 0L, "Expected 0, got $remaining")
    }

    @Test
    fun `getRemainingTime returns positive value when not ready`() {
        val world = server.addSimpleWorld("world")
        val location = world.getBlockAt(0, 64, 0).location
        val blockData = server.createBlockData(Material.STONE)

        val currentTime = System.currentTimeMillis()
        val record = BrokenBlockRecord(
            location = location,
            blockData = blockData,
            tileEntityData = null,
            breakTimestamp = currentTime,
            restoreTimestamp = currentTime + 5000,
            gameId = "test-game"
        )

        val remaining = record.getRemainingTime(currentTime)
        assertTrue(remaining > 0, "Expected positive value, got $remaining")
    }
}
