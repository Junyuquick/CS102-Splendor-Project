package engine;

import model.*;
import java.util.*;

/**
 * Handles nobles that may be attracted at the end of a turn.
 *
 * <p>This class figures out which nobles a player qualifies for and can assign one to them.
 */
public class NobleAssigner {
    
    /**
     * Returns every noble on the board that this player currently qualifies for.
     *
     * @param state the game state
     * @param player the player to check
     * @return a list of eligible nobles, which may be empty
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
     * Checks whether the player's permanent bonuses are enough for one specific noble.
     *
     * @param player the player to check
     * @param noble the noble being checked
     * @return true if the player meets all of the noble's requirements
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
     * Gives the chosen noble to the player and removes it from the board.
     *
     * @param state the game state, which will be updated
     * @param player the player claiming the noble
     * @param chosen the noble tile to assign
     * @throws IllegalStateException if the player does not actually qualify for that noble
     */
    public void assignNoble(GameState state, Player player, NobleTile chosen) {
        if (!isEligibleForNoble(player, chosen)) {
            throw new IllegalStateException("Player is not eligible for this noble: " + chosen);
        }
        
        Board board = state.getBoard();
        board.removeNoble(chosen);
        player.addNoble(chosen);
    }
    
    /**
     * Automatically gives the player a noble when there is exactly one valid choice.
     *
     * @param state the game state, updated if a noble is assigned
     * @param player the player who might receive a noble
     * @return the assigned noble, or {@code null} if no automatic assignment happened
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
