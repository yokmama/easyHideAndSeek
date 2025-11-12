package com.hideandseek.listeners

import com.hideandseek.disguise.DisguiseManager
import com.hideandseek.game.GameManager
import com.hideandseek.game.PlayerRole
import com.hideandseek.utils.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

class EntityDamageByEntityListener(
    private val disguiseManager: DisguiseManager,
    private val gameManager: GameManager,
    private val pointManager: com.hideandseek.points.PointManager?,
    private val plugin: org.bukkit.plugin.Plugin
) : Listener {

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return

        // Get actual attacker (handle projectiles like arrows)
        val attacker = when (val damager = event.damager) {
            is Player -> damager
            is org.bukkit.entity.Projectile -> damager.shooter as? Player
            else -> null
        } ?: return

        val game = gameManager.activeGame ?: return

        val attackerData = game.players[attacker.uniqueId] ?: return
        val victimData = game.players[victim.uniqueId] ?: return

        // Combat Rules:
        // 1. Seeker → Seeker: Allow damage (PK for reverting to Hider)
        // 2. Seeker → Hider: No damage, capture only
        // 3. Hider → Seeker: No damage, cancel
        // 4. Hider → Hider: No damage, cancel

        // Rule 3 & 4: Hiders cannot deal damage to anyone
        if (attackerData.role == PlayerRole.HIDER) {
            event.isCancelled = true
            // Only show message if attacking a Seeker (not other Hiders)
            if (victimData.role == PlayerRole.SEEKER) {
                MessageUtil.send(attacker, "&c人間は鬼にダメージを与えられません")
            }
            return
        }

        // Rule 1: Seeker → Seeker combat (allow real damage for PK)
        if (attackerData.role == PlayerRole.SEEKER && victimData.role == PlayerRole.SEEKER) {
            // Allow real damage between seekers (do NOT cancel event)
            // The actual combat result will be handled in PlayerDeathListener
            plugin.logger.info("[SeekerCombat] ${attacker.name} is attacking ${victim.name} (seeker vs seeker PK)")
            return
        }

        // Rule 2: Seeker → Hider (instant capture, no damage)
        event.isCancelled = true

        // Handle seeker vs hider capture
        if (victimData.role != PlayerRole.HIDER) {
            return
        }

        if (victimData.isCaptured) {
            return
        }

        if (disguiseManager.isDisguised(victim.uniqueId)) {
            disguiseManager.undisguise(victim, "capture")
        }

        // Mark as captured (for statistics)
        victimData.isCaptured = true
        attackerData.recordCapture()

        // Track victim's points before capture
        val victimPointsBefore = pointManager?.getPoints(victim.uniqueId) ?: 0

        // Handle point awarding (base points + stolen points)
        val basePoints = plugin.config.getInt("points.capture-base-points", 50)
        val stealPercentage = plugin.config.getDouble("points.capture-steal-percentage", 0.5)
        val pointsAwarded = pointManager?.handleCapture(game, attacker.uniqueId, victim.uniqueId, basePoints, stealPercentage) ?: 0

        // Broadcast demon transformation message (before role change)
        game.players.values.forEach { playerData ->
            Bukkit.getPlayer(playerData.uuid)?.let { player ->
                MessageUtil.send(player, "&c${attacker.name} &7が &a${victim.name} &7を鬼化した！ &7(鬼化済み: ${game.getCaptured().size}/${game.getHiders().size + game.getCaptured().size})")
            }
        }

        // Send point messages
        if (pointsAwarded > 0) {
            MessageUtil.send(attacker, "&e+${pointsAwarded} ポイント獲得 &7(鬼化)")
            val victimPointsAfter = pointManager?.getPoints(victim.uniqueId) ?: 0
            val pointsLost = victimPointsBefore - victimPointsAfter
            if (pointsLost > 0) {
                MessageUtil.send(victim, "&c-${pointsLost} ポイント &7(${attacker.name}に奪われた)")
            }
        }

        // Add strength points to seeker (T024: Strength accumulation)
        val strengthPointsPerCapture = plugin.config.getInt("seeker-strength.points-per-capture", 200)
        val strengthManager = (plugin as? com.hideandseek.HideAndSeekPlugin)?.seekerStrengthManager
        strengthManager?.addStrength(attacker.uniqueId, strengthPointsPerCapture)

        val totalStrength = strengthManager?.getStrength(attacker.uniqueId) ?: 0
        MessageUtil.send(attacker, "&6+${strengthPointsPerCapture} 強さポイント &7(合計: $totalStrength)")

        // Handle capture based on mode (spectator or infection)
        // This will set the appropriate role (SPECTATOR or SEEKER)
        gameManager.handleCapture(attacker, victim)

        // Check win condition based on capture mode
        val configManager = (plugin as? com.hideandseek.HideAndSeekPlugin)?.configManager
        val captureMode = configManager?.getCaptureMode() ?: com.hideandseek.config.CaptureMode.INFECTION

        // Debug logging
        val remainingHiders = game.getHiders()
        plugin.logger.info("[DEBUG] Capture mode: $captureMode")
        plugin.logger.info("[DEBUG] Remaining hiders after capture: ${remainingHiders.size}")
        game.players.forEach { (uuid, data) ->
            val playerName = Bukkit.getPlayer(uuid)?.name ?: "Unknown"
            plugin.logger.info("[DEBUG] Player: $playerName, Role: ${data.role}, isCaptured: ${data.isCaptured}")
        }

        when (captureMode) {
            com.hideandseek.config.CaptureMode.SPECTATOR -> {
                // SPECTATOR mode: Game ends when all hiders are captured (become spectators)
                if (remainingHiders.isEmpty()) {
                    plugin.logger.info("[DEBUG] All hiders captured in SPECTATOR mode - ending game")
                    gameManager.endGame(com.hideandseek.game.GameResult.SEEKER_WIN)
                }
            }
            com.hideandseek.config.CaptureMode.INFECTION -> {
                // INFECTION mode: Game ends when all players become seekers
                if (remainingHiders.isEmpty()) {
                    plugin.logger.info("[DEBUG] All players became seekers in INFECTION mode - ending game")
                    gameManager.endGame(com.hideandseek.game.GameResult.SEEKER_WIN)
                }
            }
        }
    }

}
