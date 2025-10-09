package gg.ethereallabs.blockChess.gui

import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.game.Game
import gg.ethereallabs.blockChess.gui.models.BaseMenu
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

class PromotionGUI(val gameGUI : GameGUI,
                   val onPieceChosen: (chosenPiece: Piece) -> Unit) : BaseMenu("Promotion", 27) {
    override fun draw(p: Player?) {
        val pieceToSlot = hashMapOf(
            "q" to 10,
            "r" to 12,
            "n" to 14,
            "b" to 16
        )

        pieceToSlot.forEach{ (piece,slot) ->
            val material = if (gameGUI.playerIsWhite)
                BlockChess.whitePiecesByChar[piece]
            else
                BlockChess.blackPiecesByChar[piece]

            if (material == null) return

            val name = BlockChess.instance.fenToName[piece] ?: "Unknown"
            val item = createItem(Component.text(name), material, mutableListOf(
                Component.text("Promote").decoration(TextDecoration.ITALIC, false)
                    .color(NamedTextColor.RED)
            ), 1)

            val meta = item.itemMeta
            meta.setCustomModelData(449)

            item.setItemMeta(meta)
            inv?.setItem(slot, item)
        }
    }

    override fun handleClick(p: Player?, slot: Int, e: InventoryClickEvent?) {
        val chosenPiece = when (slot) {
            10 -> Piece.WHITE_QUEEN.takeIf { gameGUI.playerIsWhite } ?: Piece.BLACK_QUEEN
            12 -> Piece.WHITE_ROOK.takeIf { gameGUI.playerIsWhite } ?: Piece.BLACK_ROOK
            14 -> Piece.WHITE_KNIGHT.takeIf { gameGUI.playerIsWhite } ?: Piece.BLACK_KNIGHT
            16 -> Piece.WHITE_BISHOP.takeIf { gameGUI.playerIsWhite } ?: Piece.BLACK_BISHOP
            else -> null
        }

        if (chosenPiece != null) {
            onPieceChosen(chosenPiece)
            p?.closeInventory()
            gameGUI.open(p!!)
        }
    }
}