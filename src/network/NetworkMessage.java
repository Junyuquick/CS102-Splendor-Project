package network;

import engine.moves.Move;
import model.GameState;

import java.io.Serializable;
import java.util.List;

/**
 * Represents one serialized message traveling between a client and the
 * server.
 *
 * Instead of exposing many constructors, this class uses named
 * factory methods so each call site clearly expresses which protocol
 * message it is building.
 */
public class NetworkMessage implements Serializable {
    /**
     * Identifies which protocol event this message represents.
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
     * Creates the first message a client sends when entering the lobby.
     *
     * @param playerName player name to register in the lobby
     * @return join message
     */
    public static NetworkMessage join(String playerName) {
        return new NetworkMessage(
                Type.JOIN,
                playerName,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Creates the acknowledgement that tells a client its lobby slot.
     *
     * @param playerIndex zero-based player index assigned by the server
     * @return join acknowledgement message
     */
    public static NetworkMessage joinAck(int playerIndex) {
        return new NetworkMessage(
                Type.JOIN_ACK,
                null,
                null,
                null,
                null,
                playerIndex,
                null,
                null,
                null
        );
    }

    /**
     * Creates a message carrying one player move to the server.
     *
     * @param move move chosen by the client
     * @return move message
     */
    public static NetworkMessage move(Move move) {
        return new NetworkMessage(
                Type.MOVE,
                null,
                move,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Creates a lobby update containing player names and host details.
     *
     * @param playerNames connected player names in lobby order
     * @param hostIndex zero-based index of the host player
     * @param minPlayers minimum players required to start
     * @return lobby-update message
     */
    public static NetworkMessage lobbyUpdate(
            List<String> playerNames,
            int hostIndex,
            int minPlayers
    ) {
        return new NetworkMessage(
                Type.LOBBY_UPDATE,
                null,
                null,
                null,
                null,
                null,
                playerNames,
                hostIndex,
                minPlayers
        );
    }

    /**
     * Creates a full game-state update for clients to render.
     *
     * @param state latest game state
     * @return state-update message
     */
    public static NetworkMessage stateUpdate(GameState state) {
        return new NetworkMessage(
                Type.STATE_UPDATE,
                null,
                null,
                state,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Creates an error message explaining why a request failed.
     *
     * @param message human-readable error text
     * @return error message
     */
    public static NetworkMessage error(String message) {
        return new NetworkMessage(
                Type.ERROR,
                null,
                null,
                null,
                message,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Creates the message that tells a client a new game has started.
     *
     * @param initialState initial game state snapshot
     * @param playerIndex zero-based player index for the receiving client
     * @return game-start message
     */
    public static NetworkMessage gameStart(
            GameState initialState,
            int playerIndex
    ) {
        return new NetworkMessage(
                Type.GAME_START,
                null,
                null,
                initialState,
                null,
                playerIndex,
                null,
                null,
                null
        );
    }

    /**
     * Creates a message announcing that the game has ended.
     *
     * @param message human-readable result summary
     * @return game-over message
     */
    public static NetworkMessage gameOver(String message) {
        return new NetworkMessage(
                Type.GAME_OVER,
                null,
                null,
                null,
                message,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Creates a request asking the server to start the lobby's game.
     *
     * @return start-request message
     */
    public static NetworkMessage startRequest() {
        return new NetworkMessage(
                Type.START_REQUEST,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Creates the final message sent when a client disconnects cleanly.
     *
     * @return disconnect message
     */
    public static NetworkMessage disconnect() {
        return new NetworkMessage(
                Type.DISCONNECT,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Returns which protocol message type this object represents.
     *
     * @return protocol message type
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the player name stored in a join-related message.
     *
     * @return player name, or null when not applicable
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Returns the move payload stored in a move message.
     *
     * @return move payload, or null when not applicable
     */
    public Move getMove() {
        return move;
    }

    /**
     * Returns the full game-state snapshot carried by this message.
     *
     * @return game state payload, or null when not applicable
     */
    public GameState getGameState() {
        return gameState;
    }

    /**
     * Returns the text payload used for errors or end-of-game messages.
     *
     * @return message text, or null when not applicable
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the player index stored in join and start messages.
     *
     * @return zero-based player index, or null when not applicable
     */
    public Integer getPlayerIndex() {
        return playerIndex;
    }

    /**
     * Returns the player-name list stored in a lobby update.
     *
     * @return player names, or null when not applicable
     */
    public List<String> getPlayerNames() {
        return playerNames;
    }

    /**
     * Returns the host player's index from a lobby update.
     *
     * @return host index, or null when not applicable
     */
    public Integer getHostIndex() {
        return hostIndex;
    }

    /**
     * Returns the minimum player count advertised in a lobby update.
     *
     * @return minimum required players, or null when not applicable
     */
    public Integer getMinPlayers() {
        return minPlayers;
    }
}
