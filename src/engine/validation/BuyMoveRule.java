package engine.validation;

import engine.moves.Move;
import engine.payment.PaymentCalculator;
import model.Board;
import model.DevelopmentCard;
import model.GameState;
import model.GemColor;
import model.Player;

import java.util.LinkedHashMap;
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

        String locationError = validateCardLocation(state, player, move, card);
        if (locationError != null) {
            return locationError;
        }

        Map<GemColor, Integer> cost = card.getCost();
        String freeCardError = validateFreeCardPayment(cost, move);
        if (freeCardError != null || isFreeCard(cost)) {
            return freeCardError;
        }

        if (!PaymentCalculator.canAfford(player, cost)) {
            return buildAffordabilityError(player, cost);
        }

        return validatePaymentTokens(player, cost, move.getPaymentTokens());
    }

    private String validateCardLocation(
            GameState state,
            Player player,
            Move move,
            DevelopmentCard card
    ) {
        if (move.isFromReserved()) {
            return validateReservedCard(player, card);
        }

        return validateFaceUpCard(state.getBoard(), card);
    }

    private String validateReservedCard(Player player, DevelopmentCard card) {
        if (!player.getReservedCards().contains(card)) {
            return "Card is not in player's reserved cards";
        }

        return null;
    }

    private String validateFaceUpCard(Board board, DevelopmentCard card) {
        if (!board.getFaceUpCards().contains(card)) {
            return "Card is not face-up on the board";
        }

        return null;
    }

    private String validateFreeCardPayment(
            Map<GemColor, Integer> cost,
            Move move
    ) {
        if (!isFreeCard(cost)) {
            return null;
        }

        if (!normalizeTokenMap(move.getPaymentTokens()).isEmpty()) {
            return "BUY payment tokens must be empty for a free card";
        }

        return null;
    }

    private boolean isFreeCard(Map<GemColor, Integer> cost) {
        return cost == null || cost.isEmpty();
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
        Map<GemColor, Integer> normalized = new LinkedHashMap<>();
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
            goldNeeded += countGoldNeededForColor(player, entry.getKey(),
                    entry.getValue());
        }

        int playerGold = player.getTokenCount(GemColor.GOLD);
        if (playerGold < goldNeeded) {
            return "Cannot afford card: need " + goldNeeded
                    + " gold tokens, have " + playerGold;
        }

        return null;
    }

    private int countGoldNeededForColor(
            Player player,
            GemColor color,
            int needed
    ) {
        int remaining = needed - player.getBonusCount(color);
        if (remaining <= 0) {
            return 0;
        }

        int usableTokens = Math.min(player.getTokenCount(color), remaining);
        remaining -= usableTokens;

        return Math.max(remaining, 0);
    }
}
