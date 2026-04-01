package engine.moves;

import model.DevelopmentCard;
import model.GemColor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable description of a single player action.
 *
 * <p>A move records only the data needed by validation and execution, such as selected
 * tokens, the targeted card, and any payment-token breakdown for purchases.
 */
public abstract class Move implements Serializable {
    /**
     * Returns a stable label for the concrete move class.
     *
     * @return display name used in logs and messages
     */
    public abstract String getTypeName();

    /**
     * Returns the tokens involved in this move (for token-taking moves).
     *
     * @return the token map, or empty map if not applicable
     */
    public Map<GemColor, Integer> getTokens() {
        return new HashMap<>();
    }

    /**
     * Returns the card involved in this move (for reserve or buy).
     *
     * @return the card, or null if not applicable
     */
    public DevelopmentCard getCard() {
        return null;
    }

    /**
     * Returns the payment token breakdown for a buy move.
     * Maps color to the number of tokens of that color used to pay.
     *
     * @return the payment token map, or empty map if not applicable
     */
    public Map<GemColor, Integer> getPaymentTokens() {
        return new HashMap<>();
    }

    /**
     * Returns whether the card is being bought from reserved cards (true) or from the board (false).
     * Only applicable for buy moves.
     *
     * @return true if buying from reserved, false otherwise
     */
    public boolean isFromReserved() {
        return false;
    }

    /**
     * Returns the deck level to reserve from (1, 2, or 3), or -1 if reserving a face-up card.
     * Only applicable for reserve moves.
     *
     * @return the level (1-3) or -1 for face-up
     */
    public int getCardLevel() {
        return -1;
    }

    /**
     * Creates a TAKE_THREE_DIFFERENT move.
     *
     * @param tokens map with 3 entries, each with count 1
     * @return the move
     */
    public static Move takeDifferent(Map<GemColor, Integer> tokens) {
        return new TakeDifferentMove(tokens);
    }

    /**
     * Creates a TAKE_TWO_SAME move.
     *
     * @param tokens map with 1 entry, count 2
     * @return the move
     */
    public static Move takeSame(Map<GemColor, Integer> tokens) {
        return new TakeSameMove(tokens);
    }

    /**
     * Creates a RESERVE move for a face-up card.
     *
     * @param card the card to reserve
     * @return the move
     */
    public static Move reserveFaceUp(DevelopmentCard card) {
        return new ReserveMove(card, -1);
    }

    /**
     * Creates a RESERVE move for a card from a specific deck level.
     *
     * @param card the card to reserve (from the deck)
     * @param level the level (1, 2, or 3)
     * @return the move
     */
    public static Move reserveFromDeck(DevelopmentCard card, int level) {
        return new ReserveMove(card, level);
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
        return new BuyMove(card, paymentTokens, fromReserved);
    }

    /**
     * Creates a PASS move that ends the player's turn without changing state.
     *
     * @return the move
     */
    public static Move pass() {
        return new PassMove();
    }

    /**
     * Creates a RETURN_TOKENS move for voluntarily giving tokens back to the bank.
     *
     * @param tokens the tokens to return
     * @return the move
     */
    public static Move returnTokens(Map<GemColor, Integer> tokens) {
        return new ReturnTokensMove(tokens);
    }

    /**
     * Returns a debug-friendly description of the move contents.
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
