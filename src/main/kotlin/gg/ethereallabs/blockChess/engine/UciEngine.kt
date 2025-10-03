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