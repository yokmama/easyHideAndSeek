package org.example.hello.easyHideAndSeek

import com.hideandseek.arena.ArenaManager
import com.hideandseek.blockrestoration.BlockRestorationManager
import com.hideandseek.config.BlockRestorationConfig
import com.hideandseek.config.ConfigManager
import com.hideandseek.disguise.DisguiseManager
import com.hideandseek.game.GameManager
import com.hideandseek.listeners.BlockBreakListener
import com.hideandseek.listeners.PlayerDeathListener
import com.hideandseek.points.PointManager
import com.hideandseek.respawn.RespawnManager
// ScoreboardManager removed - using simple teams only
import com.hideandseek.shop.ShopManager
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class Main : JavaPlugin() {

    private lateinit var configManager: ConfigManager
    private lateinit var gameManager: GameManager
    private lateinit var arenaManager: ArenaManager
    private lateinit var shopManager: ShopManager
    private lateinit var disguiseManager: DisguiseManager
    private lateinit var pointManager: PointManager
    private lateinit var blockRestorationManager: BlockRestorationManager
    private lateinit var respawnManager: RespawnManager

    override fun onEnable() {
        logger.info("EasyHideAndSeek is starting...")

        // Load configuration files
        configManager = ConfigManager(this)
        configManager.load()

        // Initialize managers
        gameManager = GameManager(this, configManager)
        arenaManager = ArenaManager(this, configManager)
        shopManager = ShopManager(this, configManager)
        disguiseManager = DisguiseManager(this, gameManager)
        pointManager = PointManager(this)

        // Wire up manager dependencies
        gameManager.shopManager = shopManager
        gameManager.disguiseManager = disguiseManager
        gameManager.pointManager = pointManager
        gameManager.arenaManager = arenaManager

        // Initialize block restoration system
        val blockRestorationConfigFile = File(dataFolder, "block-restoration.yml")
        if (!blockRestorationConfigFile.exists()) {
            saveResource("block-restoration.yml", false)
        }
        val blockRestorationYaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(blockRestorationConfigFile)
        val blockRestorationConfig = BlockRestorationConfig(blockRestorationYaml)

        blockRestorationManager = BlockRestorationManager(this, blockRestorationConfig, disguiseManager)

        // Start block restoration task
        blockRestorationManager.start()
        logger.info("Block restoration system started")

        // Initialize respawn manager
        respawnManager = RespawnManager(this, blockRestorationConfig, gameManager)
        logger.info("Respawn system initialized")

        // Register event listeners
        registerListeners()

        logger.info("EasyHideAndSeek enabled successfully!")
    }

    override fun onDisable() {
        logger.info("EasyHideAndSeek is shutting down...")

        // Stop and cleanup block restoration
        blockRestorationManager.stop()
        val clearedCount = blockRestorationManager.clearAll()
        logger.info("Block restoration system stopped ($clearedCount pending blocks cleared)")

        // Cleanup game state
        gameManager.activeGame?.let {
            // End game with appropriate result or force end
            val result = it.checkWinCondition(configManager.config.getLong("game.seek-time", 600000))
                ?: com.hideandseek.game.GameResult.SEEKER_WIN
            gameManager.endGame(result)
        }

        logger.info("EasyHideAndSeek disabled successfully!")
    }

    private fun registerListeners() {
        val pluginManager = Bukkit.getPluginManager()

        // Register block restoration listener
        pluginManager.registerEvents(BlockBreakListener(gameManager, blockRestorationManager, this), this)

        // Register player death listener
        pluginManager.registerEvents(PlayerDeathListener(this, gameManager, respawnManager), this)

        logger.info("Event listeners registered (BlockBreakListener, PlayerDeathListener)")
    }
}

