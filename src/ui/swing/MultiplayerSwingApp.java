package ui.swing;

import engine.Move;
import engine.MoveValidator;
import model.GameState;
import model.Player;
import network.GameClient;
import network.NetworkMessage;

import javax.swing.JOptionPane;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Swing UI for multiplayer Splendor client.
 * Connects to a GameServer and synchronizes game state.
 */
public class MultiplayerSwingApp extends AbstractSwingSplendorFrame {
    private final GameClient client;
    private final Map<Player, JPanel> playerCardPanels = new LinkedHashMap<>();
    private final Map<Player, JEditorPane> playerCardAreas = new LinkedHashMap<>();
    private int myPlayerIndex = -1;
    private int hostPlayerIndex = 0;
    private int minPlayersToStart;
    private List<String> lobbyPlayers = new ArrayList<>();
    private boolean shuttingDown = false;

    /**
     * Creates a multiplayer game window using the default configuration.
     *
     * @param client connected multiplayer client
     */
    public MultiplayerSwingApp(GameClient client) {
        this(client, SwingConfigSupport.loadConfig());
    }

    private MultiplayerSwingApp(GameClient client, config.Config config) {
        super("Splendor Multiplayer", config, null, new MoveValidator(config));
        this.client = client;
        this.minPlayersToStart = config.getMinPlayers();

        buildSharedUi(true, true, 4);
        bindSharedActions();
        startGameButton.addActionListener(e -> onStartGame());

        new Thread(this::listenForMessages, "Splendor-Client-Listener").start();
    }

