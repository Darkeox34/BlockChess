package gg.ethereallabs.blockChess.gui

import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import gg.ethereallabs.blockChess.BlockChess
import com.github.bhlangonijr.chesslib.move.Move
import gg.ethereallabs.blockChess.config.Config
import gg.ethereallabs.blockChess.game.Game
import gg.ethereallabs.blockChess.gui.models.BaseMenu
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta

class GameGUI(val game: Game, val playerIsWhite: Boolean) : BaseMenu(
    when{
        !Config.resourcepack -> "BlockChess"
        playerIsWhite -> "<shift:-48>ꔉ"
        else -> "<shift:-48>ꔈ"
    },
    54
) {

    private var selected: Square? = null
    private var legalFromSelected: List<Move> = emptyList()

    private fun formatTime(ms: Long): String {
        val totalSec = (if (ms > 0) ms else 0L) / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%d:%02d", m, s)
    }

    private fun squareFromRawSlot(slot: Int): Square? {
        // Top inventory (0..53)
        if (slot in 0..53) {
            val visualRank = slot / 9
            val visualFile = slot % 9
            if (visualFile == 8) return null // number column
            val boardRank = if (playerIsWhite) 7 - visualRank else visualRank
            val boardFile = if (playerIsWhite) visualFile else 7 - visualFile
            val squareIndex = boardRank * 8 + boardFile
            return Square.squareAt(squareIndex)
        }
        // Bottom inventory (54..89)
        if (slot in 54..89) {
            val idx = slot - 54
            val bottomRow = idx / 9
            val bottomCol = idx % 9
            if (bottomCol == 8) return null // number column
            if (bottomRow !in 0..1) return null // only 2 rows used for board
            val visualRank = 6 + bottomRow
            val boardRank = if (playerIsWhite) 7 - visualRank else visualRank
            val boardFile = if (playerIsWhite) bottomCol else 7 - bottomCol
            val squareIndex = boardRank * 8 + boardFile
            return Square.squareAt(squareIndex)
        }
        return null
    }

    private fun setOverlayAtSquare(p: Player?, square: Square, item: ItemStack) {
        // Determine visual coordinates for this square
        val squareIndex = square.ordinal
        val boardRank = squareIndex / 8
        val boardFile = squareIndex % 8
        val visualRank = if (playerIsWhite) 7 - boardRank else boardRank
        val visualFile = if (playerIsWhite) boardFile else 7 - boardFile

        if (visualRank < 6) {
            val chestSlot = visualRank * 9 + visualFile
            inv?.setItem(chestSlot, item)
        } else {
            val playerInvRow = visualRank - 6
            val playerSlot = playerInvRow * 9 + visualFile + 9
            p?.inventory?.setItem(playerSlot, item)
        }
    }

    fun getClockItem() : ItemStack{
        val clockItem = if(Config.resourcepack){
            createItem(Component.text("Remaining Time"), Material.IRON_NUGGET, mutableListOf(
                BlockChess.mm.deserialize("<gray>White: <yellow>${formatTime(game.whiteTimeMs)}"),
                BlockChess.mm.deserialize("<gray>Black: <yellow>${formatTime(game.blackTimeMs)}")
            ), 1)
        }
        else{
            createItem(Component.text("Remaining Time"), Material.CLOCK, mutableListOf(
                BlockChess.mm.deserialize("<gray>White: <yellow>${formatTime(game.whiteTimeMs)}"),
                BlockChess.mm.deserialize("<gray>Black: <yellow>${formatTime(game.blackTimeMs)}")
            ), 1)
        }
        val clockMeta = clockItem.itemMeta
        clockMeta.setCustomModelData(449)
        clockItem.setItemMeta(clockMeta)

        return clockItem
    }

    fun getSurrendItem() : ItemStack{
        val surrendItem = if(Config.resourcepack){
            createItem(Component.text("Surrender"), Material.IRON_INGOT, mutableListOf(), 1)
        }
        else{
            createItem(Component.text("Surrender"), Material.WHITE_BANNER, mutableListOf(), 1)
        }
        val surrendMeta = surrendItem.itemMeta
        surrendMeta.setCustomModelData(449)
        surrendItem.setItemMeta(surrendMeta)

        return surrendItem
    }

    fun getDrawItem() : ItemStack{
        val drawItem = if(Config.resourcepack){
            createItem(Component.text("Request Draw"), Material.GOLD_INGOT, mutableListOf(), 1)
        }
        else{
            createItem(Component.text("Request Draw"), Material.GRAY_BANNER, mutableListOf(), 1)
        }
        val drawMeta = drawItem.itemMeta
        drawMeta.setCustomModelData(449)
        drawItem.setItemMeta(drawMeta)

        return drawItem
    }

    fun giveCapturedPiece(player: Player, piece: Piece) {
        val inv = player.inventory

        val fen = piece.fenSymbol.lowercase()
        val material = if (piece.pieceSide == Side.WHITE)
            BlockChess.whitePiecesByChar[fen]
        else
            BlockChess.blackPiecesByChar[fen]

        if(material != null) {
            val name = BlockChess.instance.fenToName[fen] ?: "Unknown"
            val item = createItem(
                Component.text(name), material, mutableListOf(
                    Component.text("Eaten").decoration(TextDecoration.ITALIC, false),
                ), 1
            )

            for (slot in 0..8) {
                val current = inv.getItem(slot)
                if (current != null && current.type == item.type) {
                    current.amount += 1
                    inv.setItem(slot, current)
                    return
                }
            }

            for (slot in 0..8) {
                if (inv.getItem(slot) == null) {
                    inv.setItem(slot, item)
                    return
                }
            }
        }
    }

    fun updateClock() {
        val clock = getClockItem()
        inv?.setItem(26, clock)
    }

    override fun draw(p: Player?) {
        inv?.clear()
        p?.inventory?.clear()

        val board = game.board

        val clock = getClockItem()

        inv?.setItem(26, clock)

        val surrender = getSurrendItem()

        inv?.setItem(53, surrender)

        val draw = getDrawItem()

        inv?.setItem(44, draw)

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

                val meta = item.itemMeta
                meta.setCustomModelData(449)

                if(item.type == Material.LEATHER_HORSE_ARMOR){
                    val leatherArmorMeta : LeatherArmorMeta = item.itemMeta as LeatherArmorMeta
                    leatherArmorMeta.setColor(null)
                }

                item.setItemMeta(meta)

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
        if (game.ended) {
            e?.isCancelled = true
            return
        }

        when(slot) {
            53 -> { // Surrender
                game.end()
                return
            }
            44 -> { // Draw

                return
            }
        }

        val clickedSquare = squareFromRawSlot(slot) ?: return

        val sideViewing = if (playerIsWhite) Side.WHITE else Side.BLACK
        val board = game.board

        // If nothing selected, attempt to select a piece of the side to move
        if (selected == null) {
            val piece = board.getPiece(clickedSquare)
            if (piece == null || piece.pieceType == null || piece.pieceType.name == "NONE") return
            if (piece.pieceSide != sideViewing) return
            if (board.sideToMove != sideViewing) return

            // Generate legal moves from this square
            val moves = try {
                board.legalMoves()
            } catch (ex: Exception) {
                emptyList()
            }
            legalFromSelected = moves.filter { it.from == clickedSquare }
            selected = clickedSquare


            for (mv in legalFromSelected) {
                val target = mv.to
                val targetPiece = board.getPiece(target)
                val isCapture = (targetPiece != null && targetPiece.pieceType != null && targetPiece.pieceType.name != "NONE") ||
                        (board.enPassantTarget == target && piece.pieceType?.name == "PAWN")

                val overlayName = if (isCapture) Component.text("Capture " + (BlockChess.instance.fenToName[targetPiece.fenSymbol.lowercase()] ?: "Piece"))
                else Component.text("Move")
                val overlayMat = if (isCapture) Material.RED_STAINED_GLASS_PANE else Material.YELLOW_STAINED_GLASS_PANE
                val overlay = createItem(overlayName, overlayMat, mutableListOf(), 1)
                setOverlayAtSquare(p, target, overlay)
            }
            return
        }

        // If a piece is selected, check if clicked square is a legal target
        val targetMove = legalFromSelected.firstOrNull { it.to == clickedSquare }
        if (targetMove != null) {
            try {
                if(targetMove.promotion != Piece.NONE) {
                    BlockChess.instance.logger.info("${p?.name} promoted to ${targetMove.promotion}")
                }
                val oldPiece = game.board.getPiece(targetMove.to)
                val movingPiece = game.board.getPiece(targetMove.from)
                board.doMove(targetMove)
                if (oldPiece != Piece.NONE && oldPiece.pieceType != null && oldPiece.pieceType.name != "NONE") {
                    if (movingPiece.pieceSide == Side.WHITE && playerIsWhite && p != null) {
                        giveCapturedPiece(p, oldPiece)
                    }
                    else if (movingPiece.pieceSide == Side.BLACK && !playerIsWhite && p != null) {
                        giveCapturedPiece(p, oldPiece)
                    }
                }
            } catch (_: Exception) {
                // ignore if something goes wrong
            }
            selected = null
            legalFromSelected = emptyList()
            game.onMoveMade(targetMove)
            return
        }

        val piece = board.getPiece(clickedSquare)
        if (piece != null && piece.pieceType != null && piece.pieceType.name != "NONE" && piece.pieceSide == sideViewing) {

            draw(p)
            val moves = try { board.legalMoves() } catch (_: Exception) { emptyList() }
            legalFromSelected = moves.filter { it.from == clickedSquare }
            selected = clickedSquare
            for (mv in legalFromSelected) {
                val target = mv.to
                val targetPiece = board.getPiece(target)
                val isCapture = (targetPiece != null && targetPiece.pieceType != null && targetPiece.pieceType.name != "NONE") ||
                        (board.enPassantTarget == target && piece.pieceType?.name == "PAWN")
                val overlayName = if (isCapture) Component.text("Capture " + (BlockChess.instance.fenToName[targetPiece.fenSymbol.lowercase()] ?: "Piece")) else Component.text("Move")
                val overlayMat = if (isCapture) Material.RED_STAINED_GLASS_PANE else Material.YELLOW_STAINED_GLASS_PANE
                val overlay = createItem(overlayName, overlayMat, mutableListOf(), 1)
                setOverlayAtSquare(p, target, overlay)
            }
            return
        }

        // Otherwise, clear selection
        selected = null
        legalFromSelected = emptyList()
        draw(p)
    }

}