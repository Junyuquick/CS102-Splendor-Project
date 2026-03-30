package engine;

import config.Config;
import model.*;
import java.util.*;

/**
 * Decides when the endgame starts and who wins once the game is over.
 *
 * <p>The final round starts when someone reaches the point threshold. After that,
 * the winner is the player with the most prestige, with ties broken by fewer bought cards.
 */
public class WinnerChecker {
    
    private final Config config;
    
    /**
     * Creates a winner checker using the current victory rules.
     *
     * @param config game settings containing the win threshold
     */
    public WinnerChecker(Config config) {
        this.config = config;
    }
    
    /**
     * Checks whether the game should enter the final round now.
     *
     * @param state the game state
     * @return true if the final round should start, false otherwise
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
     * <p>Right now this simply mirrors the final-round flag in the game state. Code that
     * needs to know whether the last turn has actually finished should combine this with
     * {@link TurnManager#hasFinalRoundCompleted(model.GameState)}.
     *
     * @param state the game state
     * @return true if the game is over, false otherwise
     */
    public boolean isGameOver(GameState state) {
        return state.isFinalRound();
    }
    
    /**
     * Picks the winner once the game has ended.
     *
     * @param state the game state
     * @return the winning player
     * @throws IllegalStateException if the game is not over yet
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
