package ai;

import config.Config;
import engine.moves.Move;
import engine.validation.MoveValidator;
import model.GameState;
import model.Player;

import java.util.List;
import java.util.Random;

/**
 * Selects a legal AI move using simple heuristics for token-taking,
 * reserving, and buying.
 */
public class GreedyStrategy {
    private final Random random = new Random();
    private final AiMoveGenerator moveGenerator;

    /**
     * Creates the strategy with access to rule configuration values.
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
     * Chooses among legal buys, reserves, and token-taking moves using
     * a lightweight weighted random policy.
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

    private Move randomMove(List<Move> moves) {
        return moves.get(random.nextInt(moves.size()));
    }
}
