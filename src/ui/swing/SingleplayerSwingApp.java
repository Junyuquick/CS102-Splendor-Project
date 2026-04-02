package ui.swing;

import ai.GreedyStrategy;
import engine.moves.Move;
import engine.turn.MoveExecutor;
import engine.validation.MoveValidator;
import engine.turn.NobleAssigner;
import engine.turn.TurnPostProcessor;
import engine.turn.TurnProgressionService;
import engine.turn.TurnManager;
import engine.turn.TurnAdvanceResult;
import engine.turn.TurnResolutionResult;
import engine.turn.TurnResolutionService;
import engine.turn.WinnerChecker;
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
public class SingleplayerSwingApp extends AbstractSwingSplendorFrame {
    private enum LocalMode {
        SINGLE_PLAYER_VS_AI,
        SAME_LAPTOP_MULTIPLAYER
    }

    private final MoveExecutor executor;
    private final NobleAssigner nobleAssigner;
    private final TurnPostProcessor turnPostProcessor;
    private final WinnerChecker winnerChecker;
    private final TurnManager turnManager;
    private final TurnProgressionService turnProgressionService;
    private final TurnResolutionService turnResolutionService;
    private final GreedyStrategy aiStrategy;
    private final Map<Player, JPanel> playerCardPanels = new LinkedHashMap<>();
    private final Map<Player, javax.swing.JEditorPane> playerCardAreas = new LinkedHashMap<>();
    private final Set<Player> computerPlayers = new HashSet<>();

    private boolean finalGameOver = false;
    private boolean computerThinking = false;
    private final LocalMode localMode;
    private final List<String> sameLaptopFixedNames;
    private final String singlePlayerFixedHumanName;
    private final Integer singlePlayerFixedComputerCount;

    /**
     * Creates a new single-player game window using the default configuration.
     */
    public SingleplayerSwingApp() {
        this(SwingConfigSupport.loadConfig(), LocalMode.SINGLE_PLAYER_VS_AI, -1, null, null, null);
    }

    /**
     * Creates a local same-laptop multiplayer game window with all-human players.
     *
     * @param playerCount number of human players sharing the same laptop
     */
    public SingleplayerSwingApp(int playerCount) {
        this(SwingConfigSupport.loadConfig(), LocalMode.SAME_LAPTOP_MULTIPLAYER, playerCount, null, null, null);
    }

