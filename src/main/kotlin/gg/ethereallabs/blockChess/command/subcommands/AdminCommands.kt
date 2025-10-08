package gg.ethereallabs.blockChess.command.subcommands

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.command.abstract.BaseCommand
import gg.ethereallabs.blockChess.data.LocalStorage
import gg.ethereallabs.blockChess.elo.EloManager
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AdminCommands : BaseCommand("admin") {
    override fun execute(
        sender: CommandSender,
        args: Array<out String>
    ): Boolean {
        if(args.isEmpty()){
            return true
        }

        when(args[0]){
            "elo" -> {
                when(args[1]){
                    "set" -> handleSetElo(sender, args)
                    "add" -> handleAddElo(sender, args)
                    "remove" -> handleRemoveElo(sender, args)
                    else -> ""
                }
            }
        }

        return true
    }

    fun handleSetElo(sender : CommandSender, args: Array<out String>){
        if(args.size < 3){
            return
        }
        val target = sender.server.getPlayerExact(args[2])
        if (target == null) {
            BlockChess.instance.sendMessage("<red>This player is not online: ${args[3]}", sender)
            return
        }

        val amount = args[3].toInt()

        if(EloManager.players[target.uniqueId] == null){
            LocalStorage.loadPlayerData(sender as Player)
        }

        EloManager.players[target.uniqueId]?.rating = amount

        BlockChess.instance.sendMessage("<yellow>You've set <gray>${target.name}'s</gray> ELO to <gray>$amount</gray>", sender)
        BlockChess.instance.sendMessage("<yellow>An admin has set your ELO to <gray>$amount</gray>", sender)
    }

    fun handleAddElo(sender : CommandSender, args: Array<out String>){
        if(args.size < 3){
            return
        }
        val target = sender.server.getPlayerExact(args[2])
        if (target == null) {
            BlockChess.instance.sendMessage("<red>This player is not online: ${args[3]}", sender)
            return
        }

        val amount = args[3].toInt()

        if(EloManager.players[target.uniqueId] == null){
            LocalStorage.loadPlayerData(sender as Player)
        }

        EloManager.players[target.uniqueId]?.rating += amount

        BlockChess.instance.sendMessage("<yellow>You've added <gray>$amount</gray> ELO to <gray>$target.name</gray>", sender)
        BlockChess.instance.sendMessage("<yellow>An admin has increased your ELO of <gray>$amount</gray>", sender)
    }

    fun handleRemoveElo(sender : CommandSender, args: Array<out String>){
        if(args.size < 3){
            return
        }
        val target = sender.server.getPlayerExact(args[2])
        if (target == null) {
            BlockChess.instance.sendMessage("<red>This player is not online: ${args[3]}", sender)
            return
        }

        val amount = args[3].toInt()

        if(EloManager.players[target.uniqueId] == null){
            LocalStorage.loadPlayerData(sender as Player)
        }

        EloManager.players[target.uniqueId]?.rating += amount

        BlockChess.instance.sendMessage("<yellow>You've removed <gray>$amount</gray> ELO to <gray>$target.name</gray>", sender)
        BlockChess.instance.sendMessage("<yellow>An admin has decreased your ELO of <gray>$amount</gray>", sender)
    }
}