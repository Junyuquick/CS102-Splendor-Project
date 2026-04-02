package engine.payment;

import model.GemColor;
import model.Player;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Works out which tokens a player actually needs to spend to buy a card.
 */
public final class PaymentCalculator {
    private PaymentCalculator() {
    }

    /**
     * Figures out the colored and gold tokens needed after permanent bonuses are taken into account.
     *
     * @param player player making the purchase
     * @param cost card cost by gem color
     * @return the tokens that should be removed from the player
     */
    public static Map<GemColor, Integer> computePaymentTokens(
            Player player,
            Map<GemColor, Integer> cost
    ) {
        Map<GemColor, Integer> payment = new LinkedHashMap<>();
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

            // Whatever is still unpaid after using colored tokens has to be covered by gold.
            goldNeeded += remaining - useColor;
        }

        if (goldNeeded > 0) {
            payment.put(GemColor.GOLD, goldNeeded);
        }
        return payment;
    }

    /**
     * Returns whether the player can fully pay the supplied cost using bonuses, colored tokens, and gold.
     *
     * @param player player attempting to pay
     * @param cost card cost by gem color
     * @return true if the player can afford the cost, false otherwise
     */
    public static boolean canAfford(Player player, Map<GemColor, Integer> cost) {
        Map<GemColor, Integer> payment = computePaymentTokens(player, cost);
        int goldRequired = payment.getOrDefault(GemColor.GOLD, 0);

        return player.getTokenCount(GemColor.GOLD) >= goldRequired;
    }
}
