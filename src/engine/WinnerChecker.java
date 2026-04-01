package engine;

import config.Config;
import model.*;
import java.util.*;

/**
 * Evaluates endgame conditions and chooses the winning player.
 *
 * <p>The final round is triggered once a player reaches the configured prestige threshold.
 * Winner selection then prefers highest prestige and breaks ties by fewest purchased cards.
 */
public class WinnerChecker {
    
    private final Config config;
    
    /**
     * Creates a checker that uses the supplied victory configuration.
     *
     * @param config game configuration containing the win threshold
     */
    public WinnerChecker(Config config) {
        this.config = config;
    }
    
    /**
     * Determines if the final round should be triggered.
     * The final round begins when any player reaches or exceeds the configured 
     * points to win threshold, and the final round is not already active.
     * 
     * @param state the game state
     * @return true if final round should be triggered, false otherwise
     */
    public boolean shouldTriggerFinalRound(GameState state) {
        if (state.isFinalRound()) {
            return false;
        }
        
        int winThreshold = config.getPointsToWin();
        
        for (Player player : state.getPlayers()) {
            if (player.getPrestigePoints() >= winThreshold) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Returns whether the game is in its endgame state.
     *
     * <p>This implementation reflects only the {@link GameState#isFinalRound()} flag. Callers
     * that need to know whether the last turn has completed must combine this with
     * {@link TurnManager#hasFinalRoundCompleted(model.GameState)}.
     * 
     * @param state the game state
     * @return true if the game is over, false otherwise
     */
    public boolean isFinalRoundActive(GameState state) {
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
        if (!isFinalRoundActive(state)) {
            throw new IllegalStateException("Cannot determine winner: final round is not active");
        }
        
        List<Player> players = state.getPlayers();
        if (players.isEmpty()) {
            throw new IllegalStateException("No players in game state");
        }
        
        Player winner = players.get(0);
        
        for (int i = 1; i < players.size(); i++) {
            Player challenger = players.get(i);
            
            int winnerPrestige = winner.getPrestigePoints();
            int challengerPrestige = challenger.getPrestigePoints();
            
            if (challengerPrestige > winnerPrestige) {
                winner = challenger;
            } else if (challengerPrestige == winnerPrestige) {
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
