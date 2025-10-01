package gg.ethereallabs.blockChess.gui

import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.game.Game
import gg.ethereallabs.blockChess.gui.models.BaseMenu
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

class GameGUI(val game: Game, val playerIsWhite: Boolean) : BaseMenu("BlockChess", 54) {

    override fun draw(p: Player?) {
        inv?.clear()
        p?.inventory?.clear()

        val board = game.board

        // Draw the numbered rightmost column (8 panes total)
        for (visualRank in 0..7) {
            val number = if (playerIsWhite) 8 - visualRank else visualRank + 1

            val item = createItem(Component.text(number.toString()), Material.GRAY_STAINED_GLASS_PANE, mutableListOf(), 1)

            if (visualRank < 6) {
                // Place in chest inventory's rightmost column
                val chestSlot = visualRank * 9 + 8
                inv?.setItem(chestSlot, item)
            } else {
                // Place in player inventory's rightmost column
                val playerInvRow = visualRank - 6 // 0 or 1
                val playerSlot = playerInvRow * 9 + 8 + 9
                p?.inventory?.setItem(playerSlot, item)
            }
        }

        // Draw the lettered bottom row in player inventory
        for (visualFile in 0..7) {
            val letter = ('A' + visualFile).toString()

            val item = createItem(Component.text(letter), Material.GRAY_STAINED_GLASS_PANE, mutableListOf(), 1)
            val playerSlot = 2 * 9 + visualFile + 9
            p?.inventory?.setItem(playerSlot, item)
        }

        val clock = createItem(Component.text("Remaining Time"), Material.CLOCK, mutableListOf(
            BlockChess.mm.deserialize("<gray>White: <yellow>5:00"),
            BlockChess.mm.deserialize("<gray>Black: <yellow>5:00")
        ), 1)

        p?.inventory?.setItem(35, clock)

        val surrender = createItem(Component.text("Surrender"), Material.WHITE_BANNER, mutableListOf(), 1)

        p?.inventory?.setItem(3, surrender)

        val draw = createItem(Component.text("Request Draw"), Material.GRAY_BANNER, mutableListOf(), 1)

        p?.inventory?.setItem(5, draw)

        for (visualRank in 0..7) {
            for (visualFile in 0..7) {

                // Map visual coordinates to board coordinates based on player's perspective
                val boardRank = if (playerIsWhite) 7 - visualRank else visualRank
                val boardFile = if (playerIsWhite) visualFile else 7 - visualFile

                val squareIndex = boardRank * 8 + boardFile
                val square = Square.squareAt(squareIndex)
                val piece = board.getPiece(square)

                if (piece == null || piece.pieceType == null || piece.pieceType.name == "NONE") continue

                val fen = piece.fenSymbol.lowercase()
                val material = if (piece.pieceSide == Side.WHITE)
                    BlockChess.whitePiecesByChar[fen]
                else
                    BlockChess.blackPiecesByChar[fen]

                if (material == null) continue

                val name = BlockChess.instance.fenToName[fen] ?: "Unknown"
                val item = createItem(Component.text(name), material, mutableListOf(
                    Component.text(square.name).decoration(TextDecoration.ITALIC, false),
                ), 1)

                // Map visual coordinates to inventory slots
                if (visualRank < 6) {
                    // Place in chest inventory (top 6 rows of the board)
                    val chestSlot = visualRank * 9 + visualFile
                    inv?.setItem(chestSlot, item)
                } else {
                    // Place in player inventory (bottom 2 rows of the board)
                    val playerInvRow = visualRank - 6 // 0 or 1
                    val playerSlot = playerInvRow * 9 + visualFile + 9
                    p?.inventory?.setItem(playerSlot, item)
                }
            }
        }
    }

    override fun handleClick(p: Player?, slot: Int, e: InventoryClickEvent?) {
        // TODO: Implement piece movement logic
    }

}