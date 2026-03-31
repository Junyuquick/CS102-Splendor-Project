# Multiplayer Splendor

This project now supports multiplayer networking for the Splendor board game.

## How to Host a Game

1. On the host machine, run the server:
   ```
   javac -d classes $(find src -name '*.java')
   java -cp classes network.GameServer [port]
   ```
   
   note:
   - you should use "ifconfig" to get your private IP address(usually starts with 10, 172 or 192)
   - tip: use ctrl f and type "en0", your private ip is in that block of information
   - you may choose to use your loopback address if u want though
   
   Default port is 12345.

2. The server will wait for players to join.
3. The server creator joins as the host and can press `Start Game` once at least 2 players are in the lobby.

## How to Join a Game

1. On each client machine, run the client:
   ```
   javac -d classes $(find src -name '*.java')
   java -cp classes network.ClientMain
   ```

2. Enter the host IP address, port (default 12345), and your player name.

3. Wait in the lobby for the host to start the game (requires at least 2 players).

## Testing on the Same Machine

- Start the server in one terminal: `java -cp classes network.GameServer`
- Start two clients in separate terminals: `java -cp classes network.ClientMain`
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

### Modified Files:
- All model classes (`GameState.java`, `Player.java`, etc.) - Added `implements Serializable`
- `src/engine/Move.java` - Added `implements Serializable`

## Limitations

- Supports 2-4 players joining before game starts.
- Only the host/server creator can start the match from the lobby.
- No reconnection support.
- Simple error handling.
- UI refresh methods in MultiplayerSwingApp are stubs (need implementation for full functionality).
