package gg.ethereallabs.blockChess.gui.models

import gg.ethereallabs.blockChess.BlockChess
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

abstract class BaseMenu(private val title: Component, val size: Int) : Listener {
    protected var inv: Inventory? = null
    private val viewers: MutableSet<UUID?> = HashSet<UUID?>()

    constructor(title: String, size: Int) : this(LegacyComponentSerializer.legacyAmpersand().deserialize(title), size)

    fun open(p: Player) {
        inv = Bukkit.createInventory(null, size, title.color(NamedTextColor.WHITE))
        Bukkit.getPluginManager().registerEvents(this, BlockChess.instance)
        viewers.add(p.getUniqueId())
        draw(p)
        p.openInventory(inv!!)
    }

    abstract fun draw(p: Player?)

    abstract fun handleClick(p: Player?, slot: Int, e: InventoryClickEvent?)

    @EventHandler
    fun onClick(e: InventoryClickEvent) {
        if (e.whoClicked !is Player) return

        // If our menu is open as the top inventory, intercept clicks both on top and bottom
        if (e.view.topInventory === inv) {
            e.isCancelled = true
            // Use rawSlot to identify clicks across both inventories consistently
            handleClick(e.whoClicked as Player?, e.rawSlot, e)
        }
    }

    @EventHandler
    fun onClose(e: InventoryCloseEvent) {
        if (inv == null || e.getInventory() !== inv) return
        if (e.player !is Player) return
        viewers.remove(e.player.uniqueId)
        if (viewers.isEmpty()) {
            HandlerList.unregisterAll(this)
        }
    }

    protected fun createItem(
        name: Component?,
        material: Material,
        lore: MutableList<Component?>?,
        qty: Int
    ): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        item.amount = qty
        if (meta != null) {
            meta.customName(name?.decoration(TextDecoration.ITALIC, false))
            meta.lore(lore)
            item.setItemMeta(meta)
        }

        return item
    }

    fun getInventory(): Inventory? {
        return inv
    }
}