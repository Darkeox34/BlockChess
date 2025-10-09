package gg.ethereallabs.blockChess.gui

import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.game.Game
import gg.ethereallabs.blockChess.game.GameManager
import gg.ethereallabs.blockChess.gui.models.BaseMenu
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

class SurrendGUI(val gameGUI : GameGUI,
                   val player: Player,
                   val choice: (surrender: Boolean) -> Unit) : BaseMenu("Promotion", 27) {

    init {
        GameManager.playersSurrending.put(player.uniqueId, this)
    }

    override fun draw(p: Player?) {
        val acceptItem = createItem(BlockChess.mm.deserialize("<green>Surrender"), Material.GREEN_WOOL,
                mutableListOf(BlockChess.mm.deserialize("<gray>Give your resignation").decoration(TextDecoration.ITALIC, false)),
            1
            )

        val cancelItem = createItem(BlockChess.mm.deserialize("<red>Cancel"), Material.GREEN_WOOL,
            mutableListOf(BlockChess.mm.deserialize("<gray>Don't resign").decoration(TextDecoration.ITALIC, false)),
            1
        )

        inv?.setItem(12, acceptItem)
        inv?.setItem(14, cancelItem)
    }

    override fun handleClick(p: Player?, slot: Int, e: InventoryClickEvent?) {
        val resign = when (slot) {
            12 -> true
            14 -> false
            else -> null
        }

        if (resign != null) {
            choice(resign)
            p?.closeInventory()
            gameGUI.open(p!!)
        }
    }
}