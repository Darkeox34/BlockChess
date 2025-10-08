package gg.ethereallabs.blockChess.command.subcommands

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.command.abstract.BaseCommand
import gg.ethereallabs.blockChess.game.GameManager
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class InviteAcceptCommand : BaseCommand("accept") {
    override fun execute(
        sender: CommandSender,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            BlockChess.instance.sendMessage("<red>Specify the inviter: /chess accept <player>", sender)
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

        GameManager.accept(sender, args[0])
        return true
    }
}