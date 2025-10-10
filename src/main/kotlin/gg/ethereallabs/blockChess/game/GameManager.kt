package gg.ethereallabs.blockChess.game

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.elo.EloManager
import gg.ethereallabs.blockChess.gui.subgui.AcceptDrawGUI
import gg.ethereallabs.blockChess.gui.subgui.RequestDrawGUI
import gg.ethereallabs.blockChess.gui.subgui.PromotionGUI
import gg.ethereallabs.blockChess.gui.subgui.SurrendGUI
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object GameManager {

    private val pendingInvites: MutableMap<UUID, Invitation> = ConcurrentHashMap()
    private val activeGamesByPlayer: MutableMap<UUID, Game> = ConcurrentHashMap()

    val playersPromoting: MutableMap<UUID, PromotionGUI?> = ConcurrentHashMap()
    val playersSurrending : MutableMap<UUID, SurrendGUI?> = ConcurrentHashMap()
    val playersRequestingDraw : MutableMap<UUID, RequestDrawGUI> = ConcurrentHashMap()
    val playersAcceptingDraw : MutableMap<UUID, AcceptDrawGUI> = ConcurrentHashMap()

    fun getGame(p: Player): Game? = activeGamesByPlayer[p.uniqueId]

    fun invite(inviter: Player, invitee: Player) {
        if (inviter.uniqueId == invitee.uniqueId) {
            BlockChess.instance.sendMessage("<red>You can't invite yourself.", inviter)
            return
        }

        if (activeGamesByPlayer.containsKey(inviter.uniqueId) || activeGamesByPlayer.containsKey(invitee.uniqueId)) {
            BlockChess.instance.sendMessage("<red>You or the invited player are already playing a match.", inviter)
            return
        }

        val inv = Invitation(inviter.uniqueId, invitee.uniqueId)
        pendingInvites[invitee.uniqueId] = inv

        val inviterName = EloManager.getChessistName(inviter)
        val inviteeName = EloManager.getChessistName(invitee)

        BlockChess.instance.sendMessage("<yellow>Invite sent to $inviteeName.", inviter)
        BlockChess.instance.sendMessage("<yellow>$inviterName</yellow> invited you to play chess (5+0).", invitee)
        BlockChess.instance.sendMessage("<gray>Use <yellow>/chess accept $inviterName</yellow> to accept, <red>/chess decline ${inviter.name}</red> to decline.", invitee)

        // Auto-expire
        Bukkit.getScheduler().runTaskLater(BlockChess.instance, Runnable {
            val current = pendingInvites[invitee.uniqueId]
            if (current != null && current.inviter == inviter.uniqueId) {
                pendingInvites.remove(invitee.uniqueId)
                BlockChess.instance.sendMessage("<yellow>The invite to $inviteeName <yellow>has expired.", inviter)
                BlockChess.instance.sendMessage("<yellow>The invite from $inviterName <yellow>has expired.", invitee)
            }
        }, 20L * 60)
    }

    fun accept(invitee: Player, inviterName: String): Boolean {
        val inviter = Bukkit.getPlayerExact(inviterName) ?: run {
            invitee.sendMessage(BlockChess.mm.deserialize("<red>Player not online: $inviterName"))
            return false
        }

        val inviterName = EloManager.getChessistName(inviter)
        val inviteeName = EloManager.getChessistName(invitee)

        val inv = pendingInvites[invitee.uniqueId]
        if (inv == null || inv.inviter != inviter.uniqueId) {
            BlockChess.instance.sendMessage("<red>No valid invites from $inviterName.", invitee)
            return false
        }

        pendingInvites.remove(invitee.uniqueId)

        val game = Game()
        // White will be the inviter by default
        game.start(inviter, invitee)
        activeGamesByPlayer[inviter.uniqueId] = game
        activeGamesByPlayer[invitee.uniqueId] = game

        BlockChess.instance.sendMessage("<yellow>Match started against <yellow>$inviteeName</yellow>. White: <white>$inviterName</white>", inviter)
        BlockChess.instance.sendMessage("<yellow>Match started against <yellow>$inviterName</yellow>. Black: <dark_gray>$inviteeName</dark_gray>", invitee)
        return true
    }

    fun decline(invitee: Player, inviterName: String): Boolean {
        val inviter = Bukkit.getPlayerExact(inviterName) ?: return false
        val inv = pendingInvites[invitee.uniqueId]
        if (inv == null || inv.inviter != inviter.uniqueId) return false
        pendingInvites.remove(invitee.uniqueId)

        val inviterName = EloManager.getChessistName(inviter)
        val inviteeName = EloManager.getChessistName(invitee)

        BlockChess.instance.sendMessage("$inviteeName <yellow>has declined your invite.", inviter)
        BlockChess.instance.sendMessage("<yellow>You have declined $inviterName's invite.", invitee)
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