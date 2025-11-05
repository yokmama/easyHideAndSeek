package com.hideandseek.spectator

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import java.util.UUID

/**
 * Manages the spectator team for grouping spectator players
 */
class SpectatorTeam(private val scoreboard: Scoreboard) {

    companion object {
        private const val TEAM_NAME = "spectators"
    }

    private val team: Team
    private val members: MutableSet<UUID> = mutableSetOf()

    init {
        // Create or get existing team
        team = scoreboard.getTeam(TEAM_NAME) ?: scoreboard.registerNewTeam(TEAM_NAME).apply {
            // Configure team
            setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM)
            color(NamedTextColor.GRAY)
            prefix(Component.text("[観戦] ", NamedTextColor.GRAY))
        }
    }

    /**
     * Add a player to the spectator team
     *
     * @param player Player to add
     */
    fun addPlayer(player: Player) {
        team.addEntry(player.name)
        members.add(player.uniqueId)
    }

    /**
     * Remove a player from the spectator team
     *
     * @param player Player to remove
     */
    fun removePlayer(player: Player) {
        team.removeEntry(player.name)
        members.remove(player.uniqueId)
    }

    /**
     * Check if a player is in the spectator team
     *
     * @param player Player to check
     * @return true if player is in the team, false otherwise
     */
    fun contains(player: Player): Boolean {
        return members.contains(player.uniqueId)
    }

    /**
     * Get all online members of the spectator team
     *
     * @return List of online spectator players
     */
    fun getMembers(): List<Player> {
        return members.mapNotNull { uuid ->
            Bukkit.getPlayer(uuid)
        }.filter { it.isOnline }
    }

    /**
     * Get the number of spectators
     *
     * @return Number of online spectators
     */
    fun getSize(): Int {
        return getMembers().size
    }

    /**
     * Clear all members from the spectator team
     */
    fun clear() {
        members.toList().forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { removePlayer(it) }
        }
        members.clear()
    }

    /**
     * Unregister the team from the scoreboard
     */
    fun unregister() {
        clear()
        team.unregister()
    }
}
