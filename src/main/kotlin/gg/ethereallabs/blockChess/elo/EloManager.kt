package gg.ethereallabs.blockChess.elo

import gg.ethereallabs.blockChess.BlockChess
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object EloManager {

    val players: ConcurrentHashMap<UUID, PlayerData> = ConcurrentHashMap()
    private val pendingRemoval = ConcurrentHashMap<UUID, BukkitRunnable>()
    const val defaultElo = 800

    fun scheduleRemoval(playerId: UUID, plugin: org.bukkit.plugin.Plugin) {
        if (pendingRemoval.containsKey(playerId)) return

        val task = object : BukkitRunnable() {
            override fun run() {
                players.remove(playerId)
                pendingRemoval.remove(playerId)
            }
        }

        task.runTaskLater(plugin, 20L * 60 * 10)
        pendingRemoval[playerId] = task
    }

    fun cancelRemoval(playerId: UUID) {
        pendingRemoval.remove(playerId)?.cancel()
    }

    /**
     * Calculates the Elo change for a match.
     *
     * @param player The player whose rating changes
     * @param opponent The opponent
     * @param result 1.0 = win, 0.5 = draw, 0.0 = loss
     * @return The Elo change (positive or negative integer)
     */
    fun updateElo(player: PlayerData, opponent: PlayerData, result: Double): Int {
        val ratingA = player.rating
        val ratingB = opponent.rating
        val diff = abs(ratingA - ratingB)

        val isPlayerHigher = ratingA > ratingB
        val lowerRating = minOf(ratingA, ratingB)
        val higherRating = maxOf(ratingA, ratingB)

        // Base K depends on whether the player is provisional
        val k = if (isProvisional(player)) 20 else 8
        val baseGain = when {
            diff < 50 -> k // near equal → ±8
            diff < 150 -> (k + 2)
            diff < 300 -> (k + 4)
            else -> (k + 6)
        }
        val lowerWinGain = baseGain + (diff / 50)

        val change = when (result) {
            1.0 -> { // Win
                if (ratingA < ratingB) {
                    // Lower rated wins → more reward
                    lowerWinGain
                } else if (ratingA > ratingB) {
                    // Higher rated wins → smaller gain
                    (k - diff / 150).coerceAtLeast(2)
                } else {
                    k
                }
            }
            0.0 -> { // Loss
                -updateElo(opponent, player, 1.0) // symmetric
            }
            0.5 -> { // Draw
                if (abs(ratingA - ratingB) < 50) {
                    0 // equal ratings → no change
                } else {
                    val drawChange = (lowerWinGain - 8).coerceAtLeast(0)
                    if (ratingA < ratingB) drawChange else -drawChange
                }
            }
            else -> 0
        }

        // --- Apply change ---
        player.rating += change
        player.gamesPlayed += 1
        player.lastPlayed = Instant.now()

        return change
    }

    /**
     * Whether the player's rating is provisional (hasn't played in a while or few games)
     */
    fun isProvisional(player: PlayerData): Boolean {
        val daysSinceLast = player.lastPlayed?.let {
            java.time.Duration.between(it, Instant.now()).toDays()
        } ?: Long.MAX_VALUE

        return player.gamesPlayed < 30 || daysSinceLast > 30
    }

    fun getChessistName(player: Player): String {
        val playerData = players[player.uniqueId] ?: return "<gray>${player.name}"

        return when {
            playerData.rating > 2600 -> "<#7d1515><bold>SGM</bold> ${player.name}"
            playerData.rating > 2500 -> "<red><bold>GM</bold> ${player.name}"
            playerData.rating > 2400 -> "<gold><bold>IM</bold> ${player.name}"
            playerData.rating > 2200 -> "<green><bold>NM</bold> ${player.name}"
            playerData.rating > 2000 -> "<blue><bold>EXPERT</bold> ${player.name}"
            else -> "<gray>${player.name}"
        }
    }
}
