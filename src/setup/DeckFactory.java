package setup;

import config.Config;
import io.CardLoader;
import model.Cost;
import model.DevelopmentCard;
import model.GemColor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Loads development-card decks and provides fallback sample decks when
 * external data is unavailable.
 */
class DeckFactory {
    private final Config config;

    /**
     * Creates a deck factory backed by the supplied configuration.
     *
     * @param config validated game configuration
     */
    DeckFactory(Config config) {
        this.config = config;
    }

    /**
     * Loads deck data from CSV files or falls back to built-in sample
     * cards.
     *
     * @param logger sink used to report fallback reasons
     * @return deck lists keyed by tier
     */
    Map<Integer, List<DevelopmentCard>> buildDecks(Consumer<String> logger) {
        try {
            return new CardLoader().load(
                    config.getCardsPath(1),
                    config.getCardsPath(2),
                    config.getCardsPath(3)
            );
        } catch (IOException | IllegalArgumentException e) {
            logger.accept(
                    "Failed to load cards from CSV. Falling back to sample "
                            + "deck. Reason: " + e.getMessage()
            );
        }

        Map<Integer, List<DevelopmentCard>> decks = new HashMap<>();
        decks.put(1, shuffled(List.of(
                card(1, 0, GemColor.WHITE, mapCost(0, 1, 1, 1, 1)),
                card(1, 0, GemColor.BLUE, mapCost(1, 0, 1, 1, 1)),
                card(1, 0, GemColor.GREEN, mapCost(1, 1, 0, 1, 1)),
                card(1, 0, GemColor.RED, mapCost(1, 1, 1, 0, 1)),
                card(1, 0, GemColor.BLACK, mapCost(1, 1, 1, 1, 0)),
                card(1, 1, GemColor.WHITE, mapCost(0, 0, 2, 2, 0)),
                card(1, 1, GemColor.BLUE, mapCost(2, 0, 0, 2, 0)),
                card(1, 1, GemColor.GREEN, mapCost(2, 2, 0, 0, 0))
        )));
        decks.put(2, shuffled(List.of(
                card(2, 1, GemColor.WHITE, mapCost(0, 2, 2, 2, 0)),
                card(2, 1, GemColor.BLUE, mapCost(0, 0, 3, 2, 2)),
                card(2, 1, GemColor.GREEN, mapCost(2, 0, 0, 3, 2)),
                card(2, 2, GemColor.RED, mapCost(0, 3, 0, 2, 3)),
                card(2, 2, GemColor.BLACK, mapCost(3, 2, 0, 0, 3)),
                card(2, 2, GemColor.WHITE, mapCost(3, 0, 3, 2, 0))
        )));
        decks.put(3, shuffled(List.of(
                card(3, 3, GemColor.WHITE, mapCost(0, 3, 3, 5, 3)),
                card(3, 3, GemColor.BLUE, mapCost(3, 0, 3, 3, 5)),
                card(3, 4, GemColor.GREEN, mapCost(3, 3, 0, 3, 6)),
                card(3, 4, GemColor.RED, mapCost(6, 3, 3, 0, 3)),
                card(3, 5, GemColor.BLACK, mapCost(3, 6, 3, 3, 0))
        )));
        return decks;
    }

    /**
     * Returns a shuffled copy of the supplied list.
     *
     * @param items source items
     * @param <T> element type
     * @return shuffled copy
     */
    private <T> List<T> shuffled(List<T> items) {
        List<T> copy = new ArrayList<>(items);
        Collections.shuffle(copy);
        return copy;
    }

    /**
     * Creates a fallback development card.
     */
    private DevelopmentCard card(
            int level,
            int prestige,
            GemColor bonus,
            Map<GemColor, Integer> cost
    ) {
        return new DevelopmentCard(0, level, prestige, bonus, toCost(cost));
    }

    /**
     * Creates a sparse cost map from fixed color amounts.
     */
    private Map<GemColor, Integer> mapCost(
            int white,
            int blue,
            int green,
            int red,
            int black
    ) {
        Map<GemColor, Integer> cost = new LinkedHashMap<>();
        if (white > 0) {
            cost.put(GemColor.WHITE, white);
        }
        if (blue > 0) {
            cost.put(GemColor.BLUE, blue);
        }
        if (green > 0) {
            cost.put(GemColor.GREEN, green);
        }
        if (red > 0) {
            cost.put(GemColor.RED, red);
        }
        if (black > 0) {
            cost.put(GemColor.BLACK, black);
        }
        return cost;
    }

    /**
     * Converts a color-count map into the mutable Cost value object
     * used by the model.
     */
    private Cost toCost(Map<GemColor, Integer> values) {
        Cost cost = new Cost();
        for (Map.Entry<GemColor, Integer> entry : values.entrySet()) {
            cost.set(entry.getKey(), entry.getValue());
        }
        return cost;
    }
}
