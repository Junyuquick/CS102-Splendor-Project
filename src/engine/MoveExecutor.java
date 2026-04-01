package engine;

import config.Config;
import model.*;
import java.util.*;

/**
 * Applies a move to the game after it has already been checked for legality.
 *
 * <p>This keeps all of the actual state changes in one place, so turns are handled the same
 * way no matter where the move came from.
 */
public class MoveExecutor {
    
    private final Config config;
    
    /**
     * Creates a move executor with the current game rules.
     *
     * @param config game settings used for details like reserve bonuses
     */
    public MoveExecutor(Config config) {
        this.config = config;
    }
    
    /**
     * Applies a legal move and updates the game state to match it.
     *
     * @param state the current game state, which will be updated
     * @param player the player making the move
     * @param move the move to apply
     * @throws IllegalArgumentException if the move type is not recognized
     */
    public void execute(GameState state, Player player, Move move) {
        if (move instanceof TakeDifferentMove) {
            applyTokenTake(state, player, move);
            return;
        }
        if (move instanceof TakeSameMove) {
            applyTokenTake(state, player, move);
            return;
        }
        if (move instanceof ReserveMove) {
            applyReserve(state, player, move);
            return;
        }
        if (move instanceof BuyMove) {
            applyBuy(state, player, move);
            return;
        }
        if (move instanceof ReturnTokensMove) {
            applyTokenReturn(state, player, move);
            return;
        }
        if (move instanceof PassMove) {
            return;
        }

        throw new IllegalArgumentException("Unsupported move type: " + move.getTypeName());
    }
    
    /**
     * Handles the move where the player takes one token from each chosen color.
     *
     * @param state game state to update
     * @param player player receiving the tokens
     * @param move move containing the chosen colors
     */
    private void applyTokenTake(GameState state, Player player, Move move) {
        GemBank bank = state.getBank();
        Map<GemColor, Integer> tokensTaken = move.getTokens();
        bank.removeTokens(tokensTaken);
        player.addTokens(tokensTaken);
    }
    
    /**
     * Handles reserving a card, including the gold bonus when one is available.
     *
     * @param state game state to update
     * @param player player reserving the card
     * @param move move pointing to the card being reserved
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
     * Handles buying a card and returning the spent tokens to the bank.
     *
     * @param state game state to update
     * @param player player buying the card
     * @param move move containing the card and payment details
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

    /**
     * Handles voluntarily returning tokens to the bank as the player's full turn.
     *
     * @param state game state to update
     * @param player player returning the tokens
     * @param move move containing the chosen return tokens
     */
    private void applyTokenReturn(GameState state, Player player, Move move) {
        Map<GemColor, Integer> returnedTokens = move.getTokens();
        player.removeTokens(returnedTokens);
        state.getBank().addTokens(returnedTokens);
    }
}
