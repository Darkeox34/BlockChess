package gg.ethereallabs.blockChess.game

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.gui.GameGUI
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class Game {

    val board = Board()

    var white: Player? = null
    var black: Player? = null

    private var taskId: Int = -1
    var whiteTimeMs: Long = 5 * 60 * 1000
    var blackTimeMs: Long = 5 * 60 * 1000
    private var lastTickMs: Long = System.currentTimeMillis()

    private var guiWhite: GameGUI? = null
    private var guiBlack: GameGUI? = null

    fun start(pWhite: Player, pBlack: Player) {
        white = pWhite
        black = pBlack
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        guiWhite = GameGUI(this, true)
        guiBlack = GameGUI(this, false)
        guiWhite?.open(pWhite)
        guiBlack?.open(pBlack)
        startTimer()
    }

    fun stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId)
            taskId = -1
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
                    BlockChess.mm.deserialize("<red>Tempo scaduto: vince il Nero.")
                else
                    BlockChess.mm.deserialize("<red>Tempo scaduto: vince il Bianco.")
                GameManager.end(this, reason)
            } else {
                // Refresh clock display for both
                guiWhite?.draw(white)
                guiBlack?.draw(black)
            }
        }, 20L, 20L)
    }

    fun onMoveMade() {
        // Switch side automatically handled by board
        // No increment (5+0), timer continues for the next side
        // Redraw both UIs after move
        guiWhite?.draw(white)
        guiBlack?.draw(black)
    }
}