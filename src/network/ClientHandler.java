package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Represents one server-side connection to one player.
 *
 * The server creates one handler per socket. This class reads
 * messages coming from that client and forwards them to GameServer,
 * and it also sends server responses back out on the same connection.
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
     * Runs the full lifecycle of the client connection.
     *
     * The first message must be a join request. After that, the
     * handler keeps listening for gameplay and lobby messages until the
     * client disconnects or the socket fails.
     */
    @Override
    public void run() {
        try {
            // Create the output stream first so both sides agree on the
            // object-stream handshake order.
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
                // Every later message is routed back into the main server
                // so the server stays the single place that owns game state.
                msg = (NetworkMessage) in.readObject();
                switch (msg.getType()) {
                    case MOVE:
                        server.handleMove(this, msg.getMove());
                        break;
                    case START_REQUEST:
                        server.handleStartRequest(this);
                        break;
                    case DISCONNECT:
                        System.out.println(
                                "Player disconnected: " + playerName
                        );
                        return;
                    default:
                        sendMessage(
                                NetworkMessage.error("Unknown message type")
                        );
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // If shutdown is already in progress, a close failure does
                // not change the outcome, so we simply finish cleanup.
            }
            server.removeClient(this);
        }
    }

    /**
     * Sends one protocol message to the client currently attached to
     * this handler.
     *
     * @param msg message to send
     */
    public void sendMessage(NetworkMessage msg) {
        try {
            // ObjectOutputStream remembers previously-sent objects.
            // Resetting here forces each state update to go out as a fresh
            // snapshot instead of reusing stale object references.
            out.reset();
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println(
                    "Failed to send message to client: " + e.getMessage()
            );
        }
    }

    /**
     * Returns the display name sent in the JOIN message.
     *
     * @return player name, or null before the join message is processed
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Returns the player slot assigned by the server for the current game.
     *
     * @return player index, or null if not assigned yet
     */
    public Integer getPlayerIndex() {
        return playerIndex;
    }

    /**
     * Stores the player slot assigned to this client for the current game.
     *
     * @param index zero-based player index
     */
    public void setPlayerIndex(int index) {
        this.playerIndex = index;
    }
}
