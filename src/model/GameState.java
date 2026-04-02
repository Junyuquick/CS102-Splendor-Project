package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Captures the entire Splendor game state at a specific moment.
 *
 * This is the engine's authoritative state container. It stores the
 * ordered players, the shared board and bank, the active turn index,
 * and the final-round flag.
 */
public class GameState implements Serializable {

    private final List<Player> players;
    private final Board board;
    private final GemBank bank;
    private int currentPlayerIndex;
    private boolean finalRound;

    /**
     * Creates a new game state.
     *
     * @param players the ordered list of players
     * @param board the shared board state
     * @param bank the shared gem token supply
     */
    public GameState(List<Player> players, Board board, GemBank bank) {
        this.players = new ArrayList<>(players);
        this.board = board;
        this.bank = bank;
        this.currentPlayerIndex = 0;
        this.finalRound = false;
    }

    /**
     * Returns the players in turn order.
     *
     * @return an unmodifiable view of the players list
     */
    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    /**
     * Returns the player at the specified index.
     *
     * @param index player index
     * @return player at that index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Player getPlayer(int index) {
        return players.get(index);
    }

    /**
     * Returns the shared board reference.
     *
     * @return the board
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Returns the shared gem bank reference.
     *
     * @return the gem bank
     */
    public GemBank getBank() {
        return bank;
    }

    /**
     * Returns the index of the current player.
     *
     * @return the current player index
     */
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    /**
     * Returns the player whose turn it is.
     *
     * @return the current player
     */
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    /**
     * Sets the current player index.
     *
     * @param index new current player index
     */
    public void setCurrentPlayerIndex(int index) {
        this.currentPlayerIndex = index;
    }

    /**
     * Returns whether the final round flag is set.
     *
     * @return true if the final round has been triggered, otherwise false
     */
    public boolean isFinalRound() {
        return finalRound;
    }

    /**
     * Sets the final round flag.
     *
     * @param value true to enable final round, otherwise false
     */
    public void setFinalRound(boolean value) {
        this.finalRound = value;
    }
}
