package gg.ethereallabs.blockChess.events

import gg.ethereallabs.blockChess.BlockChess
import gg.ethereallabs.blockChess.data.LocalStorage
import gg.ethereallabs.blockChess.elo.EloManager
import gg.ethereallabs.blockChess.game.Game
import gg.ethereallabs.blockChess.game.GameManager
import org.bukkit.Bukkit
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

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as Player
        val game = GameManager.getGame(player)
        if (game == null) {
            return
        }

        val promoGUI = GameManager.playersPromoting[player.uniqueId]
        val surrendGUI = GameManager.playersSurrending[player.uniqueId]
        val gui = game.getPlayerGUI(player)

        if (event.inventory == promoGUI?.getInventory()) {
            GameManager.playersPromoting.remove(player.uniqueId)
            BlockChess.instance.sendMessage("<red>You need to choose a piece!", player)
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            Bukkit.getScheduler().runTaskLater(BlockChess.instance, Runnable {
                promoGUI.open(player)
            }, 3L)
            return
        }

        if (event.inventory == surrendGUI?.getInventory()) {
            GameManager.playersSurrending.remove(player.uniqueId)
            Bukkit.getScheduler().runTaskLater(BlockChess.instance, Runnable {
                gui?.open(player)
            }, 3L)
            return
        }
        if (gui?.getInventory() == event.inventory &&
            !GameManager.playersPromoting.containsKey(player.uniqueId) &&
            !GameManager.playersSurrending.containsKey(player.uniqueId) &&
            !GameManager.playersDrawing.containsKey(player.uniqueId)
        ) {
            Bukkit.getScheduler().runTaskLater(BlockChess.instance, Runnable {
                gui.open(player)
            }, 3L)
            BlockChess.instance.sendMessage("<red>You can't close the GUI while in a game!", player)
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
        }
    }
}