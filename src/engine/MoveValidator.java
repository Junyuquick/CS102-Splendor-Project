package engine;

import config.Config;
import model.*;
import java.util.*;

/**
 * Checks whether a move is legal before anything in the game state changes.
 *
 * <p>If a move is valid, this class returns {@code null}. If not, it returns a message
 * explaining what was wrong.
 */
public class MoveValidator {
    
    private final Config config;
    
    /**
     * Creates a validator that uses the current game rules.
     *
     * @param config game settings that contain the numeric move limits
     */
    public MoveValidator(Config config) {
        this.config = config;
    }
    
    /**
     * Checks whether the given move is allowed right now.
     *
     * @param state the current game state
     * @param player the player making the move
     * @param move the move to check
     * @return {@code null} if the move is legal, otherwise an error message
     */
    public String validate(GameState state, Player player, Move move) {
        if (move == null) {
            return "Move cannot be null";
        }

        if (move instanceof TakeDifferentMove) {
            return validateTakeThreeDiff(state, player, move);
        }
        if (move instanceof TakeSameMove) {
            return validateTakeTwoSame(state, player, move);
        }
        if (move instanceof ReserveMove) {
            return validateReserve(state, player, move);
        }
        if (move instanceof BuyMove) {
            return validateBuy(state, player, move);
        }

        return "Unknown move type: " + move.getTypeName();
    }
    
    /**
     * Convenience helper that turns the validation result into a simple true or false.
     *
     * @param state the game state
     * @param player the player making the move
     * @param move the move to check
     * @return true if the move is legal, false otherwise
     */
    public boolean isLegal(GameState state, Player player, Move move) {
        return validate(state, player, move) == null;
    }
    
    /**
     * Checks the move where a player takes three different colors.
     *
     * @param state the game state
     * @param player the player
     * @param move the move containing the selected tokens
     * @return {@code null} if valid, otherwise an error message
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
     * Checks the move where a player takes two tokens of the same color.
     *
     * @param state the game state
     * @param player the player
     * @param move the move containing the selected tokens
     * @return {@code null} if valid, otherwise an error message
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
     * Checks whether the player can reserve the chosen card.
     *
     * <p>This mainly makes sure a card was supplied and the player has not already hit
     * the reserve limit.
     *
     * @param state the game state
     * @param player the player
     * @param move the move containing the card and level
     * @return {@code null} if valid, otherwise an error message
     */
    private String validateReserve(GameState state, Player player, Move move) {
        DevelopmentCard card = move.getCard();
        
        if (card == null) {
            return "RESERVE requires a card";
        }
        
        Board board = state.getBoard();
        int cardLevel = move.getCardLevel();

        if (cardLevel == -1) {
            List<DevelopmentCard> faceUp = board.getFaceUpCards();
            if (!faceUp.contains(card)) {
                return "Card is not face-up on the board";
            }
        } else if (cardLevel < 1 || cardLevel > config.getNumLevels()) {
            return "Invalid reserve deck level: " + cardLevel;
        }
        
        List<DevelopmentCard> reserved = player.getReservedCards();
        if (reserved.size() >= config.getMaxReservedCards()) {
            return "Player has already reserved " + reserved.size() + " cards (max: " + config.getMaxReservedCards() + ")";
        }
        
        GemBank bank = state.getBank();
        if (bank.getTokenCount(GemColor.GOLD) > 0) {
            // Reserving is still allowed even if there is no gold token to take.
        }
        
        return null;
    }
    
    /**
     * Checks whether the player can afford and buy the chosen card.
     *
     * @param state the game state
     * @param player the player
     * @param move the move containing the card and payment details
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
            if (!normalizeTokenMap(move.getPaymentTokens()).isEmpty()) {
                return "BUY payment tokens must be empty for a free card";
            }
            return null;
        }

        if (!PaymentCalculator.canAfford(player, cost)) {
            return buildAffordabilityError(player, cost);
        }
        return validatePaymentTokens(player, cost, move.getPaymentTokens());
    }

    private String validatePaymentTokens(Player player, Map<GemColor, Integer> cost, Map<GemColor, Integer> paymentTokens) {
        Map<GemColor, Integer> expected = PaymentCalculator.computePaymentTokens(player, cost);
        Map<GemColor, Integer> provided = normalizeTokenMap(paymentTokens);

        for (Map.Entry<GemColor, Integer> entry : provided.entrySet()) {
            if (entry.getValue() < 0) {
                return "BUY payment tokens cannot be negative";
            }
        }

        if (!provided.equals(expected)) {
            return "BUY payment tokens do not match the required payment: expected " + expected + ", got " + provided;
        }
        return null;
    }

    private Map<GemColor, Integer> normalizeTokenMap(Map<GemColor, Integer> tokens) {
        Map<GemColor, Integer> normalized = new EnumMap<>(GemColor.class);
        if (tokens == null) {
            return normalized;
        }

        for (Map.Entry<GemColor, Integer> entry : tokens.entrySet()) {
            GemColor color = entry.getKey();
            Integer amount = entry.getValue();
            if (color == null || amount == null) {
                continue;
            }
            if (amount != 0) {
                normalized.put(color, amount);
            }
        }
        return normalized;
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
    private String buildAffordabilityError(Player player, Map<GemColor, Integer> cost) {
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
