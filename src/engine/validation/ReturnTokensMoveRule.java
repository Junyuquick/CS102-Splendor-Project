package engine.validation;

import config.Config;
import engine.moves.Move;
import model.GemColor;
import model.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * Validates return-tokens moves.
 */
final class ReturnTokensMoveRule {
    private final Config config;

    ReturnTokensMoveRule(Config config) {
        this.config = config;
    }

    String validate(Player player, Move move) {
        if (!canUseReturnTurn(player)) {
            return "RETURN_TOKENS is only available when the player already"
                    + " has " + config.getMaxTokensPerPlayer()
                    + " or more tokens";
        }

        Map<GemColor, Integer> tokens = normalizeTokenMap(move.getTokens());
        if (tokens.isEmpty()) {
            return "RETURN_TOKENS requires at least 1 token";
        }

        for (Map.Entry<GemColor, Integer> entry : tokens.entrySet()) {
            GemColor color = entry.getKey();
            int requested = entry.getValue();
            if (requested <= 0) {
                return "RETURN_TOKENS counts must be positive";
            }
            if (player.getTokenCount(color) < requested) {
                return "Player has only " + player.getTokenCount(color)
                        + " " + color + " tokens, cannot return "
                        + requested;
            }
        }

        return null;
    }

    boolean canUseReturnTurn(Player player) {
        return player.getTotalTokens() >= config.getMaxTokensPerPlayer();
    }

    private Map<GemColor, Integer> normalizeTokenMap(Map<GemColor, Integer> tokens) {
        Map<GemColor, Integer> normalized = new EnumMap<>(GemColor.class);
        if (tokens == null) {
            return normalized;
        }

        for (Map.Entry<GemColor, Integer> entry : tokens.entrySet()) {
            GemColor color = entry.getKey();
            Integer amount = entry.getValue();
            if (color == null || amount == null) {
                continue;
            }
            if (amount != 0) {
                normalized.put(color, amount);
            }
        }

        return normalized;
    }
}
