package gg.ethereallabs.blockChess.game

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.gui.GameGUI
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object GameManager {

    private val pendingInvites: MutableMap<UUID, Invitation> = ConcurrentHashMap()
    private val activeGamesByPlayer: MutableMap<UUID, Game> = ConcurrentHashMap()

    fun getGame(p: Player): Game? = activeGamesByPlayer[p.uniqueId]

    fun invite(inviter: Player, invitee: Player) {
        if (inviter.uniqueId == invitee.uniqueId) {
            inviter.sendMessage(BlockChess.mm.deserialize("<red>Non puoi invitare te stesso."))
            return
        }

        if (activeGamesByPlayer.containsKey(inviter.uniqueId) || activeGamesByPlayer.containsKey(invitee.uniqueId)) {
            inviter.sendMessage(BlockChess.mm.deserialize("<red>Tu o il giocatore invitato siete già in partita."))
            return
        }

        val inv = Invitation(inviter.uniqueId, invitee.uniqueId)
        pendingInvites[invitee.uniqueId] = inv

        inviter.sendMessage(BlockChess.mm.deserialize("<gray>Invito inviato a <yellow>${invitee.name}</yellow>."))
        invitee.sendMessage(BlockChess.mm.deserialize("<yellow>${inviter.name}</yellow> ti ha invitato a giocare a scacchi (5+0)."))
        invitee.sendMessage(BlockChess.mm.deserialize("<gray>Usa <aqua>/chess accept ${inviter.name}</aqua> per accettare, <red>/chess decline ${inviter.name}</red> per rifiutare."))

        // Auto-expire
        Bukkit.getScheduler().runTaskLater(BlockChess.instance, Runnable {
            val current = pendingInvites[invitee.uniqueId]
            if (current != null && current.inviter == inviter.uniqueId) {
                pendingInvites.remove(invitee.uniqueId)
                inviter.sendMessage(BlockChess.mm.deserialize("<gray>L'invito a <yellow>${invitee.name}</yellow> è scaduto."))
                invitee.sendMessage(BlockChess.mm.deserialize("<gray>L'invito di <yellow>${inviter.name}</yellow> è scaduto."))
            }
        }, 20L * 60)
    }

    fun accept(invitee: Player, inviterName: String): Boolean {
        val inviter = Bukkit.getPlayerExact(inviterName) ?: run {
            invitee.sendMessage(BlockChess.mm.deserialize("<red>Giocatore non online: ${inviterName}"))
            return false
        }

        val inv = pendingInvites[invitee.uniqueId]
        if (inv == null || inv.inviter != inviter.uniqueId) {
            invitee.sendMessage(BlockChess.mm.deserialize("<red>Nessun invito valido da ${inviter.name}."))
            return false
        }

        pendingInvites.remove(invitee.uniqueId)

        val game = Game()
        // White will be the inviter by default
        game.start(inviter, invitee)
        activeGamesByPlayer[inviter.uniqueId] = game
        activeGamesByPlayer[invitee.uniqueId] = game

        inviter.sendMessage(BlockChess.mm.deserialize("<gray>Partita avviata contro <yellow>${invitee.name}</yellow>. Bianco: <white>${inviter.name}</white>"))
        invitee.sendMessage(BlockChess.mm.deserialize("<gray>Partita avviata contro <yellow>${inviter.name}</yellow>. Nero: <dark_gray>${invitee.name}</dark_gray>"))
        return true
    }

    fun decline(invitee: Player, inviterName: String): Boolean {
        val inviter = Bukkit.getPlayerExact(inviterName) ?: return false
        val inv = pendingInvites[invitee.uniqueId]
        if (inv == null || inv.inviter != inviter.uniqueId) return false
        pendingInvites.remove(invitee.uniqueId)
        inviter.sendMessage(BlockChess.mm.deserialize("<gray>${invitee.name} ha rifiutato l'invito."))
        invitee.sendMessage(BlockChess.mm.deserialize("<gray>Hai rifiutato l'invito di ${inviter.name}."))
        return true
    }

    fun end(game: Game, reason: Component) {
        val white = game.white
        val black = game.black
        game.stop()
        if (white != null) {
            activeGamesByPlayer.remove(white.uniqueId)
            white.sendMessage(reason)
        }
        if (black != null) {
            activeGamesByPlayer.remove(black.uniqueId)
            black.sendMessage(reason)
        }
    }
}