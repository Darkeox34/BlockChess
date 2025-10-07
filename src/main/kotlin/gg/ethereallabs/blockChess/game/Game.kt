package gg.ethereallabs.blockChess.game

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.BoardEventType
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveList
import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.config.Config
import gg.ethereallabs.blockChess.engine.UciEngine
import gg.ethereallabs.blockChess.events.ChessListener
import gg.ethereallabs.blockChess.gui.GameGUI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player

class Game {

    val board = Board()

    var white: Player? = null
    var black: Player? = null

    var moveList: MoveList = MoveList()

    var ended = false
        private set

    private var taskId: Int = -1
    var whiteTimeMs: Long = 5 * 60 * 1000
    var blackTimeMs: Long = 5 * 60 * 1000
    private var lastTickMs: Long = System.currentTimeMillis()

    private var guiWhite: GameGUI? = null
    private var guiBlack: GameGUI? = null

    // Engine
    var againstBot: Boolean = false
    private var engineSide: Side? = null
    private var engine: UciEngine? = null
    private var engineThinking: Boolean = false
    private var engineSkill: Int? = null

    fun start(pWhite: Player, pBlack: Player) {
        white = pWhite
        black = pBlack
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        guiWhite = GameGUI(this, true)
        guiBlack = GameGUI(this, false)
        guiWhite?.open(pWhite)
        guiBlack?.open(pBlack)
        board.addEventListener(BoardEventType.ON_MOVE, ChessListener())
        startTimer()
    }

    fun startAgainstBot(human: Player, difficulty: Int, humanIsWhite: Boolean = true) {
        againstBot = true
        engineSkill = difficulty
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")

        if (humanIsWhite) {
            white = human
            engineSide = Side.BLACK
            guiWhite = GameGUI(this, true)
            guiWhite?.open(human)
        } else {
            black = human
            engineSide = Side.WHITE
            guiBlack = GameGUI(this, false)
            guiBlack?.open(human)
        }

        // Start and init engine
        engine = UciEngine(Config.enginePath)
        try {
            engine!!.start()
            engine!!.initLevel(engineSkill)
            human.sendMessage(BlockChess.mm.deserialize("<gray>Stockfish initialized (skill <aqua>${engineSkill}</aqua>)."))
        } catch (ex: Exception) {
            human.sendMessage(BlockChess.mm.deserialize("<red>Impossible to start chess engine: ${ex.message}"))
        }

        startTimer()

        // If engine starts to move, trigger immediately
        if (board.sideToMove == engineSide) {
            triggerEngineMove()
        }
    }

    fun end(){
        val fen = moveList.toString()
        ended = true
        if(fen != null) {
            val message = Component.text("PGN (Left-Click to Copy): ", TextColor.color(0xFFFFFF))
                .append(
                    Component.text(fen, TextColor.color(0x00FF00))
                        .clickEvent(ClickEvent.copyToClipboard(fen))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to copy PGN")))
                )

            white?.sendMessage(message)
            black?.sendMessage(message)
        }
        GameManager.end(this)
    }

