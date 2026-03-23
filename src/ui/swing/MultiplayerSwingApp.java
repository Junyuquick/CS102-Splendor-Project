package ui.swing;

import config.Config;
import config.ConfigLoader;
import engine.Move;
import engine.MoveValidator;
import model.Board;
import model.DevelopmentCard;
import model.GameState;
import model.GemBank;
import model.GemColor;
import model.NobleTile;
import model.Player;
import network.GameClient;
import network.NetworkMessage;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Swing UI for multiplayer Splendor client.
 * Connects to a GameServer and synchronizes game state.
 */
public class MultiplayerSwingApp extends JFrame {
    private static final int DEV_CARD_ICON_WIDTH = 96;
    private static final int DEV_CARD_ICON_HEIGHT = 134;
    private static final int DEV_CARD_BUTTON_WIDTH = 108;
    private static final int DEV_CARD_BUTTON_HEIGHT = 146;
    private static final int NOBLE_ICON_SIZE = 110;
    private static final int TOKEN_BUTTON_SIZE = 60;
    private static final int TOKEN_ICON_SIZE = 48;
    private static final int NOBLE_CARD_WIDTH = 122;
    private static final int NOBLE_CARD_HEIGHT = 122;
    private static final Color APP_BG = new Color(24, 27, 31);
    private static final Color PANEL_BG = new Color(31, 36, 42);
    private static final Color PANEL_BG_ALT = new Color(39, 45, 52);
    private static final Color SURFACE_BG = new Color(46, 53, 61);
    private static final Color EMPTY_BG = new Color(58, 63, 70);
    private static final Color TEXT_PRIMARY = new Color(232, 236, 241);
    private static final Color TEXT_MUTED = new Color(182, 190, 200);
    private static final Color BORDER_COLOR = new Color(92, 102, 114);
    private static final Color ACCENT_BLUE = new Color(78, 144, 255);
    private static final Color ACCENT_GREEN = new Color(60, 179, 113);
    private static final Color ACCENT_RED = new Color(212, 88, 88);
    private static final Color SELECTED_BG = new Color(52, 71, 96);

    private enum Mode {
        IDLE, TAKE_THREE, TAKE_TWO, RESERVE, BUY
    }

    private final Config config = buildConfig();
    private GameState state;
    private final GameClient client;
    private final MoveValidator validator;

    private final JLabel statusLabel = new JLabel("", SwingConstants.LEFT);
    private final JLabel phaseLabel = new JLabel("", SwingConstants.RIGHT);
    private final JLabel lobbyStatusLabel = new JLabel("", SwingConstants.CENTER);
    private final JPanel marketPanel = new JPanel(new GridLayout(3, 4, 10, 10));
    private final JPanel noblesPanel = new JPanel(new GridLayout(1, 5, 8, 8));
    private final JTextArea logArea = new JTextArea();
    private final JPanel playersPanel = new JPanel();
    private final JTextArea helpArea = new JTextArea();
    private final JLabel latestComputerMoveLabel = new JLabel("Latest Computer Move: -");
    private final JPanel bankCountPanel = new JPanel(new GridLayout(6, 1, 4, 4));
    private final Map<GemColor, JButton> tokenButtons = new EnumMap<>(GemColor.class);
    private final Map<GemColor, Integer> selectedTokenCounts = new EnumMap<>(GemColor.class);
    private final Map<String, JButton> cardButtons = new LinkedHashMap<>();
    private final JButton actionTakeThree = new JButton("Take 3 Different");
    private final JButton actionTakeTwo = new JButton("Take 2 Same");
    private final JButton actionReserve = new JButton("Reserve");
    private final JButton actionBuy = new JButton("Buy");
    private final JButton actionCancel = new JButton("Cancel");
    private final JButton actionConfirm = new JButton("Confirm");
    private final JButton startGameButton = new JButton("Start Game");
    private final DefaultListModel<DevelopmentCard> reservedModel = new DefaultListModel<>();
    private final JList<DevelopmentCard> reservedList = new JList<>(reservedModel);
    private final JLabel reservedLabel = new JLabel("Your Reserved Cards");

    private Mode mode = Mode.IDLE;
    private DevelopmentCard selectedBoardCard;
    private DevelopmentCard selectedReservedCard;
    private int myPlayerIndex = -1;
    private int hostPlayerIndex = 0;
    private int minPlayersToStart = 2;
    private List<String> lobbyPlayers = new ArrayList<>();

    public MultiplayerSwingApp(GameClient client) {
        super("Splendor Multiplayer");
        this.client = client;
        this.validator = new MoveValidator(config);

        buildUi();
        bindActions();

        // Start message listener thread
        new Thread(this::listenForMessages).start();
    }

