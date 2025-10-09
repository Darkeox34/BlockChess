package gg.ethereallabs.blockChess.command.subcommands

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.command.abstract.BaseCommand
import gg.ethereallabs.blockChess.elo.EloManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import kotlin.text.startsWith

class InfoCommand : BaseCommand("info"){
    override fun execute(
        sender: CommandSender,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            BlockChess.instance.sendMessage("<red>Specify a player: /chess info <player>", sender)
            return true
        }

        val target = sender.server.getPlayerExact(args[0])
        if (target == null) {
            BlockChess.instance.sendMessage("<red>This player is not online: ${args[0]}", sender)
            return true
        }

        val playerData = EloManager.players[target.uniqueId]

        if(playerData == null) {
            BlockChess.instance.sendMessage("<red>Could not retrieve ${target.name}'s informations.")
            return true
        }

        BlockChess.instance.sendMessage("${EloManager.getChessistName(target)}<yellow> info:", sender)
        BlockChess.instance.sendMessage(" <gray>Elo: <yellow>${playerData.rating}", sender)
        BlockChess.instance.sendMessage(" <gray>Matches Played: <yellow>${playerData.gamesPlayed}", sender)
        BlockChess.instance.sendMessage(" <gray>Wins: <yellow>${playerData.wins}", sender)
        BlockChess.instance.sendMessage(" <gray>Losses: <yellow>${playerData.losses}", sender)
        BlockChess.instance.sendMessage(" <gray>Draws: <yellow>${playerData.draws}", sender)

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if(args.size == 1){
            return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }
        }
        return emptyList()
    }
}