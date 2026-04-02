package setup;

import config.Config;
import model.GameState;
import model.GemBank;
import model.GemColor;
import model.Player;
import model.Board;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds a new GameState by coordinating the setup helpers.
 *
 * This class stays responsible for the overall startup flow, while the
 * deck and noble loading details live in smaller helper classes.
 */
public class GameStateFactory {
    /**
     * Selects which built-in sample data set should be used if CSV
     * loading fails.
     */
    public enum FallbackProfile {
        LOCAL_APP,
        SERVER
    }

    private final Config config;
    private final DeckFactory deckFactory;
    private final NobleFactory nobleFactory;

    /**
     * Creates a game-state factory.
     *
     * @param config validated game configuration
     * @param fallbackProfile built-in sample profile to use if data loading fails
     */
    public GameStateFactory(Config config, FallbackProfile fallbackProfile) {
        this.config = config;
        this.deckFactory = new DeckFactory(config);
        this.nobleFactory = new NobleFactory(config, fallbackProfile);
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
                deckFactory.buildDecks(logger),
                nobleFactory.buildNobles(playerCount, logger),
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
     * Builds the initial token-count map used by the board view of
     * the bank.
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
}
