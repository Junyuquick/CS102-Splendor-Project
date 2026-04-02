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

        String tokenError = validateTokenSelection(tokens);
        if (tokenError != null) {
            return tokenError;
        }

        GemBank bank = state.getBank();
        String bankError = validateBankAvailability(tokens, bank);
        if (bankError != null) {
            return bankError;
        }

        String fallbackError = validateColorCountRules(
                state,
                player,
                tokens,
                bank
        );
        if (fallbackError != null) {
            return fallbackError;
        }

        return validatePlayerTokenLimit(player, tokens.size());
    }

    private String validateTokenSelection(Map<GemColor, Integer> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "TAKE_DIFFERENT requires tokens";
        }

        int maxColors = config.getTakeDifferentCount();
        if (tokens.size() > maxColors) {
            return "TAKE_DIFFERENT allows at most " + maxColors
                    + " different colors, got " + tokens.size();
        }

        return null;
    }

    private String validateBankAvailability(
            Map<GemColor, Integer> tokens,
            GemBank bank
    ) {
        for (Map.Entry<GemColor, Integer> entry : tokens.entrySet()) {
            String error = validateSingleColorRequest(entry, bank);
            if (error != null) {
                return error;
            }
        }

        return null;
    }

    private String validateSingleColorRequest(
            Map.Entry<GemColor, Integer> entry,
            GemBank bank
    ) {
        GemColor color = entry.getKey();
        int requested = entry.getValue();

        if (requested != 1) {
            return "TAKE_DIFFERENT: each color must have count 1, got "
                    + color + " = " + requested;
        }

        if (bank.getTokenCount(color) < 1) {
            return "Bank has no " + color + " tokens available";
        }

        return null;
    }

    private String validateColorCountRules(
            GameState state,
            Player player,
            Map<GemColor, Integer> tokens,
            GemBank bank
    ) {
        int chosenColors = tokens.size();
        int requiredColors = config.getTakeDifferentCount();

        if (chosenColors == requiredColors) {
            return null;
        }

        if (chosenColors < 1) {
            return "TAKE_DIFFERENT requires at least 1 color";
        }

        int availableNormalColors = countAvailableNormalColors(bank);
        if (availableNormalColors >= requiredColors) {
            return "TAKE_DIFFERENT requires exactly " + requiredColors
                    + " colors while that many are available";
        }

        if (hasAnyFallbackMove(state, player)) {
            return "TAKE_DIFFERENT may take only " + chosenColors
                    + " colors here only when reserve, buy, and"
                    + " return-tokens moves are all unavailable";
        }

        return null;
    }

    private boolean hasAnyFallbackMove(GameState state, Player player) {
        return availableMovesInspector.hasAnyLegalReserve(state, player)
                || availableMovesInspector.hasAnyLegalBuy(state, player)
                || availableMovesInspector.hasAnyLegalReturnTokens(player);
    }

    private String validatePlayerTokenLimit(Player player, int tokenCount) {
        int totalAfter = player.getTotalTokens() + tokenCount;
        if (totalAfter > config.getMaxTokensPerPlayer()) {
            return "Taking " + tokenCount
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
