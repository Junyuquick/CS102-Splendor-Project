package network;

import engine.Move;
import model.GameState;

import java.io.Serializable;
import java.util.List;

/**
 * Serializable protocol message exchanged between multiplayer clients and the server.
 *
 * <p>Instances are created through static factory methods so each message type carries only
 * the fields relevant to that protocol event.
 */
public class NetworkMessage implements Serializable {
    /**
     * Identifies the kind of payload carried by a {@link NetworkMessage}.
     */
    public enum Type {
        JOIN,
        JOIN_ACK,
        MOVE,
        START_REQUEST,
        LOBBY_UPDATE,
        STATE_UPDATE,
        ERROR,
        GAME_START,
        GAME_OVER,
        DISCONNECT
    }

    private final Type type;
    private final String playerName;
    private final Move move;
    private final GameState gameState;
    private final String errorMessage;
    private final Integer playerIndex;
    private final List<String> playerNames;
    private final Integer hostIndex;
    private final Integer minPlayers;

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

    /**
     * Creates a join request for a newly connecting client.
     *
     * @param playerName player name to register in the lobby
     * @return join message
     */
    public static NetworkMessage join(String playerName) {
        return new NetworkMessage(Type.JOIN, playerName, null, null, null, null, null, null, null);
    }

    /**
     * Creates a server acknowledgement assigning a lobby seat.
     *
     * @param playerIndex zero-based player index assigned by the server
     * @return join acknowledgement message
     */
    public static NetworkMessage joinAck(int playerIndex) {
        return new NetworkMessage(Type.JOIN_ACK, null, null, null, null, playerIndex, null, null, null);
    }

    /**
     * Creates a move-submission message.
     *
     * @param move move chosen by the client
     * @return move message
     */
    public static NetworkMessage move(Move move) {
        return new NetworkMessage(Type.MOVE, null, move, null, null, null, null, null, null);
    }

    /**
     * Creates a lobby-state update.
     *
     * @param playerNames connected player names in lobby order
     * @param hostIndex zero-based index of the host player
     * @param minPlayers minimum players required to start
     * @return lobby-update message
     */
    public static NetworkMessage lobbyUpdate(List<String> playerNames, int hostIndex, int minPlayers) {
        return new NetworkMessage(Type.LOBBY_UPDATE, null, null, null, null, null, playerNames, hostIndex, minPlayers);
    }

    /**
     * Creates a message carrying a fresh game-state snapshot.
     *
     * @param state latest game state
     * @return state-update message
     */
    public static NetworkMessage stateUpdate(GameState state) {
        return new NetworkMessage(Type.STATE_UPDATE, null, null, state, null, null, null, null, null);
    }

    /**
     * Creates an error message for the client.
     *
     * @param message human-readable error text
     * @return error message
     */
    public static NetworkMessage error(String message) {
        return new NetworkMessage(Type.ERROR, null, null, null, message, null, null, null, null);
    }

    /**
     * Creates a message that starts a new multiplayer game.
     *
     * @param initialState initial game state snapshot
     * @param playerIndex zero-based player index for the receiving client
     * @return game-start message
     */
    public static NetworkMessage gameStart(GameState initialState, int playerIndex) {
        return new NetworkMessage(Type.GAME_START, null, null, initialState, null, playerIndex, null, null, null);
    }

    /**
     * Creates a game-over notification.
     *
     * @param message human-readable result summary
     * @return game-over message
     */
    public static NetworkMessage gameOver(String message) {
        return new NetworkMessage(Type.GAME_OVER, null, null, null, message, null, null, null, null);
    }

    /**
     * Creates a request asking the host server to start the game.
     *
     * @return start-request message
     */
    public static NetworkMessage startRequest() {
        return new NetworkMessage(Type.START_REQUEST, null, null, null, null, null, null, null, null);
    }

    /**
     * Creates a disconnect notification.
     *
     * @return disconnect message
     */
    public static NetworkMessage disconnect() {
        return new NetworkMessage(Type.DISCONNECT, null, null, null, null, null, null, null, null);
    }

    /**
     * Returns the message type.
     *
     * @return protocol message type
     */
    public Type getType() { return type; }
    /**
     * Returns the player name associated with a join message.
     *
     * @return player name, or {@code null} when not applicable
     */
    public String getPlayerName() { return playerName; }
    /**
     * Returns the move carried by a move message.
     *
     * @return move payload, or {@code null} when not applicable
     */
    public Move getMove() { return move; }
    /**
     * Returns the game-state snapshot carried by the message.
     *
     * @return game state payload, or {@code null} when not applicable
     */
    public GameState getGameState() { return gameState; }
    /**
     * Returns the error or result text carried by the message.
     *
     * @return message text, or {@code null} when not applicable
     */
    public String getErrorMessage() { return errorMessage; }
    /**
     * Returns the assigned player index carried by the message.
     *
     * @return zero-based player index, or {@code null} when not applicable
     */
    public Integer getPlayerIndex() { return playerIndex; }
    /**
     * Returns the lobby player list carried by a lobby update.
     *
     * @return player names, or {@code null} when not applicable
     */
    public List<String> getPlayerNames() { return playerNames; }
    /**
     * Returns the host player index carried by a lobby update.
     *
     * @return host index, or {@code null} when not applicable
     */
    public Integer getHostIndex() { return hostIndex; }
    /**
     * Returns the lobby's minimum start requirement.
     *
     * @return minimum required players, or {@code null} when not applicable
     */
    public Integer getMinPlayers() { return minPlayers; }
}
