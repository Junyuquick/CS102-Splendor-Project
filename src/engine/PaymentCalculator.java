package engine;

import model.GemColor;
import model.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * Calculates which player tokens must be spent to pay for a card after permanent bonuses are applied.
 */
public final class PaymentCalculator {
    private PaymentCalculator() {
    }

    /**
     * Computes the colored and gold tokens a player must spend to cover a card cost.
     *
     * @param player player making the purchase
     * @param cost card cost keyed by token color
     * @return the tokens that must be removed from the player to complete the purchase
     */
    public static Map<GemColor, Integer> computePaymentTokens(Player player, Map<GemColor, Integer> cost) {
        Map<GemColor, Integer> payment = new EnumMap<>(GemColor.class);
        int goldNeeded = 0;

        for (Map.Entry<GemColor, Integer> entry : cost.entrySet()) {
            GemColor color = entry.getKey();
            int remaining = Math.max(0, entry.getValue() - player.getBonusCount(color));
            if (remaining == 0) {
                continue;
            }

            int useColor = Math.min(player.getTokenCount(color), remaining);
            if (useColor > 0) {
                payment.put(color, useColor);
            }
            // Any remaining unpaid amount must be covered by gold tokens.
            goldNeeded += remaining - useColor;
        }

        if (goldNeeded > 0) {
            payment.put(GemColor.GOLD, goldNeeded);
        }
        return payment;
    }
}
