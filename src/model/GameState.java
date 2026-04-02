package model;

import java.io.Serializable;
import java.util.*;

/**
 * Captures the entire Splendor game state at a specific moment.
 * 
 * Every move and check that occurs in the game will read, write and modify the data in this class
 * 
 * Centralizes the whole game:
 * - Players (in turns)
 * - Board (decks, face-up cards, nobles)
 * - Bank (gem token supply)
 * - Current player turn
 * - Final round indicator
 */
public class GameState implements Serializable {
    
    private final List<Player> players;
    private final Board board;
    private final GemBank bank;
    private int currentPlayerIndex;
    private boolean finalRound;
    
    /**
     * Creates a new game state with the players, board, and the gem bank.
     * Starts with player 0 as the current player and finalRound indicator as false.
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
     * Returns the list of players (ordered by turns order)
     * 
     */
    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }
    
    /**
     * Returns the player based on player index.
     * 
     */
    public Player getPlayer(int index) {
        return players.get(index);
    }
    
    /**
     * Returns the shared board.
     *
     */
    public Board getBoard() {
        return board;
    }
    
    /**
     * Returns the shared gem bank.
     * 
     */
    public GemBank getBank() {
        return bank;
    }
    
    /**
     * Returns the index of the player whose turn is up.
     * 
     */
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }
    
    /**
     * Returns the player whose turn is up.
     * 
     */
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }
    
    /**
     * set the current player's index.
     * To be used by TurnManager or GameEngine.
     * 
     * @param index the new current player index
     */
    public void setCurrentPlayerIndex(int index) {
        this.currentPlayerIndex = index;
    }
    
    /**
     * Returns whether the final round flag is set.
     * The final round starts when a player meets the required prestige points to win the game
     * Game ends when the final round ends.
     * 
     * @return true if game is in final round
     */
    public boolean isFinalRound() {
        return finalRound;
    }
    
    /**
     * Sets the final round indicator.
    *
     * @param value true to start final round, false otherwise
     */
    public void setFinalRound(boolean value) {
        this.finalRound = value;
    }
}
