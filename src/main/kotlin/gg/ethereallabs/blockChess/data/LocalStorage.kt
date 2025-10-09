package gg.ethereallabs.blockChess.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonSerializationContext
import com.google.gson.reflect.TypeToken
import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.elo.EloManager
import gg.ethereallabs.blockChess.elo.PlayerData
import gg.ethereallabs.blockChess.utils.SyncHelper.runSync
import org.bukkit.entity.Player
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Type
import java.util.UUID
import java.util.logging.Level
import java.time.Instant
import java.util.concurrent.CompletableFuture.runAsync

object LocalStorage {
    private val dataFolder: File = File(BlockChess.instance.dataFolder, "playerdata")

    val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Instant::class.java, object : com.google.gson.JsonSerializer<Instant>,
            com.google.gson.JsonDeserializer<Instant> {

            override fun serialize(src: Instant?, typeOfSrc: Type?, context: JsonSerializationContext?): com.google.gson.JsonElement {
                return com.google.gson.JsonPrimitive(src?.toString())
            }

            override fun deserialize(json: com.google.gson.JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Instant? {
                return json?.asString?.let { Instant.parse(it) }
            }
        })
        .create()



    private fun getPlayerFile(playerUuid: UUID): File {
        return File(dataFolder, "$playerUuid.json")
    }

    fun playerHasData(player : Player): Boolean {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        val playerFile = getPlayerFile(player.uniqueId)

        if (!playerFile.exists()) {
            return false
        }
        return true
    }

    fun loadPlayerData(player: Player) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        val playerFile = getPlayerFile(player.uniqueId)

        if (!playerFile.exists()) {
            createDefaultPlayerData(player)
        }
        runAsync {
            try {
                FileReader(playerFile).use { reader ->
                    val type: Type = object : TypeToken<PlayerData>() {}.type
                    val playerData: PlayerData = gson.fromJson(reader, type)
                    runSync{
                        EloManager.players[player.uniqueId] = playerData
                    }
                }
            } catch (e: Exception) {
                BlockChess.instance.logger.log(Level.SEVERE, "Error loading player data for ${player.name}", e)
                createDefaultPlayerData(player)
                runSync{
                    EloManager.players[player.uniqueId] = PlayerData()
                }
            }
        }
    }
    fun savePlayerData(player: Player) {
        val playerFile = getPlayerFile(player.uniqueId)

        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        val playerData = EloManager.players[player.uniqueId] ?: PlayerData()
        runAsync {
            try {
                FileWriter(playerFile).use { writer ->
                    gson.toJson(playerData, writer)
                }
            } catch (e: Exception) {
                BlockChess.instance.logger.log(Level.SEVERE, "Error saving player data for ${player.name}", e)
            }
        }
    }

    fun createDefaultPlayerData(player: Player) {
        val playerData = PlayerData(rating = EloManager.defaultElo, gamesPlayed = 0)
        runAsync {
            try {
                FileWriter(getPlayerFile(player.uniqueId)).use { writer ->
                    gson.toJson(playerData, writer)
                }
            } catch (e: Exception) {
                BlockChess.instance.logger.log(Level.SEVERE, "Error creating default player data for ${player.name}", e)
            }
            runSync {
                EloManager.players[player.uniqueId] = playerData
            }
        }
    }
}