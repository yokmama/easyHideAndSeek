package com.hideandseek.listeners

import com.hideandseek.effects.EffectManager
import com.hideandseek.game.GameManager
import com.hideandseek.game.PlayerRole
import com.hideandseek.shop.*
import com.hideandseek.utils.MessageUtil
import net.milkbowl.vault.economy.Economy
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.plugin.Plugin

class ShopListener(
    private val plugin: Plugin,
    private val shopManager: ShopManager,
    private val gameManager: GameManager,
    private val effectManager: EffectManager?,
    private val purchaseStorage: PurchaseStorage,
    private val economy: Economy?
) : Listener {

    var disguiseManager: com.hideandseek.disguise.DisguiseManager? = null
    var messageManager: com.hideandseek.i18n.MessageManager? = null

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // Debug: Log ALL inventory clicks
        player.sendMessage("§8[Debug] Inventory click detected")

        val clickedItem = event.currentItem ?: return

        val view = event.view
        val title = view.title

        // Debug logging
        player.sendMessage("§7[Debug] Clicked item: ${clickedItem.type}")
        player.sendMessage("§7[Debug] Inventory title: $title")

        // Check if this is a shop inventory by checking if clicked item has shop metadata
        val itemId = shopManager.getItemIdFromItem(clickedItem)
        val categoryId = shopManager.getCategoryFromItem(clickedItem)

        player.sendMessage("§7[Debug] ItemId: $itemId, CategoryId: $categoryId")

        // If item has no shop metadata, it's not a shop item
        if (itemId == null && categoryId == null && !shopManager.isShopItem(clickedItem)) {
            player.sendMessage("§c[Debug] Not a shop item, ignoring")
            return
        }

        // Cancel all clicks in shop inventories
        event.isCancelled = true
        player.sendMessage("§a[Debug] Shop item detected!")

        // Get player role for filtering
        val game = gameManager.activeGame
        val playerRole = if (game != null && game.players.containsKey(player.uniqueId)) {
            game.players[player.uniqueId]?.role?.name
        } else {
            null
        }

        // Handle special items (close, back)
        when (itemId) {
            "close" -> {
                player.closeInventory()
                return
            }
            "back" -> {
                shopManager.openMainMenu(player, playerRole)
                return
            }
        }

        // Handle category selection (from main menu)
        if (categoryId != null && itemId == null) {
            shopManager.openCategory(player, categoryId, playerRole)
            return
        }

        // Handle item purchase
        if (categoryId != null && itemId != null) {
            handleItemPurchase(player, categoryId, itemId)
            return
        }
    }

    private fun handleItemPurchase(player: Player, categoryId: String, itemId: String) {
        player.sendMessage("§e[Debug] handleItemPurchase called: category=$categoryId, item=$itemId")

        // Check if player is in a game
        val game = gameManager.activeGame
        if (game == null) {
            MessageUtil.send(player, "&cYou must be in a game to purchase items")
            player.closeInventory()
            return
        }

        if (!game.players.containsKey(player.uniqueId)) {
            MessageUtil.send(player, "&cYou are not in this game")
            player.closeInventory()
            return
        }

        // Get player role
        val playerData = game.players[player.uniqueId]
        if (playerData == null) {
            MessageUtil.send(player, "&cPlayer data not found")
            player.closeInventory()
            return
        }

        val playerRole = playerData.role.name // "SEEKER", "HIDER", or "SPECTATOR"

        // Get the shop item
        val shopItem = shopManager.getShopItem(categoryId, itemId)
        if (shopItem == null) {
            player.sendMessage("§c[Debug] Shop item not found!")
            MessageUtil.send(player, "&cItem not found")
            return
        }

        player.sendMessage("§a[Debug] Shop item found: ${shopItem.displayName}")

        // Execute the shop action
        when (val action = shopItem.action) {
            is ShopAction.Disguise -> {
                handleDisguisePurchase(player, action, shopItem)
            }
            is ShopAction.UseEffectItem -> {
                // T026-T028: Full effect item purchase flow
                handleEffectItemPurchase(player, action, shopItem, playerRole, game.arena.name)
            }
            is ShopAction.UseTauntItem -> {
                player.sendMessage("§c[Debug] ERROR: UseTauntItem action detected - this should not happen!")
                player.sendMessage("§c[Debug] Item: ${shopItem.id}, tauntType: ${action.tauntType}")
                // DO NOT call handleTauntItemPurchase - this is the old instant-use behavior
                MessageUtil.send(player, "&cこのアイテムは古い形式です。管理者に連絡してください。")
            }
            is ShopAction.GiveItem -> {
                handleGiveItemPurchase(player, action, shopItem)
            }
            is ShopAction.GoBack -> {
                shopManager.openMainMenu(player, playerRole)
            }
            is ShopAction.Close -> {
                player.closeInventory()
            }
        }
    }

    /**
     * Handle disguise item purchase (existing functionality)
     */
    private fun handleDisguisePurchase(player: Player, action: ShopAction.Disguise, shopItem: ShopItem) {
        player.sendMessage("§b[Debug] Disguise action triggered for: ${action.blockType}")
        val disguiseMgr = disguiseManager
        if (disguiseMgr != null) {
            player.sendMessage("§b[Debug] DisguiseManager found, attempting disguise...")
            val success = disguiseMgr.disguise(player, action.blockType)
            player.sendMessage("§b[Debug] Disguise result: $success")
            if (success) {
                player.closeInventory()
            }
        } else {
            player.sendMessage("§c[Debug] DisguiseManager is NULL!")
            MessageUtil.send(player, "&cDisguise system not available")
            player.closeInventory()
        }
    }

    /**
     * T026-T028: Handle effect item purchase with full validation and economy integration
     */
    private fun handleEffectItemPurchase(
        player: Player,
        action: ShopAction.UseEffectItem,
        shopItem: ShopItem,
        playerRole: String,
        gameId: String
    ) {
        player.sendMessage("§b[Debug] Effect item purchase: ${action.effectType}")

        try {
            // T026: Validate purchase
            PurchaseValidator.validatePurchase(
                player = player,
                item = shopItem,
                playerRole = playerRole,
                gameId = gameId,
                economy = economy,
                purchaseStorage = purchaseStorage
            )

            // T028: Deduct funds (async if economy available)
            if (economy != null && shopItem.price > 0) {
                val response = economy.withdrawPlayer(player, shopItem.price.toDouble())
                if (!response.transactionSuccess()) {
                    throw InsufficientFundsException(
                        required = shopItem.price,
                        actual = economy.getBalance(player)
                    )
                }
            }

            // Apply effect
            if (effectManager != null) {
                // Prepare metadata (e.g., store original view distance for VISION)
                val metadata = mutableMapOf<String, Any>()
                if (action.effectType == com.hideandseek.effects.EffectType.VISION) {
                    metadata["originalViewDistance"] = player.clientViewDistance
                }

                val success = effectManager.applyEffect(
                    player = player,
                    effectType = action.effectType,
                    durationSeconds = action.duration,
                    intensity = action.intensity,
                    metadata = metadata
                )

                if (success) {
                    // Record purchase
                    val record = ItemPurchaseRecord(
                        playerId = player.uniqueId,
                        itemId = shopItem.id,
                        purchaseTime = java.time.Instant.now(),
                        gameId = gameId,
                        price = shopItem.price,
                        used = true
                    )
                    purchaseStorage.recordPurchase(record)

                    // T053: Localized success message
                    val msgMgr = messageManager
                    if (msgMgr != null) {
                        msgMgr.send(player, "shop.purchase.success", shopItem.displayName)
                    } else {
                        MessageUtil.send(player, "&aアイテムを購入しました: &e${shopItem.displayName}")
                    }
                    MessageUtil.send(
                        player,
                        "&b${action.effectType.displayName} &aが適用されました（&e${action.duration}秒間&a）"
                    )
                    player.closeInventory()
                } else {
                    // Effect application failed - refund
                    if (economy != null && shopItem.price > 0) {
                        economy.depositPlayer(player, shopItem.price.toDouble())
                    }
                    MessageUtil.send(player, "&cエフェクトの適用に失敗しました")
                }
            } else {
                // No effect manager - refund
                if (economy != null && shopItem.price > 0) {
                    economy.depositPlayer(player, shopItem.price.toDouble())
                }
                MessageUtil.send(player, "&cエフェクトシステムが利用できません")
            }

        } catch (e: InsufficientFundsException) {
            // T054: Localized error messages
            val msgMgr = messageManager
            if (msgMgr != null) {
                msgMgr.send(player, "shop.purchase.failed.insufficient", e.required, e.actual.toInt())
            } else {
                MessageUtil.send(
                    player,
                    "&cお金が足りません。必要: &e${e.required} coins &c/ 所持: &e${e.actual.toInt()} coins"
                )
            }
        } catch (e: PurchaseLimitException) {
            val msgMgr = messageManager
            if (msgMgr != null) {
                msgMgr.send(player, "shop.purchase.failed.limit")
            } else {
                MessageUtil.send(
                    player,
                    "&cこのアイテムは購入制限に達しています (${e.current}/${e.limit})"
                )
            }
        } catch (e: CooldownException) {
            val msgMgr = messageManager
            if (msgMgr != null) {
                msgMgr.send(player, "shop.purchase.failed.cooldown", e.remainingSeconds)
            } else {
                MessageUtil.send(
                    player,
                    "&cクールダウン中です。残り時間: &e${e.remainingSeconds}秒"
                )
            }
        } catch (e: InventoryFullException) {
            MessageUtil.send(player, "&cインベントリがいっぱいです")
        } catch (e: RoleRestrictionException) {
            if (e.requiredRole.equals("SEEKER", ignoreCase = true)) {
                MessageUtil.send(player, "&cこのアイテムはシーカー専用です")
            } else {
                MessageUtil.send(player, "&cこのアイテムはハイダー専用です")
            }
        } catch (e: UsageRestrictionException) {
            MessageUtil.send(player, "&c${e.reason}")
        } catch (e: Exception) {
            player.sendMessage("§c[Debug] Unexpected error: ${e.message}")
            e.printStackTrace()
            MessageUtil.send(player, "&c購入処理中にエラーが発生しました")
        }
    }

    /**
     * Handle taunt item purchase (instant execution)
     */
    private fun handleTauntItemPurchase(player: Player, action: ShopAction.UseTauntItem, shopItem: ShopItem) {
        player.sendMessage("§b[Debug] Taunt item purchase: ${action.tauntType}")

        // Get point manager from game manager
        val game = gameManager.activeGame
        if (game == null) {
            MessageUtil.send(player, "&cゲームが見つかりません")
            return
        }

        // Execute taunt immediately based on type
        when (action.tauntType.uppercase()) {
            "SNOWBALL" -> {
                // Spawn snowball projectile
                val snowball = player.launchProjectile(org.bukkit.entity.Snowball::class.java)
                snowball.velocity = player.location.direction.multiply(1.5)

                // Award bonus points
                val pointMgr = gameManager.pointManager
                pointMgr?.awardTauntBonus(player.uniqueId, action.bonusPoints)

                MessageUtil.send(player, "&a雪玉を投げました！")
                MessageUtil.send(player, "&e+${action.bonusPoints} ポイント &7(雪玉挑発)")
                player.sendMessage("§c警告: シーカーに位置がバレやすくなりました！")
            }
            "FIREWORK" -> {
                // Spawn firework
                spawnTauntFirework(player)

                // Award bonus points
                val pointMgr = gameManager.pointManager
                pointMgr?.awardTauntBonus(player.uniqueId, action.bonusPoints)

                MessageUtil.send(player, "&a花火を打ち上げました！")
                MessageUtil.send(player, "&e+${action.bonusPoints} ポイント &7(花火挑発)")
                player.sendMessage("§c警告: シーカーに位置が非常にバレやすくなりました！")
            }
            else -> {
                MessageUtil.send(player, "&c不明な挑発タイプです")
                return
            }
        }

        player.closeInventory()
    }

    /**
     * Spawn a colorful firework for taunt
     */
    private fun spawnTauntFirework(player: Player) {
        val location = player.location.clone().add(0.0, 1.0, 0.0)
        val firework = player.world.spawnEntity(location, org.bukkit.entity.EntityType.FIREWORK_ROCKET) as org.bukkit.entity.Firework
        val meta = firework.fireworkMeta

        // Create colorful effect
        val effect = org.bukkit.FireworkEffect.builder()
            .withColor(org.bukkit.Color.RED, org.bukkit.Color.YELLOW, org.bukkit.Color.LIME, org.bukkit.Color.AQUA, org.bukkit.Color.FUCHSIA)
            .with(org.bukkit.FireworkEffect.Type.BALL_LARGE)
            .withTrail()
            .withFlicker()
            .build()

        meta.addEffect(effect)
        meta.power = 1
        firework.fireworkMeta = meta
    }

    /**
     * Handle giving items to player's inventory
     */
    private fun handleGiveItemPurchase(player: Player, action: ShopAction.GiveItem, shopItem: ShopItem) {
        player.sendMessage("§b[Debug] GiveItem action triggered: ${action.material} x${action.amount}")

        try {
            // Check if player has enough coins
            val price = shopItem.getEffectivePrice()
            val pointMgr = gameManager.pointManager
            if (pointMgr == null) {
                MessageUtil.send(player, "&cポイントシステムが利用できません")
                return
            }

            val currentPoints = pointMgr.getPoints(player.uniqueId)
            if (currentPoints < price) {
                MessageUtil.send(player, "&c購入に失敗しました - コインが足りません")
                MessageUtil.send(player, "&7必要: &e${price} &7/ 所持: &e${currentPoints}")
                return
            }

            // Deduct points (use negative value with addPoints)
            pointMgr.addPoints(player.uniqueId, -price)

            // Give items to player
            val itemStack = org.bukkit.inventory.ItemStack(action.material, action.amount)
            val remaining = player.inventory.addItem(itemStack)

            if (remaining.isEmpty()) {
                MessageUtil.send(player, "&a${shopItem.displayName} &7を &e${action.amount}個 &7購入しました！")
                MessageUtil.send(player, "&7-${price} コイン &7(残り: &e${currentPoints - price}&7)")
                player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
            } else {
                // Inventory full - refund
                pointMgr.addPoints(player.uniqueId, price)
                MessageUtil.send(player, "&cインベントリがいっぱいです")
            }

            player.closeInventory()
        } catch (e: Exception) {
            plugin.logger.warning("Error purchasing item: ${e.message}")
            e.printStackTrace()
            MessageUtil.send(player, "&c購入処理中にエラーが発生しました")
        }
    }
}
