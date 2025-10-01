package gg.ethereallabs.blockChess.gui.models

import gg.ethereallabs.blockChess.BlockChess
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiConsumer

class ChatPrompts : Listener {
    @JvmRecord
    private data class Flow(val step: BiConsumer<Player?, String?>?) {
        fun execute(player: Player?, message: String?) {
            if (message != null) {
                step!!.accept(player, message)
            }
        }
    }

    private val pending: MutableMap<UUID?, Flow?> = ConcurrentHashMap<UUID?, Flow?>()

    fun ask(player: Player, question: String?, onAnswer: BiConsumer<Player?, String?>?) {
        player.closeInventory()
        player.sendMessage(BlockChess.mm.deserialize("<aqua>» $question <gray>(cancel: <red>!cancel<gray>)"))
        pending.put(player.uniqueId, Flow(onAnswer))
    }

    fun cancel(player: Player) {
        pending.remove(player.uniqueId)
        player.sendMessage(BlockChess.mm.deserialize("<gray>Operation cancelled."))
    }

    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        val player = event.getPlayer()
        val flow = pending[player.uniqueId]
        if (flow == null) return

        event.isCancelled = true

        val msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim { it <= ' ' }

        if (msg.equals("!cancel", ignoreCase = true)) {
            cancel(player)
            return
        }

        pending.remove(player.uniqueId)
        Bukkit.getScheduler().runTask(BlockChess.instance, Runnable { flow.execute(player, msg) })
    }

    companion object {
        val instance: ChatPrompts = ChatPrompts()
    }
}
