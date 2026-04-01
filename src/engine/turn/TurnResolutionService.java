package engine.turn;

import engine.moves.Move;
import engine.validation.MoveValidator;
import model.GameState;
import model.GemColor;
import model.NobleTile;
import model.Player;

import java.util.List;
import java.util.Map;

/**
 * Coordinates validation, execution, post-processing, and turn progression for one move.
 */
public final class TurnResolutionService {
    private final MoveValidator validator;
    private final MoveExecutor executor;
    private final TurnPostProcessor turnPostProcessor;
    private final WinnerChecker winnerChecker;
    private final TurnManager turnManager;
    private final TurnProgressionService turnProgressionService;

    public TurnResolutionService(
            MoveValidator validator,
            MoveExecutor executor,
            TurnPostProcessor turnPostProcessor,
            WinnerChecker winnerChecker,
            TurnManager turnManager,
            TurnProgressionService turnProgressionService
    ) {
        this.validator = validator;
        this.executor = executor;
        this.turnPostProcessor = turnPostProcessor;
        this.winnerChecker = winnerChecker;
        this.turnManager = turnManager;
        this.turnProgressionService = turnProgressionService;
    }

    /**
     * Attempts to apply one move and returns everything important that happened.
     *
     * @param state current game state
     * @param player acting player
     * @param move requested move
     * @return turn-resolution result
     */
    public TurnResolutionResult resolveTurn(GameState state, Player player, Move move) {
        String error = validator.validate(state, player, move);
        if (error != null) {
            return TurnResolutionResult.invalid(error);
        }

        executor.execute(state, player, move);
        Map<GemColor, Integer> discardedTokens =
                turnPostProcessor.enforceTokenLimit(state, player);
        List<NobleTile> assignedNobles =
                turnPostProcessor.assignBestAvailableNobles(state, player);
        TurnAdvanceResult turnAdvanceResult =
                turnProgressionService.progressTurn(
                        state,
                        winnerChecker,
                        turnManager
                );

        return TurnResolutionResult.success(
                discardedTokens,
                assignedNobles,
                turnAdvanceResult.isFinalRoundTriggered(),
                turnAdvanceResult.getWinner()
        );
    }
}
