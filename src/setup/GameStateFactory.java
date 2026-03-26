package setup;

import config.Config;
import io.CardLoader;
import io.NobleLoader;
import model.Board;
import model.Cost;
import model.DevelopmentCard;
import model.GameState;
import model.GemBank;
import model.GemColor;
import model.NobleTile;
import model.Player;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds a new {@link GameState} from configuration data and fallback sample content when needed.
 */
public class GameStateFactory {
    /**
     * Selects which built-in sample data set should be used if CSV loading fails.
     */
    public enum FallbackProfile {
        LOCAL_APP,
        SERVER
    }

    private final Config config;
    private final FallbackProfile fallbackProfile;

    /**
     * Creates a game-state factory.
     *
     * @param config validated game configuration
     * @param fallbackProfile built-in sample profile to use if data loading fails
     */
    public GameStateFactory(Config config, FallbackProfile fallbackProfile) {
        this.config = config;
        this.fallbackProfile = fallbackProfile;
    }

    /**
     * Creates a fresh game state for the supplied players.
     *
     * @param players players participating in the new game
     * @param logger sink used to report fallback-loading messages
     * @return the initialized game state
     */
    public GameState createGame(List<Player> players, Consumer<String> logger) {
        int playerCount = players.size();
        GemBank bank = createBank(playerCount);
        Board board = new Board(
                buildDecks(logger),
                buildNobles(playerCount, logger),
                buildInitialGems(playerCount),
                bank,
                config.getOpenCardsPerLevel()
        );
        return new GameState(new ArrayList<>(players), board, bank);
    }

    /**
     * Creates the shared bank for the requested player count.
     *
     * @param playerCount number of players in the new game
     * @return initialized bank
     */
    private GemBank createBank(int playerCount) {
        GemBank bank = new GemBank();
        Map<GemColor, Integer> initialCounts = buildGemCounts(playerCount);
        for (Map.Entry<GemColor, Integer> entry : initialCounts.entrySet()) {
            bank.addGems(entry.getKey(), entry.getValue());
        }
        return bank;
    }

    /**
     * Builds the initial token-count map used by the board view of the bank.
     *
     * @param playerCount number of players in the new game
     * @return initial token counts by color
     */
    private Map<GemColor, Integer> buildInitialGems(int playerCount) {
        return new EnumMap<>(buildGemCounts(playerCount));
    }

    private Map<GemColor, Integer> buildGemCounts(int playerCount) {
        Map<GemColor, Integer> counts = new EnumMap<>(GemColor.class);
        for (GemColor color : GemColor.values()) {
            int amount = color == GemColor.GOLD
                    ? config.getInitialGoldGemCount(playerCount)
                    : config.getInitialNormalGemCount(playerCount);
            counts.put(color, amount);
        }
        return counts;
    }

    /**
     * Loads deck data from CSV files or falls back to built-in sample cards.
     *
     * @param logger sink used to report fallback reasons
     * @return deck lists keyed by tier
     */
    private Map<Integer, List<DevelopmentCard>> buildDecks(Consumer<String> logger) {
        try {
            return new CardLoader().load(
                    config.getCardsPath(1),
                    config.getCardsPath(2),
                    config.getCardsPath(3)
            );
        } catch (IOException | IllegalArgumentException e) {
            logger.accept("Failed to load cards from CSV. Falling back to sample deck. Reason: " + e.getMessage());
        }

        // Keep a playable deck available even when external data files cannot be loaded.
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
     * Loads noble data from CSV or falls back to a built-in sample set.
     *
     * @param playerCount number of players in the new game
     * @param logger sink used to report fallback reasons
     * @return nobles to place on the board
     */
    private List<NobleTile> buildNobles(int playerCount, Consumer<String> logger) {
        Path csv = config.getNoblesPath();
        try {
            List<NobleTile> nobles = new ArrayList<>(new NobleLoader().load(csv));
            Collections.shuffle(nobles);
            return new ArrayList<>(nobles.subList(0, Math.min(config.getNoblesCount(playerCount), nobles.size())));
        } catch (IOException | IllegalArgumentException e) {
            logger.accept("Failed to load nobles from CSV. Falling back to sample nobles. Reason: " + e.getMessage());
        }

        // The fallback sets differ slightly so local and server modes keep their expected sample data.
        List<NobleTile> fallback = fallbackProfile == FallbackProfile.SERVER
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
        return new ArrayList<>(fallback.subList(0, Math.min(config.getNoblesCount(playerCount), fallback.size())));
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
    private DevelopmentCard card(int level, int prestige, GemColor bonus, Map<GemColor, Integer> cost) {
        return new DevelopmentCard(0, level, prestige, bonus, toCost(cost));
    }

    /**
     * Creates a fallback noble tile.
     */
    private NobleTile noble(int id, int points, Map<GemColor, Integer> requirement) {
        return new NobleTile(id, points, toCost(requirement));
    }

    /**
     * Creates a sparse cost map from fixed color amounts.
     */
    private Map<GemColor, Integer> mapCost(int white, int blue, int green, int red, int black) {
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
     * Converts a color-count map into the mutable {@link Cost} value object used by the model.
     */
    private Cost toCost(Map<GemColor, Integer> values) {
        Cost cost = new Cost();
        for (Map.Entry<GemColor, Integer> entry : values.entrySet()) {
            cost.set(entry.getKey(), entry.getValue());
        }
        return cost;
    }
}
