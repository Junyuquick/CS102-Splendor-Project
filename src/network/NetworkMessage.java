package network;

import engine.Move;
import model.GameState;

import java.io.Serializable;
import java.util.List;

/**
 * Simple message class for client-server communication.
 * Used to wrap different types of messages in the protocol.
 */
public class NetworkMessage implements Serializable {
    public enum Type {
        JOIN,           // Client joining with player name
        JOIN_ACK,       // Server confirms lobby seat / identity
        MOVE,           // Client sending a move
        START_REQUEST,  // Client requests host-start of multiplayer game
        LOBBY_UPDATE,   // Server broadcasts current lobby information
        STATE_UPDATE,   // Server sending updated game state
        ERROR,          // Server sending error message
        GAME_START,     // Server notifying game has started
        DISCONNECT      // Client disconnecting
    }

    private final Type type;
    private final String playerName;    // For JOIN
    private final Move move;            // For MOVE
    private final GameState gameState;  // For STATE_UPDATE
    private final String errorMessage;  // For ERROR
    private final Integer playerIndex;  // Assigned player index for JOIN response
    private final List<String> playerNames; // For LOBBY_UPDATE
    private final Integer hostIndex;    // For LOBBY_UPDATE
    private final Integer minPlayers;   // For LOBBY_UPDATE

    // Private constructor, use factory methods
    private NetworkMessage(
            Type type,
            String playerName,
            Move move,
            GameState gameState,
            String errorMessage,
            Integer playerIndex,
            List<String> playerNames,
            Integer hostIndex,
            Integer minPlayers
    ) {
        this.type = type;
        this.playerName = playerName;
        this.move = move;
        this.gameState = gameState;
        this.errorMessage = errorMessage;
        this.playerIndex = playerIndex;
        this.playerNames = playerNames;
        this.hostIndex = hostIndex;
        this.minPlayers = minPlayers;
    }

    // Factory methods
    public static NetworkMessage join(String playerName) {
        return new NetworkMessage(Type.JOIN, playerName, null, null, null, null, null, null, null);
    }

    public static NetworkMessage joinAck(int playerIndex) {
        return new NetworkMessage(Type.JOIN_ACK, null, null, null, null, playerIndex, null, null, null);
    }

    public static NetworkMessage move(Move move) {
        return new NetworkMessage(Type.MOVE, null, move, null, null, null, null, null, null);
    }

    public static NetworkMessage lobbyUpdate(List<String> playerNames, int hostIndex, int minPlayers) {
        return new NetworkMessage(Type.LOBBY_UPDATE, null, null, null, null, null, playerNames, hostIndex, minPlayers);
    }

    public static NetworkMessage stateUpdate(GameState state) {
        return new NetworkMessage(Type.STATE_UPDATE, null, null, state, null, null, null, null, null);
    }

    public static NetworkMessage error(String message) {
        return new NetworkMessage(Type.ERROR, null, null, null, message, null, null, null, null);
    }

    public static NetworkMessage gameStart(GameState initialState, int playerIndex) {
        return new NetworkMessage(Type.GAME_START, null, null, initialState, null, playerIndex, null, null, null);
    }

    public static NetworkMessage startRequest() {
        return new NetworkMessage(Type.START_REQUEST, null, null, null, null, null, null, null, null);
    }

    public static NetworkMessage disconnect() {
        return new NetworkMessage(Type.DISCONNECT, null, null, null, null, null, null, null, null);
    }

    // Getters
    public Type getType() { return type; }
    public String getPlayerName() { return playerName; }
    public Move getMove() { return move; }
    public GameState getGameState() { return gameState; }
    public String getErrorMessage() { return errorMessage; }
    public Integer getPlayerIndex() { return playerIndex; }
    public List<String> getPlayerNames() { return playerNames; }
    public Integer getHostIndex() { return hostIndex; }
    public Integer getMinPlayers() { return minPlayers; }
}
