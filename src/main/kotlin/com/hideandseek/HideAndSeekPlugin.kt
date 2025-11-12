package com.hideandseek

import com.hideandseek.arena.ArenaManager
import com.hideandseek.blockrestoration.BlockRestorationManager
import com.hideandseek.commands.AdminCommand
import com.hideandseek.commands.HideAndSeekCommand
import com.hideandseek.commands.JoinCommand
import com.hideandseek.commands.LeaveCommand
import com.hideandseek.config.BlockRestorationConfig
import com.hideandseek.config.ConfigManager
import com.hideandseek.disguise.DisguiseManager
import com.hideandseek.effects.EffectManagerImpl
import com.hideandseek.effects.EffectStorage
import com.hideandseek.game.GameManager
import com.hideandseek.i18n.LanguagePreferenceManager
import com.hideandseek.i18n.LanguagePreferenceManagerImpl
import com.hideandseek.i18n.MessageManager
import com.hideandseek.i18n.MessageManagerImpl
import com.hideandseek.listeners.*
import com.hideandseek.respawn.RespawnManager
import com.hideandseek.scoreboard.GameScoreboard
import com.hideandseek.shop.PurchaseStorage
import com.hideandseek.shop.ShopManager
import com.hideandseek.utils.EffectScheduler
import com.hideandseek.utils.TaskScheduler
import net.milkbowl.vault.economy.Economy
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

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
    lateinit var taskScheduler: TaskScheduler
        private set

    // Effect system components
    lateinit var effectManager: EffectManagerImpl
        private set
    lateinit var purchaseStorage: PurchaseStorage
        private set
    var economy: Economy? = null
        private set

    // Point system
    lateinit var pointManager: com.hideandseek.points.PointManager
        private set

    // Block restoration system
    lateinit var blockRestorationManager: BlockRestorationManager
        private set
    lateinit var respawnManager: RespawnManager
        private set

    // Spectator system
    lateinit var spectatorManager: com.hideandseek.spectator.SpectatorManager
        private set

    // Scoreboard system
    lateinit var gameScoreboard: GameScoreboard
        private set
    lateinit var localizedScoreboard: com.hideandseek.scoreboard.LocalizedScoreboardManager
        private set

    // Seeker strength system
    lateinit var seekerStrengthManager: com.hideandseek.strength.SeekerStrengthManager
        private set

    // Internationalization (i18n) system
    lateinit var languagePreferenceManager: LanguagePreferenceManager
        private set
    lateinit var messageManager: MessageManager
        private set

    override fun onEnable() {
        logger.info("HideAndSeek plugin enabling...")

        // Initialize i18n system FIRST (other systems may use it)
        languagePreferenceManager = LanguagePreferenceManagerImpl(this)
        messageManager = MessageManagerImpl(this, languagePreferenceManager)
        logger.info("Internationalization system initialized (${languagePreferenceManager.getAvailableLanguages().size} languages)")

        configManager = ConfigManager(this)
        configManager.load()

        taskScheduler = TaskScheduler(this)
        arenaManager = ArenaManager(this, configManager)
        arenaManager.loadArenas()

        gameManager = GameManager(this, configManager)

        shopManager = ShopManager(this, configManager)
        shopManager.loadCategories()

        disguiseManager = DisguiseManager(this, gameManager)

        // Initialize effect system
        val effectStorage = EffectStorage()
        val effectScheduler = EffectScheduler(this)
        effectManager = EffectManagerImpl(this, effectStorage, effectScheduler, logger)
        purchaseStorage = PurchaseStorage()

        // Initialize point system
        pointManager = com.hideandseek.points.PointManager(this)

        // Initialize block restoration system
        val blockRestorationConfigFile = File(dataFolder, "block-restoration.yml")
        if (!blockRestorationConfigFile.exists()) {
            saveResource("block-restoration.yml", false)
        }
        val blockRestorationYaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(blockRestorationConfigFile)
        val blockRestorationConfig = BlockRestorationConfig(blockRestorationYaml)

        blockRestorationManager = BlockRestorationManager(this, blockRestorationConfig, disguiseManager)
        blockRestorationManager.start()
        logger.info("Block restoration system started")

        // Initialize respawn manager
        respawnManager = RespawnManager(this, blockRestorationConfig, gameManager)
        logger.info("Respawn system initialized")

        // Initialize spectator system
        spectatorManager = com.hideandseek.spectator.SpectatorManager(this)
        spectatorManager.initialize()
        logger.info("Spectator system initialized")

        // Initialize scoreboard system
        gameScoreboard = GameScoreboard(this)
        localizedScoreboard = com.hideandseek.scoreboard.LocalizedScoreboardManager(this, messageManager)
        logger.info("Scoreboard system initialized (including localized scoreboard)")

        // Initialize seeker strength system
        seekerStrengthManager = com.hideandseek.strength.SeekerStrengthManager(this)
        logger.info("Seeker strength system initialized")

        // Register effect handlers (old EffectHandler interface)
        effectManager.registerHandler(
            com.hideandseek.effects.EffectType.VISION,
            com.hideandseek.items.VisionEffectHandler()
        )
        effectManager.registerHandler(
            com.hideandseek.effects.EffectType.GLOW,
            com.hideandseek.items.GlowDetectorHandler(this, disguiseManager)
        )
        effectManager.registerHandler(
            com.hideandseek.effects.EffectType.SPEED,
            com.hideandseek.items.SpeedBoostHandler()
        )
        effectManager.registerHandler(
            com.hideandseek.effects.EffectType.REACH,
            com.hideandseek.items.ReachExtenderHandler()
        )

        // Register new ItemEffectHandler-based handlers via adapter
        effectManager.registerHandler(
            com.hideandseek.effects.EffectType.SHADOW_SPRINT,
            com.hideandseek.effects.ItemEffectHandlerAdapter(
                com.hideandseek.items.ShadowSprintHandler(),
                gameManager
            )
        )
        effectManager.registerHandler(
            com.hideandseek.effects.EffectType.SECOND_CHANCE,
            com.hideandseek.effects.ItemEffectHandlerAdapter(
                com.hideandseek.items.SecondChanceHandler(),
                gameManager
            )
        )
        effectManager.registerHandler(
            com.hideandseek.effects.EffectType.DECOY_BLOCK,
            com.hideandseek.effects.ItemEffectHandlerAdapter(
                com.hideandseek.items.DecoyBlockHandler(),
                gameManager
            )
        )
        effectManager.registerHandler(
            com.hideandseek.effects.EffectType.TRACKER_COMPASS,
            com.hideandseek.effects.ItemEffectHandlerAdapter(
                com.hideandseek.items.TrackerCompassHandler(),
                gameManager
            )
        )
        effectManager.registerHandler(
            com.hideandseek.effects.EffectType.AREA_SCAN,
            com.hideandseek.effects.ItemEffectHandlerAdapter(
                com.hideandseek.items.AreaScanHandler(),
                gameManager
            )
        )
        effectManager.registerHandler(
            com.hideandseek.effects.EffectType.EAGLE_EYE,
            com.hideandseek.effects.ItemEffectHandlerAdapter(
                com.hideandseek.items.EagleEyeHandler(),
                gameManager
            )
        )
        effectManager.registerHandler(
            com.hideandseek.effects.EffectType.TRACKER_INSIGHT,
            com.hideandseek.effects.ItemEffectHandlerAdapter(
                com.hideandseek.items.TrackerInsightHandler(),
                gameManager
            )
        )
        effectManager.registerHandler(
            com.hideandseek.effects.EffectType.CAPTURE_NET,
            com.hideandseek.effects.ItemEffectHandlerAdapter(
                com.hideandseek.items.CaptureNetHandler(),
                gameManager
            )
        )

        // Setup Vault economy
        setupEconomy()

        gameManager.shopManager = shopManager
        gameManager.disguiseManager = disguiseManager
        gameManager.messageManager = messageManager
        gameManager.pointManager = pointManager
        gameManager.arenaManager = arenaManager
        gameManager.gameScoreboard = gameScoreboard
        gameManager.localizedScoreboard = localizedScoreboard
        gameManager.seekerStrengthManager = seekerStrengthManager

        // T055: Set messageManager in shopManager for localization
        shopManager.messageManager = messageManager

        registerCommands()
        registerListeners()

        logger.info("HideAndSeek plugin enabled!")
    }

    override fun onDisable() {
        logger.info("HideAndSeek plugin disabling...")

        // Save language preferences FIRST (before clearing other data)
        languagePreferenceManager.saveAll()
        logger.info("Language preferences saved (${languagePreferenceManager.getPreferenceCount()} players)")

        // End any active game and restore all players
        gameManager.activeGame?.let { game ->
            logger.info("Cleaning up active game...")
            gameManager.endGame(com.hideandseek.game.GameResult.CANCELLED)
        }

        // Clear all disguises
        disguiseManager.clearAllDisguises()

        // Clear all active effects for all online players
        server.onlinePlayers.forEach { player ->
            effectManager.removeAllEffects(player.uniqueId)

            // Remove all potion effects
            player.activePotionEffects.forEach { effect ->
                player.removePotionEffect(effect.type)
            }

            // Reset view distance
            player.sendViewDistance = 10
        }

        // Stop and cleanup block restoration
        blockRestorationManager.stop()
        val clearedCount = blockRestorationManager.clearAll()
        logger.info("Block restoration system stopped ($clearedCount pending blocks cleared)")

        // Clear all scoreboards
        gameScoreboard.clearAll()
        localizedScoreboard.clearAll()
        logger.info("Scoreboard system cleared")

        // Shutdown spectator system
        spectatorManager.shutdown()
        spectatorManager.getConfig().save()

        taskScheduler.cancelAll()
        logger.info("HideAndSeek plugin disabled!")
    }

    private fun registerCommands() {
        logger.info("Registering commands...")

        val shopCommand = com.hideandseek.commands.ShopCommand(shopManager, gameManager, messageManager)
        val adminCommand = AdminCommand(arenaManager, gameManager, messageManager)
        val spectatorCommand = com.hideandseek.commands.SpectatorCommand(spectatorManager, gameManager)
        val langCommand = com.hideandseek.commands.LangCommand(languagePreferenceManager, messageManager)
        val mainCommand = HideAndSeekCommand(adminCommand, shopCommand, spectatorCommand, langCommand, messageManager)

        val command = getCommand("hideandseek")
        if (command == null) {
            logger.severe("Failed to register command 'hideandseek' - command not found in plugin.yml!")
            return
        }

        command.setExecutor(mainCommand)
        command.tabCompleter = mainCommand

        logger.info("Command 'hideandseek' (alias: 'hs') registered successfully")
        logger.info("Available subcommands: shop, lang, spectator, admin")
        logger.info("Auto-join enabled: Players automatically join game on login")
    }

    private fun registerListeners() {
        val pluginManager = server.pluginManager

        val shopListener = ShopListener(this, shopManager, gameManager, effectManager, purchaseStorage, economy)
        shopListener.disguiseManager = disguiseManager
        // T055: Set messageManager in shopListener for localization
        shopListener.messageManager = messageManager

        val playerMoveListener = PlayerMoveListener(disguiseManager)
        playerMoveListener.setPlugin(this)

        val effectCleanupListener = com.hideandseek.listeners.EffectCleanupListener(effectManager, gameManager, logger)
        val playerJoinListener = com.hideandseek.listeners.PlayerJoinListener(this, disguiseManager, effectManager, gameManager, spectatorManager, messageManager)
        val spectatorEventListener = com.hideandseek.listeners.SpectatorEventListener(spectatorManager, gameManager)
        val tauntListener = com.hideandseek.listeners.TauntListener(this, gameManager, pointManager)

        pluginManager.registerEvents(shopListener, this)
        pluginManager.registerEvents(InventoryListener(shopManager), this)
        pluginManager.registerEvents(PlayerInteractListener(shopManager, gameManager), this)
        pluginManager.registerEvents(playerMoveListener, this)
        pluginManager.registerEvents(BlockDamageListener(disguiseManager, gameManager), this)
        pluginManager.registerEvents(EntityDamageByEntityListener(disguiseManager, gameManager, pointManager, this), this)
        pluginManager.registerEvents(BoundaryListener(gameManager), this)
        pluginManager.registerEvents(effectCleanupListener, this)
        pluginManager.registerEvents(playerJoinListener, this)
        pluginManager.registerEvents(spectatorEventListener, this)
        pluginManager.registerEvents(tauntListener, this)

        // Block restoration listeners
        pluginManager.registerEvents(BlockBreakListener(gameManager, blockRestorationManager, this), this)
        pluginManager.registerEvents(PlayerDeathListener(this, gameManager, respawnManager), this)

        logger.info("Listeners registered (including TauntListener, BlockBreakListener, PlayerDeathListener, and SpectatorEventListener)")
    }

    /**
     * Setup Vault economy integration
     */
    private fun setupEconomy() {
        if (server.pluginManager.getPlugin("Vault") == null) {
            logger.warning("Vault not found! Economy features will be disabled.")
            return
        }

        val rsp = server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            logger.warning("Economy provider not found! Economy features will be disabled.")
            return
        }

        economy = rsp.provider
        logger.info("Vault economy integration enabled (${economy!!.name})")
    }
}
