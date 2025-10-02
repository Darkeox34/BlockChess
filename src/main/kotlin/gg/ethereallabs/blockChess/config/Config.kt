package gg.ethereallabs.blockChess.config

import gg.ethereallabs.blockChess.BlockChess

object Config {
    var resourcepack = true

    fun load(plugin: BlockChess) {
        val cfg = plugin.config
        resourcepack = cfg.getBoolean("resourcepack.enabled", true)
    }
}