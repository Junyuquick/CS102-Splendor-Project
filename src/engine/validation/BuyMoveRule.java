package engine.validation;

import engine.moves.Move;
import engine.payment.PaymentCalculator;
import model.Board;
import model.DevelopmentCard;
import model.GameState;
import model.GemColor;
import model.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * Validates buy moves.
 */
final class BuyMoveRule {
    String validate(GameState state, Player player, Move move) {
        DevelopmentCard card = move.getCard();

        if (card == null) {
            return "BUY requires a card";
        }

        boolean fromReserved = move.isFromReserved();
        if (fromReserved) {
            if (!player.getReservedCards().contains(card)) {
                return "Card is not in player's reserved cards";
            }
        } else {
            Board board = state.getBoard();
            if (!board.getFaceUpCards().contains(card)) {
                return "Card is not face-up on the board";
            }
        }

        Map<GemColor, Integer> cost = card.getCost();
        if (cost == null || cost.isEmpty()) {
            if (!normalizeTokenMap(move.getPaymentTokens()).isEmpty()) {
                return "BUY payment tokens must be empty for a free card";
            }
            return null;
        }

        if (!PaymentCalculator.canAfford(player, cost)) {
            return buildAffordabilityError(player, cost);
        }

        return validatePaymentTokens(player, cost, move.getPaymentTokens());
    }

    private String validatePaymentTokens(
            Player player,
            Map<GemColor, Integer> cost,
            Map<GemColor, Integer> paymentTokens
    ) {
        Map<GemColor, Integer> expected =
                PaymentCalculator.computePaymentTokens(player, cost);
        Map<GemColor, Integer> provided = normalizeTokenMap(paymentTokens);

        for (Map.Entry<GemColor, Integer> entry : provided.entrySet()) {
            if (entry.getValue() < 0) {
                return "BUY payment tokens cannot be negative";
            }
        }

        if (!provided.equals(expected)) {
            return "BUY payment tokens do not match the required payment:"
                    + " expected " + expected + ", got " + provided;
        }
        return null;
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

    private String buildAffordabilityError(Player player, Map<GemColor, Integer> cost) {
        int goldNeeded = 0;

        for (Map.Entry<GemColor, Integer> entry : cost.entrySet()) {
            GemColor color = entry.getKey();
            int needed = entry.getValue();

            int bonusCount = player.getBonusCount(color);
            int remaining = needed - bonusCount;

            if (remaining <= 0) {
                continue;
            }

            int playerTokens = player.getTokenCount(color);
            int tokensUsable = Math.min(playerTokens, remaining);
            remaining -= tokensUsable;

            if (remaining > 0) {
                goldNeeded += remaining;
            }
        }

        int playerGold = player.getTokenCount(GemColor.GOLD);
        if (playerGold < goldNeeded) {
            return "Cannot afford card: need " + goldNeeded
                    + " gold tokens, have " + playerGold;
        }

        return null;
    }
}
