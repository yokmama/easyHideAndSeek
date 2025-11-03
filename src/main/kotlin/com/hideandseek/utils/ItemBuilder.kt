package com.hideandseek.utils

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

/**
 * Fluent builder for creating ItemStacks with metadata
 *
 * Example:
 * ```
 * val item = ItemBuilder(Material.DIAMOND_SWORD)
 *     .displayName("&bLegendary Sword")
 *     .lore("&7A powerful weapon", "&7Damage: +10")
 *     .glow(true)
 *     .build()
 * ```
 */
class ItemBuilder(private val material: Material) {

    private var displayName: String? = null
    private val lore = mutableListOf<String>()
    private var amount = 1
    private var glow = false
    private val flags = mutableSetOf<ItemFlag>()
    private val persistentData = mutableMapOf<Pair<NamespacedKey, PersistentDataType<*, *>>, Any>()

    /**
     * Set display name with & color codes
     *
     * @param name Display name (e.g., "&aGreen Name")
     * @return This builder
     */
    fun displayName(name: String): ItemBuilder {
        this.displayName = name
        return this
    }

    /**
     * Add lore lines with & color codes
     *
     * @param lines Lore lines
     * @return This builder
     */
    fun lore(vararg lines: String): ItemBuilder {
        this.lore.addAll(lines)
        return this
    }

    /**
     * Add lore lines from a list
     *
     * @param lines Lore lines
     * @return This builder
     */
    fun lore(lines: List<String>): ItemBuilder {
        this.lore.addAll(lines)
        return this
    }

    /**
     * Set item amount
     *
     * @param amount Stack size (1-64)
     * @return This builder
     */
    fun amount(amount: Int): ItemBuilder {
        this.amount = amount.coerceIn(1, 64)
        return this
    }

    /**
     * Make item glow (adds fake enchantment + hide flag)
     *
     * @param glow Whether to glow
     * @return This builder
     */
    fun glow(glow: Boolean): ItemBuilder {
        this.glow = glow
        return this
    }

    /**
     * Add item flags
     *
     * @param flags Flags to hide (e.g., HIDE_ENCHANTS)
     * @return This builder
     */
    fun flags(vararg flags: ItemFlag): ItemBuilder {
        this.flags.addAll(flags)
        return this
    }

    /**
     * Add persistent data to item
     *
     * @param key Namespaced key
     * @param type Data type
     * @param value Data value
     * @return This builder
     */
    fun <T, Z : Any> persistentData(key: NamespacedKey, type: PersistentDataType<T, Z>, value: Z): ItemBuilder {
        @Suppress("UNCHECKED_CAST")
        this.persistentData[key to type as PersistentDataType<*, *>] = value as Any
        return this
    }

    /**
     * Build the ItemStack
     *
     * @return Configured ItemStack
     */
    fun build(): ItemStack {
        val item = ItemStack(material, amount)
        val meta: ItemMeta = item.itemMeta ?: return item

        // Apply display name
        displayName?.let {
            meta.displayName(MessageUtil.colorize(it))
        }

        // Apply lore
        if (lore.isNotEmpty()) {
            meta.lore(lore.map { MessageUtil.colorize(it) })
        }

        // Apply glow effect
        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }

        // Apply flags
        if (flags.isNotEmpty()) {
            meta.addItemFlags(*flags.toTypedArray())
        }

        // Apply persistent data
        if (persistentData.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            persistentData.forEach { (keyType, value) ->
                val (key, type) = keyType
                meta.persistentDataContainer.set(key, type as PersistentDataType<Any, Any>, value)
            }
        }

        item.itemMeta = meta
        return item
    }

    companion object {
        /**
         * Quick method to create an ItemBuilder
         *
         * @param material Item material
         * @return New ItemBuilder
         */
        fun of(material: Material): ItemBuilder {
            return ItemBuilder(material)
        }
    }
}
