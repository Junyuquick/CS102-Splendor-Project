package engine;

import model.GemColor;
import model.NobleTile;
import model.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Describes the full outcome of attempting to resolve one turn.
 */
public final class TurnResolutionResult {
    private final boolean success;
    private final String validationError;
    private final Map<GemColor, Integer> discardedTokens;
    private final List<NobleTile> assignedNobles;
    private final boolean finalRoundTriggered;
    private final Player winner;

    public TurnResolutionResult(
            boolean success,
            String validationError,
            Map<GemColor, Integer> discardedTokens,
            List<NobleTile> assignedNobles,
            boolean finalRoundTriggered,
            Player winner
    ) {
        this.success = success;
        this.validationError = validationError;
        this.discardedTokens = new EnumMap<>(GemColor.class);
        if (discardedTokens != null) {
            this.discardedTokens.putAll(discardedTokens);
        }
        this.assignedNobles = assignedNobles == null ? new ArrayList<>() : new ArrayList<>(assignedNobles);
        this.finalRoundTriggered = finalRoundTriggered;
        this.winner = winner;
    }

    /**
     * Returns a failed result for an invalid move.
     *
     * @param validationError explanation for the failure
     * @return failed turn result
     */
    public static TurnResolutionResult invalid(String validationError) {
        return new TurnResolutionResult(false, validationError, null, null, false, null);
    }

    /**
     * Returns a successful result.
     *
     * @param discardedTokens tokens discarded to satisfy the hand limit
     * @param assignedNobles nobles assigned after the move
     * @param finalRoundTriggered whether the move started the final round
     * @param winner winner if the game ended, otherwise {@code null}
     * @return successful turn result
     */
    public static TurnResolutionResult success(
            Map<GemColor, Integer> discardedTokens,
            List<NobleTile> assignedNobles,
            boolean finalRoundTriggered,
            Player winner
    ) {
        return new TurnResolutionResult(true, null, discardedTokens, assignedNobles, finalRoundTriggered, winner);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getValidationError() {
        return validationError;
    }

    public Map<GemColor, Integer> getDiscardedTokens() {
        return Collections.unmodifiableMap(discardedTokens);
    }

    public List<NobleTile> getAssignedNobles() {
        return Collections.unmodifiableList(assignedNobles);
    }

    public boolean isFinalRoundTriggered() {
        return finalRoundTriggered;
    }

    public Player getWinner() {
        return winner;
    }
}
