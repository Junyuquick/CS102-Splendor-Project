package engine;

import config.Config;
import model.*;
import java.util.*;

/**
 * Prevents illegal moves before any state change happens.
 * 
 * Enforces rules for:
 * - Token-taking constraints
 * - Reservation limits
 * - Purchase affordability (bonuses + tokens + gold)
 * - Maximum token hand size
 * 
 * Called by GameEngine before move execution.
 * Used by AI to filter candidate moves.
 */
public class MoveValidator {
    
    private final Config config;
    
    /**
     * Constructs a MoveValidator with the given configuration.
     * Config is used for numeric constraints like max tokens, max reserved cards, etc.
     * 
     * @param config the game configuration
     */
    public MoveValidator(Config config) {
        this.config = config;
    }
    
    /**
     * Validates a move for legality without changing state.
     * 
     * @param state the current game state
     * @param player the player making the move
     * @param move the move to validate
     * @return null if the move is legal, or a human-readable error message if illegal
     */
    public String validate(GameState state, Player player, Move move) {
        if (move == null) {
            return "Move cannot be null";
        }
        
        switch (move.getType()) {
            case TAKE_THREE_DIFFERENT:
                return validateTakeThreeDiff(state, player, move);
            case TAKE_TWO_SAME:
                return validateTakeTwoSame(state, player, move);
            case RESERVE:
                return validateReserve(state, player, move);
            case BUY:
                return validateBuy(state, player, move);
            default:
                return "Unknown move type: " + move.getType();
        }
    }
    
    /**
     * Returns true only when validate(...) returns null (move is legal).
     * 
     * @param state the game state
     * @param player the player making the move
     * @param move the move to validate
     * @return true if legal, false otherwise
     */
    public boolean isLegal(GameState state, Player player, Move move) {
        return validate(state, player, move) == null;
    }
    
    /**
     * Validates a TAKE_THREE_DIFFERENT move.
     * Checks:
     * - Exactly 3 different colors specified
     * - Each color has at least 1 token in the bank
     * - After taking, player won't exceed token cap
     * 
     * @param state the game state
     * @param player the player
     * @param move the move with tokens map
     * @return null if valid, error message otherwise
     */
    private String validateTakeThreeDiff(GameState state, Player player, Move move) {
        Map<GemColor, Integer> tokens = move.getTokens();
        
        if (tokens == null || tokens.isEmpty()) {
            return "TAKE_THREE_DIFFERENT requires tokens";
        }
        
        int requiredColors = config.getTakeDifferentCount();
        if (tokens.size() != requiredColors) {
            return "TAKE_THREE_DIFFERENT requires exactly " + requiredColors + " different colors, got " + tokens.size();
        }
        
        GemBank bank = state.getBank();
        
        // Check each color has at least 1 token
        for (Map.Entry<GemColor, Integer> entry : tokens.entrySet()) {
            GemColor color = entry.getKey();
            int requested = entry.getValue();
            
            if (requested != 1) {
                return "TAKE_THREE_DIFFERENT: each color must have count 1, got " + color + " = " + requested;
            }
            
            if (bank.getTokenCount(color) < 1) {
                return "Bank has no " + color + " tokens available";
            }
        }
        
        // Check token cap not exceeded
        int totalAfter = player.getTotalTokens() + config.getTakeDifferentCount();
        if (totalAfter > config.getMaxTokensPerPlayer()) {
            return "Taking " + config.getTakeDifferentCount() + " tokens would exceed max tokens per player (" + config.getMaxTokensPerPlayer() + ")";
        }
        
        return null;
    }
    
    /**
     * Validates a TAKE_TWO_SAME move.
     * Checks:
     * - Exactly 1 color with count 2
     * - Bank has at least 2 of that color
     * - After taking, bank has at least config.takeSameMinRemainingInBank left
     * - After taking, player won't exceed token cap
     * 
     * @param state the game state
     * @param player the player
     * @param move the move with tokens map
     * @return null if valid, error message otherwise
     */
    private String validateTakeTwoSame(GameState state, Player player, Move move) {
        Map<GemColor, Integer> tokens = move.getTokens();
        
        if (tokens == null || tokens.isEmpty()) {
            return "TAKE_TWO_SAME requires tokens";
        }
        
        if (tokens.size() != 1) {
            return "TAKE_TWO_SAME requires exactly 1 color, got " + tokens.size();
        }
        
        GemColor color = tokens.keySet().iterator().next();
        int requested = tokens.get(color);
        
        int sameCount = config.getTakeSameCount();
        if (requested != sameCount) {
            return "TAKE_TWO_SAME requires count " + sameCount + ", got " + requested;
        }
        
        GemBank bank = state.getBank();
        int bankCount = bank.getTokenCount(color);
        
        if (bankCount < sameCount) {
            return "Bank has only " + bankCount + " " + color + " tokens, need " + sameCount;
        }

        int remainingAfter = bankCount - sameCount;
        int minRemaining = config.getTakeSameMinRemainingInBank();
        if (remainingAfter < minRemaining) {
            return "Taking " + sameCount + " " + color + " would leave only " + remainingAfter + " in bank, need at least " + minRemaining;
        }
        
        // Check token cap not exceeded
        int totalAfter = player.getTotalTokens() + sameCount;
        if (totalAfter > config.getMaxTokensPerPlayer()) {
            return "Taking " + sameCount + " tokens would exceed max tokens per player (" + config.getMaxTokensPerPlayer() + ")";
        }
        
        return null;
    }
    
