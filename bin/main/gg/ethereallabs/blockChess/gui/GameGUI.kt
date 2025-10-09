package gg.ethereallabs.blockChess.gui

import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.game.GameResult
import gg.ethereallabs.blockChess.BlockChess
import com.github.bhlangonijr.chesslib.move.Move
import gg.ethereallabs.blockChess.config.Config
import gg.ethereallabs.blockChess.game.Game
import gg.ethereallabs.blockChess.gui.models.BaseMenu
import gg.ethereallabs.blockChess.utils.SyncHelper.runSync
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import java.util.concurrent.CompletableFuture.runAsync

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

    // --- Utility helpers ---
    private fun clearInventories(p: Player?) {
        inv?.clear()
        p?.inventory?.clear()
    }

    private fun renderStaticControls() {
        val clock = getClockItem()
        inv?.setItem(26, clock)

        val surrender = getSurrendItem()
        inv?.setItem(53, surrender)

        val draw = getDrawItem()
        inv?.setItem(44, draw)
    }

    private fun renderCapturedForPerspective(p: Player?) {
        if (playerIsWhite) giveCapturedPieces(p, game.blackEaten) else giveCapturedPieces(p, game.whiteEaten)
    }

    private fun createPieceItem(piece: Piece, square: Square): ItemStack? {
        if (piece.pieceType == null || piece.pieceType.name == "NONE") return null
        val fen = piece.fenSymbol.lowercase()
        val material = if (piece.pieceSide == Side.WHITE)
            BlockChess.whitePiecesByChar[fen]
        else
            BlockChess.blackPiecesByChar[fen]
        if (material == null) return null

        val name = BlockChess.instance.fenToName[fen] ?: "Unknown"
        val item = createItem(Component.text(name), material, mutableListOf(
            Component.text(square.name).decoration(TextDecoration.ITALIC, false),
        ), 1)

        val meta = item.itemMeta
        meta.setCustomModelData(449)

        if (item.type == Material.LEATHER_HORSE_ARMOR) {
            val leatherArmorMeta: LeatherArmorMeta = item.itemMeta as LeatherArmorMeta
            leatherArmorMeta.setColor(null)
        }

        item.setItemMeta(meta)
        return item
    }

    private fun placeItemAtVisual(p: Player?, visualRank: Int, visualFile: Int, item: ItemStack) {
        if (visualRank < 6) {
            val chestSlot = visualRank * 9 + visualFile
            inv?.setItem(chestSlot, item)
        } else {
            val playerInvRow = visualRank - 6
            val playerSlot = playerInvRow * 9 + visualFile + 9
            p?.inventory?.setItem(playerSlot, item)
        }
    }

    private fun renderBoardPieces(p: Player?) {
        val board = game.board
        for (visualRank in 0..7) {
            for (visualFile in 0..7) {
                val boardRank = if (playerIsWhite) 7 - visualRank else visualRank
                val boardFile = if (playerIsWhite) visualFile else 7 - visualFile
                val squareIndex = boardRank * 8 + boardFile
                val square = Square.squareAt(squareIndex)
                val piece = board.getPiece(square)
                if (piece == null) continue
                val item = createPieceItem(piece, square) ?: continue
                placeItemAtVisual(p, visualRank, visualFile, item)
            }
        }
    }

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

    fun giveCapturedPieces(player: Player?, pieces : MutableList<Piece>) {
        val inv = player?.inventory
        pieces.sort()

        val pieceToSlot = hashMapOf(
            "p" to 0,
            "n" to 1,
            "b" to 2,
            "r" to 3,
            "q" to 4
        )

        for (piece in pieces.distinct()) {
            val fen = piece.fenSymbol.lowercase()
            val material = if (piece.pieceSide == Side.WHITE)
                BlockChess.whitePiecesByChar[fen]
            else
                BlockChess.blackPiecesByChar[fen]

            if (material == null) continue

            val amount = pieces.count { it == piece }

            val slot = pieceToSlot[fen]

            if(slot == null) continue

            val name = BlockChess.instance.fenToName[fen] ?: "Unknown"
            val item = createItem(Component.text(name), material, mutableListOf(
                Component.text("Eaten").decoration(TextDecoration.ITALIC, false)
                    .color(NamedTextColor.RED)
            ), amount)

            val meta = item.itemMeta
            meta.setCustomModelData(449)

            item.setItemMeta(meta)
            inv?.setItem(slot, item)
        }
    }

    fun updateClock() {
        val clock = getClockItem()
        inv?.setItem(26, clock)
    }

    override fun draw(p: Player?) {
        clearInventories(p)
        renderStaticControls()
        renderCapturedForPerspective(p)
        renderBoardPieces(p)
    }

    override fun handleClick(p: Player?, slot: Int, e: InventoryClickEvent?) {
        if (game.ended) {
            e?.isCancelled = true
            return
        }

        if (handleControlClicks(slot)) return

        val clickedSquare = squareFromRawSlot(slot) ?: return
        val sideViewing = if (playerIsWhite) Side.WHITE else Side.BLACK
        val board = game.board

        if (selected == null) {
            trySelectPiece(p, board, clickedSquare, sideViewing)
            return
        }

        if (attemptMove(p, clickedSquare)) return

        tryReselectOrReset(p, board, clickedSquare, sideViewing)
    }

    private fun handleControlClicks(slot: Int): Boolean {
        when (slot) {
            53 -> {
                val player = if(playerIsWhite) game.white else game.black

                if(player == null) return true

                SurrendGUI(this,player) { choice ->
                        try {
                            if(choice) {
                                game.finalizeGame(if (playerIsWhite) Game.ResultType.WHITE_RESIGN else Game.ResultType.BLACK_RESIGN)
                            }
                        } catch (_: Exception) {}
                }.open(player)

                return true
            }
            44 -> {

                return true
            }
        }
        return false
    }

    private fun trySelectPiece(p: Player?, board: com.github.bhlangonijr.chesslib.Board, clickedSquare: Square, sideViewing: Side) {
        val piece = board.getPiece(clickedSquare)
        if (piece == null || piece.pieceType == null || piece.pieceType.name == "NONE") return
        if (piece.pieceSide != sideViewing) return
        if (board.sideToMove != sideViewing) return

        computeAndShowLegalMovesFrom(p, board, clickedSquare, piece)
    }

    private fun computeAndShowLegalMovesFrom(p: Player?, board: com.github.bhlangonijr.chesslib.Board, clickedSquare: Square, piece: Piece) {
        runAsync {
            val moves = try { board.legalMoves() } catch (_: Exception) { emptyList() }
            val legalMoves = moves.filter { it.from == clickedSquare }

            Bukkit.getScheduler().runTask(BlockChess.instance, Runnable {
                legalFromSelected = legalMoves
                selected = clickedSquare
                for (mv in legalFromSelected) {
                    val target = mv.to
                    val targetPiece = board.getPiece(target)
                    val isCapture = (targetPiece != null && targetPiece.pieceType != null && targetPiece.pieceType.name != "NONE") ||
                            (board.enPassantTarget == target && piece.pieceType?.name == "PAWN")

                    val overlayName = if (isCapture)
                        Component.text("Capture " + (BlockChess.instance.fenToName[targetPiece.fenSymbol.lowercase()] ?: "Piece"))
                    else Component.text("Move")

                    val overlayMat = if (isCapture) Material.RED_STAINED_GLASS_PANE else Material.YELLOW_STAINED_GLASS_PANE
                    val overlay = createItem(overlayName, overlayMat, mutableListOf(), 1)
                    setOverlayAtSquare(p, target, overlay)
                }
            })
        }
    }

    private fun attemptMove(p: Player?, clickedSquare: Square): Boolean {
        val targetMove = legalFromSelected.firstOrNull { it.to == clickedSquare } ?: return false
        performMoveAsync(p, targetMove)
        return true
    }

    private fun performMoveAsync(p: Player?, targetMove: Move) {
        runAsync {
            try {
                val oldPiece = game.board.getPiece(targetMove.to)

                if (targetMove.promotion != Piece.NONE) {
                    runSync {
                        p?.closeInventory()
                        if(p == null)
                            return@runSync

                        PromotionGUI(this, p) { chosenPiece ->
                            runAsync {
                                try {
                                    val promoMove = Move(targetMove.from, targetMove.to, chosenPiece)
                                    game.board.doMove(promoMove)

                                    runSync {
                                        if (oldPiece != Piece.NONE && oldPiece.pieceType != null && oldPiece.pieceType.name != "NONE") {
                                            when (oldPiece.pieceSide) {
                                                Side.WHITE -> game.whiteEaten.add(oldPiece)
                                                Side.BLACK -> game.blackEaten.add(oldPiece)
                                            }
                                        }
                                        selected = null
                                        legalFromSelected = emptyList()
                                        game.onMoveMade(promoMove)
                                    }
                                } catch (_: Exception) {}
                            }
                        }.open(p)
                    }
                    return@runAsync
                }

                game.board.doMove(targetMove)
                runSync {
                    if (oldPiece != Piece.NONE && oldPiece.pieceType != null && oldPiece.pieceType.name != "NONE") {
                        when (oldPiece.pieceSide) {
                            Side.WHITE -> game.whiteEaten.add(oldPiece)
                            Side.BLACK -> game.blackEaten.add(oldPiece)
                        }
                    }
                    selected = null
                    legalFromSelected = emptyList()
                    game.onMoveMade(targetMove)
                }
            } catch (_: Exception) {}
        }
    }

    private fun tryReselectOrReset(p: Player?, board: com.github.bhlangonijr.chesslib.Board, clickedSquare: Square, sideViewing: Side) {
        val piece = board.getPiece(clickedSquare)
        if (piece != null && piece.pieceType != null && piece.pieceType.name != "NONE" && piece.pieceSide == sideViewing) {
            draw(p)
            computeAndShowLegalMovesFrom(p, board, clickedSquare, piece)
            return
        }
        selected = null
        legalFromSelected = emptyList()
        draw(p)
    }

}