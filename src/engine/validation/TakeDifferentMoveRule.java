package engine.validation;

import config.Config;
import engine.moves.Move;
import model.GameState;
import model.GemBank;
import model.GemColor;
import model.Player;

import java.util.List;
import java.util.Map;

/**
 * Validates take-different moves.
 */
final class TakeDifferentMoveRule {
    private final Config config;
    private final AvailableMovesInspector availableMovesInspector;

    TakeDifferentMoveRule(
            Config config,
            AvailableMovesInspector availableMovesInspector
    ) {
        this.config = config;
        this.availableMovesInspector = availableMovesInspector;
    }

    String validate(GameState state, Player player, Move move) {
        Map<GemColor, Integer> tokens = move.getTokens();

        if (tokens == null || tokens.isEmpty()) {
            return "TAKE_DIFFERENT requires tokens";
        }

        int maxColors = config.getTakeDifferentCount();
        if (tokens.size() > maxColors) {
            return "TAKE_DIFFERENT allows at most " + maxColors
                    + " different colors, got " + tokens.size();
        }

        GemBank bank = state.getBank();

        for (Map.Entry<GemColor, Integer> entry : tokens.entrySet()) {
            GemColor color = entry.getKey();
            int requested = entry.getValue();

            if (requested != 1) {
                return "TAKE_DIFFERENT: each color must have count 1, got "
                        + color + " = " + requested;
            }

            if (bank.getTokenCount(color) < 1) {
                return "Bank has no " + color + " tokens available";
            }
        }

        int availableNormalColors = countAvailableNormalColors(bank);
        if (tokens.size() != maxColors) {
            if (tokens.size() < 1) {
                return "TAKE_DIFFERENT requires at least 1 color";
            }
            if (availableNormalColors >= maxColors) {
                return "TAKE_DIFFERENT requires exactly " + maxColors
                        + " colors while that many are available";
            }
            if (availableMovesInspector.hasAnyLegalReserve(state, player)
                    || availableMovesInspector.hasAnyLegalBuy(state, player)
                    || availableMovesInspector.hasAnyLegalReturnTokens(player)) {
                return "TAKE_DIFFERENT may take only " + tokens.size()
                        + " colors here only when reserve, buy, and"
                        + " return-tokens moves are all unavailable";
            }
        }

        int totalAfter = player.getTotalTokens() + tokens.size();
        if (totalAfter > config.getMaxTokensPerPlayer()) {
            return "Taking " + tokens.size()
                    + " tokens would exceed max tokens per player ("
                    + config.getMaxTokensPerPlayer() + ")";
        }

        return null;
    }

    private int countAvailableNormalColors(GemBank bank) {
        int count = 0;
        for (GemColor color : List.of(
                GemColor.WHITE,
                GemColor.BLUE,
                GemColor.GREEN,
                GemColor.RED,
                GemColor.BLACK
        )) {
            if (bank.getTokenCount(color) > 0) {
                count++;
            }
        }

        return count;
    }
}
