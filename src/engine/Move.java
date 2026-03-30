package engine;

import model.*;
import java.io.Serializable;
import java.util.*;

/**
 * Represents one action a player wants to make on their turn.
 *
 * <p>Each move carries only the information the game needs to validate and apply it,
 * like chosen tokens, the selected card, or how a purchase is being paid for.
 */
public abstract class Move implements Serializable {
    /**
     * Gives a readable name for this move type.
     *
     * @return name used in logs and status messages
     */
    public abstract String getTypeName();

    /**
     * Returns the tokens involved in this move.
     * Most move types do not use tokens directly, so the default is an empty map.
     *
     * @return the token map, or empty map if not applicable
     */
    public Map<GemColor, Integer> getTokens() {
        return new HashMap<>();
    }

    /**
     * Returns the card this move is acting on, if there is one.
     *
     * @return the card, or null if not applicable
     */
    public DevelopmentCard getCard() {
        return null;
    }

    /**
     * Returns the token payment used for a buy move.
     * For moves that do not buy a card, this stays empty.
     *
     * @return the payment token map, or empty map if not applicable
     */
    public Map<GemColor, Integer> getPaymentTokens() {
        return new HashMap<>();
    }

    /**
     * Tells whether a buy is coming from the player's reserved cards instead of the board.
     * For non-buy moves, this is always false.
     *
     * @return true if buying from reserved, false otherwise
     */
    public boolean isFromReserved() {
        return false;
    }

    /**
     * Returns which deck level a reserve came from.
     * A value of -1 means the player reserved a face-up card instead.
     *
     * @return the level (1-3) or -1 for face-up
     */
    public int getCardLevel() {
        return -1;
    }
    
    /**
     * Creates a move for taking three different gem colors.
     * 
     * @param tokens map with 3 entries, each with count 1
     * @return the move
     */
    public static Move takeDifferent(Map<GemColor, Integer> tokens) {
        return new TakeDifferentMove(tokens);
    }
    
    /**
     * Creates a move for taking two tokens of the same color.
     * 
     * @param tokens map with 1 entry, count 2
     * @return the move
     */
    public static Move takeSame(Map<GemColor, Integer> tokens) {
        return new TakeSameMove(tokens);
    }
    
    /**
     * Creates a move for reserving a face-up card from the board.
     * 
     * @param card the card to reserve
     * @return the move
     */
    public static Move reserveFaceUp(DevelopmentCard card) {
        return new ReserveMove(card, -1);
    }
    
    /**
     * Creates a move for reserving a hidden card from one of the decks.
     * 
     * @param card the card to reserve (from the deck)
     * @param level the level (1, 2, or 3)
     * @return the move
     */
    public static Move reserveFromDeck(DevelopmentCard card, int level) {
        return new ReserveMove(card, level);
    }
    
    /**
     * Creates a move for buying a card, whether it is on the board or already reserved.
     * 
     * @param card the card to buy
     * @param paymentTokens the token breakdown used to pay
     * @param fromReserved true if buying from reserved cards, false if from board
     * @return the move
     */
    public static Move buy(DevelopmentCard card, Map<GemColor, Integer> paymentTokens, boolean fromReserved) {
        return new BuyMove(card, paymentTokens, fromReserved);
    }
    
    /**
     * Returns a readable summary of the move, mainly for debugging.
     *
     * @return string form of this move
     */
    @Override
    public String toString() {
        return "Move{" +
                "type=" + getTypeName() +
                ", tokens=" + getTokens() +
                ", card=" + getCard() +
                ", paymentTokens=" + getPaymentTokens() +
                ", fromReserved=" + isFromReserved() +
                ", cardLevel=" + getCardLevel() +
                '}';
    }
}
