package gg.ethereallabs.blockChess.events

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.data.LocalStorage
import gg.ethereallabs.blockChess.elo.EloManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener: Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        EloManager.cancelRemoval(player.uniqueId)

        if (LocalStorage.playerHasData(player))
            LocalStorage.loadPlayerData(player)
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        val player = event.player
        EloManager.scheduleRemoval(player.uniqueId, BlockChess.instance)
    }
}