    /**
     * Starts the background receive loop that feeds server messages back onto the Swing thread.
     */
    private void listenForMessages() {
        while (client.isConnected()) {
            NetworkMessage message = client.receiveMessage();
            if (message == null) {
                break;
            }
            javax.swing.SwingUtilities.invokeLater(() -> handleMessage(message));
        }

        if (!shuttingDown) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Disconnected from server", "Connection Lost", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            });
        }
    }

    /**
     * Applies a received network message to the local Swing state.
     *
     * @param message message received from the server
     */
    private void handleMessage(NetworkMessage message) {
        switch (message.getType()) {
            case JOIN_ACK -> {
                myPlayerIndex = message.getPlayerIndex() == null ? -1 : message.getPlayerIndex();
                refreshStatus();
                updateLegalUi();
            }
            case LOBBY_UPDATE -> {
                lobbyPlayers = message.getPlayerNames() == null ? new ArrayList<>() : new ArrayList<>(message.getPlayerNames());
                hostPlayerIndex = message.getHostIndex() == null ? 0 : message.getHostIndex();
                minPlayersToStart = message.getMinPlayers() == null ? config.getMinPlayers() : message.getMinPlayers();
                log("Lobby updated: " + lobbyPlayers.size() + " player(s) connected.");
                refreshAll();
            }
            case GAME_START -> {
                int previousCurrentPlayerIndex = currentPlayerIndex();
                state = message.getGameState();
                myPlayerIndex = message.getPlayerIndex();
                log("Game started! You are player " + (myPlayerIndex + 1));
                refreshAll();
                maybeShowTurnPopup(previousCurrentPlayerIndex);
            }
            case STATE_UPDATE -> {
                int previousCurrentPlayerIndex = currentPlayerIndex();
                state = message.getGameState();
                refreshAll();
                maybeShowTurnPopup(previousCurrentPlayerIndex);
            }
            case GAME_OVER -> handleGameOver(message.getErrorMessage());
            case ERROR -> log("Error: " + message.getErrorMessage());
        }
    }

    /**
     * Handles the end-of-game prompt shown after the server reports a winner.
     *
     * @param message human-readable game-over message
     */
    private void handleGameOver(String message) {
        disableAllInputs();
        int choice = JOptionPane.showConfirmDialog(
                this,
                message + "\n\nPlay again with the same multiplayer lobby?",
                "Game Over",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            if (isHost()) {
                JOptionPane.showMessageDialog(this, "The lobby has reopened. Press Start Game when everyone is ready.", "Play Again", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "The lobby has reopened. Wait for the host to start the next game.", "Play Again", JOptionPane.INFORMATION_MESSAGE);
            }
            return;
        }

        shuttingDown = true;
        client.disconnect();
        dispose();
    }

    /**
     * Refreshes all multiplayer panels based on the latest lobby or game state.
     */
    private void refreshAll() {
        if (state != null) {
            rebuildMarketButtons();
            rebuildNobleCards();
            refreshBankCounts();
            Player current = state.getCurrentPlayer();
            refreshReservedCards(current);
        } else {
            clearPreGamePanels();
        }
        refreshPlayers();
        refreshStatus();
        updateLegalUi();
    }

    /**
     * Refreshes either the pre-game lobby panels or the in-game player summaries.
     */
    private void refreshPlayers() {
        if (state == null) {
            playerCardPanels.clear();
            playerCardAreas.clear();
            SwingPlayerPanelSupport.renderLobbyPlayers(
                    this,
                    playersPanel,
                    lobbyPlayers,
                    hostPlayerIndex,
                    myPlayerIndex,
                    minPlayersToStart
            );
            return;
        }

        ensureInGamePlayerPanels();
        SwingPlayerPanelSupport.refreshSinglePlayerPanels(
                config,
                state.getCurrentPlayer(),
                playerCardPanels,
                playerCardAreas
        );
    }

    /**
     * Refreshes the status labels for either lobby or active-game mode.
     */
    private void refreshStatus() {
        if (state == null) {
            int needed = Math.max(0, minPlayersToStart - lobbyPlayers.size());
            statusLabel.setText(isHost() ? "Lobby: you are the host" : "Lobby: waiting for host");
            phaseLabel.setText(needed == 0 ? "Ready to start" : "Need " + needed + " more player" + (needed == 1 ? "" : "s"));
            lobbyStatusLabel.setText("Players connected: " + lobbyPlayers.size() + " / " + minPlayersToStart + "+");
            reservedLabel.setText("Your Reserved Cards");
            return;
        }

        Player current = state.getCurrentPlayer();
        statusLabel.setText("You: " + client.getPlayerName());
        phaseLabel.setText("Turn: " + current.getName() + (state.isFinalRound() ? " | Final Round" : ""));
        lobbyStatusLabel.setText("Turn: " + current.getName());
        reservedLabel.setText(current.getName() + " Reserved Cards");
    }

    /**
     * Indicates whether the local client may switch into the requested interaction mode.
     *
     * @param newMode requested interaction mode
     * @return {@code true} when it is the local player's turn
     */
    @Override
    protected boolean canSwitchMode(SwingGameMode newMode) {
        return state != null && myPlayerIndex == state.getCurrentPlayerIndex();
    }

    /**
     * Logs a short explanation when interaction is blocked.
     *
     * @param newMode rejected interaction mode
     */
    @Override
    protected void onModeSwitchRejected(SwingGameMode newMode) {
        if (state == null) {
            log("Game has not started yet");
        } else {
            log("Not your turn");
        }
    }

    /**
     * Validates the current selection locally and sends the resulting move to the server.
     */
    @Override
    protected void handleConfirmAction() {
        if (state == null || myPlayerIndex != state.getCurrentPlayerIndex()) {
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

        client.sendMove(move);
        log("Sent move: " + move.getType());
        clearSelection();
    }

    /**
     * Refreshes action availability for either the lobby state or the current multiplayer turn.
     */
    @Override
    protected void updateLegalUi() {
        boolean gameStarted = state != null;
        boolean myTurn = gameStarted && myPlayerIndex == state.getCurrentPlayerIndex();
        boolean hostCanStart = !gameStarted && isHost() && lobbyPlayers.size() >= minPlayersToStart;

        startGameButton.setEnabled(hostCanStart);
        startGameButton.setVisible(!gameStarted);

        if (!gameStarted) {
            disableAllInputs();
            helpArea.setText(isHost()
                    ? "Wait for at least " + minPlayersToStart + " players, then press Start Game."
                    : "Waiting for the host to start the game.");
            return;
        }

        Player current = state.getCurrentPlayer();
        statusLabel.setText("You: " + client.getPlayerName());
        phaseLabel.setText("Turn: " + current.getName()
                + " | Phase: " + mode.name().replace('_', ' ')
                + (state.isFinalRound() ? " | Final Round" : ""));
        helpArea.setText(myTurn ? activeTurnHelpText() : "Wait for the other player to finish their turn.");

        actionTakeThree.setEnabled(myTurn && hasAnyLegalTakeThree(current));
        actionTakeTwo.setEnabled(myTurn && hasAnyLegalTakeTwo(current));
        actionReserve.setEnabled(myTurn && hasAnyLegalReserve(current));
        actionBuy.setEnabled(myTurn && hasAnyLegalBuy(current));
        actionCancel.setEnabled(myTurn && mode != SwingGameMode.IDLE);
        reservedList.setEnabled(myTurn && mode == SwingGameMode.BUY);

        boolean cardMode = mode == SwingGameMode.RESERVE || mode == SwingGameMode.BUY;
        updateCardSelectionState(current, myTurn, cardMode);
        applyTokenModeRules(current, myTurn);

        Move pending = myTurn ? buildPendingMove() : null;
        actionConfirm.setEnabled(myTurn
                && mode != SwingGameMode.IDLE
                && pending != null
                && validator.validate(state, current, pending) == null);
    }

    @Override
    protected boolean showTokenBankCountsOnButtons() {
        return true;
    }

    private void ensureInGamePlayerPanels() {
        if (playerCardPanels.size() == state.getPlayers().size()
                && playerCardAreas.size() == state.getPlayers().size()
                && playerCardPanels.keySet().containsAll(state.getPlayers())
                && playerCardAreas.keySet().containsAll(state.getPlayers())) {
            return;
        }

        SwingPlayerPanelSupport.initialiseSinglePlayerPanels(
                this,
                playersPanel,
                state,
                playerCardPanels,
                playerCardAreas
        );
    }

    /**
     * Sends a start-game request when the local client is the host and the lobby is ready.
     */
    private void onStartGame() {
        if (state != null) {
            return;
        }
        if (!isHost()) {
            log("Only the server creator can start the game.");
            return;
        }
        if (lobbyPlayers.size() < minPlayersToStart) {
            log("Need at least " + minPlayersToStart + " players to start.");
            return;
        }
        client.sendStartRequest();
        log("Start request sent.");
    }

    /**
     * Indicates whether the local client is currently the lobby host.
     *
     * @return {@code true} when the local player is the host
     */
    private boolean isHost() {
        return myPlayerIndex >= 0 && myPlayerIndex == hostPlayerIndex;
    }

    /**
     * Returns the current player index from the latest state snapshot.
     *
     * @return current player index, or {@code -1} if no game is active
     */
    private int currentPlayerIndex() {
        return state == null ? -1 : state.getCurrentPlayerIndex();
    }

    /**
     * Shows a popup when control passes to the local player.
     *
     * @param previousCurrentPlayerIndex player whose turn it was before the latest update
     */
    private void maybeShowTurnPopup(int previousCurrentPlayerIndex) {
        if (state == null || myPlayerIndex < 0) {
            return;
        }

        int currentPlayerIndex = state.getCurrentPlayerIndex();
        if (currentPlayerIndex == myPlayerIndex && previousCurrentPlayerIndex != myPlayerIndex) {
            String currentPlayerName = state.getCurrentPlayer().getName();
            JOptionPane.showMessageDialog(
                    this,
                    "It's your turn, " + currentPlayerName + ".",
                    "Your Turn",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }
}
