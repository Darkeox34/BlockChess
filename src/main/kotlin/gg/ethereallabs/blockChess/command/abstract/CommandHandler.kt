package gg.ethereallabs.blockChess.command.abstract

import org.bukkit.command.CommandSender

interface CommandHandler {
    fun execute(sender: CommandSender, args: Array<out String>): Boolean
    fun tabComplete(sender: CommandSender, args: Array<out String>): List<String>
}