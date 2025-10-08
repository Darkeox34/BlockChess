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
import gg.ethereallabs.blockChess.elo.EloManager
import gg.ethereallabs.blockChess.elo.PlayerData
import gg.ethereallabs.blockChess.data.LocalStorage
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

    private enum class ResultType {
        WHITE_WIN,
        BLACK_WIN,
        DRAW_STALEMATE,
        DRAW_REPETITION,
        DRAW_INSUFFICIENT,
        DRAW_100MOVES,
        TIMEOUT_WHITE,
        TIMEOUT_BLACK,
        MANUAL_END
    }

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

    fun end() {
        // Manual end: gracefully stop without Elo updates
        finalizeGame(ResultType.MANUAL_END)
    }

    private fun finalizeGame(result: ResultType) {
        if (ended) return
        ended = true

        // Stop timers and engine
        stop()

        // Build and send PGN (actually move list) to both players
        val pgn = moveList.toString()
        val pgnMsg = Component.text("PGN (Click to copy): ", TextColor.color(0xFFFFFF))
            .append(
                Component.text(pgn, TextColor.color(0x00FF00))
                    .clickEvent(ClickEvent.copyToClipboard(pgn))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to copy PGN")))
            )
        white?.sendMessage(pgnMsg)
        black?.sendMessage(pgnMsg)

        // Compose end messages and Elo updates (skip Elo if vs bot)
        if (!againstBot && white != null && black != null) {
            val w = white!!
            val b = black!!
            val wData = EloManager.players[w.uniqueId] ?: PlayerData().also { EloManager.players[w.uniqueId] = it }
            val bData = EloManager.players[b.uniqueId] ?: PlayerData().also { EloManager.players[b.uniqueId] = it }

            when (result) {
                ResultType.WHITE_WIN, ResultType.TIMEOUT_BLACK -> {
                    EloManager.updateEloWithDifference(wData, bData, 1.0)
                    EloManager.updateEloWithDifference(bData, wData, 0.0)
                    wData.wins += 1
                    bData.losses += 1
                    val winnerName = EloManager.getChessistName(w)
                    val loserName = EloManager.getChessistName(b)
                    w.sendMessage(BlockChess.mm.deserialize("<green>🏆 Victory!</green> You defeated ").append(loserName!!))
                    b.sendMessage(BlockChess.mm.deserialize("<red>❌ Defeat!</red> ").append(winnerName!!).append(BlockChess.mm.deserialize(" has won the match.")))
                }
                ResultType.BLACK_WIN, ResultType.TIMEOUT_WHITE -> {
                    EloManager.updateEloWithDifference(bData, wData, 1.0)
                    EloManager.updateEloWithDifference(wData, bData, 0.0)
                    bData.wins += 1
                    wData.losses += 1
                    val winnerName = EloManager.getChessistName(b)
                    val loserName = EloManager.getChessistName(w)
                    b.sendMessage(BlockChess.mm.deserialize("<green>🏆 Victory!</green> You defeated ").append(loserName!!))
                    w.sendMessage(BlockChess.mm.deserialize("<red>❌ Defeat!</red> ").append(winnerName!!).append(BlockChess.mm.deserialize(" has won the match.")))
                }
                ResultType.DRAW_STALEMATE -> {
                    EloManager.updateEloWithDifference(wData, bData, 0.5)
                    EloManager.updateEloWithDifference(bData, wData, 0.5)
                    wData.draws += 1
                    bData.draws += 1
                    w.sendMessage(BlockChess.mm.deserialize("<yellow>⚖️ Patta per stallo.</yellow>"))
                    b.sendMessage(BlockChess.mm.deserialize("<yellow>⚖️ Patta per stallo.</yellow>"))
                }
                ResultType.DRAW_REPETITION -> {
                    EloManager.updateEloWithDifference(wData, bData, 0.5)
                    EloManager.updateEloWithDifference(bData, wData, 0.5)
                    wData.draws += 1
                    bData.draws += 1
                    w.sendMessage(BlockChess.mm.deserialize("<yellow>🔁 Patta per ripetizione.</yellow>"))
                    b.sendMessage(BlockChess.mm.deserialize("<yellow>🔁 Patta per ripetizione.</yellow>"))
                }
                ResultType.DRAW_INSUFFICIENT -> {
                    EloManager.updateEloWithDifference(wData, bData, 0.5)
                    EloManager.updateEloWithDifference(bData, wData, 0.5)
                    wData.draws += 1
                    bData.draws += 1
                    w.sendMessage(BlockChess.mm.deserialize("<yellow>🪶 Patta per materiale insufficiente.</yellow>"))
                    b.sendMessage(BlockChess.mm.deserialize("<yellow>🪶 Patta per materiale insufficiente.</yellow>"))
                }
                ResultType.DRAW_100MOVES -> {
                    EloManager.updateEloWithDifference(wData, bData, 0.5)
                    EloManager.updateEloWithDifference(bData, wData, 0.5)
                    wData.draws += 1
                    bData.draws += 1
                    w.sendMessage(BlockChess.mm.deserialize("<yellow>⏳ Patta dopo 100 mosse senza progresso.</yellow>"))
                    b.sendMessage(BlockChess.mm.deserialize("<yellow>⏳ Patta dopo 100 mosse senza progresso.</yellow>"))
                }
                ResultType.MANUAL_END -> {
                    w.sendMessage(BlockChess.mm.deserialize("<gray>Partita terminata.</gray>"))
                    b.sendMessage(BlockChess.mm.deserialize("<gray>Partita terminata.</gray>"))
                }
            }

            LocalStorage.savePlayerData(w)
            LocalStorage.savePlayerData(b)
        } else {
            when (result) {
                ResultType.WHITE_WIN -> black?.sendMessage(BlockChess.mm.deserialize("<gray>Il bot ha perso contro ").append(EloManager.getChessistName(white!!)!!))
                ResultType.BLACK_WIN -> white?.sendMessage(BlockChess.mm.deserialize("<gray>Il bot ha perso contro ").append(EloManager.getChessistName(black!!)!!))
                ResultType.TIMEOUT_WHITE -> black?.sendMessage(BlockChess.mm.deserialize("<gray>Tempo scaduto per ").append(EloManager.getChessistName(white!!)!!))
                ResultType.TIMEOUT_BLACK -> white?.sendMessage(BlockChess.mm.deserialize("<gray>Tempo scaduto per ").append(EloManager.getChessistName(black!!)!!))
                ResultType.DRAW_STALEMATE -> {
                    white?.sendMessage(BlockChess.mm.deserialize("<yellow>⚖️ Patta per stallo.</yellow>"))
                    black?.sendMessage(BlockChess.mm.deserialize("<yellow>⚖️ Patta per stallo.</yellow>"))
                }
                ResultType.DRAW_REPETITION -> {
                    white?.sendMessage(BlockChess.mm.deserialize("<yellow>🔁 Patta per ripetizione.</yellow>"))
                    black?.sendMessage(BlockChess.mm.deserialize("<yellow>🔁 Patta per ripetizione.</yellow>"))
                }
                ResultType.DRAW_INSUFFICIENT -> {
                    white?.sendMessage(BlockChess.mm.deserialize("<yellow>🪶 Patta per materiale insufficiente.</yellow>"))
                    black?.sendMessage(BlockChess.mm.deserialize("<yellow>🪶 Patta per materiale insufficiente.</yellow>"))
                }
                ResultType.DRAW_100MOVES -> {
                    white?.sendMessage(BlockChess.mm.deserialize("<yellow>⏳ Patta dopo 100 mosse senza progresso.</yellow>"))
                    black?.sendMessage(BlockChess.mm.deserialize("<yellow>⏳ Patta dopo 100 mosse senza progresso.</yellow>"))
                }
                ResultType.MANUAL_END -> {
                    white?.sendMessage(BlockChess.mm.deserialize("<gray>Partita terminata.</gray>"))
                    black?.sendMessage(BlockChess.mm.deserialize("<gray>Partita terminata.</gray>"))
                }
            }
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
                val outcome = if (whiteTimeMs <= 0) ResultType.TIMEOUT_WHITE else ResultType.TIMEOUT_BLACK
                finalizeGame(outcome)
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
            finalizeGame(ResultType.DRAW_STALEMATE)
            return
        }

        if (board.isMated()) {
            val outcome = if (board.sideToMove == Side.WHITE) ResultType.BLACK_WIN else ResultType.WHITE_WIN
            finalizeGame(outcome)
            return
        }


        if (board.isRepetition) {
            finalizeGame(ResultType.DRAW_REPETITION)
            return
        }

        if (board.isInsufficientMaterial()) {
            finalizeGame(ResultType.DRAW_INSUFFICIENT)
            return
        }

        if (board.halfMoveCounter >= 100) {
            finalizeGame(ResultType.DRAW_100MOVES)
            return
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