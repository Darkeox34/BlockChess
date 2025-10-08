package gg.ethereallabs.blockChess

import gg.ethereallabs.blockChess.command.CommandRegistry
import gg.ethereallabs.blockChess.config.Config
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class BlockChess : JavaPlugin() {

    companion object {
        lateinit var instance : BlockChess
        lateinit var mm: MiniMessage
        lateinit var whitePiecesByChar: Map<String, Material>
        lateinit var blackPiecesByChar: Map<String, Material>
    }

    val fenToName : HashMap<String, String> = hashMapOf(
        "p" to "Pawn",
        "r" to "Rook",
        "n" to "Knight",
        "b" to "Bishop",
        "q" to "Queen",
        "k" to "King"
    )

    val whitePieces : HashMap<Material, String> = hashMapOf(
        Material.BIRCH_BUTTON to "p",
        Material.IRON_DOOR to "r",
        Material.IRON_HORSE_ARMOR to "n",
        Material.END_ROD to "b",
        Material.WHITE_CANDLE to "q",
        Material.IRON_BLOCK to "k"
    )
    val blackPieces : HashMap<Material, String> = hashMapOf(
        Material.POLISHED_BLACKSTONE_BUTTON to "p",
        Material.DARK_OAK_DOOR to "r",
        Material.LEATHER_HORSE_ARMOR to "n",
        Material.LIGHTNING_ROD to "b",
        Material.BLACK_CANDLE to "q",
        Material.NETHERITE_BLOCK to "k"
    )

    override fun onEnable() {
        instance = this
        mm = MiniMessage.miniMessage()
        saveDefaultConfig()
        Config.load(this)
        whitePiecesByChar = whitePieces.entries.associate { (k, v) -> v to k }
        blackPiecesByChar = blackPieces.entries.associate { (k, v) -> v to k }
        getCommand("chess")?.setExecutor(CommandRegistry())
        getCommand("chess")?.tabCompleter = CommandRegistry()
    }

    override fun onDisable() {

    }

    fun sendMessage(sender: CommandSender?, message: String) {
        sender?.sendMessage(mm.deserialize(message))
    }
}
