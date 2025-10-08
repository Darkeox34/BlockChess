package gg.ethereallabs.blockChess.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.elo.EloManager
import gg.ethereallabs.blockChess.elo.PlayerData
import org.bukkit.entity.Player
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Type
import java.util.UUID
import java.util.logging.Level

object LocalStorage {
    private val dataFolder: File = File(BlockChess.instance.dataFolder, "playerdata")
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    const val defaultElo = 800.0

    private fun getPlayerFile(playerUuid: UUID): File {
        return File(dataFolder, "$playerUuid.json")
    }

    fun loadPlayerData(player: Player) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        val playerFile = getPlayerFile(player.uniqueId)

        if (!playerFile.exists()) {
            createDefaultPlayerData(player)
        }

        try {
            FileReader(playerFile).use { reader ->
                val type: Type = object : TypeToken<PlayerData>() {}.type
                val playerData: PlayerData = gson.fromJson(reader, type)

                EloManager.players[player.uniqueId] = playerData
            }
        } catch (e: Exception) {
            BlockChess.instance.logger.log(Level.SEVERE, "Error loading player data for ${player.name}", e)
            createDefaultPlayerData(player)
            EloManager.players[player.uniqueId] = PlayerData()
        }
    }
    fun savePlayerData(player: Player) {
        val playerFile = getPlayerFile(player.uniqueId)

        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        val playerData = EloManager.players[player.uniqueId] ?: PlayerData()

        try {
            FileWriter(playerFile).use { writer ->
                gson.toJson(playerData, writer)
            }
        } catch (e: Exception) {
            BlockChess.instance.logger.log(Level.SEVERE, "Error saving player data for ${player.name}", e)
        }
    }

    fun createDefaultPlayerData(player: Player) {
        val playerData = PlayerData(rating = defaultElo, gamesPlayed = 0)

        try {
            FileWriter(getPlayerFile(player.uniqueId)).use { writer ->
                gson.toJson(playerData, writer)
            }
        } catch (e: Exception) {
            BlockChess.instance.logger.log(Level.SEVERE, "Error creating default player data for ${player.name}", e)
        }

        EloManager.players[player.uniqueId] = playerData
    }
}