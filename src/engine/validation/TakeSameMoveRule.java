package engine.validation;

import config.Config;
import engine.moves.Move;
import model.GameState;
import model.GemBank;
import model.GemColor;
import model.Player;

import java.util.Map;

/**
 * Validates take-two-same moves.
 */
final class TakeSameMoveRule {
    private final Config config;

    TakeSameMoveRule(Config config) {
        this.config = config;
    }

    String validate(GameState state, Player player, Move move) {
        Map<GemColor, Integer> tokens = move.getTokens();

        if (tokens == null || tokens.isEmpty()) {
            return "TAKE_TWO_SAME requires tokens";
        }

        if (tokens.size() != 1) {
            return "TAKE_TWO_SAME requires exactly 1 color, got "
                    + tokens.size();
        }

        GemColor color = tokens.keySet().iterator().next();
        int requested = tokens.get(color);

        int sameCount = config.getTakeSameCount();
        if (requested != sameCount) {
            return "TAKE_TWO_SAME requires count " + sameCount
                    + ", got " + requested;
        }

        GemBank bank = state.getBank();
        int bankCount = bank.getTokenCount(color);

        if (bankCount < sameCount) {
            return "Bank has only " + bankCount + " " + color
                    + " tokens, need " + sameCount;
        }

        int remainingAfter = bankCount - sameCount;
        int minRemaining = config.getTakeSameMinRemainingInBank();
        if (remainingAfter < minRemaining) {
            return "Taking " + sameCount + " " + color
                    + " would leave only " + remainingAfter
                    + " in bank, need at least " + minRemaining;
        }

        int totalAfter = player.getTotalTokens() + sameCount;
        if (totalAfter > config.getMaxTokensPerPlayer()) {
            return "Taking " + sameCount
                    + " tokens would exceed max tokens per player ("
                    + config.getMaxTokensPerPlayer() + ")";
        }

        return null;
    }
}
