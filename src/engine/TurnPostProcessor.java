package engine;

import config.Config;
import model.GameState;
import model.GemColor;
import model.NobleTile;
import model.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class TurnPostProcessor {
    private final Config config;
    private final NobleAssigner nobleAssigner;

    public TurnPostProcessor(Config config, NobleAssigner nobleAssigner) {
        this.config = config;
        this.nobleAssigner = nobleAssigner;
    }

    public Map<GemColor, Integer> enforceTokenLimit(GameState state, Player player) {
        Map<GemColor, Integer> discarded = new EnumMap<>(GemColor.class);
        int maxTokens = config.getMaxTokensPerPlayer();
        while (player.getTotalTokens() > maxTokens) {
            int excess = player.getTotalTokens() - maxTokens;
            Map<GemColor, Integer> roundDiscard = chooseDiscardTokens(player, excess);
            player.removeTokens(roundDiscard);
            state.getBank().addTokens(roundDiscard);
            mergeDiscard(discarded, roundDiscard);
        }
        return discarded;
    }

    public void assignBestAvailableNobles(GameState state, Player player) {
        List<NobleTile> eligibleNobles = new ArrayList<>(nobleAssigner.findEligibleNobles(state, player));
        int noblesThisTurn = Math.min(config.getMaxNoblesPerTurn(), eligibleNobles.size());
        for (int i = 0; i < noblesThisTurn; i++) {
            NobleTile chosen = chooseBestNoble(eligibleNobles);
            nobleAssigner.assignNoble(state, player, chosen);
            eligibleNobles.remove(chosen);
        }
    }

    public NobleTile chooseBestNoble(List<NobleTile> eligibleNobles) {
        return eligibleNobles.stream()
                .max((a, b) -> {
                    int points = Integer.compare(a.getPrestigePoints(), b.getPrestigePoints());
                    if (points != 0) {
                        return points;
                    }
                    int aReq = a.getRequirement().asMap().values().stream().mapToInt(Integer::intValue).sum();
                    int bReq = b.getRequirement().asMap().values().stream().mapToInt(Integer::intValue).sum();
                    return Integer.compare(bReq, aReq);
                })
                .orElse(eligibleNobles.get(0));
    }

    private Map<GemColor, Integer> chooseDiscardTokens(Player player, int excess) {
        Map<GemColor, Integer> discard = new EnumMap<>(GemColor.class);
        Map<GemColor, Integer> working = new EnumMap<>(GemColor.class);
        working.putAll(player.getTokens());
        while (excess > 0) {
            GemColor candidate = null;
            int max = 0;
            for (Map.Entry<GemColor, Integer> entry : working.entrySet()) {
                if (entry.getValue() > max) {
                    max = entry.getValue();
                    candidate = entry.getKey();
                }
            }
            if (candidate == null || max == 0) {
                break;
            }
            discard.put(candidate, discard.getOrDefault(candidate, 0) + 1);
            working.put(candidate, max - 1);
            excess--;
        }
        return discard;
    }

    private void mergeDiscard(Map<GemColor, Integer> aggregate, Map<GemColor, Integer> roundDiscard) {
        for (Map.Entry<GemColor, Integer> entry : roundDiscard.entrySet()) {
            aggregate.put(entry.getKey(), aggregate.getOrDefault(entry.getKey(), 0) + entry.getValue());
        }
    }
}
