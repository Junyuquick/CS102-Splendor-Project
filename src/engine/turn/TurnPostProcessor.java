package engine.turn;

import config.Config;
import model.GameState;
import model.GemColor;
import model.NobleTile;
import model.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles cleanup and bonus effects that happen after a move is applied.
 */
public class TurnPostProcessor {
    private final Config config;
    private final NobleAssigner nobleAssigner;

    /**
     * Creates a helper for the extra rule checks that happen at the end of a turn.
     *
     * @param config game settings used for end-of-turn limits
     * @param nobleAssigner helper used to find and assign nobles
     */
    public TurnPostProcessor(Config config, NobleAssigner nobleAssigner) {
        this.config = config;
        this.nobleAssigner = nobleAssigner;
    }

    /**
     * Makes the player discard down to the token limit if they are holding too many.
     *
     * @param state game state whose bank receives the discarded tokens
     * @param player player whose hand is being reduced
     * @return all discarded tokens grouped by color
     */
    public Map<GemColor, Integer> enforceTokenLimit(GameState state, Player player) {
        Map<GemColor, Integer> discarded = new LinkedHashMap<>();
        int maxTokens = config.getMaxTokensPerPlayer();

        while (playerHasTooManyTokens(player, maxTokens)) {
            discardOneRound(state, player, discarded, maxTokens);
        }

        return discarded;
    }

    /**
     * Gives the player as many eligible nobles as the rules allow this turn.
     *
     * @param state current game state
     * @param player player receiving nobles
     * @return nobles assigned during this turn
     */
    public List<NobleTile> assignBestAvailableNobles(GameState state, Player player) {
        List<NobleTile> eligibleNobles =
                new ArrayList<>(nobleAssigner.findEligibleNobles(state, player));
        List<NobleTile> assignedNobles = new ArrayList<>();
        int noblesThisTurn = getNoblesToAssignThisTurn(eligibleNobles);

        for (int i = 0; i < noblesThisTurn; i++) {
            assignChosenNoble(state, player, eligibleNobles, assignedNobles);
        }

        return assignedNobles;
    }

    /**
     * Picks the best noble from the available choices.
     *
     * @param eligibleNobles nobles the player currently qualifies for
     * @return the chosen noble
     */
    public NobleTile chooseBestNoble(List<NobleTile> eligibleNobles) {
        return eligibleNobles.stream()
                .max((a, b) -> {
                    int points = compareNoblePoints(a, b);
                    if (points != 0) {
                        return points;
                    }

                    return compareNobleRequirements(a, b);
                })
                .orElse(eligibleNobles.get(0));
    }

    private boolean playerHasTooManyTokens(Player player, int maxTokens) {
        return player.getTotalTokens() > maxTokens;
    }

    private void discardOneRound(
            GameState state,
            Player player,
            Map<GemColor, Integer> discarded,
            int maxTokens
    ) {
        int excess = player.getTotalTokens() - maxTokens;
        Map<GemColor, Integer> roundDiscard = chooseDiscardTokens(player, excess);

        player.removeTokens(roundDiscard);
        state.getBank().addTokens(roundDiscard);
        mergeDiscard(discarded, roundDiscard);
    }

    private int getNoblesToAssignThisTurn(List<NobleTile> eligibleNobles) {
        return Math.min(config.getMaxNoblesPerTurn(), eligibleNobles.size());
    }

    private void assignChosenNoble(
            GameState state,
            Player player,
            List<NobleTile> eligibleNobles,
            List<NobleTile> assignedNobles
    ) {
        NobleTile chosen = chooseBestNoble(eligibleNobles);
        nobleAssigner.assignNoble(state, player, chosen);
        eligibleNobles.remove(chosen);
        assignedNobles.add(chosen);
    }

    private int compareNoblePoints(NobleTile first, NobleTile second) {
        return Integer.compare(
                first.getPrestigePoints(),
                second.getPrestigePoints()
        );
    }

    private int compareNobleRequirements(
            NobleTile first,
            NobleTile second
    ) {
        int firstRequirementTotal = countRequirementTotal(first);
        int secondRequirementTotal = countRequirementTotal(second);
        return Integer.compare(secondRequirementTotal, firstRequirementTotal);
    }

    private int countRequirementTotal(NobleTile noble) {
        return noble.getRequirement().asMap().values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    private Map<GemColor, Integer> chooseDiscardTokens(Player player, int excess) {
        Map<GemColor, Integer> discard = new LinkedHashMap<>();
        Map<GemColor, Integer> working = new LinkedHashMap<>();

        working.putAll(player.getTokens());
        while (excess > 0) {
            GemColor candidate = findLargestTokenStackColor(working);
            if (candidate == null || working.get(candidate) == 0) {
                break;
            }
            discard.put(candidate, discard.getOrDefault(candidate, 0) + 1);
            working.put(candidate, working.get(candidate) - 1);
            excess--;
        }

        return discard;
    }

    private GemColor findLargestTokenStackColor(Map<GemColor, Integer> working) {
        GemColor candidate = null;
        int largestStack = 0;

        for (Map.Entry<GemColor, Integer> entry : working.entrySet()) {
            if (entry.getValue() > largestStack) {
                largestStack = entry.getValue();
                candidate = entry.getKey();
            }
        }

        return candidate;
    }

    private void mergeDiscard(
            Map<GemColor, Integer> aggregate,
            Map<GemColor, Integer> roundDiscard
    ) {
        for (Map.Entry<GemColor, Integer> entry : roundDiscard.entrySet()) {
            aggregate.put(
                    entry.getKey(),
                    aggregate.getOrDefault(entry.getKey(), 0) + entry.getValue()
            );
        }
    }
}
