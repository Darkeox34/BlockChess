package gg.ethereallabs.blockChess.events

import com.github.bhlangonijr.chesslib.BoardEvent
import com.github.bhlangonijr.chesslib.BoardEventListener
import com.github.bhlangonijr.chesslib.BoardEventType
import com.github.bhlangonijr.chesslib.move.Move


class ChessListener : BoardEventListener{
    override fun onEvent(event: BoardEvent) {
        if (event.type == BoardEventType.ON_MOVE) {
            val move: Move = event as Move

            println("Move $move was played")
        }
    }
}