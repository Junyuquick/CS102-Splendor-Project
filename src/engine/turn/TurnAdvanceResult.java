package engine.turn;

import model.Player;

/**
 * Small result object that describes what happened when a turn was advanced.
 */
public final class TurnAdvanceResult {
    private final boolean finalRoundTriggered;
    private final Player winner;

    public TurnAdvanceResult(boolean finalRoundTriggered, Player winner) {
        this.finalRoundTriggered = finalRoundTriggered;
        this.winner = winner;
    }

    /**
     * Tells whether this turn started the final round.
     *
     * @return {@code true} if the final round was triggered
     */
    public boolean isFinalRoundTriggered() {
        return finalRoundTriggered;
    }

    /**
     * Returns the winner if the game has ended by this point.
     *
     * @return the winner, or {@code null} if the game is still going
     */
    public Player getWinner() {
        return winner;
    }
}
