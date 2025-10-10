package gg.ethereallabs.blockChess.gui.subgui

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.game.GameManager
import gg.ethereallabs.blockChess.gui.GameGUI
import gg.ethereallabs.blockChess.gui.models.BaseMenu
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

class RequestDrawGUI(val gameGUI : GameGUI,
                     val player: Player,
                     val choice: (surrender: Boolean) -> Unit) : BaseMenu("Request Draw", 27) {

    init {
        GameManager.playersRequestingDraw.put(player.uniqueId, this)
    }

    override fun draw(p: Player?) {
        val acceptItem = createItem(BlockChess.mm.deserialize("<green>Request Draw"), Material.GREEN_WOOL,
            mutableListOf(BlockChess.mm.deserialize("<gray>Request a draw").decoration(TextDecoration.ITALIC, false)),
            1
        )

        val cancelItem = createItem(BlockChess.mm.deserialize("<red>Cancel"), Material.RED_WOOL,
            mutableListOf(BlockChess.mm.deserialize("<gray>Don't request a draw").decoration(TextDecoration.ITALIC, false)),
            1
        )

        inv?.setItem(12, acceptItem)
        inv?.setItem(14, cancelItem)
    }

    override fun handleClick(p: Player?, slot: Int, e: InventoryClickEvent?) {
        val draw = when (slot) {
            12 -> true
            14 -> false
            else -> null
        }

        if (draw != null) {
            choice(draw)
            p?.closeInventory()
            gameGUI.open(p!!)
            BlockChess.instance.sendMessage("<gray>You've requested a draw.")
        }
    }
}