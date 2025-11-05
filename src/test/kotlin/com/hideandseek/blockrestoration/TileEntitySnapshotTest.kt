package com.hideandseek.blockrestoration

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import be.seeseemelk.mockbukkit.block.state.ChestMock
import be.seeseemelk.mockbukkit.block.state.SignMock
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TileEntitySnapshotTest {
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
    fun `captureTileEntity returns null for non-tile blocks`() {
        val world = server.addSimpleWorld("world")
        val block = world.getBlockAt(0, 64, 0)
        block.type = Material.STONE

        val snapshot = captureTileEntity(block.state)
        assertNull(snapshot, "Stone block should not have tile entity data")
    }

    @Test
    fun `captureTileEntity captures chest inventory`() {
        val world = server.addSimpleWorld("world")
        val block = world.getBlockAt(0, 64, 0)
        block.type = Material.CHEST

        val chest = block.state as ChestMock
        chest.inventory.setItem(0, ItemStack(Material.DIAMOND, 5))
        chest.update()

        val snapshot = captureTileEntity(block.state)
        assertNotNull(snapshot, "Chest should have tile entity snapshot")
        assertEquals(TileEntityType.CONTAINER, snapshot.tileEntityType)
        assertTrue(snapshot.data.containsKey("inventory"))
    }

    @Test
    fun `captureTileEntity captures sign text`() {
        val world = server.addSimpleWorld("world")
        val block = world.getBlockAt(0, 64, 0)
        block.type = Material.OAK_SIGN

        val sign = block.state as SignMock
        sign.update()

        val snapshot = captureTileEntity(block.state)
        assertNotNull(snapshot, "Sign should have tile entity snapshot")
        assertEquals(TileEntityType.SIGN, snapshot.tileEntityType)
        assertTrue(snapshot.data.containsKey("front_lines"))
        assertTrue(snapshot.data.containsKey("back_lines"))
    }
}
