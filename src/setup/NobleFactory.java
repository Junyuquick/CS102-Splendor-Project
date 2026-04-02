package setup;

import config.Config;
import io.NobleLoader;
import model.Cost;
import model.GemColor;
import model.NobleTile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Loads noble tiles and provides fallback noble sets when external
 * data is unavailable.
 */
class NobleFactory {
    private final Config config;
    private final GameStateFactory.FallbackProfile fallbackProfile;

    /**
     * Creates a noble factory backed by the supplied configuration.
     *
     * @param config validated game configuration
     * @param fallbackProfile fallback sample profile to use
     */
    NobleFactory(
            Config config,
            GameStateFactory.FallbackProfile fallbackProfile
    ) {
        this.config = config;
        this.fallbackProfile = fallbackProfile;
    }

    /**
     * Loads noble data from CSV or falls back to a built-in sample set.
     *
     * @param playerCount number of players in the new game
     * @param logger sink used to report fallback reasons
     * @return nobles to place on the board
     */
    List<NobleTile> buildNobles(int playerCount, Consumer<String> logger) {
        Path csv = config.getNoblesPath();
        try {
            List<NobleTile> nobles = new ArrayList<>(
                    new NobleLoader().load(csv)
            );
            Collections.shuffle(nobles);
            return new ArrayList<>(
                    nobles.subList(
                            0,
                            Math.min(
                                    config.getNoblesCount(playerCount),
                                    nobles.size()
                            )
                    )
            );
        } catch (IOException | IllegalArgumentException e) {
            logger.accept(
                    "Failed to load nobles from CSV. Falling back to sample "
                            + "nobles. Reason: " + e.getMessage()
            );
        }

        List<NobleTile> fallback = fallbackProfile
                == GameStateFactory.FallbackProfile.SERVER
                ? shuffled(List.of(
                noble(1, 3, mapCost(3, 3, 3, 0, 0)),
                noble(2, 3, mapCost(0, 3, 3, 3, 0)),
                noble(3, 3, mapCost(0, 0, 3, 3, 3)),
                noble(4, 3, mapCost(3, 0, 0, 3, 3))
        ))
                : shuffled(List.of(
                noble(1, 3, mapCost(3, 3, 0, 0, 0)),
                noble(2, 3, mapCost(0, 3, 3, 0, 0)),
                noble(3, 3, mapCost(0, 0, 3, 3, 0)),
                noble(4, 3, mapCost(0, 0, 0, 3, 3)),
                noble(5, 3, mapCost(3, 0, 0, 0, 3))
        ));
        return new ArrayList<>(
                fallback.subList(
                        0,
                        Math.min(
                                config.getNoblesCount(playerCount),
                                fallback.size()
                        )
                )
        );
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
     * Creates a fallback noble tile.
     */
    private NobleTile noble(
            int id,
            int points,
            Map<GemColor, Integer> requirement
    ) {
        return new NobleTile(id, points, toCost(requirement));
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
        Map<GemColor, Integer> cost = new EnumMap<>(GemColor.class);
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
