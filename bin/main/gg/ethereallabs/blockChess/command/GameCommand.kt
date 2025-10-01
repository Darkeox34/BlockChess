package gg.ethereallabs.blockChess.command

import gg.ethereallabs.blockChess.game.Game
import gg.ethereallabs.blockChess.gui.GameGUI
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GameCommand : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            return true
        }
        val game = Game()
        val gui = GameGUI(game, false)
        gui.open(sender)
        return true
    }

}