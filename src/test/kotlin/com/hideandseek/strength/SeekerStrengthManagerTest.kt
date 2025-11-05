package com.hideandseek.strength

import be.seeseemelk.mockbukkit.MockBukkit
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class SeekerStrengthManagerTest {

    private lateinit var manager: SeekerStrengthManager

    @BeforeEach
    fun setUp() {
        MockBukkit.mock()
        val plugin = MockBukkit.createMockPlugin()
        manager = SeekerStrengthManager(plugin)
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `getStrength should return 0 for player with no data`() {
        val playerId = UUID.randomUUID()
        assertEquals(0, manager.getStrength(playerId))
    }

    @Test
    fun `addStrength should increase strength points correctly`() {
        val playerId = UUID.randomUUID()

        manager.addStrength(playerId, 200)
        assertEquals(200, manager.getStrength(playerId))

        manager.addStrength(playerId, 200)
        assertEquals(400, manager.getStrength(playerId))
    }

    @Test
    fun `resetStrength should reset strength to zero`() {
        val playerId = UUID.randomUUID()

        manager.addStrength(playerId, 600)
        assertEquals(600, manager.getStrength(playerId))

        manager.resetStrength(playerId)
        assertEquals(0, manager.getStrength(playerId))
    }

    @Test
    fun `compareStrength should return negative when attacker is weaker`() {
        val attackerId = UUID.randomUUID()
        val victimId = UUID.randomUUID()

        manager.addStrength(attackerId, 200) // Attacker: 200
        manager.addStrength(victimId, 400)   // Victim: 400

        val result = manager.compareStrength(attackerId, victimId)
        assertTrue(result < 0, "Attacker (200) should be weaker than victim (400)")
    }

    @Test
    fun `compareStrength should return positive when attacker is stronger`() {
        val attackerId = UUID.randomUUID()
        val victimId = UUID.randomUUID()

        manager.addStrength(attackerId, 600) // Attacker: 600
        manager.addStrength(victimId, 200)   // Victim: 200

        val result = manager.compareStrength(attackerId, victimId)
        assertTrue(result > 0, "Attacker (600) should be stronger than victim (200)")
    }

    @Test
    fun `compareStrength should return zero when strength is equal`() {
        val attackerId = UUID.randomUUID()
        val victimId = UUID.randomUUID()

        manager.addStrength(attackerId, 400) // Attacker: 400
        manager.addStrength(victimId, 400)   // Victim: 400

        val result = manager.compareStrength(attackerId, victimId)
        assertEquals(0, result, "Attacker and victim should have equal strength")
    }

    @Test
    fun `getCaptureCount should track captures correctly`() {
        val playerId = UUID.randomUUID()

        assertEquals(0, manager.getCaptureCount(playerId))

        manager.addStrength(playerId, 200)
        assertEquals(1, manager.getCaptureCount(playerId))

        manager.addStrength(playerId, 200)
        assertEquals(2, manager.getCaptureCount(playerId))
    }

    @Test
    fun `clearAll should remove all strength data`() {
        val player1 = UUID.randomUUID()
        val player2 = UUID.randomUUID()

        manager.addStrength(player1, 200)
        manager.addStrength(player2, 400)

        manager.clearAll()

        assertEquals(0, manager.getStrength(player1))
        assertEquals(0, manager.getStrength(player2))
    }
}
