package network;

import java.io.*;
import java.net.*;

/**
 * Handles communication with a single client.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameServer server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String playerName;
    private Integer playerIndex;

    /**
     * Creates a handler for one accepted client socket.
     *
     * @param socket connected client socket
     * @param server server coordinating the lobby and game state
     */
    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }

    /**
     * Processes the client's incoming message stream until the connection closes.
     */
    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            NetworkMessage msg = (NetworkMessage) in.readObject();
            if (msg.getType() == NetworkMessage.Type.JOIN) {
                playerName = msg.getPlayerName();
                System.out.println("Player joined: " + playerName);
                server.handleJoin(this);
            } else {
                sendMessage(NetworkMessage.error("Expected JOIN message"));
                return;
            }

            while (true) {
                msg = (NetworkMessage) in.readObject();
                switch (msg.getType()) {
                    case MOVE:
                        server.handleMove(this, msg.getMove());
                        break;
                    case START_REQUEST:
                        server.handleStartRequest(this);
                        break;
                    case DISCONNECT:
                        System.out.println("Player disconnected: " + playerName);
                        return;
                    default:
                        sendMessage(NetworkMessage.error("Unknown message type"));
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore close failures during connection teardown.
            }
            server.removeClient(this);
        }
    }

    /**
     * Sends a protocol message to the connected client.
     *
     * @param msg message to send
     */
    public void sendMessage(NetworkMessage msg) {
        try {
            // ObjectOutputStream caches object identities; reset so each update sends
            // the latest mutable game state instead of back-references to stale data.
            out.reset();
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to send message to client: " + e.getMessage());
        }
    }

    /**
     * Returns the player's joined display name.
     *
     * @return player name, or {@code null} before the join message is processed
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Returns the zero-based player index assigned by the server.
     *
     * @return player index, or {@code null} if not assigned yet
     */
    public Integer getPlayerIndex() {
        return playerIndex;
    }

    /**
     * Stores the player index assigned for the current game.
     *
     * @param index zero-based player index
     */
    public void setPlayerIndex(int index) {
        this.playerIndex = index;
    }
}
