package gg.ethereallabs.blockChess.command.subcommands

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.command.abstract.BaseCommand
import gg.ethereallabs.blockChess.game.GameManager
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class InviteCommand : BaseCommand("invite") {
    override fun execute(
        sender: CommandSender,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            BlockChess.instance.sendMessage(sender,"<red>Specify a player: /chess invite <player>")
            return true
        }

        if(sender !is Player) {
            BlockChess.instance.sendMessage(sender, "<red>This command can only be executed by players!")
            return true
        }

        val target = sender.server.getPlayerExact(args[0])
        if (target == null) {
            BlockChess.instance.sendMessage(sender,"<red>This player is not online: ${args[0]}")
            return true
        }
        GameManager.invite(sender, target)
        return true
    }
}