package gg.ethereallabs.blockChess.command.subcommands

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.command.abstract.BaseCommand
import gg.ethereallabs.blockChess.game.GameManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.text.startsWith

class BotCommand : BaseCommand("bot") {
    override fun execute(
        sender: CommandSender,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            BlockChess.instance.sendMessage("<red>Choose a difficulty: /chess bot <1-12>", sender)
            return true
        }

        val diff = args[0].toIntOrNull()
        if (diff == null || diff !in 1..12) {
            BlockChess.instance.sendMessage("<red>Not a valid difficulty. Use a number between 1-12.", sender)
            return true
        }

        if(sender !is Player) {
            BlockChess.instance.sendMessage( "<red>This command can only be executed by players!", sender)
            return true
        }

        GameManager.startBot(sender, diff)
        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if(args.size == 1){
            return (1..12).map { it.toString() }
        }
        return emptyList()
    }
}