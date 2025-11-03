package com.hideandseek

import com.hideandseek.arena.ArenaManager
import com.hideandseek.commands.AdminCommand
import com.hideandseek.commands.HideAndSeekCommand
import com.hideandseek.commands.JoinCommand
import com.hideandseek.commands.LeaveCommand
import com.hideandseek.config.ConfigManager
import com.hideandseek.disguise.DisguiseManager
import com.hideandseek.game.GameManager
import com.hideandseek.listeners.*
import com.hideandseek.scoreboard.ScoreboardManager
import com.hideandseek.shop.ShopManager
import com.hideandseek.utils.TaskScheduler
import org.bukkit.plugin.java.JavaPlugin

/**
 * Hide and Seek minigame plugin main class
 */
class HideAndSeekPlugin : JavaPlugin() {

    lateinit var configManager: ConfigManager
        private set
    lateinit var arenaManager: ArenaManager
        private set
    lateinit var gameManager: GameManager
        private set
    lateinit var shopManager: ShopManager
        private set
    lateinit var disguiseManager: DisguiseManager
        private set
    lateinit var scoreboardManager: ScoreboardManager
        private set
    lateinit var taskScheduler: TaskScheduler
        private set

    override fun onEnable() {
        logger.info("HideAndSeek plugin enabling...")

        configManager = ConfigManager(this)
        configManager.load()

        taskScheduler = TaskScheduler(this)
        arenaManager = ArenaManager(this, configManager)
        arenaManager.loadArenas()

        gameManager = GameManager(this, configManager)

        shopManager = ShopManager(this, configManager)
        shopManager.loadCategories()

        disguiseManager = DisguiseManager(this, gameManager)
        scoreboardManager = ScoreboardManager(this)

        gameManager.shopManager = shopManager
        gameManager.disguiseManager = disguiseManager
        gameManager.scoreboardManager = scoreboardManager

        registerCommands()
        registerListeners()

        logger.info("HideAndSeek plugin enabled!")
    }

    override fun onDisable() {
        logger.info("HideAndSeek plugin disabling...")
        taskScheduler.cancelAll()
        logger.info("HideAndSeek plugin disabled!")
    }

    private fun registerCommands() {
        logger.info("Registering commands...")

        val joinCommand = JoinCommand(gameManager)
        val leaveCommand = LeaveCommand(gameManager)
        val shopCommand = com.hideandseek.commands.ShopCommand(shopManager, gameManager)
        val adminCommand = AdminCommand(arenaManager, gameManager)
        val mainCommand = HideAndSeekCommand(joinCommand, leaveCommand, adminCommand, shopCommand)

        val command = getCommand("hideandseek")
        if (command == null) {
            logger.severe("Failed to register command 'hideandseek' - command not found in plugin.yml!")
            return
        }

        command.setExecutor(mainCommand)
        command.tabCompleter = mainCommand

        logger.info("Command 'hideandseek' (alias: 'hs') registered successfully")
        logger.info("Available subcommands: join, leave, shop, admin")
    }

    private fun registerListeners() {
        val pluginManager = server.pluginManager

        val shopListener = ShopListener(shopManager, gameManager)
        shopListener.disguiseManager = disguiseManager

        val playerMoveListener = PlayerMoveListener(disguiseManager)
        playerMoveListener.setPlugin(this)

        pluginManager.registerEvents(shopListener, this)
        pluginManager.registerEvents(InventoryListener(shopManager), this)
        pluginManager.registerEvents(PlayerInteractListener(shopManager, gameManager), this)
        pluginManager.registerEvents(playerMoveListener, this)
        pluginManager.registerEvents(BlockDamageListener(disguiseManager, gameManager), this)
        pluginManager.registerEvents(EntityDamageByEntityListener(disguiseManager, gameManager), this)
        pluginManager.registerEvents(BoundaryListener(gameManager), this)

        logger.info("Listeners registered")
    }
}
