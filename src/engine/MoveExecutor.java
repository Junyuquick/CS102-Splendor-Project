package engine;

import config.Config;
import model.*;
import java.util.*;

/**
 * Applies validated moves to GameState.
 * 
 * Centralizes all mutations to keep state updates consistent. Handles:
 * - Player token count updates
 * - Player card collections (reserved and purchased)
 * - Bank token supply changes
 * - Board face-up card refills when needed
 * 
 * Called by GameEngine after MoveValidator approves a move.
 * Typically followed by NobleAssigner and WinnerChecker.
 */
public class MoveExecutor {
    
    private final Config config;
    
    /**
     * Constructs a MoveExecutor with the given configuration.
     * Config is used for execution rules like reserve gold bonus and token cap handling.
     * 
     * @param config the game configuration
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
     * Executes taking three different colored tokens.
     * 
     * Implementation:
     * - Takes exactly one token of each specified color from the bank
     * - Adds tokens to the player
     * - Enforces token cap (if exceeded, caller must handle discard logic)
     * 
     * @param state the game state (mutated)
     * @param player the player taking tokens
     * @param move the move with token colors specified
     */
    private void applyTakeThreeDiff(GameState state, Player player, Move move) {
        GemBank bank = state.getBank();
        Map<GemColor, Integer> tokensTaken = move.getTokens(); // {color1: 1, color2: 1, color3: 1}
        
        // Remove tokens from bank
        bank.removeTokens(tokensTaken);
        
        // Add tokens to player
        player.addTokens(tokensTaken);
    }
    
    /**
     * Executes taking two of the same color token.
     * Respects the minimum bank constraint: bank must have at least takeSameMinRemainingInBank after removal.
     * 
     * Implementation:
     * - Takes two tokens of the specified color from the bank
     * - Adds tokens to the player
     * - Enforces token cap (if exceeded, caller must handle discard logic)
     * 
     * @param state the game state (mutated)
     * @param player the player taking tokens
     * @param move the move with a single color and count = 2
     */
    private void applyTakeTwoSame(GameState state, Player player, Move move) {
        GemBank bank = state.getBank();
        Map<GemColor, Integer> tokensTaken = move.getTokens(); // {color: 2}
        
        // Remove tokens from bank
        bank.removeTokens(tokensTaken);
        
        // Add tokens to player
        player.addTokens(tokensTaken);
    }
    
    /**
     * Executes reserving a card.
     * If the bank has gold tokens available, the player receives one.
     * 
     * Implementation:
     * - Removes the card from the board (face-up or from a deck)
     * - Adds the card to the player's reserved cards
     * - If bank has gold tokens, removes one and adds to player
     * - Refills the board slot if the card came from a face-up slot
     * 
     * @param state the game state (mutated)
     * @param player the player reserving the card
     * @param move the move specifying which card to reserve
     */
    private void applyReserve(GameState state, Player player, Move move) {
        Board board = state.getBoard();
        GemBank bank = state.getBank();
        DevelopmentCard cardToReserve = move.getCard();
        
        // Remove card from board
        board.removeCard(cardToReserve);
        
        // Add card to player's reserved cards
        player.addReservedCard(cardToReserve);
        
        // Apply gold bonus if available
        if (bank.getTokenCount(GemColor.GOLD) > 0) {
            Map<GemColor, Integer> goldDelta = new HashMap<>();
            goldDelta.put(GemColor.GOLD, config.getReserveGoldBonus());
            bank.removeTokens(goldDelta);
            player.addTokens(goldDelta);
        }
        
        // Refill board slot if applicable
        board.refillSlot(move.getCard());
    }
    
    /**
     * Executes buying a development card.
     * Pays the cost using permanent bonuses, colored tokens, and gold (as wildcard).
     * 
     * Implementation:
     * - Removes the card from the player's reserved cards (if applicable)
     * - Computes the payment breakdown: bonuses, colored tokens, gold
     * - Removes payment tokens from the player
     * - Returns payment tokens to the bank
     * - Adds the card to the player's purchased cards
     * - Refills the board slot if the card came from a face-up slot
     * 
     * @param state the game state (mutated)
     * @param player the player buying the card
     * @param move the move specifying which card and payment breakdown
     */
    private void applyBuy(GameState state, Player player, Move move) {
        Board board = state.getBoard();
        GemBank bank = state.getBank();
        DevelopmentCard cardToBuy = move.getCard();
        
        // Remove from reserved if applicable
        if (move.isFromReserved()) {
            player.removeReservedCard(cardToBuy);
        } else {
            // Remove from board
            board.removeCard(cardToBuy);
        }
        
        // Get the cost breakdown
        Map<GemColor, Integer> paymentTokens = move.getPaymentTokens(); // {GOLD: 2, RED: 1, ...}
        
        // Remove payment tokens from player
        player.removeTokens(paymentTokens);
        
        // Return payment tokens to bank
        bank.addTokens(paymentTokens);
        
        // Add purchased card to player (affects prestige and bonuses)
        player.addPurchasedCard(cardToBuy);
        
        // Refill board slot if the card came from face-up
        if (!move.isFromReserved()) {
            board.refillSlot(cardToBuy);
        }
    }
}
