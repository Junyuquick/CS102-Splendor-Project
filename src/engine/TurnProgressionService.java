package engine;

import model.GameState;
import model.Player;

/**
 * Applies post-move turn progression shared by local and network game flows.
 */
public final class TurnProgressionService {
    /**
     * Advances turn order and determines whether the game has just ended.
     *
     * @param state mutable game state
     * @param winnerChecker endgame evaluator
     * @param turnManager turn-order manager
     * @return progression result containing final-round trigger and winner info
     */
    public TurnAdvanceResult progressTurn(GameState state, WinnerChecker winnerChecker, TurnManager turnManager) {
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
