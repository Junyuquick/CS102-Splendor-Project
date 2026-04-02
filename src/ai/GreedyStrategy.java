package ai;

import config.Config;
import engine.moves.Move;
import engine.validation.MoveValidator;
import model.GameState;
import model.Player;

import java.util.List;
import java.util.Random;

/**
 * Entry point for the simple built-in AI.
 *
 * This class does not score every move itself. Instead, it asks helper
 * classes to build legal candidates, then chooses among those
 * candidates using a lightweight weighted policy.
 */
public class GreedyStrategy {
    private final Random random = new Random();
    private final AiMoveGenerator moveGenerator;

    /**
     * Creates the strategy and wires together the helper classes the
     * AI uses to build and score moves.
     *
     * @param config game configuration
     */
    public GreedyStrategy(Config config) {
        AiMoveScorer moveScorer = new AiMoveScorer(config);
        this.moveGenerator = new AiMoveGenerator(config, moveScorer);
    }

    /**
     * Chooses one legal move for the given player.
     *
     * @param state current game state
     * @param player player whose turn is being played
     * @param validator move validator used to filter illegal options
     * @return the selected move
     */
    public Move chooseMove(
            GameState state,
            Player player,
            MoveValidator validator
    ) {
        return chooseEasyMove(state, player, validator);
    }

    /**
     * Chooses among the legal move categories the AI knows about.
     *
     * The policy is intentionally simple:
     * token-taking is preferred most often,
     * reserving is the next fallback,
     * buying or returning tokens is used when those lists make more
     * sense than passing.
     *
     * @param state current game state
     * @param player player whose turn is being played
     * @param validator move validator used to filter illegal options
     * @return chosen legal move
     */
    private Move chooseEasyMove(
            GameState state,
            Player player,
            MoveValidator validator
    ) {
        List<Move> buyMoves = moveGenerator.generateBuyMoves(
                state,
                player,
                validator
        );
        List<Move> reserveMoves = moveGenerator.generateReserveMoves(
                state,
                player,
                validator
        );
        List<Move> takeMoves = moveGenerator.generateTakeMoves(
                state,
                player,
                validator
        );
        List<Move> returnMoves = moveGenerator.generateReturnMoves(
                state,
                player,
                validator
        );

        int roll = random.nextInt(100);
        if (roll < 60 && !takeMoves.isEmpty()) {
            return randomMove(takeMoves);
        }
        if (roll < 90 && !reserveMoves.isEmpty()) {
            return randomMove(reserveMoves);
        }
        if (!buyMoves.isEmpty()) {
            return randomMove(buyMoves);
        }
        if (!takeMoves.isEmpty()) {
            return randomMove(takeMoves);
        }
        if (!reserveMoves.isEmpty()) {
            return randomMove(reserveMoves);
        }
        if (!returnMoves.isEmpty()) {
            return randomMove(returnMoves);
        }
        return Move.pass();
    }

    /**
     * Selects one move uniformly from a list of legal candidates.
     *
     * @param moves legal moves to choose from
     * @return randomly selected move
     */
    private Move randomMove(List<Move> moves) {
        return moves.get(random.nextInt(moves.size()));
    }
}
