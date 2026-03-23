# Multiplayer Splendor

This project now supports multiplayer networking for the Splendor board game.

## How to Host a Game

1. On the host machine, run the server:
   ```
   ./runServer.sh [port]
   ```
   Default port is 12345.

2. The server will wait for players to join.

## How to Join a Game

1. On each client machine, run the client:
   ```
   ./runClient.sh
   ```

2. Enter the host IP address, port (default 12345), and your player name.

3. Wait for the game to start (requires at least 2 players).

## Testing on the Same Machine

- Start the server in one terminal: `./runServer.sh`
- Start two clients in separate terminals: `./runClient.sh`
- Use "localhost" as the host IP.

## Testing Across Different Machines

- Ensure both machines are on the same network.
- Find the host machine's IP address (e.g., using `ifconfig` or `ip addr`).
- Clients connect using that IP and port 12345.

## Networking Architecture

- **Server**: Authoritative game state, validates moves, broadcasts updates.
- **Clients**: Send moves to server, receive state updates, render UI.
- **Protocol**: Simple object serialization over TCP sockets.
- **Synchronization**: All game actions go through server, clients stay in sync.

## Files Added/Modified

### New Files:
- `src/network/NetworkMessage.java` - Message types for client-server communication
- `src/network/GameServer.java` - Server implementation
- `src/network/ClientHandler.java` - Handles individual client connections
- `src/network/GameClient.java` - Client networking logic
- `src/network/ClientMain.java` - Client entry point with connection dialog
- `src/ui/swing/MultiplayerSwingApp.java` - Multiplayer client UI
- `runServer.sh` - Script to start server
- `runClient.sh` - Script to start client

### Modified Files:
- All model classes (`GameState.java`, `Player.java`, etc.) - Added `implements Serializable`
- `src/engine/Move.java` - Added `implements Serializable`

## Limitations

- Supports 2-4 players joining before game starts.
- No reconnection support.
- Simple error handling.
- UI refresh methods in MultiplayerSwingApp are stubs (need implementation for full functionality).