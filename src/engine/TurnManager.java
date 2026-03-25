package engine;

import model.*;

/**
 * Advances turn order and tracks which player triggered the final round.
 *
 * <p>When the final round starts, the manager records the triggering player so callers can
 * later detect when turn order has wrapped back to that player.
 */
public class TurnManager {
    
    private int finalRoundStartingPlayerIndex = -1;
    
    /**
     * Creates a TurnManager service instance.
     */
    public TurnManager() {
    }
    
    /**
     * Advances the turn to the next player in strict order, wrapping at the end.
     * 
     * If the final round is active, this method also checks if the turn order
     * has cycled back to the player who triggered it. When that happens,
     * the final round completes (but GameEngine/WinnerChecker handle the actual end-of-game logic).
     * 
     * @param state the game state (mutated to advance current player)
     */
    public void advanceTurn(GameState state) {
        int playerCount = state.getPlayers().size();
        int currentIndex = state.getCurrentPlayerIndex();
        
        int nextIndex = (currentIndex + 1) % playerCount;
        state.setCurrentPlayerIndex(nextIndex);
    }
    
    /**
     * Returns the current player index from the state.
     * 
     * @param state the game state
     * @return the current player index
     */
    public int getCurrentPlayerIndex(GameState state) {
        return state.getCurrentPlayerIndex();
    }
    
    /**
     * Returns whether the final round flag is active in the state.
     * The final round begins when a player reaches the win threshold
     * and continues until turn order cycles back to that player.
     * 
     * @param state the game state
     * @return true if final round is active, false otherwise
     */
    public boolean isFinalRound(GameState state) {
        return state.isFinalRound();
    }
    
    /**
     * Marks that the final round has been triggered.
     * Records the current player index so TurnManager can detect when
     * the turn order cycles back to this player (signaling end of final round).
     * 
     * Called by GameEngine when WinnerChecker detects a player has reached
     * the win threshold.
     * 
     * @param state the game state (read to get current player index)
     */
    public void markFinalRound(GameState state) {
        state.setFinalRound(true);
        this.finalRoundStartingPlayerIndex = state.getCurrentPlayerIndex();
    }
    
    /**
     * Checks if the final round has completed (cycled back to the triggering player).
     * Returns true only if:
     * - Final round is active
     * - The current player index has cycled back to the player who triggered it
     * 
     * GameEngine uses this to determine when the game should end.
     * 
     * @param state the game state
     * @return true if final round just completed, false otherwise
     */
    public boolean hasFinalRoundCompleted(GameState state) {
        if (!state.isFinalRound()) {
            return false;
        }
        
        if (finalRoundStartingPlayerIndex == -1) {
            return false;
        }
        return state.getCurrentPlayerIndex() == finalRoundStartingPlayerIndex;
    }
    
    /**
     * Resets the final round state (called after game ends).
     * Clears the recorded starting player index.
     */
    public void resetFinalRound() {
        this.finalRoundStartingPlayerIndex = -1;
    }
}
