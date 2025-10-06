package gg.ethereallabs.blockChess.engine

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.concurrent.LinkedBlockingQueue
import org.bukkit.Bukkit

class UciEngine(private val executablePath: String) {
    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null
    private val lines = LinkedBlockingQueue<String>()

    fun start() {
        val pb = ProcessBuilder(executablePath)
        pb.redirectErrorStream(true)
        process = pb.start()
        writer = OutputStreamWriter(process!!.outputStream, StandardCharsets.UTF_8)
        reader = BufferedReader(InputStreamReader(process!!.inputStream, StandardCharsets.UTF_8))
        Thread {
            try {
                while (true) {
                    val line = reader!!.readLine() ?: break
                    lines.put(line)
                }
            } catch (_: Exception) {}
        }.apply { isDaemon = true }.start()
    }

    fun stop() {
        try {
            send("quit")
        } catch (_: Exception) {}
        process?.destroy()
    }

    fun send(cmd: String) {
        writer?.apply {
            write(cmd)
            write("\n")
            flush()
        }
    }

    fun initLevel(enemyIndex: Int?) {
        send("uci")
        waitFor("uciok", 5000)

        send("setoption name UCI_LimitStrength value false")

        when (enemyIndex) {
            1 -> {
                send("setoption name UCI_LimitStrength value true")
                send("setoption name UCI_Elo value 200")
            }
            2 -> {
                send("setoption name UCI_LimitStrength value true")
                send("setoption name UCI_Elo value 500")
            }
            3 -> {
                send("setoption name UCI_LimitStrength value true")
                send("setoption name UCI_Elo value 820")
            }
            4 -> {
                send("setoption name UCI_LimitStrength value true")
                send("setoption name UCI_Elo value 1060")
            }
            5 -> {
                send("setoption name UCI_LimitStrength value true")
                send("setoption name UCI_Elo value 1350")
            }
            6 -> {
                send("setoption name UCI_LimitStrength value true")
                send("setoption name UCI_Elo value 1600")
            }
            7 -> {
                send("setoption name UCI_LimitStrength value true")
                send("setoption name UCI_Elo value 1900")
            }
            8 -> {
                send("setoption name UCI_LimitStrength value true")
                send("setoption name UCI_Elo value 2200")
            }
            9 -> {
                send("setoption name UCI_LimitStrength value true")
                send("setoption name UCI_Elo value 2500")
            }
            10 -> {
                send("setoption name UCI_LimitStrength value true")
                send("setoption name UCI_Elo value 2800")
            }
            11 -> {
                send("setoption name UCI_LimitStrength value true")
                send("setoption name UCI_Elo value 3050")
            }
            12 -> {
                send("setoption name UCI_LimitStrength value false")
            }
        }

        send("isready")
        waitFor("readyok", 5000)
        send("ucinewgame")
    }

    fun init(skillLevel: Int?) {
        send("uci")
        waitFor("uciok", 5000)
        if (skillLevel != null) {
            send("setoption name Skill_Level value ${skillLevel}")
        }
        send("isready")
        waitFor("readyok", 5000)
        send("ucinewgame")
    }

    fun positionFen(fen: String) {
        send("position fen $fen")
    }

    fun goBestMoveWTimeBTime(wtimeMs: Long, btimeMs: Long, wincMs: Long = 0, bincMs: Long = 0): String {
        send("go wtime $wtimeMs btime $btimeMs winc $wincMs binc $bincMs")
        return waitBestMove()
    }

    fun goBestMoveMovetime(movetimeMs: Long): String {
        send("go movetime $movetimeMs")
        return waitBestMove()
    }

    private fun waitBestMove(timeoutMs: Long = 30000): String {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val line = lines.poll() ?: continue
            if (line.startsWith("bestmove")) {
                val bm = line.split(" ")[1]
                return bm
            }
        }
        throw RuntimeException("UCI engine timeout waiting for bestmove")
    }

    private fun waitFor(token: String, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val line = lines.poll() ?: continue
            if (line.contains(token)) return true
        }
        return false
    }
}