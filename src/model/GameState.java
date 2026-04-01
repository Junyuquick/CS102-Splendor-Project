package model;

import java.io.Serializable;
import java.util.*;

/**
 * Captures the entire Splendor game state at a specific moment.
 * 
 * Serves as the engine's authoritative state container. Every rule check and move
 * execution reads and writes to the same data in this class.
 * 
 * Centralizes the core game objects and flags:
 * - Players (in turn order)
 * - Board (decks, face-up cards, nobles)
 * - Bank (gem token supply)
 * - Current player turn
 * - Final round flag
 */
public class GameState implements Serializable {
    
    private final List<Player> players;
    private final Board board;
    private final GemBank bank;
    private int currentPlayerIndex;
    private boolean finalRound;
    
    /**
     * Creates a new game state with initialized players, board, and bank.
     * Starts with player 0 as the current player and finalRound as false.
     * 
     * @param players the ordered list of players (defines turn order)
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
     * Returns an unmodifiable view of the ordered list of players.
     * The list order defines turn order.
     * 
     * @return an unmodifiable view of the players list
     */
    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }
    
    /**
     * Returns the player at the specified index.
     * 
     * @param index the player index
     * @return the player at that index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Player getPlayer(int index) {
        return players.get(index);
    }
    
    /**
     * Returns the shared board reference.
     * Includes decks, face-up cards, and available nobles.
     * 
     * @return the board
     */
    public Board getBoard() {
        return board;
    }
    
    /**
     * Returns the shared gem bank reference.
     * Players draw tokens from it and return tokens to it.
     * 
     * @return the gem bank
     */
    public GemBank getBank() {
        return bank;
    }
    
    /**
     * Returns the index of the player whose turn it currently is.
     * 
     * @return the current player index
     */
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }
    
    /**
     * Returns the player whose turn it is.
     * Derived from currentPlayerIndex.
     * 
     * @return the current player
     */
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }
    
    /**
     * Updates the current player index.
     * Typically controlled by TurnManager or GameEngine.
     * 
     * @param index the new current player index
     */
    public void setCurrentPlayerIndex(int index) {
        this.currentPlayerIndex = index;
    }
    
    /**
     * Returns whether the final round flag is set.
     * The final round is triggered once a player reaches the winning prestige threshold.
     * The game continues until turn order returns to the triggering point.
     * 
     * @return true if the final round has been triggered, false otherwise
     */
    public boolean isFinalRound() {
        return finalRound;
    }
    
    /**
     * Sets the final round flag.
     * Typically set when a player first reaches the configured winning prestige points.
     * The TurnManager enforces the actual end-of-game logic.
     * 
     * @param value true to enable final round, false to disable
     */
    public void setFinalRound(boolean value) {
        this.finalRound = value;
    }
}