    fun stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId)
            taskId = -1
        }
        if (againstBot) {
            try { engine?.stop() } catch (_: Exception) {}
        }
    }

    private fun startTimer() {
        lastTickMs = System.currentTimeMillis()
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(BlockChess.instance, Runnable {
            val now = System.currentTimeMillis()
            val delta = now - lastTickMs
            lastTickMs = now

            if (board.sideToMove == Side.WHITE) {
                whiteTimeMs -= delta
            } else {
                blackTimeMs -= delta
            }

            if (whiteTimeMs <= 0 || blackTimeMs <= 0) {
                val loser = if (whiteTimeMs <= 0) white else black
                val reason = if (loser === white)
                    BlockChess.mm.deserialize("<red>⏰ Time’s up!</red> <gray>Black wins the match.</gray>")
                else
                    BlockChess.mm.deserialize("<red>⏰ Time’s up!</red> <gray>White wins the match.</gray>")
                end()
            } else {
                // Refresh clock display for both
                guiWhite?.updateClock()
                guiBlack?.updateClock()
                // If playing vs bot and it's engine's turn, ensure it thinks
                if (againstBot && board.sideToMove == engineSide && !engineThinking) {
                    triggerEngineMove()
                }
            }
        }, 20L, 20L)
    }

    fun onMoveMade(move: Move) {
        guiWhite?.draw(white)
        guiBlack?.draw(black)

        moveList.add(move)

        if(white != null)
            white?.playSound(white!!.location, Sound.BLOCK_NETHER_WOOD_BREAK, 1f, 1f)
        if(black != null)
            black?.playSound(black!!.location, Sound.BLOCK_NETHER_WOOD_BREAK, 1f, 1f)

        if (board.isStaleMate()) {
            BlockChess.instance.sendMessage(white, "<yellow>⚖️ The match has ended in a <bold>stalemate</bold>!</yellow>")
            BlockChess.instance.sendMessage(black, "<yellow>⚖️ The match has ended in a <bold>stalemate</bold>!</yellow>")
            end()
        }

        if (board.isMated()) {
            if (board.sideToMove == Side.WHITE) {
                BlockChess.instance.sendMessage(white, "<red>♚ Checkmate!</red> <gray>Black wins the match.</gray>")
                BlockChess.instance.sendMessage(black, "<green>♛ Checkmate!</green> <gray>You have defeated your opponent.</gray>")
            } else {
                BlockChess.instance.sendMessage(white, "<green>♛ Checkmate!</green> <gray>You have defeated your opponent.</gray>")
                BlockChess.instance.sendMessage(black, "<red>♚ Checkmate!</red> <gray>White wins the match.</gray>")
            }
            end()
        }


        if (board.isRepetition) {
            BlockChess.instance.sendMessage(white, "<yellow>🔁 The match has ended in a <bold>draw by repetition</bold>.</yellow>")
            BlockChess.instance.sendMessage(black, "<yellow>🔁 The match has ended in a <bold>draw by repetition</bold>.</yellow>")
            end()
        }

        if (board.isInsufficientMaterial()) {
            BlockChess.instance.sendMessage(white, "<yellow>🪶 The match ended in a <bold>draw</bold> due to insufficient material.</yellow>")
            BlockChess.instance.sendMessage(black, "<yellow>🪶 The match ended in a <bold>draw</bold> due to insufficient material.</yellow>")
            end()
        }

        if (board.halfMoveCounter >= 100) {
            BlockChess.instance.sendMessage(white, "<yellow>⏳ The match ended in a <bold>draw</bold> after 100 moves without progress.</yellow>")
            BlockChess.instance.sendMessage(black, "<yellow>⏳ The match ended in a <bold>draw</bold> after 100 moves without progress.</yellow>")
            end()
        }
    }

    private fun triggerEngineMove() {
        if (!againstBot || engine == null || engineThinking) return
        engineThinking = true
        val fen = try { board.fen
        } catch (_: Exception) { board.fen
        }
        val wtime = whiteTimeMs
        val btime = blackTimeMs
        val human = if (engineSide == Side.WHITE) black else white
        if(human != null)
            BlockChess.instance.sendMessage(human,"<yellow>Stockfish <gray>is thinking...")

        Bukkit.getScheduler().runTaskAsynchronously(BlockChess.instance, Runnable {
            try {
                engine!!.positionFen(fen)
                val best: String = try {
                    if ((engineSkill ?: 5) in 1..4) {
                        val movetime = when(engineSkill) {
                            1 -> 50L
                            2 -> 100L
                            3 -> 150L
                            4 -> 200L
                            else -> 200L
                        }
                        engine!!.goBestMoveMovetime(movetime)
                    } else {
                        engine!!.goBestMoveWTimeBTime(wtime, btime, 0, 0)
                    }
                } catch (_: Exception) {
                    val alloc = ((if (board.sideToMove == Side.WHITE) wtime else btime) / 20).coerceIn(100, 2000)
                    engine!!.goBestMoveMovetime(alloc)
                }
                if(human != null)
                    BlockChess.instance.sendMessage(human,"<gray>Stockfish choose: <yellow>${best}</yellow></gray>")

                Bukkit.getScheduler().runTask(BlockChess.instance, Runnable {
                    try {
                        val mv = uciToLegalMove(best)
                        if (mv != null) {
                            board.doMove(mv)
                            onMoveMade(mv)
                        }
                    } catch (_: Exception) {}
                    engineThinking = false
                })
            } catch (_: Exception) {
                engineThinking = false
            }
        })
    }


    private fun uciToLegalMove(uci: String): Move? {
        if (uci.length < 4) return null
        val fromFile = uci[0] - 'a'
        val fromRank = uci[1] - '1'
        val toFile = uci[2] - 'a'
        val toRank = uci[3] - '1'
        val from = com.github.bhlangonijr.chesslib.Square.squareAt(fromRank * 8 + fromFile)
        val to = com.github.bhlangonijr.chesslib.Square.squareAt(toRank * 8 + toFile)
        val promoPiece = if (uci.length >= 5) promoPieceFromChar(uci[4], board.sideToMove) else Piece.NONE
        val legal = try { board.legalMoves() } catch (_: Exception) { emptyList<Move>() }
        return legal.firstOrNull { it.from == from && it.to == to && (promoPiece == Piece.NONE || it.promotion == promoPiece) }
    }

    private fun promoPieceFromChar(c: Char, side: Side): Piece {
        return when (c.lowercaseChar()) {
            'q' -> if (side == Side.WHITE) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN
            'r' -> if (side == Side.WHITE) Piece.WHITE_ROOK else Piece.BLACK_ROOK
            'b' -> if (side == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
            'n' -> if (side == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
            else -> Piece.NONE
        }
    }
}