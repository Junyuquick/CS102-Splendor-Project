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

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // First message should be JOIN
            NetworkMessage msg = (NetworkMessage) in.readObject();
            if (msg.getType() == NetworkMessage.Type.JOIN) {
                playerName = msg.getPlayerName();
                System.out.println("Player joined: " + playerName);
                server.handleJoin(this);
            } else {
                sendMessage(NetworkMessage.error("Expected JOIN message"));
                return;
            }

            // Handle subsequent messages
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
                // ignore
            }
            server.removeClient(this);
        }
    }

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

    public String getPlayerName() {
        return playerName;
    }

    public Integer getPlayerIndex() {
        return playerIndex;
    }

    public void setPlayerIndex(int index) {
        this.playerIndex = index;
    }
}
