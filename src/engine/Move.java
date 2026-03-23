package engine;

import model.*;
import java.io.Serializable;
import java.util.*;

/**
 * Represents exactly one player action for a turn.
 * 
 * Avoids parsing raw input inside the engine.
 * Each turn produces one Move instance.
 * 
 * Immutable. Instances are created by InputParser (human input) or AiController (computer players).
 * Consumed by MoveValidator and MoveExecutor.
 * Categorized by MoveType.
 */
public class Move implements Serializable {
    
    private final MoveType type;
    private final Map<GemColor, Integer> tokens;           // For TAKE_THREE_DIFFERENT, TAKE_TWO_SAME
    private final DevelopmentCard card;                    // For RESERVE, BUY
    private final Map<GemColor, Integer> paymentTokens;   // For BUY: token breakdown used to pay
    private final boolean fromReserved;                    // For BUY: true if buying from reserved, false if from board
    private final int cardLevel;                           // For RESERVE: level of deck to reserve from (1, 2, 3), -1 if face-up
    
    /**
     * Private constructor to enforce builder pattern or static factory methods.
     */
    private Move(MoveType type,
                 Map<GemColor, Integer> tokens,
                 DevelopmentCard card,
                 Map<GemColor, Integer> paymentTokens,
                 boolean fromReserved,
                 int cardLevel) {
        this.type = type;
        this.tokens = tokens;
        this.card = card;
        this.paymentTokens = paymentTokens;
        this.fromReserved = fromReserved;
        this.cardLevel = cardLevel;
    }
    
    /**
     * Returns the action type so the validator and executor can dispatch correctly.
     * 
     * @return the move type
     */
    public MoveType getType() {
        return type;
    }
    
    /**
     * Returns the tokens involved in this move (for TAKE_THREE_DIFFERENT or TAKE_TWO_SAME).
     * Returns a defensive copy to prevent external mutation.
     * 
     * @return the token map, or empty map if not applicable
     */
    public Map<GemColor, Integer> getTokens() {
        return tokens != null ? new HashMap<>(tokens) : new HashMap<>();
    }
    
    /**
     * Returns the card involved in this move (for RESERVE or BUY).
     * 
     * @return the card, or null if not applicable
     */
    public DevelopmentCard getCard() {
        return card;
    }
    
    /**
     * Returns the payment token breakdown for a BUY move.
     * Maps color to the number of tokens of that color used to pay.
     * Returns a defensive copy to prevent external mutation.
     * 
     * @return the payment token map, or empty map if not applicable
     */
    public Map<GemColor, Integer> getPaymentTokens() {
        return paymentTokens != null ? new HashMap<>(paymentTokens) : new HashMap<>();
    }
    
    /**
     * Returns whether the card is being bought from reserved cards (true) or from the board (false).
     * Only applicable for BUY moves.
     * 
     * @return true if buying from reserved, false otherwise
     */
    public boolean isFromReserved() {
        return fromReserved;
    }
    
    /**
     * Returns the deck level to reserve from (1, 2, or 3), or -1 if reserving a face-up card.
     * Only applicable for RESERVE moves.
     * 
     * @return the level (1-3) or -1 for face-up
     */
    public int getCardLevel() {
        return cardLevel;
    }
    
    // ============ FACTORY METHODS ============
    
    /**
     * Creates a TAKE_THREE_DIFFERENT move.
     * 
     * @param tokens map with 3 entries, each with count 1
     * @return the move
     */
    public static Move takeDifferent(Map<GemColor, Integer> tokens) {
        return new Move(MoveType.TAKE_THREE_DIFFERENT, new HashMap<>(tokens), null, null, false, -1);
    }
    
    /**
     * Creates a TAKE_TWO_SAME move.
     * 
     * @param tokens map with 1 entry, count 2
     * @return the move
     */
    public static Move takeSame(Map<GemColor, Integer> tokens) {
        return new Move(MoveType.TAKE_TWO_SAME, new HashMap<>(tokens), null, null, false, -1);
    }
    
    /**
     * Creates a RESERVE move for a face-up card.
     * 
     * @param card the card to reserve
     * @return the move
     */
    public static Move reserveFaceUp(DevelopmentCard card) {
        return new Move(MoveType.RESERVE, null, card, null, false, -1);
    }
    
    /**
     * Creates a RESERVE move for a card from a specific deck level.
     * 
     * @param card the card to reserve (from the deck)
     * @param level the level (1, 2, or 3)
     * @return the move
     */
    public static Move reserveFromDeck(DevelopmentCard card, int level) {
        return new Move(MoveType.RESERVE, null, card, null, false, level);
    }
    
    /**
     * Creates a BUY move for a face-up or reserved card.
     * 
     * @param card the card to buy
     * @param paymentTokens the token breakdown used to pay
     * @param fromReserved true if buying from reserved cards, false if from board
     * @return the move
     */
    public static Move buy(DevelopmentCard card, Map<GemColor, Integer> paymentTokens, boolean fromReserved) {
        return new Move(MoveType.BUY, null, card, new HashMap<>(paymentTokens), fromReserved, -1);
    }
    
    @Override
    public String toString() {
        return "Move{" +
                "type=" + type +
                ", tokens=" + tokens +
                ", card=" + card +
                ", paymentTokens=" + paymentTokens +
                ", fromReserved=" + fromReserved +
                ", cardLevel=" + cardLevel +
                '}';
    }
}
