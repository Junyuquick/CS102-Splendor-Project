package ui.swing;

import ai.GreedyStrategy;
import engine.Move;
import engine.MoveExecutor;
import engine.MoveValidator;
import engine.NobleAssigner;
import engine.TurnPostProcessor;
import engine.TurnManager;
import engine.WinnerChecker;
import model.DevelopmentCard;
import model.GameState;
import model.GemColor;
import model.NobleTile;
import model.Player;
import setup.GameStateFactory;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Single-player Swing implementation that runs the full game loop locally.
 */
public class SwingSplendorApp extends AbstractSwingSplendorFrame {
    private final MoveExecutor executor;
    private final NobleAssigner nobleAssigner;
    private final TurnPostProcessor turnPostProcessor;
    private final WinnerChecker winnerChecker;
    private final TurnManager turnManager;
    private final GreedyStrategy aiStrategy;
    private final Map<Player, JPanel> playerCardPanels = new LinkedHashMap<>();
    private final Map<Player, javax.swing.JEditorPane> playerCardAreas = new LinkedHashMap<>();
    private final Set<Player> computerPlayers = new HashSet<>();

    private boolean finalGameOver = false;
    private boolean computerThinking = false;

    /**
     * Creates a new single-player game window using the default configuration.
     */
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
        this.turnPostProcessor = new TurnPostProcessor(config, nobleAssigner);
        this.winnerChecker = new WinnerChecker(config);
        this.turnManager = new TurnManager();
        this.aiStrategy = new GreedyStrategy(config);

        buildSharedUi(false, false, state.getPlayers().size());
        initialisePlayerPanels();
        bindSharedActions();
        refreshAll();
    }

    /**
     * Prompts for players and builds the initial local game state.
     *
     * @return initialized game state
     */
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

        return new GameStateFactory(config, GameStateFactory.FallbackProfile.LOCAL_APP)
                .createGame(players, this::log);
    }

    /**
     * Creates the player summary panels for the local game.
     */
    private void initialisePlayerPanels() {
        SwingPlayerPanelSupport.initialiseSinglePlayerPanels(
                this,
                playersPanel,
                state,
                playerCardPanels,
                playerCardAreas
        );
    }

    /**
     * Refreshes all board, player, and action displays from the current local state.
     */
    private void refreshAll() {
        rebuildMarketButtons();
        rebuildNobleCards();
        refreshBankCounts();

        Player current = state.getCurrentPlayer();
        statusLabel.setText("You: " + state.getPlayer(0).getName());
        reservedLabel.setText(current.getName() + " Reserved Cards");

        SwingPlayerPanelSupport.refreshSinglePlayerPanels(config, current, playerCardPanels, playerCardAreas);

        refreshReservedCards(current);
        updateLegalUi();
        scheduleComputerTurnIfNeeded();
    }

    /**
     * Applies the pending move, resolves post-turn effects, and advances the local game.
     */
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

    /**
     * Refreshes action availability and prompts for the local player's current state.
     */
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

    /**
     * Enforces the token cap after a move and logs any forced discards.
     *
     * @param player player whose tokens may need to be reduced
     */
    private void resolveTokenCapIfNeeded(Player player) {
        Map<GemColor, Integer> discard = turnPostProcessor.enforceTokenLimit(state, player);
        if (!discard.isEmpty()) {
            log(player.getName() + " discarded " + discard);
        }
    }

    /**
     * Assigns any nobles the player qualifies for, prompting only when multiple choices exist.
     *
     * @param player player who may attract nobles
     * @param automaticChoice whether to auto-pick the best noble instead of prompting
     */
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

    /**
     * Chooses a noble from the current eligible set.
     *
     * @param eligible nobles the player can currently claim
     * @param automaticChoice whether to choose automatically
     * @return selected noble
     */
    private NobleTile chooseNoble(List<NobleTile> eligible, boolean automaticChoice) {
        if (eligible.size() == 1) {
            return eligible.get(0);
        }
        if (automaticChoice) {
            return turnPostProcessor.chooseBestNoble(eligible);
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

    /**
     * Prompts for a player name and falls back to a default when the dialog is blank or cancelled.
     *
     * @param prompt dialog prompt text
     * @return chosen player name
     */
    private String promptName(String prompt) {
        String name = JOptionPane.showInputDialog(this, prompt, "Player Setup", JOptionPane.QUESTION_MESSAGE);
        if (name == null || name.isBlank()) {
            return "Player";
        }
        return name.trim();
    }

    /**
     * Prompts for the number of computer opponents within the configured bounds.
     *
     * @return number of AI opponents to create
     */
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

    /**
     * Indicates whether the current turn belongs to an AI-controlled player.
     *
     * @return {@code true} when the current player is computer-controlled
     */
    private boolean isComputerTurn() {
        return computerPlayers.contains(state.getCurrentPlayer());
    }

    /**
     * Shows the local game-over dialog and optionally starts a new single-player game.
     *
     * @param winner winning player
     */
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

    /**
     * Schedules a short pause before running the next AI turn.
     */
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

    /**
     * Executes one AI turn and then refreshes the local game state.
     */
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
