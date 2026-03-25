package engine;

import config.Config;
import model.*;
import java.util.*;

/**
 * Applies already validated moves to the mutable game state.
 *
 * <p>This class centralizes board, bank, and player mutations so turn-processing code can
 * execute moves consistently before any post-turn effects are applied.
 */
public class MoveExecutor {
    
    private final Config config;
    
    /**
     * Creates an executor that uses the supplied rule configuration.
     *
     * @param config game configuration used for execution details such as reserve bonuses
     */
    public MoveExecutor(Config config) {
        this.config = config;
    }
    
    /**
     * Applies a validated move to the game state.
     * Mutates state to reflect the move:
     * - Moves tokens between bank and player
     * - Moves cards to reserved or purchased
     * - Refills board slots as required
     * 
     * @param state the current game state (will be mutated)
     * @param player the player making the move
     * @param move the move to execute
     * @throws IllegalArgumentException if the move type is not supported
     */
    public void execute(GameState state, Player player, Move move) {
        switch (move.getType()) {
            case TAKE_THREE_DIFFERENT:
                applyTakeThreeDiff(state, player, move);
                break;
            case TAKE_TWO_SAME:
                applyTakeTwoSame(state, player, move);
                break;
            case RESERVE:
                applyReserve(state, player, move);
                break;
            case BUY:
                applyBuy(state, player, move);
                break;
            default:
                throw new IllegalArgumentException("Unsupported move type: " + move.getType());
        }
    }
    
    /**
     * Applies a move that takes one token from each selected color.
     *
     * @param state game state to mutate
     * @param player player receiving the tokens
     * @param move move carrying the token selection
     */
    private void applyTakeThreeDiff(GameState state, Player player, Move move) {
        GemBank bank = state.getBank();
        Map<GemColor, Integer> tokensTaken = move.getTokens();
        bank.removeTokens(tokensTaken);
        player.addTokens(tokensTaken);
    }
    
    /**
     * Applies a move that takes multiple tokens of a single color.
     *
     * @param state game state to mutate
     * @param player player receiving the tokens
     * @param move move carrying the token selection
     */
    private void applyTakeTwoSame(GameState state, Player player, Move move) {
        GemBank bank = state.getBank();
        Map<GemColor, Integer> tokensTaken = move.getTokens();
        bank.removeTokens(tokensTaken);
        player.addTokens(tokensTaken);
    }
    
    /**
     * Applies a reserve move, including the optional gold bonus.
     *
     * @param state game state to mutate
     * @param player player reserving the card
     * @param move move identifying the reserved card
     */
    private void applyReserve(GameState state, Player player, Move move) {
        Board board = state.getBoard();
        GemBank bank = state.getBank();
        DevelopmentCard cardToReserve = move.getCard();
        
        board.removeCard(cardToReserve);
        player.addReservedCard(cardToReserve);
        if (bank.getTokenCount(GemColor.GOLD) > 0) {
            Map<GemColor, Integer> goldDelta = new HashMap<>();
            goldDelta.put(GemColor.GOLD, config.getReserveGoldBonus());
            bank.removeTokens(goldDelta);
            player.addTokens(goldDelta);
        }
        board.refillSlot(move.getCard());
    }
    
    /**
     * Applies a purchase move and transfers the paid tokens back to the bank.
     *
     * @param state game state to mutate
     * @param player player buying the card
     * @param move move identifying the purchased card and token payment
     */
    private void applyBuy(GameState state, Player player, Move move) {
        Board board = state.getBoard();
        GemBank bank = state.getBank();
        DevelopmentCard cardToBuy = move.getCard();
        
        if (move.isFromReserved()) {
            player.removeReservedCard(cardToBuy);
        } else {
            board.removeCard(cardToBuy);
        }
        Map<GemColor, Integer> paymentTokens = move.getPaymentTokens();
        player.removeTokens(paymentTokens);
        bank.addTokens(paymentTokens);
        player.addPurchasedCard(cardToBuy);
        if (!move.isFromReserved()) {
            board.refillSlot(cardToBuy);
        }
    }
}
