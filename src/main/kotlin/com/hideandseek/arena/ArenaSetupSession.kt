package com.hideandseek.arena

import org.bukkit.Location
import java.util.UUID

/**
 * Temporary state for arena creation
 *
 * Tracks position selection during arena setup via commands
 */
data class ArenaSetupSession(
    val adminUuid: UUID,
    var pos1: Location? = null,
    var pos2: Location? = null,
    var seekerSpawn: Location? = null,
    var hiderSpawn: Location? = null
) {
    /**
     * Check if all positions are set
     *
     * @return True if ready to create arena
     */
    fun isComplete(): Boolean {
        return pos1 != null && pos2 != null && seekerSpawn != null && hiderSpawn != null
    }

    /**
     * Validate arena setup
     *
     * @return List of error messages (empty if valid)
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (pos1 == null) errors.add("pos1 is not set. Use /hs admin setpos1")
        if (pos2 == null) errors.add("pos2 is not set. Use /hs admin setpos2")
        if (seekerSpawn == null) errors.add("seeker spawn is not set. Use /hs admin setspawn seeker")
        if (hiderSpawn == null) errors.add("hider spawn is not set. Use /hs admin setspawn hider")

        // Check same world
        val worlds = listOfNotNull(pos1?.world, pos2?.world, seekerSpawn?.world, hiderSpawn?.world).distinct()
        if (worlds.size > 1) {
            errors.add("All positions must be in the same world (found: ${worlds.joinToString { it.name }})")
        }

        return errors
    }

    /**
     * Create Arena from this session
     *
     * @param name Arena unique name
     * @param displayName Arena display name
     * @return Created Arena
     * @throws IllegalStateException if session is not complete or valid
     */
    fun toArena(name: String, displayName: String): Arena {
        val validationErrors = validate()
        require(validationErrors.isEmpty()) { 
            "Cannot create arena. Missing:\n" + validationErrors.joinToString("\n- ", "- ")
        }

        return Arena(
            name = name,
            displayName = displayName,
            world = pos1!!.world!!,
            boundaries = ArenaBoundaries(pos1!!, pos2!!),
            spawns = ArenaSpawns(seekerSpawn!!, hiderSpawn!!)
        )
    }
}