    private static Config buildConfig() {
        try {
            return new ConfigLoader().load(Path.of("config.properties"));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Failed to load config.properties. Falling back to defaults. Reason: " + e.getMessage());
        }

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
                3,  // noblesCount2p
                4,  // noblesCount3p
                5,  // noblesCount4p
                4,  // bankNormal2p
                5,  // bankNormal3p
                7,  // bankNormal4p
                5,  // bankGold
                3,  // takeDifferentCount
                2,  // takeSameCount
                2,  // takeSameMinRemainingInBank
                1,  // reserveGoldBonus
                here, here, here, here,
                here, here, here
        );
    }

    private void listenForMessages() {
        while (client.isConnected()) {
            NetworkMessage msg = client.receiveMessage();
            if (msg == null) break;

            SwingUtilities.invokeLater(() -> handleMessage(msg));
        }
        // Disconnected
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "Disconnected from server", "Connection Lost", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        });
    }

    private void handleMessage(NetworkMessage msg) {
        switch (msg.getType()) {
            case JOIN_ACK:
                this.myPlayerIndex = msg.getPlayerIndex() == null ? -1 : msg.getPlayerIndex();
                refreshStatus();
                updateLegalUi();
                break;
            case LOBBY_UPDATE:
                this.lobbyPlayers = msg.getPlayerNames() == null ? new ArrayList<>() : new ArrayList<>(msg.getPlayerNames());
                this.hostPlayerIndex = msg.getHostIndex() == null ? 0 : msg.getHostIndex();
                this.minPlayersToStart = msg.getMinPlayers() == null ? 2 : msg.getMinPlayers();
                log("Lobby updated: " + lobbyPlayers.size() + " player(s) connected.");
                refreshAll();
                break;
            case GAME_START:
                this.state = msg.getGameState();
                this.myPlayerIndex = msg.getPlayerIndex();
                log("Game started! You are player " + (myPlayerIndex + 1));
                refreshAll();
                break;
            case STATE_UPDATE:
                this.state = msg.getGameState();
                refreshAll();
                break;
            case ERROR:
                log("Error: " + msg.getErrorMessage());
                break;
        }
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1500, 1020);
        setMinimumSize(new Dimension(1450, 980));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(APP_BG);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(APP_BG);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 16f));
        statusLabel.setForeground(TEXT_PRIMARY);
        lobbyStatusLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        lobbyStatusLabel.setForeground(TEXT_MUTED);
        phaseLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        phaseLabel.setFont(phaseLabel.getFont().deriveFont(Font.BOLD, 14f));
        phaseLabel.setForeground(TEXT_MUTED);
        styleActionButton(startGameButton);
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        topRight.setBackground(APP_BG);
        topRight.add(phaseLabel);
        topRight.add(startGameButton);
        topBar.add(statusLabel, BorderLayout.WEST);
        topBar.add(lobbyStatusLabel, BorderLayout.CENTER);
        topBar.add(topRight, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        JPanel leftBank = new JPanel(new BorderLayout(8, 8));
        leftBank.setBackground(PANEL_BG);
        leftBank.setBorder(createTitledBorder("Bank"));
        bankCountPanel.setBackground(PANEL_BG);
        leftBank.add(bankCountPanel, BorderLayout.NORTH);
        JPanel tokenPanel = new JPanel(new GridLayout(3, 2, 6, 6));
        tokenPanel.setBackground(PANEL_BG);
        for (GemColor color : GemColor.values()) {
            JButton button = new JButton(color.name());
            button.setFocusPainted(false);
            button.setBackground(tokenColor(color));
            button.setContentAreaFilled(true);
            button.setOpaque(true);
            button.setForeground(color == GemColor.BLACK ? TEXT_PRIMARY : new Color(20, 20, 20));
            button.setBorder(new LineBorder(BORDER_COLOR, 1, true));
            button.setPreferredSize(new Dimension(TOKEN_BUTTON_SIZE, TOKEN_BUTTON_SIZE));
            button.setMargin(new Insets(0, 0, 0, 0));
            button.setToolTipText("Click to select " + color.name() + " token(s)");
            ImageIcon icon = loadTokenIcon(color);
            if (icon != null) {
                button.setIcon(icon);
                button.setText("");
            }
            tokenButtons.put(color, button);
            selectedTokenCounts.put(color, 0);
            tokenPanel.add(button);
        }
        leftBank.add(tokenPanel, BorderLayout.CENTER);
        leftBank.setPreferredSize(new Dimension(260, 400));
        add(leftBank, BorderLayout.WEST);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBackground(PANEL_BG);
        center.setBorder(createTitledBorder("Market"));
        marketPanel.setBackground(PANEL_BG);
        marketPanel.setPreferredSize(new Dimension((DEV_CARD_BUTTON_WIDTH * 4) + (10 * 3), (DEV_CARD_BUTTON_HEIGHT * 3) + (10 * 2)));
        JPanel marketWrapper = new JPanel(new GridBagLayout());
        marketWrapper.setBackground(PANEL_BG);
        marketWrapper.add(marketPanel);
        center.add(marketWrapper, BorderLayout.CENTER);

        JPanel nobleWrap = new JPanel(new BorderLayout());
        nobleWrap.setBackground(PANEL_BG);
        nobleWrap.setBorder(createTitledBorder("Nobles"));
        noblesPanel.setBackground(PANEL_BG);
        nobleWrap.add(noblesPanel, BorderLayout.CENTER);
        center.add(nobleWrap, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.setBackground(PANEL_BG);
        right.setBorder(createTitledBorder("Players"));
        playersPanel.setLayout(new GridLayout(4, 1, 8, 8));
        playersPanel.setBackground(PANEL_BG);
        JScrollPane playersScroll = new JScrollPane(playersPanel);
        playersScroll.setPreferredSize(new Dimension(420, 400));
        right.add(playersScroll, BorderLayout.CENTER);

        reservedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reservedList.setBackground(PANEL_BG_ALT);
        reservedList.setForeground(TEXT_PRIMARY);
        reservedList.setSelectionBackground(SELECTED_BG);
        reservedList.setSelectionForeground(TEXT_PRIMARY);

        JPanel reservedPanel = new JPanel(new BorderLayout(4, 4));
        reservedPanel.setBackground(PANEL_BG);
        reservedLabel.setForeground(TEXT_PRIMARY);
        reservedPanel.add(reservedLabel, BorderLayout.NORTH);
        JScrollPane reservedScroll = new JScrollPane(reservedList);
        styleScrollPane(reservedScroll, PANEL_BG_ALT);
        reservedScroll.setPreferredSize(new Dimension(320, 210));
        reservedPanel.add(reservedScroll, BorderLayout.CENTER);
        right.add(reservedPanel, BorderLayout.SOUTH);
        right.setPreferredSize(new Dimension(360, 400));
        add(right, BorderLayout.EAST);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setBackground(PANEL_BG);
        JPanel actions = new JPanel(new GridLayout(1, 6, 6, 6));
        actions.setBackground(PANEL_BG);
        actions.add(actionTakeThree);
        actions.add(actionTakeTwo);
        actions.add(actionReserve);
        actions.add(actionBuy);
        actions.add(actionCancel);
        actions.add(actionConfirm);
        bottom.add(actions, BorderLayout.NORTH);
        styleActionButton(actionTakeThree);
        styleActionButton(actionTakeTwo);
        styleActionButton(actionReserve);
        styleActionButton(actionBuy);
        styleActionButton(actionCancel);
        styleActionButton(actionConfirm);

        helpArea.setEditable(false);
        helpArea.setRows(3);
        helpArea.setLineWrap(true);
        helpArea.setWrapStyleWord(true);
        helpArea.setBackground(PANEL_BG_ALT);
        helpArea.setForeground(TEXT_PRIMARY);
        helpArea.setCaretColor(TEXT_PRIMARY);
        helpArea.setBorder(createTitledBorder("Prompt"));
        bottom.add(helpArea, BorderLayout.CENTER);

        latestComputerMoveLabel.setOpaque(true);
        latestComputerMoveLabel.setBackground(PANEL_BG_ALT);
        latestComputerMoveLabel.setForeground(TEXT_PRIMARY);
        latestComputerMoveLabel.setBorder(createTitledBorder("Computer Move"));
        bottom.add(latestComputerMoveLabel, BorderLayout.WEST);

        logArea.setEditable(false);
        logArea.setRows(7);
        logArea.setBackground(PANEL_BG_ALT);
        logArea.setForeground(TEXT_PRIMARY);
        logArea.setCaretColor(TEXT_PRIMARY);
        JScrollPane logScroll = new JScrollPane(logArea);
        styleScrollPane(logScroll, PANEL_BG_ALT);
        bottom.add(logScroll, BorderLayout.SOUTH);
        bottom.setBorder(createTitledBorder("Actions & Log"));

        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, center, bottom);
        centerSplit.setBackground(APP_BG);
        centerSplit.setBorder(BorderFactory.createEmptyBorder());
        centerSplit.setResizeWeight(0.8);
        centerSplit.setOneTouchExpandable(true);
        centerSplit.setContinuousLayout(true);
        centerSplit.setDividerLocation(650);
        add(centerSplit, BorderLayout.CENTER);
    }

    private void styleActionButton(JButton button) {
        button.setFocusPainted(false);
        button.setBackground(SURFACE_BG);
        button.setForeground(TEXT_PRIMARY);
        button.setBorder(new LineBorder(BORDER_COLOR, 1, true));
    }

    private void styleScrollPane(JScrollPane scrollPane, Color bg) {
        scrollPane.getViewport().setBackground(bg);
        scrollPane.setBackground(bg);
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1, true));
    }

    private void onTokenButtonClicked(GemColor color) {
        toggleToken(color);
        updateLegalUi();
    }

    private void bindActions() {
        actionTakeThree.addActionListener(e -> switchMode(Mode.TAKE_THREE));
        actionTakeTwo.addActionListener(e -> switchMode(Mode.TAKE_TWO));
        actionReserve.addActionListener(e -> switchMode(Mode.RESERVE));
        actionBuy.addActionListener(e -> switchMode(Mode.BUY));
        actionCancel.addActionListener(e -> switchMode(Mode.IDLE));
        actionConfirm.addActionListener(e -> onConfirm());
        startGameButton.addActionListener(e -> onStartGame());

        for (Map.Entry<GemColor, JButton> entry : tokenButtons.entrySet()) {
            GemColor color = entry.getKey();
            JButton button = entry.getValue();
            button.addActionListener(e -> {
                onTokenButtonClicked(color);
                updateLegalUi();
            });
        }

        reservedList.addListSelectionListener(e -> {
            selectedReservedCard = reservedList.getSelectedValue();
            if (selectedReservedCard != null) {
                selectedBoardCard = null;
            }
            updateLegalUi();
        });
    }

    private void switchMode(Mode newMode) {
        if (state == null) {
            log("Game has not started yet");
            return;
        }
        if (myPlayerIndex != state.getCurrentPlayerIndex()) {
            log("Not your turn");
            return;
        }
        mode = newMode;
        selectedBoardCard = null;
        selectedReservedCard = null;
        reservedList.clearSelection();
        clearTokenSelection();
        updateLegalUi();
        log("Mode: " + newMode);
    }

    private void toggleToken(GemColor color) {
        if (mode != Mode.TAKE_THREE && mode != Mode.TAKE_TWO) return;

        int current = selectedTokenCounts.get(color);
        if (current > 0) {
            selectedTokenCounts.put(color, 0);
        } else {
            if (mode == Mode.TAKE_TWO) {
                // Only one color for TAKE_TWO
                for (GemColor c : selectedTokenCounts.keySet()) {
                    selectedTokenCounts.put(c, 0);
                }
                selectedTokenCounts.put(color, 2);
            } else {
                // TAKE_THREE: up to 3 different
                int selectedCount = (int) selectedTokenCounts.values().stream().filter(v -> v > 0).count();
                if (selectedCount < 3) {
                    selectedTokenCounts.put(color, 1);
                }
            }
        }
        updateTokenButtons();
    }

    private void updateTokenButtons() {
        for (GemColor color : GemColor.values()) {
            if (color == GemColor.GOLD) continue;
            JButton btn = tokenButtons.get(color);
            int count = selectedTokenCounts.get(color);
            if (count > 0) {
                btn.setBackground(SELECTED_BG);
            } else {
                btn.setBackground(tokenColor(color));
            }
        }
    }

    private void clearSelection() {
        mode = Mode.IDLE;
        selectedBoardCard = null;
        selectedReservedCard = null;
        reservedList.clearSelection();
        clearTokenSelection();
        updateLegalUi();
    }

    private void clearTokenSelection() {
        for (GemColor color : GemColor.values()) {
            selectedTokenCounts.put(color, 0);
        }
        refreshTokenButtonLabels();
    }

    private void updateLegalUi() {
        boolean gameStarted = state != null;
        boolean myTurn = gameStarted && myPlayerIndex == state.getCurrentPlayerIndex();
        boolean hostCanStart = !gameStarted && isHost() && lobbyPlayers.size() >= minPlayersToStart;

        startGameButton.setEnabled(hostCanStart);
        startGameButton.setVisible(!gameStarted);

        if (!gameStarted) {
            actionTakeThree.setEnabled(false);
            actionTakeTwo.setEnabled(false);
            actionReserve.setEnabled(false);
            actionBuy.setEnabled(false);
            actionCancel.setEnabled(false);
            actionConfirm.setEnabled(false);
            for (JButton button : tokenButtons.values()) {
                button.setEnabled(false);
            }
            for (JButton btn : cardButtons.values()) {
                btn.setEnabled(false);
                btn.setBorder(new LineBorder(BORDER_COLOR, 2));
            }
            reservedList.setEnabled(false);
            helpArea.setText(isHost()
                    ? "Wait for at least 2 players, then press Start Game."
                    : "Waiting for the host to start the game.");
            return;
        }

        Player current = state.getCurrentPlayer();
        boolean myActiveTurn = myTurn;

        statusLabel.setText("You: " + client.getPlayerName() +
                (myTurn ? " (Your Turn)" : ""));
        phaseLabel.setText("Phase: " + mode.name().replace('_', ' '));
        helpArea.setText(helpTextForMode());

        actionTakeThree.setEnabled(myActiveTurn && hasAnyLegalTakeThree(current));
        actionTakeTwo.setEnabled(myActiveTurn && hasAnyLegalTakeTwo(current));
        actionReserve.setEnabled(myActiveTurn && hasAnyLegalReserve(current));
        actionBuy.setEnabled(myActiveTurn && hasAnyLegalBuy(current));
        actionCancel.setEnabled(myActiveTurn && mode != Mode.IDLE);

        boolean takeMode = mode == Mode.TAKE_THREE || mode == Mode.TAKE_TWO;
        boolean cardMode = mode == Mode.RESERVE || mode == Mode.BUY;

        for (Map.Entry<GemColor, JButton> entry : tokenButtons.entrySet()) {
            GemColor color = entry.getKey();
            JButton button = entry.getValue();
            button.setEnabled(myActiveTurn && takeMode && color != GemColor.GOLD);
        }

        reservedList.setEnabled(myActiveTurn && mode == Mode.BUY);

        for (JButton btn : cardButtons.values()) {
            btn.setEnabled(false);
            btn.setBorder(new LineBorder(BORDER_COLOR, 2));
        }

        if (myActiveTurn && cardMode) {
            for (int tier = 1; tier <= 3; tier++) {
                List<DevelopmentCard> cards = state.getBoard().getFaceUpCards(tier);
                for (int col = 0; col < cards.size(); col++) {
                    DevelopmentCard card = cards.get(col);
                    Move move = mode == Mode.RESERVE
                            ? Move.reserveFaceUp(card)
                            : Move.buy(card, computePaymentTokens(current, card.getCost()), false);
                    String key = "" + (char) ('a' + col) + tier;
                    JButton btn = cardButtons.get(key);
                    if (btn != null) {
                        boolean legal = validator.validate(state, current, move) == null;
                        btn.setEnabled(legal);
                        if (selectedBoardCard == card) {
                            btn.setBorder(new LineBorder(ACCENT_BLUE, 4));
                        } else if (mode == Mode.BUY && legal) {
                            btn.setBorder(new LineBorder(ACCENT_GREEN, 3));
                        } else if (mode == Mode.BUY) {
                            btn.setBorder(new LineBorder(ACCENT_RED, 2));
                        } else {
                            btn.setBorder(new LineBorder(BORDER_COLOR, 2));
                        }
                    }
                }
            }
        }

        applyTokenModeRules(current, myActiveTurn);
        Move pending = myActiveTurn ? buildPendingMove() : null;
        actionConfirm.setEnabled(myActiveTurn && mode != Mode.IDLE && pending != null
                && validator.validate(state, current, pending) == null);
    }

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

    private void onConfirm() {
        if (state == null || myPlayerIndex != state.getCurrentPlayerIndex()) return;

        Move move = buildPendingMove();
        if (move == null) {
            log("No valid selection to confirm.");
            return;
        }

        Player current = state.getCurrentPlayer();
        String err = validator.validate(state, current, move);
        if (err != null) {
            log("Illegal move: " + err);
            updateLegalUi();
            return;
        }

        // Send move to server
        client.sendMove(move);
        log("Sent move: " + move.getType());
        clearSelection();
    }

    private Move buildPendingMove() {
        Player current = state.getCurrentPlayer();
        if (mode == Mode.TAKE_THREE) {
            Map<GemColor, Integer> tokens = selectedTokens();
            if (tokens.isEmpty()) return null;
            return Move.takeDifferent(tokens);
        }
        if (mode == Mode.TAKE_TWO) {
            Map<GemColor, Integer> selected = selectedTokens();
            if (selected.size() != 1) return null;
            GemColor color = selected.keySet().iterator().next();
            Map<GemColor, Integer> tokens = new EnumMap<>(GemColor.class);
            tokens.put(color, 2);
            return Move.takeSame(tokens);
        }
        if (mode == Mode.RESERVE) {
            if (selectedBoardCard == null) return null;
            return Move.reserveFaceUp(selectedBoardCard);
        }
        if (mode == Mode.BUY) {
            if (selectedBoardCard != null) {
                return Move.buy(selectedBoardCard, computePaymentTokens(current, selectedBoardCard.getCost()), false);
            }
            if (selectedReservedCard != null) {
                return Move.buy(selectedReservedCard, computePaymentTokens(current, selectedReservedCard.getCost()), true);
            }
        }
        return null;
    }

    private void refreshAll() {
        if (state != null) {
            refreshMarket();
            refreshNobles();
            refreshBank();
            refreshReserved();
        } else {
            clearPreGamePanels();
        }
        refreshPlayers();
        refreshStatus();
        updateLegalUi();
    }

    private void clearPreGamePanels() {
        marketPanel.removeAll();
        marketPanel.revalidate();
        marketPanel.repaint();

        noblesPanel.removeAll();
        noblesPanel.revalidate();
        noblesPanel.repaint();

        bankCountPanel.removeAll();
        bankCountPanel.revalidate();
        bankCountPanel.repaint();

        reservedModel.clear();
    }

    private void refreshMarket() {
        if (state == null) return;

        marketPanel.removeAll();
        cardButtons.clear();

        for (int tier = 3; tier >= 1; tier--) {
            List<DevelopmentCard> cards = state.getBoard().getFaceUpCards(tier);
            for (int col = 0; col < 4; col++) {
                String key = "" + (char) ('a' + col) + tier;
                JButton btn = new JButton();
                btn.setFocusPainted(false);
                btn.setOpaque(true);
                btn.setHorizontalAlignment(SwingConstants.CENTER);
                btn.setPreferredSize(new Dimension(DEV_CARD_BUTTON_WIDTH, DEV_CARD_BUTTON_HEIGHT));
                if (col < cards.size()) {
                    DevelopmentCard card = cards.get(col);
                    ImageIcon icon = loadCardIcon(card);
                    if (icon != null) {
                        btn.setIcon(icon);
                        btn.setDisabledIcon(icon);
                        btn.setText("");
                        btn.setHorizontalTextPosition(SwingConstants.CENTER);
                        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
                    } else {
                        btn.setText(String.format("%s %s %d", key, card.getBonusColor(), card.getPrestigePoints()));
                    }
                    btn.setToolTipText(cardTooltip(key, card));
                    btn.setBackground(SURFACE_BG);
                    btn.setForeground(TEXT_PRIMARY);
                    btn.setBorder(new LineBorder(BORDER_COLOR, 2));
                    btn.addActionListener(e -> {
                        selectedBoardCard = card;
                        selectedReservedCard = null;
                        reservedList.clearSelection();
                        updateLegalUi();
                    });
                } else {
                    btn.setText(key + " Empty");
                    btn.setBackground(EMPTY_BG);
                    btn.setForeground(TEXT_MUTED);
                    btn.setEnabled(false);
                }
                cardButtons.put(key, btn);
                marketPanel.add(btn);
            }
        }

        marketPanel.revalidate();
        marketPanel.repaint();
    }

    private void refreshNobles() {
        noblesPanel.removeAll();
        if (state == null) {
            noblesPanel.revalidate();
            noblesPanel.repaint();
            return;
        }
        for (NobleTile noble : state.getBoard().getAvailableNobles()) {
            JLabel label = new JLabel();
            ImageIcon icon = loadNobleIcon(noble);
            if (icon != null) {
                label.setIcon(icon);
                label.setHorizontalTextPosition(SwingConstants.CENTER);
                label.setVerticalTextPosition(SwingConstants.CENTER);
                label.setText("");
            } else {
                label.setText("N" + noble.getId() + " " + noble.getPrestigePoints() + "pts");
            }
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);
            label.setForeground(TEXT_PRIMARY);
            label.setBorder(new LineBorder(BORDER_COLOR));
            noblesPanel.add(label);
        }
        noblesPanel.revalidate();
        noblesPanel.repaint();
    }

    private void refreshPlayers() {
        playersPanel.removeAll();
        if (state == null) {
            for (int i = 0; i < lobbyPlayers.size(); i++) {
                JPanel panel = new JPanel(new BorderLayout());
                panel.setBackground(PANEL_BG_ALT);
                String title = lobbyPlayers.get(i);
                if (i == hostPlayerIndex) {
                    title += " (Host)";
                }
                if (i == myPlayerIndex) {
                    title += " (You)";
                }
                panel.setBorder(createPlayerBorder(title, false));

                JTextArea area = new JTextArea();
                area.setEditable(false);
                area.setOpaque(false);
                area.setForeground(TEXT_PRIMARY);
                area.setText(i == hostPlayerIndex
                        ? "Can start the match when at least " + minPlayersToStart + " players are connected."
                        : "Waiting in lobby for the host to start the match.");
                panel.add(area, BorderLayout.CENTER);
                playersPanel.add(panel);
            }
            playersPanel.revalidate();
            playersPanel.repaint();
            return;
        }

        for (int i = 0; i < state.getPlayers().size(); i++) {
            Player player = state.getPlayer(i);
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(PANEL_BG_ALT);
            String title = (i == state.getCurrentPlayerIndex() ? "* " : "") + player.getName();
            panel.setBorder(createPlayerBorder(title, i == state.getCurrentPlayerIndex()));

            StringBuilder sb = new StringBuilder();
            sb.append("Prestige: ").append(player.getPrestigePoints()).append("\n");
            sb.append("Tokens: ").append(player.getTokens()).append("\n");
            sb.append("Bonuses: ").append(getBonusCounts(player)).append("\n");
            sb.append("Purchased: ").append(player.getPurchasedCards().size()).append("\n");
            sb.append("Reserved: ").append(player.getReservedCards().size()).append("\n");
            sb.append("Nobles: ").append(player.getNobles().size()).append("\n");

            JEditorPane area = new JEditorPane();
            area.setEditable(false);
            area.setText(sb.toString());
            area.setBackground(PANEL_BG_ALT);
            area.setForeground(TEXT_PRIMARY);

            panel.add(area, BorderLayout.CENTER);
            playersPanel.add(panel);
        }
        playersPanel.revalidate();
        playersPanel.repaint();
    }

    private Map<GemColor, Integer> getBonusCounts(Player player) {
        Map<GemColor, Integer> bonus = new EnumMap<>(GemColor.class);
        for (GemColor color : GemColor.values()) {
            if (color == GemColor.GOLD) continue;
            bonus.put(color, player.getBonusCount(color));
        }
        return bonus;
    }

    private void refreshBank() {
        bankCountPanel.removeAll();
        if (state == null) {
            bankCountPanel.revalidate();
            bankCountPanel.repaint();
            return;
        }

        GemBank bank = state.getBank();
        for (GemColor color : GemColor.values()) {
            String text = color + ": " + bank.getCount(color);
            JLabel label = new JLabel(text);
            label.setForeground(TEXT_PRIMARY);
            bankCountPanel.add(label);
        }
        bankCountPanel.revalidate();
        bankCountPanel.repaint();
    }

    private String cardTooltip(String key, DevelopmentCard card) {
        return key + " " + card.getLevel() + " " + card.getPrestigePoints() + "pt " + card.getCost();
    }

    private Color tokenColor(GemColor color) {
        return switch (color) {
            case WHITE -> new Color(240, 240, 240);
            case BLUE -> new Color(66, 133, 244);
            case GREEN -> new Color(52, 168, 83);
            case RED -> new Color(234, 67, 53);
            case BLACK -> new Color(60, 60, 60);
            case GOLD -> new Color(251, 188, 5);
        };
    }

    private void refreshReserved() {
        if (state == null || myPlayerIndex < 0) return;
        Player me = state.getPlayer(myPlayerIndex);
        reservedModel.clear();
        for (DevelopmentCard card : me.getReservedCards()) {
            reservedModel.addElement(card);
        }
        reservedList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            ImageIcon icon = loadCardIcon(value);
            JLabel cell;
            if (icon != null) {
                cell = new JLabel(icon);
                cell.setText("r" + (index + 1));
                cell.setHorizontalTextPosition(JLabel.CENTER);
                cell.setVerticalTextPosition(JLabel.BOTTOM);
            } else {
                cell = new JLabel("r" + (index + 1) + " | P:" + value.getPrestigePoints() + " " + value.getBonusColor()
                        + " | Cost " + value.getCost());
            }
            cell.setOpaque(true);
            cell.setForeground(TEXT_PRIMARY);
            cell.setBackground(isSelected ? SELECTED_BG : PANEL_BG_ALT);
            return cell;
        });
    }

    private void refreshStatus() {
        if (state == null) {
            int needed = Math.max(0, minPlayersToStart - lobbyPlayers.size());
            statusLabel.setText(isHost()
                    ? "Lobby: you are the host"
                    : "Lobby: waiting for host");
            if (needed == 0) {
                phaseLabel.setText("Ready to start");
            } else {
                phaseLabel.setText("Need " + needed + " more player" + (needed == 1 ? "" : "s"));
            }
            lobbyStatusLabel.setText("Players connected: " + lobbyPlayers.size() + " / " + minPlayersToStart + "+");
            return;
        }
        Player current = state.getCurrentPlayer();
        statusLabel.setText("You: " + client.getPlayerName() +
            (myPlayerIndex == state.getCurrentPlayerIndex() ? " (Your Turn)" : ""));
        phaseLabel.setText("Round: " + (state.isFinalRound() ? "Final" : "Normal"));
        lobbyStatusLabel.setText("Turn: " + current.getName());
    }

    private boolean isHost() {
        return myPlayerIndex >= 0 && myPlayerIndex == hostPlayerIndex;
    }

    private void log(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private ImageIcon loadTokenIcon(GemColor color) {
        String filename = switch (color) {
            case WHITE -> "white.png";
            case BLUE -> "blue.png";
            case GREEN -> "green.png";
            case RED -> "red.png";
            case BLACK -> "black.png";
            case GOLD -> "gold.png";
        };

        Path[] candidates = new Path[]{
                config.getTokenImageDir().resolve(filename),
                Path.of("assets", "tokens", filename),
                Path.of("resources", "tokens", filename),
                Path.of("src", "assets", "tokens", filename),
                Path.of("media", "tokens", filename)
        };

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return loadCircularIcon(candidate, TOKEN_ICON_SIZE);
            }
        }

        try (var stream = Files.list(Path.of("media", "tokens"))) {
            List<Path> pngs = stream
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".png");
                    })
                    .sorted()
                    .toList();

            if (!pngs.isEmpty()) {
                int idx = switch (color) {
                    case GREEN -> 0;
                    case BLACK -> 1;
                    case WHITE -> 2;
                    case RED -> 3;
                    case BLUE -> 4;
                    case GOLD -> 5;
                };
                if (idx >= 0 && idx < pngs.size()) {
                    return loadCircularIcon(pngs.get(idx), TOKEN_ICON_SIZE);
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private ImageIcon loadCardIcon(DevelopmentCard card) {
        String filename = "card_" + card.getId() + ".png";
        Path[] candidates = new Path[]{
                config.getCardImageDir().resolve("devLevel" + card.getLevel()).resolve(filename),
                config.getCardImageDir().resolve(filename),
                Path.of("media", "devLevel" + card.getLevel(), filename)
        };
        for (Path path : candidates) {
            if (Files.exists(path)) {
                return loadScaledIcon(path, DEV_CARD_ICON_WIDTH, DEV_CARD_ICON_HEIGHT);
            }
        }
        return null;
    }

    private ImageIcon loadNobleIcon(NobleTile noble) {
        String filename = "card_" + noble.getId() + ".png";
        Path[] candidates = new Path[]{
                config.getNobleImageDir().resolve(filename),
                Path.of("media", "nobles", filename)
        };
        for (Path path : candidates) {
            if (Files.exists(path)) {
                return loadScaledIcon(path, NOBLE_ICON_SIZE, NOBLE_ICON_SIZE);
            }
        }
        return null;
    }

    private ImageIcon loadScaledIcon(Path imagePath, int targetW, int targetH) {
        ImageIcon icon = new ImageIcon(imagePath.toString());
        Image src = icon.getImage();
        int imgW = src.getWidth(null);
        int imgH = src.getHeight(null);
        if (imgW <= 0 || imgH <= 0) {
            imgW = 1;
            imgH = 1;
        }
        double scale = Math.min((double) targetW / imgW, (double) targetH / imgH);
        int newW = (int) (imgW * scale);
        int newH = (int) (imgH * scale);
        int x = (targetW - newW) / 2;
        int y = (targetH - newH) / 2;

        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setBackground(new Color(0, 0, 0, 0));
        g2.clearRect(0, 0, targetW, targetH);
        g2.drawImage(src, x, y, newW, newH, null);
        g2.dispose();
        return new ImageIcon(out);
    }

    private ImageIcon loadCircularIcon(Path imagePath, int diameter) {
        ImageIcon icon = new ImageIcon(imagePath.toString());
        Image src = icon.getImage();
        int imgW = src.getWidth(null);
        int imgH = src.getHeight(null);
        if (imgW <= 0 || imgH <= 0) {
            imgW = 1;
            imgH = 1;
        }

        double scale = Math.max((double) diameter / imgW, (double) diameter / imgH);
        int newW = (int) Math.ceil(imgW * scale);
        int newH = (int) Math.ceil(imgH * scale);
        int x = (diameter - newW) / 2;
        int y = (diameter - newH) / 2;

        BufferedImage out = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new Ellipse2D.Double(0, 0, diameter, diameter));
        g2.drawImage(src, x, y, newW, newH, null);
        g2.dispose();
        return new ImageIcon(out);
    }

    private Map<GemColor, Integer> selectedTokens() {
        Map<GemColor, Integer> tokens = new EnumMap<>(GemColor.class);
        for (Map.Entry<GemColor, Integer> entry : selectedTokenCounts.entrySet()) {
            int count = entry.getValue();
            if (count > 0) {
                tokens.put(entry.getKey(), count);
            }
        }
        return tokens;
    }

    private Map<GemColor, Integer> computePaymentTokens(Player player, Map<GemColor, Integer> cost) {
        Map<GemColor, Integer> payment = new EnumMap<>(GemColor.class);
        int goldNeeded = 0;

        for (Map.Entry<GemColor, Integer> entry : cost.entrySet()) {
            GemColor color = entry.getKey();
            int required = entry.getValue();
            int remaining = Math.max(0, required - player.getBonusCount(color));
            if (remaining == 0) continue;

            int availableColor = player.getTokenCount(color);
            int useColor = Math.min(availableColor, remaining);
            if (useColor > 0) {
                payment.put(color, useColor);
            }
            goldNeeded += (remaining - useColor);
        }

        if (goldNeeded > 0) {
            payment.put(GemColor.GOLD, goldNeeded);
        }
        return payment;
    }

    private void refreshTokenButtonLabels() {
        for (GemColor color : GemColor.values()) {
            JButton button = tokenButtons.get(color);
            if (button == null) {
                continue;
            }
            int count = selectedTokenCounts.getOrDefault(color, 0);
            button.setBackground(count > 0 ? SELECTED_BG : tokenColor(color));
        }
    }

    private void applyTokenModeRules(Player current, boolean myActiveTurn) {
        Map<GemColor, Integer> selected = selectedTokens();
        int totalSelected = selected.values().stream().mapToInt(Integer::intValue).sum();

        for (GemColor color : GemColor.values()) {
            JButton button = tokenButtons.get(color);
            if (button == null || color == GemColor.GOLD) {
                continue;
            }

            boolean enabled = false;
            if (myActiveTurn && mode == Mode.TAKE_THREE) {
                enabled = state.getBank().getTokenCount(color) > 0
                        && (selectedTokenCounts.getOrDefault(color, 0) > 0 || totalSelected < 3);
            } else if (myActiveTurn && mode == Mode.TAKE_TWO) {
                Map<GemColor, Integer> preview = new EnumMap<>(GemColor.class);
                preview.put(color, 2);
                enabled = validator.validate(state, current, Move.takeSame(preview)) == null;
            }

            button.setEnabled(enabled);
        }
        refreshTokenButtonLabels();
    }

    private boolean hasAnyLegalTakeThree(Player current) {
        List<GemColor> colors = new ArrayList<>();
        for (GemColor color : GemColor.values()) {
            if (color != GemColor.GOLD && state.getBank().getTokenCount(color) > 0) {
                colors.add(color);
            }
        }
        for (int i = 0; i < colors.size(); i++) {
            for (int j = i + 1; j < colors.size(); j++) {
                for (int k = j + 1; k < colors.size(); k++) {
                    Map<GemColor, Integer> tokens = new EnumMap<>(GemColor.class);
                    tokens.put(colors.get(i), 1);
                    tokens.put(colors.get(j), 1);
                    tokens.put(colors.get(k), 1);
                    if (validator.validate(state, current, Move.takeDifferent(tokens)) == null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasAnyLegalTakeTwo(Player current) {
        for (GemColor color : GemColor.values()) {
            if (color == GemColor.GOLD) continue;
            Map<GemColor, Integer> tokens = new EnumMap<>(GemColor.class);
            tokens.put(color, 2);
            if (validator.validate(state, current, Move.takeSame(tokens)) == null) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyLegalReserve(Player current) {
        for (DevelopmentCard card : state.getBoard().getFaceUpCards()) {
            if (validator.validate(state, current, Move.reserveFaceUp(card)) == null) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyLegalBuy(Player current) {
        for (DevelopmentCard card : state.getBoard().getFaceUpCards()) {
            Move move = Move.buy(card, computePaymentTokens(current, card.getCost()), false);
            if (validator.validate(state, current, move) == null) {
                return true;
            }
        }
        for (DevelopmentCard card : current.getReservedCards()) {
            Move move = Move.buy(card, computePaymentTokens(current, card.getCost()), true);
            if (validator.validate(state, current, move) == null) {
                return true;
            }
        }
        return false;
    }

    private String helpTextForMode() {
        if (state == null) {
            return isHost()
                    ? "Wait for at least 2 players, then press Start Game."
                    : "Waiting for the host to start the game.";
        }
        if (myPlayerIndex != state.getCurrentPlayerIndex()) {
            return "Wait for the other player to finish their turn.";
        }

        return switch (mode) {
            case IDLE -> "Choose an action. Take tokens, reserve a face-up card, or buy a card.";
            case TAKE_THREE -> "Select 3 different colored tokens, then confirm.";
            case TAKE_TWO -> "Select 1 color to take 2 tokens, then confirm.";
            case RESERVE -> "Select a face-up card to reserve, then confirm.";
            case BUY -> "Select a face-up or reserved card you can afford, then confirm.";
        };
    }

    private TitledBorder createTitledBorder(String title) {
        return createStyledBorder(title, BORDER_COLOR, TEXT_PRIMARY, 12);
    }

    private TitledBorder createPlayerBorder(String title, boolean active) {
        return createStyledBorder(title, active ? ACCENT_BLUE : BORDER_COLOR, active ? ACCENT_BLUE : TEXT_PRIMARY, 15);
    }

    private TitledBorder createStyledBorder(String title, Color borderColor, Color titleColor, int fontSize) {
        return BorderFactory.createTitledBorder(
                new LineBorder(borderColor, activeBorderWidth(borderColor)),
                title,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, fontSize),
                titleColor
        );
    }

    private int activeBorderWidth(Color borderColor) {
        return borderColor.equals(ACCENT_BLUE) ? 2 : 1;
    }
}
