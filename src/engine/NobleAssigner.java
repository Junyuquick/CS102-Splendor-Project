package engine;

import model.*;
import java.util.*;

/**
 * Handles end-of-turn noble attraction.
 * 
 * Enforces:
 * - Noble eligibility rules (based on player bonus counts)
 * - At most one noble gained per turn
 * - Player choice when multiple nobles are eligible
 * 
 * Called by GameEngine after the move is executed.
 */
public class NobleAssigner {
    
    /**
     * Finds all nobles on the board that the player is eligible to attract.
     * A player is eligible for a noble if their bonus counts meet or exceed
     * all of the noble's requirements.
     * 
     * @param state the game state
     * @param player the player to check eligibility for
     * @return a list of eligible nobles (may be empty)
     */
    public List<NobleTile> findEligibleNobles(GameState state, Player player) {
        Board board = state.getBoard();
        List<NobleTile> availableNobles = board.getAvailableNobles();
        List<NobleTile> eligible = new ArrayList<>();
        
        for (NobleTile noble : availableNobles) {
            if (isEligibleForNoble(player, noble)) {
                eligible.add(noble);
            }
        }
        
        return eligible;
    }
    
    /**
     * Checks if a player is eligible for a specific noble.
     * Eligibility is determined by comparing the player's bonus counts
     * with the noble's requirements.
     * 
     * @param player the player to check
     * @param noble the noble to check eligibility for
     * @return true if player bonuses meet all noble requirements
     */
    private boolean isEligibleForNoble(Player player, NobleTile noble) {
        Map<GemColor, Integer> requirements = noble.getRequirements();
        
        for (Map.Entry<GemColor, Integer> req : requirements.entrySet()) {
            GemColor color = req.getKey();
            int requiredBonus = req.getValue();
            int playerBonus = player.getBonusCount(color);
            
            if (playerBonus < requiredBonus) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Assigns a chosen noble to the player.
     * Removes the noble from the board and adds it to the player.
     * Enforces the "at most one noble per turn" rule.
     * 
     * @param state the game state (mutated)
     * @param player the player claiming the noble
     * @param chosen the noble tile to assign
     * @throws IllegalStateException if the noble is not eligible
     */
    public void assignNoble(GameState state, Player player, NobleTile chosen) {
        if (!isEligibleForNoble(player, chosen)) {
            throw new IllegalStateException("Player is not eligible for this noble: " + chosen);
        }
        
        Board board = state.getBoard();
        
        // Remove from board
        board.removeNoble(chosen);
        
        // Add to player
        player.addNoble(chosen);
    }
    
    /**
     * Automatically assigns a noble if exactly one is eligible.
     * If 0 nobles are eligible, does nothing and returns null.
     * If 1 noble is eligible, assigns it and returns it.
     * If 2+ nobles are eligible, does nothing and returns null (player must choose).
     * 
     * Enforces "at most one per turn" by limiting to single assignment.
     * 
     * @param state the game state (mutated if auto-assignment occurs)
     * @param player the player to potentially assign a noble to
     * @return the assigned noble, or null if no assignment occurred
     */
    public NobleTile autoAssignIfSingle(GameState state, Player player) {
        List<NobleTile> eligible = findEligibleNobles(state, player);
        
        if (eligible.size() == 1) {
            NobleTile chosen = eligible.get(0);
            assignNoble(state, player, chosen);
            return chosen;
        }
        
        return null;
    }
}
