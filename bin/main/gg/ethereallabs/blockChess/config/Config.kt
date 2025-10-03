package gg.ethereallabs.blockChess.config

import gg.ethereallabs.blockChess.BlockChess

object Config {
    var resourcepack = true
    var enginePath : String = ""
    fun load(plugin: BlockChess) {
        val cfg = plugin.config
        resourcepack = cfg.getBoolean("resourcepack.enabled", true)
        enginePath = cfg.getString("engine.path", "plugins/BlockChess/patricia_v3.exe")!!

        BlockChess.instance.logger.info("Loaded Engine Path: $enginePath")
    }
}