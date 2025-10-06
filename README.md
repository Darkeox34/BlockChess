# BlockChess

BlockChess is a Minecraft plugin that allows players to engage in chess matches directly within the game. It provides a user-friendly GUI, real-time gameplay, and the ability to play against other players or a powerful AI opponent powered by Fairy-Stockfish.

## Overview

BlockChess transforms the Minecraft experience by integrating the classic game of chess. Players can challenge each other to matches, accept or decline invitations, and even test their skills against a configurable AI. The plugin features an intuitive inventory-based interface where the chessboard and pieces are rendered, and players can make moves by simply clicking on the desired squares.

The plugin's architecture is designed to be lightweight and efficient, ensuring minimal impact on server performance. It leverages the power of the Fairy-Stockfish engine to provide a challenging and realistic chess experience when playing against the bot.

## Features

- **Player vs. Player (PvP):** Challenge other players on the server to a real-time chess match.
- **Player vs. Bot (PvE):** Play against the Fairy-Stockfish engine with adjustable difficulty levels.
- **Intuitive GUI:** A custom inventory-based GUI displays the chessboard, pieces, and game information.
- **Real-Time Gameplay:** Moves are updated in real-time for both players, providing a seamless experience.
- **Invitation System:** A simple invitation system allows players to invite, accept, and decline matches.
- **Configurable:** The plugin can be configured to enable or disable the resource pack and set the path to the Stockfish engine.

## Installation

1. **Download:** Get the latest version of BlockChess from the project's releases page.
2. **Install:** Place the downloaded `.jar` file into your server's `plugins` directory.
3. **Stockfish:** Download the appropriate Fairy-Stockfish executable for your server's operating system from the [Fairy-Stockfish releases page](https://github.com/fairy-stockfish/Fairy-Stockfish/releases).
4. **Configure:** Place the Stockfish executable in the `plugins/BlockChess` directory and ensure the `engine.path` in the `config.yml` file is set correctly.
5. **Restart:** Restart your server to enable the plugin.

## Usage

### Commands

- `/chess invite <player>`: Invite a player to a chess match.
- `/chess accept <player>`: Accept a chess match invitation.
- `/chess decline <player>`: Decline a chess match invitation.
- `/chess bot <1-12>`: Start a match against the bot with the specified difficulty level.

### Gameplay

Once a match starts, the chess GUI will open, displaying the board and pieces. To make a move, simply click on the piece you want to move and then click on the destination square. The board will update in real-time, and your opponent will see your move instantly.

## API Reference

BlockChess does not currently expose a public API for other plugins to use.

## Configuration

The `config.yml` file allows you to configure the following options:

- `resourcepack.enabled`: (Default: `true`) Enable or disable the custom resource pack for the chess pieces.
- `engine.path`: (Default: `plugins/BlockChess/stockfish.exe`) The path to the Fairy-Stockfish executable.

## Engine Communication

BlockChess communicates with the chess engine using the Universal Chess Interface (UCI) protocol. This allows the plugin to be compatible with any UCI-compliant chess engine, including the recommended Fairy-Stockfish.

### Engine Strength

The bot's strength can be set from level 1 to 12. The simulation of strength is achieved through a combination of UCI parameters, including ELO rating and move time limits.

For levels 1 through 4, the engine's thinking time is artificially limited to simulate a weaker opponent:

- **Level 1:** 50ms per move
- **Level 2:** 100ms per move
- **Level 3:** 150ms per move
- **Level 4:** 200ms per move

For levels 5 and above, the engine uses the standard game timers (`wtime` and `btime`) to manage its thinking time, resulting in stronger play.

Additionally, for levels 1 through 11, the engine's strength is limited by setting the `UCI_LimitStrength` option to `true` and providing a specific ELO value. For level 12, `UCI_LimitStrength` is set to `false`, allowing the engine to play at its maximum potential.

Here is a breakdown of the ELO rating for each level:

| Level | ELO Rating |
|-------|------------|
| 1     | 200        |
| 2     | 500        |
| 3     | 820        |
| 4     | 1060       |
| 5     | 1350       |
| 6     | 1600       |
| 7     | 1900       |
| 8     | 2200       |
| 9     | 2500       |
| 10    | 2800       |
| 11    | 3050       |
| 12    | Max        |

## Contribution Guidelines

We welcome contributions to BlockChess! If you'd like to contribute, please follow these steps:

1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Make your changes and ensure the code is well-formatted and documented.
4. Submit a pull request with a clear description of your changes.

## Attribution

BlockChess utilizes the following open-source projects:

- **Fairy-Stockfish:** A powerful chess engine that provides the bot's intelligence.
  - **GitHub Repository:** [https://github.com/fairy-stockfish/Fairy-Stockfish](https://github.com/fairy-stockfish/Fairy-Stockfish)
  - **License:** GNU General Public License v3.0

- **chesslib:** A Java chess library used for move generation, validation, and game state management.
  - **GitHub Repository:** [https://github.com/bhlangonijr/chesslib](https://github.com/bhlangonijr/chesslib)
  - **License:** Apache License 2.0