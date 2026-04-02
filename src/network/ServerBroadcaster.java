package network;

import config.Config;
import model.GameState;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

/**
 * Handles server messages that need to be sent to one or more clients.
 *
 * Keeping this logic in one place helps GameServer stay focused on
 * game flow instead of repeating output-message code.
 */
public class ServerBroadcaster {
    private final Config config;
    private final LobbyManager lobbyManager;

    /**
     * Creates a broadcaster that can send updates for one lobby.
     *
     * @param config game configuration
     * @param lobbyManager source of the current client roster
     */
    public ServerBroadcaster(Config config, LobbyManager lobbyManager) {
        this.config = config;
        this.lobbyManager = lobbyManager;
    }

    /**
     * Pushes the latest shared game state to every connected client.
     *
     * @param gameState current shared game state
     */
    public void broadcastState(GameState gameState) {
        NetworkMessage msg = NetworkMessage.stateUpdate(gameState);
        for (ClientHandler client : lobbyManager.getClients()) {
            client.sendMessage(msg);
        }
    }

    /**
     * Sends the same game-over message to every connected client.
     *
     * @param message human-readable game result
     */
    public void broadcastGameOver(String message) {
        NetworkMessage msg = NetworkMessage.gameOver(message);
        for (ClientHandler client : lobbyManager.getClients()) {
            client.sendMessage(msg);
        }
    }

    /**
     * Sends the latest lobby snapshot to every connected client.
     */
    public void broadcastLobbyState() {
        List<String> playerNames = lobbyManager.buildLobbyPlayerNames();
        NetworkMessage msg = NetworkMessage.lobbyUpdate(
                playerNames,
                0,
                config.getMinPlayers()
        );
        for (ClientHandler client : lobbyManager.getClients()) {
            client.sendMessage(msg);
        }
    }

    /**
     * Rejects a connection by sending one error message and closing the
     * socket immediately.
     *
     * @param socket socket to reject
     * @param message rejection reason
     */
    public void rejectConnection(Socket socket, String message) {
        try (
                socket;
                ObjectOutputStream out =
                        new ObjectOutputStream(socket.getOutputStream())
        ) {
            out.writeObject(NetworkMessage.error(message));
            out.flush();
        } catch (IOException e) {
            System.err.println(
                    "Failed to reject connection cleanly: "
                            + e.getMessage()
            );
        }
    }
}
