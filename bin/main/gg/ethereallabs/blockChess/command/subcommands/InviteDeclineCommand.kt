package gg.ethereallabs.blockChess.command.subcommands

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.command.abstract.BaseCommand
import gg.ethereallabs.blockChess.game.GameManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.text.startsWith

class InviteDeclineCommand : BaseCommand("decline") {
    override fun execute(
        sender: CommandSender,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            BlockChess.instance.sendMessage("<red>Specify the inviter: /chess decline <player>", sender)
            return true
        }
        if(sender !is Player) {
            BlockChess.instance.sendMessage( "<red>This command can only be executed by players!", sender)
            return true
        }

        val target = sender.server.getPlayerExact(args[0])
        if (target == null) {
            BlockChess.instance.sendMessage("<red>This player is not online: ${args[0]}", sender)
            return true
        }

        val ok = GameManager.decline(sender, args[1])
        if (!ok) BlockChess.instance.sendMessage("<red>No valid invites from ${args[1]}", sender)
        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if(args.size == 1){
            return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }
        }
        return emptyList()
    }
}