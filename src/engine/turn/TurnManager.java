package engine.turn;

import model.GameState;

/**
 * Keeps track of whose turn it is and remembers who started the final round.
 *
 * That lets the game detect when turn order has looped all the way back and the match
 * should end.
 */
public class TurnManager {
    private int finalRoundStartingPlayerIndex = -1;

    /**
     * Creates a turn manager.
     */
    public TurnManager() {
    }

    /**
     * Moves play to the next player, wrapping back to the start when needed.
     *
     * @param state the game state, updated with the new current player
     */
    public void advanceTurn(GameState state) {
        int playerCount = state.getPlayers().size();
        int currentIndex = state.getCurrentPlayerIndex();

        int nextIndex = (currentIndex + 1) % playerCount;
        state.setCurrentPlayerIndex(nextIndex);
    }

    /**
     * Returns the index of the player whose turn it currently is.
     *
     * @param state the game state
     * @return the current player index
     */
    public int getCurrentPlayerIndex(GameState state) {
        return state.getCurrentPlayerIndex();
    }

    /**
     * Tells whether the game is currently in the final round.
     *
     * @param state the game state
     * @return true if the final round is active, false otherwise
     */
    public boolean isFinalRound(GameState state) {
        return state.isFinalRound();
    }

    /**
     * Marks the start of the final round and remembers who triggered it.
     *
     * @param state the game state
     */
    public void markFinalRound(GameState state) {
        state.setFinalRound(true);
        this.finalRoundStartingPlayerIndex = state.getCurrentPlayerIndex();
    }

    /**
     * Checks whether turn order has looped back to the player who started the final round.
     *
     * @param state the game state
     * @return true if the final round has finished, false otherwise
     */
    public boolean hasFinalRoundCompleted(GameState state) {
        if (!state.isFinalRound()) {
            return false;
        }

        if (finalRoundStartingPlayerIndex == -1) {
            return false;
        }
        return state.getCurrentPlayerIndex() == finalRoundStartingPlayerIndex;
    }

    /**
     * Clears the saved final-round marker.
     */
    public void resetFinalRound() {
        this.finalRoundStartingPlayerIndex = -1;
    }
}
