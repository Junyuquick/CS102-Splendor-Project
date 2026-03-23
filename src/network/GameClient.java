package network;

import engine.Move;
import model.GameState;

import java.io.*;
import java.net.*;

/**
 * Client for connecting to a multiplayer Splendor game.
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

    public GameClient(String host, int port, String playerName) {
        this.host = host;
        this.port = port;
        this.playerName = playerName;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Send join message
            out.writeObject(NetworkMessage.join(playerName));
            out.flush();

            connected = true;
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e.getMessage());
            return false;
        }
    }

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
            // ignore
        }
    }

    public void sendMove(Move move) {
        if (!connected) return;
        try {
            out.writeObject(NetworkMessage.move(move));
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to send move: " + e.getMessage());
            connected = false;
        }
    }

    public void sendStartRequest() {
        if (!connected) return;
        try {
            out.writeObject(NetworkMessage.startRequest());
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to send start request: " + e.getMessage());
            connected = false;
        }
    }

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

    public boolean isConnected() {
        return connected;
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public int getMyPlayerIndex() {
        return myPlayerIndex;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setCurrentState(GameState state) {
        this.currentState = state;
    }

    public void setMyPlayerIndex(int index) {
        this.myPlayerIndex = index;
    }
}
