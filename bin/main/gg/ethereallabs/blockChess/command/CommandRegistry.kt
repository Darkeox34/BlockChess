// CommandRegistry.kt
package gg.ethereallabs.blockChess.command

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.command.abstract.BaseCommand
import gg.ethereallabs.blockChess.command.abstract.CommandHandler
import gg.ethereallabs.blockChess.command.subcommands.BotCommand
import gg.ethereallabs.blockChess.command.subcommands.EndCommand
import gg.ethereallabs.blockChess.command.subcommands.InviteAcceptCommand
import gg.ethereallabs.blockChess.command.subcommands.InviteCommand
import gg.ethereallabs.blockChess.command.subcommands.InviteDeclineCommand
import gg.ethereallabs.blockChess.gui.MainGUI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.BookMeta

class CommandRegistry : CommandExecutor, TabCompleter {
    private val commands = mutableMapOf<String, CommandHandler>()

    init {
        registerCommand(BotCommand())
        registerCommand(EndCommand())
        registerCommand(InviteAcceptCommand())
        registerCommand(InviteCommand())
        registerCommand(InviteDeclineCommand())
    }

    private fun registerCommand(handler: BaseCommand) {
        commands[handler.name] = handler
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("blockchess.commands")) {
            return true
        }

        if (args.isEmpty()) {
            val gui = MainGUI()
            if(sender is Player) {
                gui.open(sender)
            }
            else{
                BlockChess.instance.sendMessage(sender,"<red>This command can be only executed by players!")
            }
            return true
        }

        val subCommand = args[0].lowercase()
        val handler = commands[subCommand]

        return handler?.execute(sender, args.copyOfRange(1, args.size)) ?: run {
            BlockChess.instance.sendMessage(sender,"<red>Unknown command! Write /chess help to have a list of all commands.")
            true
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.isEmpty()) {
            return emptyList()
        }

        if (args.size == 1) {
            return commands.keys.filter { it.startsWith(args[0].lowercase()) }
        }

        val subCommand = args[0].lowercase()
        val handler = commands[subCommand] ?: return emptyList()

        return handler.tabComplete(sender, args.copyOfRange(1, args.size))
    }

    fun broadcast(message: String) {
        val mm = MiniMessage.miniMessage()
        val component = mm.deserialize(message)

        Bukkit.getServer().sendMessage(component)
        Bukkit.getOnlinePlayers().forEach { player ->
            player.sendMessage(component)
        }
    }
}