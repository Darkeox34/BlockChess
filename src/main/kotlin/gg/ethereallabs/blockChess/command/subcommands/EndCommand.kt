package gg.ethereallabs.blockChess.command.subcommands

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.command.abstract.BaseCommand
import gg.ethereallabs.blockChess.game.GameManager
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class EndCommand : BaseCommand("end") {
    override fun execute(
        sender: CommandSender,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            BlockChess.instance.sendMessage(sender, "<red>This command can only be executed by players!")
            return true
        }

        val game = GameManager.getGame(sender)
        game?.end()
        return true
    }
}