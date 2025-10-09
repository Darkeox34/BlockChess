package gg.ethereallabs.blockChess.utils

import gg.ethereallabs.blockChess.BlockChess
import org.bukkit.Bukkit

object SyncHelper {
    fun runSync(block: () -> Unit) {
        Bukkit.getScheduler().runTask(BlockChess.instance, Runnable { block() })
    }
}