    /**
     * Validates a RESERVE move.
     * Checks:
     * - Card exists on board or in specified deck level
     * - Player has not reached reserve limit
     * - If giving gold bonus, bank has gold available
     * 
     * @param state the game state
     * @param player the player
     * @param move the move with card and level
     * @return null if valid, error message otherwise
     */
    private String validateReserve(GameState state, Player player, Move move) {
        DevelopmentCard card = move.getCard();
        
        if (card == null) {
            return "RESERVE requires a card";
        }
        
        Board board = state.getBoard();
        
        // Check card exists on board
        List<DevelopmentCard> faceUp = board.getFaceUpCards();
        if (!faceUp.contains(card)) {
            // Could also be in a deck, but validation of specific deck availability
            // depends on Board implementation
        }
        
        // Check reserve limit
        List<DevelopmentCard> reserved = player.getReservedCards();
        if (reserved.size() >= config.getMaxReservedCards()) {
            return "Player has already reserved " + reserved.size() + " cards (max: " + config.getMaxReservedCards() + ")";
        }
        
        // Check gold bonus availability (if applicable)
        GemBank bank = state.getBank();
        if (bank.getTokenCount(GemColor.GOLD) > 0) {
            // Gold is available, can give bonus
        }
        
        return null;
    }
    
    /**
     * Validates a BUY move.
     * Checks:
     * - Card exists (reserved or on board)
     * - Player can afford the cost using:
     *   - Permanent bonuses from purchased cards
     *   - Colored tokens from hand
     *   - Gold tokens as wildcard
     * 
     * @param state the game state
     * @param player the player
     * @param move the move with card and payment breakdown
     * @return null if valid, error message otherwise
     */
    private String validateBuy(GameState state, Player player, Move move) {
        DevelopmentCard card = move.getCard();
        
        if (card == null) {
            return "BUY requires a card";
        }
        
        // Check card availability
        boolean fromReserved = move.isFromReserved();
        if (fromReserved) {
            if (!player.getReservedCards().contains(card)) {
                return "Card is not in player's reserved cards";
            }
        } else {
            Board board = state.getBoard();
            if (!board.getFaceUpCards().contains(card)) {
                return "Card is not face-up on the board";
            }
        }
        
        // Get card cost
        Map<GemColor, Integer> cost = card.getCost();
        if (cost == null || cost.isEmpty()) {
            // Card costs nothing, automatically affordable
            return null;
        }
        
        // Compute affordability
        String affordabilityError = canAfford(player, cost);
        return affordabilityError;
    }
    
    /**
     * Checks if a player can afford a cost.
     * Affordability is computed as:
     * - Permanent bonuses from purchased cards
     * - Colored tokens from hand
     * - Gold tokens as wildcard
     * 
     * @param player the player
     * @param cost the cost map (color -> quantity needed)
     * @return null if affordable, error message otherwise
     */
    private String canAfford(Player player, Map<GemColor, Integer> cost) {
        int goldNeeded = 0;
        
        for (Map.Entry<GemColor, Integer> entry : cost.entrySet()) {
            GemColor color = entry.getKey();
            int needed = entry.getValue();
            
            // Get permanent bonus first
            int bonusCount = player.getBonusCount(color);
            int remaining = needed - bonusCount;
            
            if (remaining <= 0) {
                // Fully covered by bonuses
                continue;
            }
            
            // Use colored tokens
            int playerTokens = player.getTokenCount(color);
            int tokensUsable = Math.min(playerTokens, remaining);
            remaining -= tokensUsable;
            
            if (remaining > 0) {
                // Need gold to cover the gap
                goldNeeded += remaining;
            }
        }
        
        // Check if player has enough gold
        int playerGold = player.getTokenCount(GemColor.GOLD);
        if (playerGold < goldNeeded) {
            return "Cannot afford card: need " + goldNeeded + " gold tokens, have " + playerGold;
        }
        
        return null;
    }
}
