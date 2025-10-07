package gg.ethereallabs.blockChess.command.abstract

import org.bukkit.command.CommandSender

abstract class BaseCommand(val name: String) : CommandHandler {
    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
}