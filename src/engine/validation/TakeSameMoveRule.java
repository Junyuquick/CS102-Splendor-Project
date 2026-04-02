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

        String tokenError = validateRequestedTokens(tokens);
        if (tokenError != null) {
            return tokenError;
        }

        GemColor color = getChosenColor(tokens);
        int sameCount = config.getTakeSameCount();
        String bankError = validateBankSupply(state.getBank(), color, sameCount);
        if (bankError != null) {
            return bankError;
        }

        return validatePlayerTokenLimit(player, sameCount);
    }

    private String validateRequestedTokens(Map<GemColor, Integer> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "TAKE_TWO_SAME requires tokens";
        }

        if (tokens.size() != 1) {
            return "TAKE_TWO_SAME requires exactly 1 color, got "
                    + tokens.size();
        }

        GemColor color = getChosenColor(tokens);
        int requested = tokens.get(color);
        int sameCount = config.getTakeSameCount();

        if (requested != sameCount) {
            return "TAKE_TWO_SAME requires count " + sameCount
                    + ", got " + requested;
        }

        return null;
    }

    private GemColor getChosenColor(Map<GemColor, Integer> tokens) {
        return tokens.keySet().iterator().next();
    }

    private String validateBankSupply(
            GemBank bank,
            GemColor color,
            int sameCount
    ) {
        int bankCount = bank.getTokenCount(color);
        if (bankCount < sameCount) {
            return "Bank has only " + bankCount + " " + color
                    + " tokens, need " + sameCount;
        }

        return validateMinimumRemainingTokens(bankCount, color, sameCount);
    }

    private String validateMinimumRemainingTokens(
            int bankCount,
            GemColor color,
            int sameCount
    ) {
        int remainingAfter = bankCount - sameCount;
        int minRemaining = config.getTakeSameMinRemainingInBank();
        if (remainingAfter < minRemaining) {
            return "Taking " + sameCount + " " + color
                    + " would leave only " + remainingAfter
                    + " in bank, need at least " + minRemaining;
        }

        return null;
    }

    private String validatePlayerTokenLimit(Player player, int sameCount) {
        int totalAfter = player.getTotalTokens() + sameCount;
        if (totalAfter > config.getMaxTokensPerPlayer()) {
            return "Taking " + sameCount
                    + " tokens would exceed max tokens per player ("
                    + config.getMaxTokensPerPlayer() + ")";
        }

        return null;
    }
}
