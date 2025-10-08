package gg.ethereallabs.blockChess.elo

import gg.ethereallabs.blockChess.BlockChess
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

object EloManager {

    val players : ConcurrentHashMap<UUID, PlayerData> = ConcurrentHashMap()

    fun expectedScore(ra: Double, rb: Double): Double {
        return 1.0 / (1.0 + 10.0.pow((rb - ra) / 400.0))
    }

    fun baseK(gamesPlayed: Int): Double {
        return when {
            gamesPlayed < 30 -> 40.0
            gamesPlayed < 100 -> 20.0
            else -> 10.0
        }
    }

    fun differenceBonusFactor(rating: Double, opponentRating: Double): Double {
        val delta = opponentRating - rating
        val absDelta = kotlin.math.abs(delta)
        return when {
            absDelta >= 200 -> 1.5
            absDelta >= 100 -> 1.25
            else -> 1.0
        }
    }

    fun updateEloWithDifference(player: PlayerData, opponent: PlayerData, result: Double) {
        val expected = expectedScore(player.rating, opponent.rating)
        val kb = baseK(player.gamesPlayed)

        val diffBonus = differenceBonusFactor(player.rating, opponent.rating)
        val kEff = kb * diffBonus

        val change = kEff * (result - expected)
        player.rating += change
        player.gamesPlayed += 1
    }

    fun getChessistName(player : Player) : Component?{
        val playerData = players[player.uniqueId]
        if(playerData != null){
            when{
                playerData.rating > 2600.0 -> return BlockChess.mm.deserialize("<#7d1515><bold>SGM</bold> ${player.name}")
                playerData.rating > 2500.0 -> return BlockChess.mm.deserialize("<red><bold>GM</bold> ${player.name}")
                playerData.rating > 2400.0 -> return BlockChess.mm.deserialize("<gold><bold>IM</bold> ${player.name}")
                playerData.rating > 2200.0 -> return BlockChess.mm.deserialize("<green><bold>NM</bold> ${player.name}")
                playerData.rating > 2000.0 -> return BlockChess.mm.deserialize("<blue><bold>EXPERT</bold> ${player.name}")
            }
        }
        return BlockChess.mm.deserialize("<gray>${player.name}")
    }
}