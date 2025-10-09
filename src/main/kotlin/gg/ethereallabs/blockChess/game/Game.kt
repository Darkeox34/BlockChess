package gg.ethereallabs.blockChess.game

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveList
import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.config.Config
import gg.ethereallabs.blockChess.data.LocalStorage
import gg.ethereallabs.blockChess.elo.EloManager
import gg.ethereallabs.blockChess.elo.PlayerData
import gg.ethereallabs.blockChess.engine.UciEngine
import gg.ethereallabs.blockChess.events.ChessListener
import gg.ethereallabs.blockChess.gui.GameGUI
import gg.ethereallabs.blockChess.utils.SyncHelper.runSync
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture.runAsync

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

    private var matchResult : ResultType? = null

    enum class ResultType {
        WHITE_WIN, BLACK_WIN,
        DRAW_STALEMATE, DRAW_REPETITION, DRAW_INSUFFICIENT, DRAW_100MOVES,
        TIMEOUT_WHITE, TIMEOUT_BLACK,
        WHITE_RESIGN, BLACK_RESIGN
    }

    // ─────────────────────────────────────────────
    //               GAME START METHODS
    // ─────────────────────────────────────────────
    fun start(pWhite: Player, pBlack: Player) {
        setupBoard()
        white = pWhite
        black = pBlack
        guiWhite = GameGUI(this, true)
        guiBlack = GameGUI(this, false)
        guiWhite?.open(pWhite)
        guiBlack?.open(pBlack)
        board.addEventListener(BoardEventType.ON_MOVE, ChessListener())
        startTimer()
    }

    fun startAgainstBot(human: Player, difficulty: Int, humanIsWhite: Boolean = true) {
        setupBoard()
        againstBot = true
        engineSkill = difficulty
        engineSide = if (humanIsWhite) Side.BLACK else Side.WHITE

        if (humanIsWhite) {
            white = human
            guiWhite = GameGUI(this, true)
            guiWhite?.open(human)
        } else {
            black = human
            guiBlack = GameGUI(this, false)
            guiBlack?.open(human)
        }

        // Start engine
        engine = UciEngine(Config.enginePath).apply {
            try {
                start()
                initLevel(engineSkill)
                human.sendMessage(BlockChess.mm.deserialize("<gray>Stockfish initialized (skill <aqua>$engineSkill</aqua>)."))
            } catch (ex: Exception) {
                human.sendMessage(BlockChess.mm.deserialize("<red>Impossible to start chess engine: ${ex.message}"))
            }
        }

        startTimer()

        if (board.sideToMove == engineSide) triggerEngineMove()
    }

    private fun setupBoard() {
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    }

    // ─────────────────────────────────────────────
    //                  GAME END
    // ─────────────────────────────────────────────
    fun finalizeGame(result: ResultType) {
        if (ended) return
        ended = true
        stop()

        matchResult = result

        sendPGN()

        if (againstBot) {
            handleBotResult(result)
        } else {
            handlePlayerResult(result)
        }

        GameManager.end(this)
    }

    private fun sendPGN() {
        val pgn = moveList.toString()
        val pgnMsg = Component.text("PGN (Click to copy): ", NamedTextColor.GRAY)
            .append(
                Component.text(pgn, NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.copyToClipboard(pgn))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to copy PGN")))
            )
        white?.sendMessage(pgnMsg)
        black?.sendMessage(pgnMsg)
    }

    private fun handlePlayerResult(result: ResultType) {
        val w = white ?: return
        val b = black ?: return

        val wData = EloManager.players.getOrPut(w.uniqueId) { PlayerData() }
        val bData = EloManager.players.getOrPut(b.uniqueId) { PlayerData() }

        when (result) {
            ResultType.WHITE_WIN -> announceResult(w, b, wData, bData, 1.0, 0.0, "🏆", "❌", "by Checkmate")
            ResultType.BLACK_WIN -> announceResult(b, w, wData, bData, 0.0, 1.0, "🏆", "❌", "by Checkmate")
            ResultType.TIMEOUT_BLACK -> announceResult(w, b, wData, bData, 1.0, 0.0, "🏆", "❌", "by time")
            ResultType.TIMEOUT_WHITE -> announceResult(b, w, bData, wData, 1.0, 0.0, "🏆", "❌", "by time")
            ResultType.WHITE_RESIGN -> announceResult(b, w, bData, wData, 1.0, 0.0, "🏆", "❌", "by resignation")
            ResultType.BLACK_RESIGN -> announceResult(w, b, wData, bData, 1.0, 0.0, "🏆", "❌", "by resignation")
            ResultType.DRAW_STALEMATE -> announceDraw(w, b, wData, bData, "⚖", "by stalemate")
            ResultType.DRAW_REPETITION -> announceDraw(w, b, wData, bData, "🔁", "by repetition")
            ResultType.DRAW_INSUFFICIENT -> announceDraw(w, b, wData, bData, "🪶", "by insufficient material")
            ResultType.DRAW_100MOVES -> announceDraw(w, b, wData, bData, "⏳", "by 50 move rule")
        }

        LocalStorage.savePlayerData(w)
        LocalStorage.savePlayerData(b)
    }

    private fun handleBotResult(result: ResultType) {
        val human = white ?: black ?: return
        val humanIsWhite = white == human
        val winnerIsWhite = when (result) {
            ResultType.WHITE_WIN, ResultType.TIMEOUT_BLACK, ResultType.BLACK_RESIGN -> true
            ResultType.BLACK_WIN, ResultType.TIMEOUT_WHITE, ResultType.WHITE_RESIGN -> false
            else -> null
        }

        val (winner, loser) = when (winnerIsWhite) {
            true -> Pair(humanIsWhite, !humanIsWhite)
            false -> Pair(!humanIsWhite, humanIsWhite)
            null -> Pair(null, null)
        }

        when (result) {
            ResultType.WHITE_WIN, ResultType.BLACK_WIN -> sendHumanGameMessage(human, true, "🏆", "You Won")
            ResultType.TIMEOUT_WHITE, ResultType.TIMEOUT_BLACK -> sendHumanGameMessage(human, winnerIsWhite == humanIsWhite, "⏳", "by time")
            ResultType.WHITE_RESIGN, ResultType.BLACK_RESIGN -> sendHumanGameMessage(human, winnerIsWhite == humanIsWhite, "❌", "by resignation")
            ResultType.DRAW_STALEMATE -> sendDrawMessage(human, "⚖", "by stalemate")
            ResultType.DRAW_REPETITION -> sendDrawMessage(human, "🔁", "by repetition")
            ResultType.DRAW_INSUFFICIENT -> sendDrawMessage(human, "🪶", "by insufficient material")
            ResultType.DRAW_100MOVES -> sendDrawMessage(human, "⏳", "by 50 move rule")
        }
    }

    private fun sendHumanGameMessage(human: Player, won: Boolean, emoji: String, reason: String) {
        if (won)
            BlockChess.instance.sendMessage("<bold><#f3fa6b>$emoji</bold> You won against <#97ff82>Stockfish <gray>($reason)", human)
        else
            BlockChess.instance.sendMessage("<bold><red>$emoji</bold> You lost against <#97ff82>Stockfish <gray>($reason)", human)
    }

    private fun sendDrawMessage(human: Player, emoji: String, reason: String) {
        human.sendMessage(BlockChess.mm.deserialize("<yellow>$emoji $reason</yellow>"))
    }

    // ─────────────────────────────────────────────
    //             ELO / MESSAGE HELPERS
    // ─────────────────────────────────────────────
    private fun announceResult(
        winner: Player,
        loser: Player,
        wData: PlayerData,
        lData: PlayerData,
        wScore: Double,
        lScore: Double,
        wEmoji: String,
        lEmoji: String,
        cause: String
    ) {
        val wGain = EloManager.updateElo(wData, lData, wScore)
        val bGain = EloManager.updateElo(lData, wData, lScore)
        wData.wins++
        lData.losses++
        val wGainStr = if (wGain >= 0) "+$wGain" else "$wGain"
        val bGainStr = if (bGain >= 0) "+$bGain" else "$bGain"
        val wName = EloManager.getChessistName(winner)
        val lName = EloManager.getChessistName(loser)
        val wElo = wData.rating
        val lElo = lData.rating
        BlockChess.instance.sendMessage("<bold><#f3fa6b>$wEmoji</bold> $wName <#faff9c>($wElo) <#97ff82>$wGainStr <gray>- $lName <#faff9c>($lElo)<red> $bGainStr", winner)
        BlockChess.instance.sendMessage("<bold><red>$lEmoji</bold> $lName <#faff9c>($lElo) <red>$bGainStr <gray>- $wName <#faff9c>($wElo)<#97ff82> $wGainStr", loser)
        BlockChess.instance.sendMessage("<bold>You Won", winner)
        BlockChess.instance.sendMessage("<bold>You Lost", loser)
        BlockChess.instance.sendMessage("<gray>$cause", winner, loser)
    }

    private fun announceDraw(w: Player, b: Player, wData: PlayerData, bData: PlayerData, emoji: String, cause: String) {
        val wGain = EloManager.updateElo(wData, bData, 0.5)
        val bGain = EloManager.updateElo(bData, wData, 0.5)
        wData.draws++
        bData.draws++

        val wName = EloManager.getChessistName(w)
        val bName = EloManager.getChessistName(b)
        val wElo = wData.rating
        val bElo = bData.rating
        val wGainStr = if (wGain >= 0) "+$wGain" else "$wGain"
        val bGainStr = if (bGain >= 0) "+$bGain" else "$bGain"

        BlockChess.instance.sendMessage("<#f3fa6b>$emoji $wName <#faff9c>($wElo) <gray>$wGainStr - $bName <#faff9c>($bElo) <gray>$bGainStr", w)
        BlockChess.instance.sendMessage("<#f3fa6b>$emoji $bName <#faff9c>($bElo) <gray>$bGainStr - $wName <#faff9c>($wElo) <gray>$wGainStr", b)
        BlockChess.instance.sendMessage("<gray>$cause", w, b)
    }

    // ─────────────────────────────────────────────
    //              TIMER & MOVE HANDLING
    // ─────────────────────────────────────────────
    fun stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId)
        taskId = -1
        if (againstBot) try { engine?.stop() } catch (_: Exception) {}
    }

    private fun startTimer() {
        lastTickMs = System.currentTimeMillis()
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(BlockChess.instance, Runnable {
            val now = System.currentTimeMillis()
            val delta = now - lastTickMs
            lastTickMs = now

            if (board.sideToMove == Side.WHITE) whiteTimeMs -= delta else blackTimeMs -= delta

            if (whiteTimeMs <= 0 || blackTimeMs <= 0) {
                finalizeGame(if (whiteTimeMs <= 0) ResultType.TIMEOUT_WHITE else ResultType.TIMEOUT_BLACK)
            } else {
                guiWhite?.updateClock()
                guiBlack?.updateClock()
                if (againstBot && board.sideToMove == engineSide && !engineThinking) triggerEngineMove()
            }
        }, 20L, 20L)
    }

    fun onMoveMade(move: Move) {
        guiWhite?.draw(white)
        guiBlack?.draw(black)
        moveList.add(move)

        var sound = Sound.BLOCK_NETHER_WOOD_BREAK

        if(board.isKingAttacked){
            sound = Sound.UI_BUTTON_CLICK
        }

        listOfNotNull(white, black).forEach {
            it.playSound(it.location, sound, 1f, 1f)
        }

        when {
            board.isStaleMate -> finalizeGame(ResultType.DRAW_STALEMATE)
            board.isMated -> finalizeGame(if (board.sideToMove == Side.WHITE) ResultType.BLACK_WIN else ResultType.WHITE_WIN)
            board.isRepetition -> finalizeGame(ResultType.DRAW_REPETITION)
            board.isInsufficientMaterial -> finalizeGame(ResultType.DRAW_INSUFFICIENT)
            board.halfMoveCounter >= 100 -> finalizeGame(ResultType.DRAW_100MOVES)
        }
    }

    // ─────────────────────────────────────────────
    //                 ENGINE LOGIC
    // ─────────────────────────────────────────────
    private fun triggerEngineMove() {
        if (!againstBot || engine == null || engineThinking) return
        engineThinking = true
        val fen = board.fen
        val wtime = whiteTimeMs
        val btime = blackTimeMs
        val human = if (engineSide == Side.WHITE) black else white
        human?.let { BlockChess.instance.sendMessage("<yellow>Stockfish <gray>is thinking...", it) }

        engine!!.positionFen(fen)

        val engineCallback: (String?) -> Unit = { bestMove ->
            runSync{
                try {
                    uciToLegalMove(bestMove)?.let {
                        board.doMove(it)
                        onMoveMade(it)
                    }
                } catch (_: Exception) {}
                engineThinking = false
            }
        }

        val skill = engineSkill ?: 5

        runAsync {
            try {
                if (skill in 1..4) {
                    val movetime = (skill * 50L).coerceIn(50, 200)
                    engine!!.goBestMoveMovetime(movetime, engineCallback)
                } else {
                    engine!!.goBestMoveWTimeBTime(wtime, btime, 0, 0, engineCallback)
                }
            } catch (_: Exception) {
                val alloc = ((if (board.sideToMove == Side.WHITE) wtime else btime) / 20).coerceIn(100, 2000)
                engine!!.goBestMoveMovetime(alloc, engineCallback)
            }
        }
    }


    private fun uciToLegalMove(uci: String?): Move? {
        if (uci.isNullOrBlank() || uci.length < 4) return null
        val from = Square.squareAt((uci[1] - '1') * 8 + (uci[0] - 'a'))
        val to = Square.squareAt((uci[3] - '1') * 8 + (uci[2] - 'a'))
        val promo = if (uci.length >= 5) promoPieceFromChar(uci[4], board.sideToMove) else Piece.NONE

        return board.legalMoves().firstOrNull { it.from == from && it.to == to && (promo == Piece.NONE || it.promotion == promo) }
    }

    private fun promoPieceFromChar(c: Char, side: Side): Piece = when (c.lowercaseChar()) {
        'q' -> if (side == Side.WHITE) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN
        'r' -> if (side == Side.WHITE) Piece.WHITE_ROOK else Piece.BLACK_ROOK
        'b' -> if (side == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
        'n' -> if (side == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
        else -> Piece.NONE
    }
}
