package ai;

import config.Config;
import engine.moves.Move;
import engine.payment.PaymentCalculator;
import engine.validation.MoveValidator;
import model.DevelopmentCard;
import model.GameState;
import model.GemColor;
import model.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Generates legal move candidates for the AI from the current state.
 */
final class AiMoveGenerator {
    private final Config config;
    private final AiMoveScorer moveScorer;

    /**
     * Creates a move generator.
     *
     * @param config game configuration
     * @param moveScorer helper used to score token-taking choices
     */
    AiMoveGenerator(Config config, AiMoveScorer moveScorer) {
        this.config = config;
        this.moveScorer = moveScorer;
    }

    /**
     * Generates all legal buy moves visible to the AI.
     *
     * @param state current game state
     * @param player player whose turn is being played
     * @param validator move validator used to filter illegal options
     * @return legal buy moves
     */
    List<Move> generateBuyMoves(
            GameState state,
            Player player,
            MoveValidator validator
    ) {
        List<Move> buyMoves = new ArrayList<>();

        for (DevelopmentCard card : state.getBoard().getFaceUpCards()) {
            Move buy = Move.buy(
                    card,
                    computePaymentTokens(player, card.getCost()),
                    false
            );
            if (validator.validate(state, player, buy) == null) {
                buyMoves.add(buy);
            }
        }

        for (DevelopmentCard card : player.getReservedCards()) {
            Move buy = Move.buy(
                    card,
                    computePaymentTokens(player, card.getCost()),
                    true
            );
            if (validator.validate(state, player, buy) == null) {
                buyMoves.add(buy);
            }
        }

        return buyMoves;
    }

    /**
     * Generates all legal reserve moves visible to the AI.
     *
     * @param state current game state
     * @param player player whose turn is being played
     * @param validator move validator used to filter illegal options
     * @return legal reserve moves
     */
    List<Move> generateReserveMoves(
            GameState state,
            Player player,
            MoveValidator validator
    ) {
        List<Move> reserveMoves = new ArrayList<>();

        for (DevelopmentCard card : state.getBoard().getFaceUpCards()) {
            Move reserve = Move.reserveFaceUp(card);
            if (validator.validate(state, player, reserve) == null) {
                reserveMoves.add(reserve);
            }
        }

        return reserveMoves;
    }

    /**
     * Generates the AI's preferred legal token-taking moves.
     *
     * @param state current game state
     * @param player player whose turn is being played
     * @param validator move validator used to filter illegal options
     * @return preferred legal token-taking moves
     */
    List<Move> generateTakeMoves(
            GameState state,
            Player player,
            MoveValidator validator
    ) {
        List<Move> takeMoves = new ArrayList<>();

        Move takeTwo = moveScorer.bestTakeTwoSame(state, player, validator);
        if (takeTwo != null) {
            takeMoves.add(takeTwo);
        }

        Move takeDifferent = moveScorer.bestTakeDifferent(
                state,
                player,
                validator
        );
        if (takeDifferent != null) {
            takeMoves.add(takeDifferent);
        }

        return takeMoves;
    }

    /**
     * Generates legal one-token return moves when the player is already
     * at the token limit.
     *
     * @param state current game state
     * @param player player whose turn is being played
     * @param validator move validator used to filter illegal options
     * @return legal return-token moves
     */
    List<Move> generateReturnMoves(
            GameState state,
            Player player,
            MoveValidator validator
    ) {
        List<Move> returnMoves = new ArrayList<>();
        if (player.getTotalTokens() < config.getMaxTokensPerPlayer()) {
            return returnMoves;
        }

        for (GemColor color : allColors()) {
            if (player.getTokenCount(color) <= 0) {
                continue;
            }
            Map<GemColor, Integer> tokens = new EnumMap<>(GemColor.class);
            tokens.put(color, 1);
            Move returnMove = Move.returnTokens(tokens);
            if (validator.validate(state, player, returnMove) == null) {
                returnMoves.add(returnMove);
            }
        }

        return returnMoves;
    }

    private Map<GemColor, Integer> computePaymentTokens(
            Player player,
            Map<GemColor, Integer> cost
    ) {
        return PaymentCalculator.computePaymentTokens(player, cost);
    }

    private List<GemColor> allColors() {
        List<GemColor> colors = new ArrayList<>();
        for (GemColor color : GemColor.values()) {
            colors.add(color);
        }
        return colors;
    }
}