    /**
     * Internal constructor used for fresh launches and "play again" restarts with preserved setup.
     *
     * @param config active game configuration
     * @param localMode selected local play mode
     * @param requestedPlayerCount requested player count for same-laptop mode
     * @param sameLaptopFixedNames optional preserved same-laptop player names
     * @param singlePlayerFixedHumanName optional preserved single-player human name
     * @param singlePlayerFixedComputerCount optional preserved single-player AI count
     */
    private SingleplayerSwingApp(
            config.Config config,
            LocalMode localMode,
            int requestedPlayerCount,
            List<String> sameLaptopFixedNames,
            String singlePlayerFixedHumanName,
            Integer singlePlayerFixedComputerCount
    ) {
        super(
                "Splendor (Swing)",
                config,
                null,
                new MoveValidator(config)
        );
        this.localMode = localMode;
        this.sameLaptopFixedNames = sameLaptopFixedNames == null ? null : new ArrayList<>(sameLaptopFixedNames);
        this.singlePlayerFixedHumanName = singlePlayerFixedHumanName;
        this.singlePlayerFixedComputerCount = singlePlayerFixedComputerCount;
        this.state = createInitialState(requestedPlayerCount);
        this.executor = new MoveExecutor(config);
        this.nobleAssigner = new NobleAssigner();
        this.turnPostProcessor = new TurnPostProcessor(config, nobleAssigner);
        this.winnerChecker = new WinnerChecker(config);
        this.turnManager = new TurnManager();
        this.turnProgressionService = new TurnProgressionService();
        this.turnResolutionService = new TurnResolutionService(
                validator,
                executor,
                turnPostProcessor,
                winnerChecker,
                turnManager,
                turnProgressionService
        );
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
    private GameState createInitialState(int requestedPlayerCount) {
        if (localMode == LocalMode.SAME_LAPTOP_MULTIPLAYER) {
            return createSameLaptopInitialState(requestedPlayerCount);
        }
        return createSinglePlayerInitialState();
    }

    /**
     * Builds the initial state for single-player mode using one human and N computer players.
     *
     * @return initialized game state for single-player mode
     */
    private GameState createSinglePlayerInitialState() {
        String playerName = singlePlayerFixedHumanName != null ? singlePlayerFixedHumanName : promptName("Enter Player 1 name:");
        int computerCount = singlePlayerFixedComputerCount != null ? singlePlayerFixedComputerCount : askComputerCount();
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
     * Builds the initial state for same-laptop multiplayer mode.
     *
     * @param requestedPlayerCount requested number of local human players
     * @return initialized game state for same-laptop mode
     */
    private GameState createSameLaptopInitialState(int requestedPlayerCount) {
        if (sameLaptopFixedNames != null && !sameLaptopFixedNames.isEmpty()) {
            return createSameLaptopInitialStateFromFixedNames(sameLaptopFixedNames);
        }

        int minPlayers = config.getMinPlayers();
        int maxPlayers = config.getMaxPlayers();
        int playerCount = Math.max(minPlayers, Math.min(maxPlayers, requestedPlayerCount));

        List<Player> players = new ArrayList<>();
        for (int i = 1; i <= playerCount; i++) {
            String name = promptName("Enter Player " + i + " name:");
            players.add(new Player(name.isBlank() ? "Player " + i : name));
        }

        return new GameStateFactory(config, GameStateFactory.FallbackProfile.LOCAL_APP)
                .createGame(players, this::log);
    }

    /**
     * Recreates same-laptop initial state from a fixed player-name list.
     *
     * @param fixedNames preserved player names in seat order
     * @return initialized game state with normalized names
     */
    // Checked with ChatGPT-5.3 to fix crash and added fallback to handle it
    private GameState createSameLaptopInitialStateFromFixedNames(List<String> fixedNames) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < fixedNames.size(); i++) {
            String raw = fixedNames.get(i);
            String fallback = "Player " + (i + 1);
            String name = raw == null || raw.isBlank() ? fallback : raw.trim();
            players.add(new Player(name));
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
        statusLabel.setText(localMode == LocalMode.SAME_LAPTOP_MULTIPLAYER
                ? "Mode: Same Laptop Multiplayer"
                : "You: " + state.getPlayer(0).getName());
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

        Player current = state.getCurrentPlayer();
        Move move = buildPendingMove();
        String pendingMessage = pendingMoveValidationMessage(move, null);
        if (pendingMessage != null) {
            log(pendingMessage);
            updateLegalUi();
            return;
        }
        TurnResolutionResult turnResult = turnResolutionService.resolveTurn(state, current, move);
        if (!turnResult.isSuccess()) {
            log("Illegal move: " + turnResult.getValidationError());
            updateLegalUi();
            return;
        }

        log(current.getName() + " played: " + move.getTypeName());
        logTurnOutcome(current, turnResult);
        if (turnResult.isFinalRoundTriggered()) {
            log("Final round triggered by " + current.getName());
        }
        if (turnResult.getWinner() != null) {
            Player winner = turnResult.getWinner();
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
    // Partially generated by ChatGPT-5.3.
    protected void updateLegalUi() {
        if (finalGameOver) {
            disableAllInputs();
            return;
        }

        Player current = state.getCurrentPlayer();
        statusLabel.setText(localMode == LocalMode.SAME_LAPTOP_MULTIPLAYER
                ? "Mode: Same Laptop Multiplayer"
                : "You: " + state.getPlayer(0).getName());
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
        actionReturnTokens.setEnabled(hasAnyLegalReturnTokens(current));
        actionPass.setEnabled(true);
        actionCancel.setEnabled(mode != SwingInteractionMode.IDLE);
        reservedList.setEnabled(mode == SwingInteractionMode.BUY);

        boolean cardMode = mode == SwingInteractionMode.RESERVE || mode == SwingInteractionMode.BUY;
        updateCardSelectionState(current, true, cardMode);
        applyTokenModeRules(current, true);

        Move pending = buildPendingMove();
        actionConfirm.setEnabled(mode != SwingInteractionMode.IDLE
                && pending != null
                && validator.validate(state, current, pending) == null);
    }

    /**
     * Indicates that token buttons should include live bank counts in their labels.
     *
     * @return always true for local Swing mode
     */
    @Override
    protected boolean showTokenBankCountsOnButtons() {
        return true;
    }

    /**
     * Logs discarded tokens and any nobles assigned after a successful turn.
     *
     * @param player player whose turn was resolved
     * @param turnResult post-resolution outcome details
     */
    private void logTurnOutcome(Player player, TurnResolutionResult turnResult) {
        if (!turnResult.getDiscardedTokens().isEmpty()) {
            log(player.getName() + " discarded " + turnResult.getDiscardedTokens());
        }
        for (NobleTile noble : turnResult.getAssignedNobles()) {
            log(player.getName() + " attracted noble: " + noble);
        }
    }

    /**
     * Prompts for a player name and falls back to a default when the dialog is blank or cancelled.
     *
     * @param prompt dialog prompt text
     * @return chosen player name
     */
    private String promptName(String prompt) {
        String name = JOptionPane.showInputDialog(this, prompt, "Player Setup", JOptionPane.QUESTION_MESSAGE);
        if (name == null) {
            System.exit(0);
            return "Player";
        }
        if (name.isBlank()) {
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
            System.exit(0);
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
     * @return true when the current player is computer-controlled
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
            java.awt.EventQueue.invokeLater(() -> {
                if (localMode == LocalMode.SAME_LAPTOP_MULTIPLAYER) {
                    List<String> names = state.getPlayers().stream().map(Player::getName).toList();
                    new SingleplayerSwingApp(
                            SwingConfigSupport.loadConfig(),
                            LocalMode.SAME_LAPTOP_MULTIPLAYER,
                            names.size(),
                            names,
                            null,
                            null
                    ).setVisible(true);
                } else {
                    String humanName = state.getPlayer(0).getName();
                    int computerCount = computerPlayers.size();
                    new SingleplayerSwingApp(
                            SwingConfigSupport.loadConfig(),
                            LocalMode.SINGLE_PLAYER_VS_AI,
                            -1,
                            null,
                            humanName,
                            computerCount
                    ).setVisible(true);
                }
            });
        }
        dispose();
    }

    /**
     * Schedules a short pause before running the next AI turn.
     */
    // Partially generated by ChatGPT-5.3. adviced by it to have timer
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
            Move move = chooseAndValidateComputerMove(current);
            if (move == null) {
                return;
            }
            TurnResolutionResult turnResult = turnResolutionService.resolveTurn(state, current, move);
            if (!turnResult.isSuccess()) {
                log("Computer produced illegal move: " + turnResult.getValidationError());
                return;
            }

            String moveSummary = current.getName() + " played: " + move.getTypeName();
            log(moveSummary);
            JOptionPane.showMessageDialog(
                    this,
                    moveSummary,
                    "Computer Move",
                    JOptionPane.INFORMATION_MESSAGE
            );
            logTurnOutcome(current, turnResult);

            if (turnResult.isFinalRoundTriggered()) {
                log("Final round triggered by " + current.getName());
            }
            if (turnResult.getWinner() != null) {
                Player winner = turnResult.getWinner();
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

    // Generated by ChatGPT-5.3.
    static String pendingMoveValidationMessage(Move move, String validationError) {
        if (move == null) {
            return "Incomplete move. Finish your selection before confirming.";
        }
        if (validationError != null) {
            return "Illegal move: " + validationError;
        }
        return null;
    }

    /**
     * Chooses an AI move and asserts it is legal against the current state snapshot.
     *
     * @param current computer-controlled player whose turn is active
     * @return legal move selected by the AI strategy
     * @throws IllegalStateException when the strategy returns an illegal move
     */
    // Generated by ChatGPT-5.3.
    private Move chooseAndValidateComputerMove(Player current) {
        Move move = aiStrategy.chooseMove(state, current, validator);
        String error = validator.validate(state, current, move);
        if (error != null) {
            throw new IllegalStateException(error);
        }
        return move;
    }
}
