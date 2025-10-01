package gg.ethereallabs.blockChess.game

import java.util.*

data class Invitation(
    val inviter: UUID,
    val invitee: UUID,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + 60_000 // 60s expiry
)