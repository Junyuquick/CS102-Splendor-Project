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
        boolean triggeredFinalRound = false;
        if (winnerChecker.shouldTriggerFinalRound(state)) {
            turnManager.markFinalRound(state);
            triggeredFinalRound = true;
        }

        turnManager.advanceTurn(state);

        Player winner = null;
        if (turnManager.hasFinalRoundCompleted(state)) {
            winner = winnerChecker.determineWinner(state);
        }

        return new TurnAdvanceResult(triggeredFinalRound, winner);
    }
}
