package engine;

import model.GemColor;
import model.Player;

import java.util.EnumMap;
import java.util.Map;

public final class PaymentCalculator {
    private PaymentCalculator() {
    }

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
            goldNeeded += remaining - useColor;
        }

        if (goldNeeded > 0) {
            payment.put(GemColor.GOLD, goldNeeded);
        }
        return payment;
    }
}
