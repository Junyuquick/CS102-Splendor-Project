package ui.swing;

import ai.GreedyStrategy;
import engine.Move;
import engine.MoveExecutor;
import engine.MoveValidator;
import engine.NobleAssigner;
import engine.TurnManager;
import engine.WinnerChecker;
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

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SwingSplendorApp extends AbstractSwingSplendorFrame {
    private final MoveExecutor executor;
    private final NobleAssigner nobleAssigner;
    private final WinnerChecker winnerChecker;
    private final TurnManager turnManager;
    private final GreedyStrategy aiStrategy;
    private final Map<Player, JPanel> playerCardPanels = new LinkedHashMap<>();
    private final Map<Player, javax.swing.JEditorPane> playerCardAreas = new LinkedHashMap<>();
    private final Set<Player> computerPlayers = new HashSet<>();

    private boolean finalGameOver = false;
    private boolean computerThinking = false;

    public SwingSplendorApp() {
        this(SwingConfigSupport.loadConfig());
    }

    private SwingSplendorApp(config.Config config) {
        super(
                "Splendor (Swing)",
                config,
                null,
                new MoveValidator(config)
        );
        this.state = createInitialState();
        this.executor = new MoveExecutor(config);
        this.nobleAssigner = new NobleAssigner();
        this.winnerChecker = new WinnerChecker(config);
        this.turnManager = new TurnManager();
        this.aiStrategy = new GreedyStrategy(config);

        buildSharedUi(false, false, state.getPlayers().size());
        initialisePlayerPanels();
        bindSharedActions();
        refreshAll();
    }

    private GameState createInitialState() {
        String playerName = promptName("Enter Player 1 name:");
        int computerCount = askComputerCount();
        List<Player> players = new ArrayList<>();
        Player human = new Player(playerName);
        players.add(human);
        for (int i = 1; i <= computerCount; i++) {
            Player computer = new Player(i == 1 ? "Computer" : "Computer " + i);
            computerPlayers.add(computer);
            players.add(computer);
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

        Board board = new Board(buildDecks(), buildNobles(players.size()), initialGems, bank, config.getOpenCardsPerLevel());
        return new GameState(new ArrayList<>(players), board, bank);
    }

    private void initialisePlayerPanels() {
        playersPanel.removeAll();
        playerCardPanels.clear();
        playerCardAreas.clear();
        playersPanel.setLayout(new java.awt.GridLayout(state.getPlayers().size(), 1, 8, 8));

        for (Player player : state.getPlayers()) {
            JPanel panel = createPlayerPanel(player.getName(), false);
            javax.swing.JEditorPane area = createPlayerHtmlArea();
            JScrollPane areaScroll = new JScrollPane(area);
            SwingUiTheme.styleScrollPane(areaScroll, SwingUiTheme.PANEL_BG_ALT);
            panel.add(areaScroll, BorderLayout.CENTER);
            playerCardPanels.put(player, panel);
            playerCardAreas.put(player, area);
            playersPanel.add(panel);
        }
    }

    private void refreshAll() {
        rebuildMarketButtons();
        rebuildNobleCards();
        refreshBankCounts();

        Player current = state.getCurrentPlayer();
        statusLabel.setText("You: " + state.getPlayer(0).getName());
        reservedLabel.setText(current.getName() + " Reserved Cards");

        for (Player player : state.getPlayers()) {
            javax.swing.JEditorPane area = playerCardAreas.get(player);
            if (area != null) {
                area.setText(SwingPlayerSummaryFormatter.buildRichHtml(player, config));
                area.setCaretPosition(0);
            }

            JPanel panel = playerCardPanels.get(player);
            if (panel != null) {
                if (player == current) {
                    panel.setBorder(BorderFactory.createTitledBorder(
                            new LineBorder(SwingUiTheme.ACCENT_BLUE, 2),
                            player.getName() + " (Active)"
                    ));
                    SwingUiTheme.styleTitledBorder((TitledBorder) panel.getBorder());
                } else {
                    panel.setBorder(SwingUiTheme.createTitledBorder(player.getName()));
                }
            }
        }

        refreshReservedCards(current);
        updateLegalUi();
        scheduleComputerTurnIfNeeded();
    }

    @Override
    protected void handleConfirmAction() {
        if (finalGameOver) {
            return;
        }

        Move move = buildPendingMove();
        if (move == null) {
            log("No valid selection to confirm.");
            return;
        }

        Player current = state.getCurrentPlayer();
        String error = validator.validate(state, current, move);
        if (error != null) {
            log("Illegal move: " + error);
            updateLegalUi();
            return;
        }

        executor.execute(state, current, move);
        log(current.getName() + " played: " + move.getType());
        resolveTokenCapIfNeeded(current);
        resolveNobleAttraction(current, false);

        if (winnerChecker.shouldTriggerFinalRound(state)) {
            turnManager.markFinalRound(state);
            log("Final round triggered by " + current.getName());
        }

        turnManager.advanceTurn(state);
        if (turnManager.hasFinalRoundCompleted(state)) {
            Player winner = winnerChecker.determineWinner(state);
            finalGameOver = true;
            log("Game over. Winner: " + winner.getName() + " (" + winner.getPrestigePoints() + " points)");
            showSinglePlayerGameOverDialog(winner);
            return;
        }

        clearSelection();
        refreshAll();
    }

    @Override
    protected void updateLegalUi() {
        if (finalGameOver) {
            disableAllInputs();
            return;
        }

        Player current = state.getCurrentPlayer();
        statusLabel.setText("You: " + state.getPlayer(0).getName());
        phaseLabel.setText("Turn: " + current.getName() + " | Phase: " + mode.name().replace('_', ' '));

        if (isComputerTurn() || computerThinking) {
            helpArea.setText("Computer is thinking...");
            disableAllInputs();
            return;
        }

        helpArea.setText(activeTurnHelpText());
        actionTakeThree.setEnabled(hasAnyLegalTakeThree(current));
        actionTakeTwo.setEnabled(hasAnyLegalTakeTwo(current));
        actionReserve.setEnabled(hasAnyLegalReserve(current));
        actionBuy.setEnabled(hasAnyLegalBuy(current));
        actionCancel.setEnabled(mode != SwingGameMode.IDLE);
        reservedList.setEnabled(mode == SwingGameMode.BUY);

        boolean cardMode = mode == SwingGameMode.RESERVE || mode == SwingGameMode.BUY;
        updateCardSelectionState(current, true, cardMode);
        applyTokenModeRules(current, true);

        Move pending = buildPendingMove();
        actionConfirm.setEnabled(mode != SwingGameMode.IDLE
                && pending != null
                && validator.validate(state, current, pending) == null);
    }

    @Override
    protected boolean showTokenBankCountsOnButtons() {
        return true;
    }

    private void resolveTokenCapIfNeeded(Player player) {
        int maxTokens = config.getMaxTokensPerPlayer();
        while (player.getTotalTokens() > maxTokens) {
            int excess = player.getTotalTokens() - maxTokens;
            Map<GemColor, Integer> discard = autoDiscard(player, excess);
            player.removeTokens(discard);
            state.getBank().addTokens(discard);
            log(player.getName() + " discarded " + discard);
        }
    }

    private Map<GemColor, Integer> autoDiscard(Player player, int excess) {
        Map<GemColor, Integer> discard = new EnumMap<>(GemColor.class);
        Map<GemColor, Integer> working = new EnumMap<>(GemColor.class);
        working.putAll(player.getTokens());
        while (excess > 0) {
            GemColor candidate = null;
            int max = 0;
            for (Map.Entry<GemColor, Integer> entry : working.entrySet()) {
                if (entry.getValue() > max) {
                    max = entry.getValue();
                    candidate = entry.getKey();
                }
            }
            if (candidate == null || max == 0) {
                break;
            }
            discard.put(candidate, discard.getOrDefault(candidate, 0) + 1);
            working.put(candidate, max - 1);
            excess--;
        }
        return discard;
    }

    private void resolveNobleAttraction(Player player, boolean automaticChoice) {
        List<NobleTile> eligible = new ArrayList<>(nobleAssigner.findEligibleNobles(state, player));
        if (eligible.isEmpty() || config.getMaxNoblesPerTurn() <= 0) {
            return;
        }
        int noblesToAssign = Math.min(config.getMaxNoblesPerTurn(), eligible.size());
        for (int i = 0; i < noblesToAssign; i++) {
            NobleTile chosen = chooseNoble(eligible, automaticChoice);
            nobleAssigner.assignNoble(state, player, chosen);
            eligible.remove(chosen);
            log(player.getName() + " attracted noble: " + chosen);
        }
    }

    private NobleTile chooseNoble(List<NobleTile> eligible, boolean automaticChoice) {
        if (eligible.size() == 1) {
            return eligible.get(0);
        }
        if (automaticChoice) {
            return eligible.stream()
                    .max((a, b) -> {
                        int points = Integer.compare(a.getPrestigePoints(), b.getPrestigePoints());
                        if (points != 0) {
                            return points;
                        }
                        int aReq = a.getRequirement().asMap().values().stream().mapToInt(Integer::intValue).sum();
                        int bReq = b.getRequirement().asMap().values().stream().mapToInt(Integer::intValue).sum();
                        return Integer.compare(bReq, aReq);
                    })
                    .orElse(eligible.get(0));
        }
        Object pick = JOptionPane.showInputDialog(
                this,
                "Choose a noble",
                "Noble Choice",
                JOptionPane.QUESTION_MESSAGE,
                null,
                eligible.toArray(),
                eligible.get(0)
        );
        return pick instanceof NobleTile ? (NobleTile) pick : eligible.get(0);
    }

    private String promptName(String prompt) {
        String name = JOptionPane.showInputDialog(this, prompt, "Player Setup", JOptionPane.QUESTION_MESSAGE);
        if (name == null || name.isBlank()) {
            return "Player";
        }
        return name.trim();
    }

    private int askComputerCount() {
        int minComputers = Math.max(1, config.getMinPlayers() - 1);
        int maxComputers = Math.max(minComputers, config.getMaxPlayers() - 1);
        if (minComputers == maxComputers) {
            return minComputers;
        }

        List<String> options = new ArrayList<>();
        for (int i = minComputers; i <= maxComputers; i++) {
            options.add(String.valueOf(i));
        }

        Object choice = JOptionPane.showInputDialog(
                this,
                "How many computers do you want to play with?",
                "Computer Count",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options.toArray(),
                options.get(0)
        );
        if (choice == null) {
            return minComputers;
        }
        try {
            int count = Integer.parseInt(choice.toString());
            return Math.max(minComputers, Math.min(maxComputers, count));
        } catch (NumberFormatException e) {
            return minComputers;
        }
    }

    private Map<Integer, List<DevelopmentCard>> buildDecks() {
        try {
            return new CardLoader().load(
                    config.getCardsPath(1),
                    config.getCardsPath(2),
                    config.getCardsPath(3)
            );
        } catch (IOException | IllegalArgumentException e) {
            log("Failed to load cards from CSV. Falling back to sample deck. Reason: " + e.getMessage());
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

    private List<NobleTile> buildNobles(int playerCount) {
        Path csv = config.getNoblesPath();
        try {
            List<NobleTile> nobles = new ArrayList<>(new NobleLoader().load(csv));
            Collections.shuffle(nobles);
            return new ArrayList<>(nobles.subList(0, Math.min(config.getNoblesCount(playerCount), nobles.size())));
        } catch (IOException | IllegalArgumentException e) {
            log("Failed to load nobles from CSV. Falling back to sample nobles. Reason: " + e.getMessage());
        }

        List<NobleTile> fallback = shuffled(List.of(
                noble(1, mapReq(3, 3, 0, 0, 0)),
                noble(2, mapReq(0, 3, 3, 0, 0)),
                noble(3, mapReq(0, 0, 3, 3, 0)),
                noble(4, mapReq(0, 0, 0, 3, 3)),
                noble(5, mapReq(3, 0, 0, 0, 3))
        ));
        return new ArrayList<>(fallback.subList(0, Math.min(config.getNoblesCount(playerCount), fallback.size())));
    }

    private <T> List<T> shuffled(List<T> items) {
        List<T> copy = new ArrayList<>(items);
        Collections.shuffle(copy);
        return copy;
    }

    private DevelopmentCard card(int level, int prestige, GemColor bonus, Map<GemColor, Integer> cost) {
        return new DevelopmentCard(0, level, prestige, bonus, toCost(cost));
    }

    private NobleTile noble(int id, Map<GemColor, Integer> requirement) {
        return new NobleTile(id, 3, toCost(requirement));
    }

    private Map<GemColor, Integer> mapCost(int white, int blue, int green, int red, int black) {
        Map<GemColor, Integer> cost = new EnumMap<>(GemColor.class);
        cost.put(GemColor.WHITE, white);
        cost.put(GemColor.BLUE, blue);
        cost.put(GemColor.GREEN, green);
        cost.put(GemColor.RED, red);
        cost.put(GemColor.BLACK, black);
        return cost;
    }

    private Map<GemColor, Integer> mapReq(int white, int blue, int green, int red, int black) {
        return mapCost(white, blue, green, red, black);
    }

    private Cost toCost(Map<GemColor, Integer> values) {
        Cost cost = new Cost();
        for (Map.Entry<GemColor, Integer> entry : values.entrySet()) {
            cost.set(entry.getKey(), entry.getValue());
        }
        return cost;
    }

    private boolean isComputerTurn() {
        return computerPlayers.contains(state.getCurrentPlayer());
    }

    private void showSinglePlayerGameOverDialog(Player winner) {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Game over.\nWinner: " + winner.getName() + " (" + winner.getPrestigePoints() + " points)\n\nPlay again?",
                "Winner",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION) {
            java.awt.EventQueue.invokeLater(() -> new SwingSplendorApp().setVisible(true));
        }
        dispose();
    }

    private void scheduleComputerTurnIfNeeded() {
        if (finalGameOver || !isComputerTurn() || computerThinking) {
            return;
        }
        computerThinking = true;
        updateLegalUi();

        Timer timer = new Timer(700, e -> {
            ((Timer) e.getSource()).stop();
            computerThinking = false;
            runComputerTurn();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void runComputerTurn() {
        if (finalGameOver || !isComputerTurn()) {
            return;
        }

        Player current = state.getCurrentPlayer();
        try {
            Move move = aiStrategy.chooseMove(state, current, validator);
            if (move == null) {
                log(current.getName() + " could not find a legal move.");
                return;
            }
            String error = validator.validate(state, current, move);
            if (error != null) {
                log("Computer produced illegal move: " + error);
                return;
            }

            executor.execute(state, current, move);
            resolveTokenCapIfNeeded(current);
            String moveSummary = current.getName() + " played: " + move.getType();
            latestComputerMoveLabel.setText(moveSummary);
            log(moveSummary);
            JOptionPane.showMessageDialog(
                    this,
                    moveSummary,
                    "Computer Move",
                    JOptionPane.INFORMATION_MESSAGE
            );
            resolveNobleAttraction(current, true);

            if (winnerChecker.shouldTriggerFinalRound(state)) {
                turnManager.markFinalRound(state);
                log("Final round triggered by " + current.getName());
            }

            turnManager.advanceTurn(state);
            if (turnManager.hasFinalRoundCompleted(state)) {
                Player winner = winnerChecker.determineWinner(state);
                finalGameOver = true;
                log("Game over. Winner: " + winner.getName() + " (" + winner.getPrestigePoints() + " points)");
                showSinglePlayerGameOverDialog(winner);
                return;
            }

            clearSelection();
            refreshAll();
        } catch (RuntimeException ex) {
            log("Computer move failed: " + ex.getMessage());
        }
    }
}
