package engine;

import config.Config;
import model.*;
import java.util.*;

/**
 * Checks whether a proposed move is legal before any game state is mutated.
 *
 * <p>The validator enforces token-taking rules, reserve limits, affordability, and the
 * per-player token cap. A {@code null} return value indicates that the move is legal.
 */
public class MoveValidator {
    
    private final Config config;
    
    /**
     * Creates a validator backed by the supplied rule configuration.
     *
     * @param config game configuration containing numeric move constraints
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
        
        int totalAfter = player.getTotalTokens() + sameCount;
        if (totalAfter > config.getMaxTokensPerPlayer()) {
            return "Taking " + sameCount + " tokens would exceed max tokens per player (" + config.getMaxTokensPerPlayer() + ")";
        }
        
        return null;
    }
    
    /**
     * Validates a RESERVE move.
     * Checks:
     * - A card was supplied
     * - The player has not reached the reserve limit
     *
     * <p>The current implementation accepts face-up reservations and relies on downstream
     * board logic to remove the selected card.
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
        
        List<DevelopmentCard> faceUp = board.getFaceUpCards();
        if (!faceUp.contains(card)) {
            // Deck-reservation validation is intentionally deferred because the board API does
            // not expose deck contents in a way that identifies a specific hidden card.
        }
        
        List<DevelopmentCard> reserved = player.getReservedCards();
        if (reserved.size() >= config.getMaxReservedCards()) {
            return "Player has already reserved " + reserved.size() + " cards (max: " + config.getMaxReservedCards() + ")";
        }
        
        GemBank bank = state.getBank();
        if (bank.getTokenCount(GemColor.GOLD) > 0) {
            // Reserving remains legal whether or not a gold bonus is available.
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
        
        Map<GemColor, Integer> cost = card.getCost();
        if (cost == null || cost.isEmpty()) {
            return null;
        }
        
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
            
            int bonusCount = player.getBonusCount(color);
            int remaining = needed - bonusCount;
            
            if (remaining <= 0) {
                continue;
            }
            
            int playerTokens = player.getTokenCount(color);
            int tokensUsable = Math.min(playerTokens, remaining);
            remaining -= tokensUsable;
            
            if (remaining > 0) {
                goldNeeded += remaining;
            }
        }
        
        int playerGold = player.getTokenCount(GemColor.GOLD);
        if (playerGold < goldNeeded) {
            return "Cannot afford card: need " + goldNeeded + " gold tokens, have " + playerGold;
        }
        
        return null;
    }
}
