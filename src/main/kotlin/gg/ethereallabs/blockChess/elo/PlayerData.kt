package gg.ethereallabs.blockChess.elo

data class PlayerData(
    var rating: Double = 800.0,
    var gamesPlayed: Int = 0,
    var wins: Int = 0,
    var losses: Int = 0,
    var draws: Int = 0
)
