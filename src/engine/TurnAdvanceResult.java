package engine;

import model.Player;

/**
 * Carries the outcome of advancing turn state after a completed move.
 */
public final class TurnAdvanceResult {
    private final boolean finalRoundTriggered;
    private final Player winner;

    public TurnAdvanceResult(boolean finalRoundTriggered, Player winner) {
        this.finalRoundTriggered = finalRoundTriggered;
        this.winner = winner;
    }

    /**
     * Returns whether this move triggered the final round.
     *
     * @return {@code true} if the final round was triggered during progression
     */
    public boolean isFinalRoundTriggered() {
        return finalRoundTriggered;
    }

    /**
     * Returns the game winner when the final round has completed.
     *
     * @return winner, or {@code null} if the game is not over yet
     */
    public Player getWinner() {
        return winner;
    }
}
