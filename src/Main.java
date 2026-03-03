import ai.AiController;
import ai.HumanController;
import ai.PlayerController;
import config.Config;
import engine.GameEngine;
import engine.MoveExecutor;
import engine.MoveValidator;
import engine.NobleAssigner;
import engine.TurnManager;
import engine.WinnerChecker;
import io.CardLoader;
import model.Board;
import model.Cost;
import model.DevelopmentCard;
import model.GameState;
import model.GemBank;
import model.GemColor;
import model.NobleTile;
import model.Player;
import ui.ConsoleUi;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Config config = buildConfig();
        ConsoleUi ui = new ConsoleUi();

        String p1Name = ui.promptText("Enter Player 1 name:");
        boolean vsComputer = isYes(ui.promptText("Play against computer? (y/n)"));

        List<Player> players;
        List<PlayerController> controllers;

        if (vsComputer) {
            boolean humanFirst = isYes(ui.promptText("Do you want to go first? (y/n)"));
            AiController.Level aiLevel = parseAiLevel(ui.promptText("Computer level? (high/low)"));
            Player human = new Player(p1Name);
            Player computer = new Player("Computer");
            if (humanFirst) {
                players = List.of(human, computer);
                controllers = List.of(new HumanController(), new AiController(aiLevel));
            } else {
                players = List.of(computer, human);
                controllers = List.of(new AiController(aiLevel), new HumanController());
            }
        } else {
            int playerCount = parsePlayerCount(ui.promptText("How many human players? (2-4)"));
            List<Player> humanPlayers = new ArrayList<>();
            humanPlayers.add(new Player(p1Name));
            for (int i = 2; i <= playerCount; i++) {
                String nextName = ui.promptText("Enter Player " + i + " name:");
                humanPlayers.add(new Player(nextName));
            }
            players = humanPlayers;
            List<PlayerController> humanControllers = new ArrayList<>();
            for (int i = 0; i < playerCount; i++) {
                humanControllers.add(new HumanController());
            }
            controllers = humanControllers;
        }

        GemBank bank = new GemBank();
        Map<GemColor, Integer> initialGems = new EnumMap<>(GemColor.class);
        for (GemColor color : GemColor.values()) {
            int amount = color == GemColor.GOLD
                    ? config.getInitialGoldGemCount(players.size())
                    : config.getInitialNormalGemCount(players.size());
            initialGems.put(color, amount);
            bank.addGems(color, amount);
        }

        Board board = new Board(buildDecks(), buildNobles(), initialGems, bank, config.getOpenCardsPerLevel());
        GameState state = new GameState(new ArrayList<>(players), board, bank);

        GameEngine engine = new GameEngine(
                config,
                ui,
                state,
                controllers,
                new MoveValidator(config),
                new MoveExecutor(config),
                new NobleAssigner(),
                new WinnerChecker(config),
                new TurnManager()
        );

        engine.run();
    }

    private static boolean isYes(String raw) {
        if (raw == null) {
            return false;
        }
        String value = raw.trim().toLowerCase();
        return value.equals("y") || value.equals("yes");
    }

    private static AiController.Level parseAiLevel(String raw) {
        if (raw == null) {
            return AiController.Level.HIGH;
        }
        String value = raw.trim().toLowerCase();
        if (value.equals("low") || value.equals("l")) {
            return AiController.Level.LOW;
        }
        return AiController.Level.HIGH;
    }

    private static int parsePlayerCount(String raw) {
        if (raw == null) {
            return 2;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 2) return 2;
            if (value > 4) return 4;
            return value;
        } catch (NumberFormatException e) {
            return 2;
        }
    }

    public static Config buildConfig() {
        Path here = Path.of(".");
        return new Config(
                15, // pointsToWin
                10, // maxTokensPerPlayer
                3,  // maxReservedCards
                1,  // maxNoblesPerTurn
                2,  // minPlayers
                4,  // maxPlayers
                3,  // numLevels
                4,  // openCardsPerLevel
                3,  // noblesCount.2p
                4,  // noblesCount.3p
                5,  // noblesCount.4p
                4,  // bank.normal.2p
                5,  // bank.normal.3p
                7,  // bank.normal.4p
                5,  // bank.gold
                3,  // takeDifferent.count
                2,  // takeSame.count
                2,  // takeSame.minRemainingInBank
                1,  // reserve.goldBonus
                here, here, here, here,
                here, here, here
        );
    }

    public static Map<Integer, List<DevelopmentCard>> buildDecks() {
        Path csv = Path.of("temporaryFolder", "Splendor Cards.csv");
        try {
            return new CardLoader().load(csv);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Failed to load cards from " + csv + ". Falling back to sample deck. Reason: " + e.getMessage());
        }

        Map<Integer, List<DevelopmentCard>> decks = new java.util.HashMap<>();

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

    public static List<NobleTile> buildNobles() {
        return List.of(
                noble(3, mapCost(3, 3, 3, 0, 0)),
                noble(3, mapCost(0, 3, 3, 3, 0)),
                noble(3, mapCost(0, 0, 3, 3, 3)),
                noble(3, mapCost(3, 0, 0, 3, 3))
        );
    }

    public static DevelopmentCard card(int level, int points, GemColor bonus, Map<GemColor, Integer> costs) {
        Cost cost = new Cost();
        for (Map.Entry<GemColor, Integer> entry : costs.entrySet()) {
            cost.set(entry.getKey(), entry.getValue());
        }
        return new DevelopmentCard(level, points, bonus, cost);
    }

    public static NobleTile noble(int points, Map<GemColor, Integer> reqs) {
        Cost cost = new Cost();
        for (Map.Entry<GemColor, Integer> entry : reqs.entrySet()) {
            cost.set(entry.getKey(), entry.getValue());
        }
        return new NobleTile(points, cost);
    }

    public static Map<GemColor, Integer> mapCost(int w, int b, int g, int r, int k) {
        Map<GemColor, Integer> m = new EnumMap<>(GemColor.class);
        if (w > 0) m.put(GemColor.WHITE, w);
        if (b > 0) m.put(GemColor.BLUE, b);
        if (g > 0) m.put(GemColor.GREEN, g);
        if (r > 0) m.put(GemColor.RED, r);
        if (k > 0) m.put(GemColor.BLACK, k);
        return m;
    }

    private static List<DevelopmentCard> shuffled(List<DevelopmentCard> cards) {
        List<DevelopmentCard> copy = new ArrayList<>(cards);
        Collections.shuffle(copy);
        return copy;
    }
}
