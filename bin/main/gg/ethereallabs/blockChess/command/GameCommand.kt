package gg.ethereallabs.blockChess.command

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.game.GameManager
import gg.ethereallabs.blockChess.game.Game
import gg.ethereallabs.blockChess.gui.GameGUI
import gg.ethereallabs.blockChess.gui.MainGUI
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GameCommand : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true

        if (args.isEmpty()) {
            // If in game, reopen your GUI; else show usage
            val current = GameManager.getGame(sender)
            if (current != null) {
                val isWhite = current.white?.uniqueId == sender.uniqueId
                val gui = GameGUI(current, isWhite)
                gui.open(sender)
            } else {
                sender.sendMessage(BlockChess.mm.deserialize("<gray>Usa: <aqua>/chess invite <player></aqua>, <aqua>/chess accept <player></aqua>, <aqua>/chess decline <player></aqua>, <aqua>/chess bot <1-20></aqua>"))
            }
            return true
        }

        when (args[0].lowercase()) {
            "end" -> {
                val game = GameManager.getGame(sender)
                game?.end() ?: return true
            }
            "palle" -> {
                val gui = MainGUI()
                gui.open(sender)
            }
            "bot" -> {
                if (args.size < 2) {
                    sender.sendMessage(BlockChess.mm.deserialize("<red>Specifica la difficoltà: /chess bot <1-20>"))
                    return true
                }
                val diff = args[1].toIntOrNull()
                if (diff == null || diff !in 1..20) {
                    sender.sendMessage(BlockChess.mm.deserialize("<red>Difficoltà non valida. Usa un numero tra 1 e 20."))
                    return true
                }
                GameManager.startBot(sender, diff)
            }
            "invite" -> {
                if (args.size < 2) {
                    sender.sendMessage(BlockChess.mm.deserialize("<red>Specifica un giocatore: /chess invite <player>"))
                    return true
                }
                val target = sender.server.getPlayerExact(args[1])
                if (target == null) {
                    sender.sendMessage(BlockChess.mm.deserialize("<red>Giocatore non online: ${args[1]}"))
                    return true
                }
                GameManager.invite(sender, target)
            }
            "accept" -> {
                if (args.size < 2) {
                    sender.sendMessage(BlockChess.mm.deserialize("<red>Specifica il mittente: /chess accept <player>"))
                    return true
                }
                GameManager.accept(sender, args[1])
            }
            "decline" -> {
                if (args.size < 2) {
                    sender.sendMessage(BlockChess.mm.deserialize("<red>Specifica il mittente: /chess decline <player>"))
                    return true
                }
                val ok = GameManager.decline(sender, args[1])
                if (!ok) sender.sendMessage(BlockChess.mm.deserialize("<red>Nessun invito valido da ${args[1]}"))
            }
            else -> sender.sendMessage(BlockChess.mm.deserialize("<gray>Usa: <aqua>/chess invite|accept|decline|bot</aqua>"))
        }

        return true
    }

}