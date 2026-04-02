package engine.turn;

import model.GameState;
import model.Player;

/**
 * Handles the shared turn-advance steps after a move has finished.
 */
public final class TurnProgressionService {
    /**
     * Advances the turn and checks whether that move started or finished the endgame.
     *
     * @param state mutable game state
     * @param winnerChecker helper for endgame checks
     * @param turnManager helper for turn order
     * @return result containing final-round and winner information
     */
    public TurnAdvanceResult progressTurn(
            GameState state,
            WinnerChecker winnerChecker,
            TurnManager turnManager
    ) {
        boolean triggeredFinalRound =
                triggerFinalRoundIfNeeded(state, winnerChecker, turnManager);

        turnManager.advanceTurn(state);

        Player winner = determineWinnerIfGameEnded(
                state,
                winnerChecker,
                turnManager
        );

        return new TurnAdvanceResult(triggeredFinalRound, winner);
    }

    private boolean triggerFinalRoundIfNeeded(
            GameState state,
            WinnerChecker winnerChecker,
            TurnManager turnManager
    ) {
        if (!winnerChecker.shouldTriggerFinalRound(state)) {
            return false;
        }

        turnManager.markFinalRound(state);
        return true;
    }

    private Player determineWinnerIfGameEnded(
            GameState state,
            WinnerChecker winnerChecker,
            TurnManager turnManager
    ) {
        if (!turnManager.hasFinalRoundCompleted(state)) {
            return null;
        }

        return winnerChecker.determineWinner(state);
    }
}
