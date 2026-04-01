package network;

import engine.moves.Move;
import model.GameState;

import java.io.*;
import java.net.*;

/**
 * Thin client wrapper around the multiplayer socket connection.
 *
 * <p>The Swing multiplayer UI uses this class to join a server, send outbound requests,
 * and receive serialized {@link NetworkMessage} instances.
 */
public class GameClient {
    private final String host;
    private final int port;
    private final String playerName;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private GameState currentState;
    private int myPlayerIndex = -1;
    private volatile boolean connected = false;

    /**
     * Creates a client connection target.
     *
     * @param host server host name or IP address
     * @param port server port
     * @param playerName player name announced to the server when joining
     */
    public GameClient(String host, int port, String playerName) {
        this.host = host;
        this.port = port;
        this.playerName = playerName;
    }

    /**
     * Opens the socket connection and sends the initial join message.
     *
     * @return {@code true} if the connection was established successfully
     */
    public boolean connect() {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(NetworkMessage.join(playerName));
            out.flush();

            connected = true;
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e.getMessage());
            return false;
        }
    }

    /**
     * Closes the connection after attempting to notify the server.
     */
    public void disconnect() {
        connected = false;
        try {
            if (out != null) {
                out.writeObject(NetworkMessage.disconnect());
                out.flush();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            // Best-effort shutdown: the socket may already be closed or unreachable.
        }
    }

    /**
     * Sends a validated move request to the server.
     *
     * @param move move selected by the local player
     */
    public void sendMove(Move move) {
        sendMessage(NetworkMessage.move(move), "move");
    }

    /**
     * Asks the server to start the lobby's next game.
     */
    public void sendStartRequest() {
        sendMessage(NetworkMessage.startRequest(), "start request");
    }

    /**
     * Blocks until the next message arrives from the server.
     *
     * @return the received message, or {@code null} if the connection fails
     */
    public NetworkMessage receiveMessage() {
        if (!connected) return null;
        try {
            return (NetworkMessage) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Failed to receive message: " + e.getMessage());
            connected = false;
            return null;
        }
    }

    /**
     * Indicates whether the client still considers the socket connection active.
     *
     * @return {@code true} if the client is connected
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Returns the most recently stored game state snapshot.
     *
     * @return latest cached game state, or {@code null} if none has been stored
     */
    public GameState getCurrentState() {
        return currentState;
    }

    /**
     * Returns this client's assigned player index.
     *
     * @return zero-based player index, or {@code -1} if not assigned yet
     */
    public int getMyPlayerIndex() {
        return myPlayerIndex;
    }

    /**
     * Returns the local player's display name.
     *
     * @return player name announced during join
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Stores the latest state snapshot received from the server.
     *
     * @param state latest game state
     */
    public void setCurrentState(GameState state) {
        this.currentState = state;
    }

    /**
     * Updates the player index assigned by the server.
     *
     * @param index zero-based player index
     */
    public void setMyPlayerIndex(int index) {
        this.myPlayerIndex = index;
    }

    private void sendMessage(NetworkMessage message, String actionName) {
        if (!connected) {
            return;
        }
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to send " + actionName + ": " + e.getMessage());
            connected = false;
        }
    }
}
