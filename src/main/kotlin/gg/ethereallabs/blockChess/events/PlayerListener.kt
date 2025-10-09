package gg.ethereallabs.blockChess.events

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.data.LocalStorage
import gg.ethereallabs.blockChess.elo.EloManager
import gg.ethereallabs.blockChess.game.GameManager
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
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

    /*
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as Player
        val game = GameManager.getGame(player)
        if(game == null){
            return
        }

        val gui = game.getPlayerGUI(player)

        if(gui?.getInventory() == event.inventory){
            gui.open(player)
            BlockChess.instance.sendMessage("<red>You can't close the GUI while in a game!", player)
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
        }
    }
    */
}