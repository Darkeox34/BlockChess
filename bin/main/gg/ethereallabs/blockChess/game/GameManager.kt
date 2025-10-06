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
            inviter.sendMessage(BlockChess.mm.deserialize("<red>You can't invite yourself."))
            return
        }

        if (activeGamesByPlayer.containsKey(inviter.uniqueId) || activeGamesByPlayer.containsKey(invitee.uniqueId)) {
            inviter.sendMessage(BlockChess.mm.deserialize("<red>You or the invited player are already playing a match."))
            return
        }

        val inv = Invitation(inviter.uniqueId, invitee.uniqueId)
        pendingInvites[invitee.uniqueId] = inv

        inviter.sendMessage(BlockChess.mm.deserialize("<gray>Invite sent to <yellow>${invitee.name}</yellow>."))
        invitee.sendMessage(BlockChess.mm.deserialize("<yellow>${inviter.name}</yellow> invited you to play chess (5+0)."))
        invitee.sendMessage(BlockChess.mm.deserialize("<gray>Use <aqua>/chess accept ${inviter.name}</aqua> to accept, <red>/chess decline ${inviter.name}</red> to decline."))

        // Auto-expire
        Bukkit.getScheduler().runTaskLater(BlockChess.instance, Runnable {
            val current = pendingInvites[invitee.uniqueId]
            if (current != null && current.inviter == inviter.uniqueId) {
                pendingInvites.remove(invitee.uniqueId)
                inviter.sendMessage(BlockChess.mm.deserialize("<gray>The invite to <yellow>${invitee.name}</yellow> has expired."))
                invitee.sendMessage(BlockChess.mm.deserialize("<gray>The invite to <yellow>${inviter.name}</yellow> has expired."))
            }
        }, 20L * 60)
    }

    fun accept(invitee: Player, inviterName: String): Boolean {
        val inviter = Bukkit.getPlayerExact(inviterName) ?: run {
            invitee.sendMessage(BlockChess.mm.deserialize("<red>Player not online: $inviterName"))
            return false
        }

        val inv = pendingInvites[invitee.uniqueId]
        if (inv == null || inv.inviter != inviter.uniqueId) {
            invitee.sendMessage(BlockChess.mm.deserialize("<red>No valid invites from ${inviter.name}."))
            return false
        }

        pendingInvites.remove(invitee.uniqueId)

        val game = Game()
        // White will be the inviter by default
        game.start(inviter, invitee)
        activeGamesByPlayer[inviter.uniqueId] = game
        activeGamesByPlayer[invitee.uniqueId] = game

        inviter.sendMessage(BlockChess.mm.deserialize("<gray>Match started against <yellow>${invitee.name}</yellow>. White: <white>${inviter.name}</white>"))
        invitee.sendMessage(BlockChess.mm.deserialize("<gray>Match started against <yellow>${inviter.name}</yellow>. Black: <dark_gray>${invitee.name}</dark_gray>"))
        return true
    }

    fun decline(invitee: Player, inviterName: String): Boolean {
        val inviter = Bukkit.getPlayerExact(inviterName) ?: return false
        val inv = pendingInvites[invitee.uniqueId]
        if (inv == null || inv.inviter != inviter.uniqueId) return false
        pendingInvites.remove(invitee.uniqueId)
        inviter.sendMessage(BlockChess.mm.deserialize("<gray>${invitee.name} has declined your invite."))
        invitee.sendMessage(BlockChess.mm.deserialize("<gray>You have declines ${inviter.name}'s invite."))
        return true
    }

    fun end(game: Game) {
        val white = game.white
        val black = game.black
        game.stop()
        if (white != null) {
            activeGamesByPlayer.remove(white.uniqueId)
        }
        if (black != null) {
            activeGamesByPlayer.remove(black.uniqueId)
        }
    }

    fun startBot(player: Player, difficulty: Int) {
        if (activeGamesByPlayer.containsKey(player.uniqueId)) {
            player.sendMessage(BlockChess.mm.deserialize("<red>You already are in a match."))
            return
        }
        val game = Game()
        game.startAgainstBot(player, difficulty, true)
        activeGamesByPlayer[player.uniqueId] = game
        player.sendMessage(BlockChess.mm.deserialize("<gray>Match against <yellow>Stockfish</yellow> started. Difficulty: <aqua>$difficulty</aqua>"))
    }
}