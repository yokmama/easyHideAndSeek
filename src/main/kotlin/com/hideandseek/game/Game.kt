package com.hideandseek.game

import com.hideandseek.arena.Arena
import java.util.UUID

data class Game(
    val arena: Arena,
    var phase: GamePhase,
    val players: MutableMap<UUID, PlayerGameData>,
    val startTime: Long,
    var phaseStartTime: Long,
    var worldBorderBackup: WorldBorderBackup? = null
) {
    fun getSeekers(): List<UUID> {
        return players.filter { it.value.role == PlayerRole.SEEKER }.keys.toList()
    }

    fun getHiders(): List<UUID> {
        return players.filter { it.value.role == PlayerRole.HIDER && !it.value.isCaptured }.keys.toList()
    }

    fun getCaptured(): List<UUID> {
        return players.filter { it.value.role == PlayerRole.HIDER && it.value.isCaptured }.keys.toList()
    }

    fun getElapsedTime(): Long {
        return System.currentTimeMillis() - phaseStartTime
    }

    fun getRemainingTime(maxTime: Long): Long {
        return maxTime - getElapsedTime()
    }

    fun checkWinCondition(seekTime: Long): GameResult? {
        val remainingHiders = getHiders()

        return when {
            remainingHiders.isEmpty() -> GameResult.SEEKER_WIN
            getRemainingTime(seekTime) <= 0 -> GameResult.HIDER_WIN
            else -> null
        }
    }
}
