package engine;

import config.Config;
import model.*;
import java.util.*;

/**
 * Decides when the game ends and who wins.
 * 
 * Enforces:
 * - Final-round ending rule (game continues until turn order returns to starting point)
 * - Winner selection by highest prestige points
 * - Tie-breaker by fewest purchased development cards
 * 
 * Called by GameEngine each turn after:
 * - Nobles are assigned
 * - Turn advancement is applied as needed
 */
public class WinnerChecker {
    
    private final Config config;
    
    /**
     * Constructs a WinnerChecker with the given configuration.
     * Config is used for the points-to-win threshold.
     * 
     * @param config the game configuration
     */
    public WinnerChecker(Config config) {
        this.config = config;
    }
    
    /**
     * Determines if the final round should be triggered.
     * The final round begins when any player reaches or exceeds the configured
     * points-to-win threshold, and the final round is not already active.
     * 
     * @param state the game state
     * @return true if final round should be triggered, false otherwise
     */
    public boolean shouldTriggerFinalRound(GameState state) {
        // Final round already active
        if (state.isFinalRound()) {
            return false;
        }
        
        int winThreshold = config.getpointsToWin();
        
        // Check if any player has reached the threshold
        for (Player player : state.getPlayers()) {
            if (player.getPrestigePoints() >= winThreshold) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Determines if the game is over.
     * The game ends when the final round has been triggered AND
     * the turn order has cycled back to the player who triggered it.
     * This is typically enforced by TurnManager.
     * 
     * This method checks if the final round flag is set.
     * The caller (GameEngine/TurnManager) must handle the turn-order cycling logic.
     * 
     * @param state the game state
     * @return true if the game is over, false otherwise
     */
    public boolean isGameOver(GameState state) {
        // Game is over when final round is set AND it's been confirmed complete by TurnManager.
        // This method returns true when final round has been triggered and processed.
        // In practice, TurnManager will call this when cycling back to the starting player.
        return state.isFinalRound();
    }
    
    /**
     * Determines the winner after the game ends.
     * Winner is the player with the highest prestige points.
     * Ties are broken by fewest purchased development cards.
     * 
     * @param state the game state
     * @return the winning player
     * @throws IllegalStateException if the game is not over
     */
    public Player determineWinner(GameState state) {
        if (!isGameOver(state)) {
            throw new IllegalStateException("Cannot determine winner: game is not over");
        }
        
        List<Player> players = state.getPlayers();
        if (players.isEmpty()) {
            throw new IllegalStateException("No players in game state");
        }
        
        Player winner = players.get(0);
        
        for (int i = 1; i < players.size(); i++) {
            Player challenger = players.get(i);
            
            // Compare by prestige first
            int winnerPrestige = winner.getPrestigePoints();
            int challengerPrestige = challenger.getPrestigePoints();
            
            if (challengerPrestige > winnerPrestige) {
                winner = challenger;
            } else if (challengerPrestige == winnerPrestige) {
                // Tie in prestige: compare by fewest cards
                int winnerCards = winner.getDevelopmentCardCount();
                int challengerCards = challenger.getDevelopmentCardCount();
                
                if (challengerCards < winnerCards) {
                    winner = challenger;
                }
            }
        }
        
        return winner;
    }
}
