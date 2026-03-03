package ui.swing;

import ai.AiController;
import ai.GreedyStrategy;
import config.Config;
import engine.Move;
import engine.MoveExecutor;
import engine.MoveValidator;
import engine.NobleAssigner;
import engine.TurnManager;
import engine.WinnerChecker;
import io.CardLoader;
import model.Board;
import model.DevelopmentCard;
import model.GameState;
import model.GemBank;
import model.GemColor;
import model.NobleTile;
import model.Player;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JEditorPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.ImageIcon;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class SwingSplendorApp extends JFrame {
    private enum Mode {
        IDLE, TAKE_THREE, TAKE_TWO, RESERVE, BUY
    }

    private final Config config;
    private final GameState state;
    private final MoveValidator validator;
    private final MoveExecutor executor;
    private final NobleAssigner nobleAssigner;
    private final WinnerChecker winnerChecker;
    private final TurnManager turnManager;
    private final GreedyStrategy aiStrategy = new GreedyStrategy();

    private final JLabel statusLabel = new JLabel("", SwingConstants.LEFT);
    private final JLabel phaseLabel = new JLabel("", SwingConstants.RIGHT);
    private final JPanel marketPanel = new JPanel(new GridLayout(3, 4, 10, 10));
    private final JPanel noblesPanel = new JPanel(new GridLayout(1, 5, 8, 8));
    private final JTextArea logArea = new JTextArea();
    private final JPanel playersPanel = new JPanel();
    private final Map<Player, JPanel> playerCardPanels = new LinkedHashMap<>();
    private final Map<Player, JEditorPane> playerCardAreas = new LinkedHashMap<>();
    private final JTextArea helpArea = new JTextArea();
    private final JLabel latestComputerMoveLabel = new JLabel("Latest Computer Move: -");
    private final JPanel bankCountPanel = new JPanel(new GridLayout(6, 1, 4, 4));
    private final Map<GemColor, JLabel> bankLabels = new EnumMap<>(GemColor.class);
    private final Map<GemColor, JButton> tokenButtons = new EnumMap<>(GemColor.class);
    private final Map<GemColor, ImageIcon> tokenIcons = new EnumMap<>(GemColor.class);
    private final Map<GemColor, Integer> selectedTokenCounts = new EnumMap<>(GemColor.class);
    private final Map<String, JButton> cardButtons = new LinkedHashMap<>();
    private final JButton actionTakeThree = new JButton("Take 3 Different");
    private final JButton actionTakeTwo = new JButton("Take 2 Same");
    private final JButton actionReserve = new JButton("Reserve");
    private final JButton actionBuy = new JButton("Buy");
    private final JButton actionCancel = new JButton("Cancel");
    private final JButton actionConfirm = new JButton("Confirm");
    private final DefaultListModel<DevelopmentCard> reservedModel = new DefaultListModel<>();
    private final JList<DevelopmentCard> reservedList = new JList<>(reservedModel);
    private final JLabel reservedLabel = new JLabel("Active Player Reserved Cards");

    private Mode mode = Mode.IDLE;
    private DevelopmentCard selectedBoardCard;
    private DevelopmentCard selectedReservedCard;
    private boolean finalGameOver = false;
    private boolean computerThinking = false;
    private AiController.Level aiLevel = AiController.Level.HIGH;
    private final Set<Player> computerPlayers = new HashSet<>();

    public SwingSplendorApp() {
        super("Splendor (Swing)");
        this.config = buildConfig();
        this.state = createInitialState();
        this.validator = new MoveValidator(config);
        this.executor = new MoveExecutor(config);
        this.nobleAssigner = new NobleAssigner();
        this.winnerChecker = new WinnerChecker(config);
        this.turnManager = new TurnManager();

        buildUi();
        bindActions();
        refreshAll();
    }

    private GameState createInitialState() {
        String p1 = promptName("Enter Player 1 name:");
        List<Player> players;

        boolean vsComputer = askYesNo("Play against computer?");
        if (vsComputer) {
            boolean humanFirst = askYesNo("Do you want to go first?");
            aiLevel = askAiLevel();
            Player human = new Player(p1);
            Player computer = new Player("Computer");
            computerPlayers.add(computer);
            players = humanFirst ? List.of(human, computer) : List.of(computer, human);
        } else {
            int playerCount = askPlayerCount();
            List<Player> humanPlayers = new ArrayList<>();
            humanPlayers.add(new Player(p1));
            for (int i = 2; i <= playerCount; i++) {
                humanPlayers.add(new Player(promptName("Enter Player " + i + " name:")));
            }
            players = humanPlayers;
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
        return new GameState(new ArrayList<>(players), board, bank);
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1500, 950);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel topBar = new JPanel(new BorderLayout());
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 16f));
        phaseLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        phaseLabel.setFont(phaseLabel.getFont().deriveFont(Font.BOLD, 14f));
        topBar.add(statusLabel, BorderLayout.WEST);
        topBar.add(phaseLabel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        JPanel leftBank = new JPanel(new BorderLayout(8, 8));
        leftBank.setBorder(BorderFactory.createTitledBorder("Bank"));
        leftBank.add(bankCountPanel, BorderLayout.NORTH);
        JPanel tokenPanel = new JPanel(new GridLayout(3, 2, 6, 6));
        for (GemColor color : GemColor.values()) {
            JButton button = new JButton(color.name());
            button.setFocusPainted(false);
            button.setBackground(tokenColor(color));
            button.setOpaque(true);
            button.setPreferredSize(new Dimension(96, 96));
            button.setMargin(new Insets(2, 2, 2, 2));
            button.setToolTipText("Click to select " + color.name() + " token(s)");
            ImageIcon icon = loadTokenIcon(color);
            if (icon != null) {
                button.setIcon(icon);
                button.setText("");
                tokenIcons.put(color, icon);
            }
            tokenButtons.put(color, button);
            selectedTokenCounts.put(color, 0);
            tokenPanel.add(button);
        }
        leftBank.add(tokenPanel, BorderLayout.CENTER);
        leftBank.setPreferredSize(new Dimension(260, 400));
        add(leftBank, BorderLayout.WEST);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBorder(BorderFactory.createTitledBorder("Market"));
        center.add(marketPanel, BorderLayout.CENTER);
        JPanel nobleWrap = new JPanel(new BorderLayout());
        nobleWrap.setBorder(BorderFactory.createTitledBorder("Nobles"));
        nobleWrap.add(noblesPanel, BorderLayout.CENTER);
        center.add(nobleWrap, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.setBorder(BorderFactory.createTitledBorder("Players"));
        playersPanel.setLayout(new GridLayout(state.getPlayers().size(), 1, 8, 8));

        for (Player player : state.getPlayers()) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createTitledBorder(player.getName()));
            JEditorPane area = new JEditorPane();
            area.setContentType("text/html");
            area.setEditable(false);
            area.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            panel.add(new JScrollPane(area), BorderLayout.CENTER);
            playerCardPanels.put(player, panel);
            playerCardAreas.put(player, area);
            playersPanel.add(panel);
        }
        right.add(new JScrollPane(playersPanel), BorderLayout.CENTER);

        reservedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel reservedPanel = new JPanel(new BorderLayout(4, 4));
        reservedPanel.add(reservedLabel, BorderLayout.NORTH);
        JScrollPane reservedScroll = new JScrollPane(reservedList);
        reservedScroll.setPreferredSize(new Dimension(320, 210));
        reservedPanel.add(reservedScroll, BorderLayout.CENTER);
        right.add(reservedPanel, BorderLayout.SOUTH);
        right.setPreferredSize(new Dimension(360, 400));
        add(right, BorderLayout.EAST);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        JPanel actions = new JPanel(new GridLayout(1, 6, 6, 6));
        actions.add(actionTakeThree);
        actions.add(actionTakeTwo);
        actions.add(actionReserve);
        actions.add(actionBuy);
        actions.add(actionCancel);
        actions.add(actionConfirm);
        bottom.add(actions, BorderLayout.NORTH);

        helpArea.setEditable(false);
        helpArea.setRows(3);
        helpArea.setLineWrap(true);
        helpArea.setWrapStyleWord(true);
        helpArea.setBorder(BorderFactory.createTitledBorder("Prompt"));
        bottom.add(helpArea, BorderLayout.CENTER);

        latestComputerMoveLabel.setBorder(BorderFactory.createTitledBorder("Computer Move"));
        bottom.add(latestComputerMoveLabel, BorderLayout.WEST);

        logArea.setEditable(false);
        logArea.setRows(7);
        bottom.add(new JScrollPane(logArea), BorderLayout.SOUTH);
        bottom.setBorder(BorderFactory.createTitledBorder("Actions & Log"));
        add(bottom, BorderLayout.SOUTH);
    }

    private void bindActions() {
        actionTakeThree.addActionListener(e -> switchMode(Mode.TAKE_THREE));
        actionTakeTwo.addActionListener(e -> switchMode(Mode.TAKE_TWO));
        actionReserve.addActionListener(e -> switchMode(Mode.RESERVE));
        actionBuy.addActionListener(e -> switchMode(Mode.BUY));
        actionCancel.addActionListener(e -> switchMode(Mode.IDLE));
        actionConfirm.addActionListener(e -> onConfirm());

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

    private void rebuildMarketButtons() {
        marketPanel.removeAll();
        cardButtons.clear();
        for (int tier = 1; tier <= 3; tier++) {
            List<DevelopmentCard> cards = state.getBoard().getFaceUpCards(tier);
            for (int col = 0; col < 4; col++) {
                char colChar = (char) ('a' + col);
                String key = "" + colChar + tier;
                JButton btn = new JButton();
                btn.setVerticalAlignment(SwingConstants.TOP);
                btn.setHorizontalAlignment(SwingConstants.LEFT);
                btn.setFocusPainted(false);
                btn.setOpaque(true);
                if (col < cards.size()) {
                    DevelopmentCard card = cards.get(col);
                    btn.setText(cardHtml(key, card));
                    btn.setToolTipText(cardTooltip(key, card));
                    btn.setBackground(colorForBonus(card.getBonusColor()));
                    btn.setBorder(new LineBorder(new Color(120, 120, 120), 2));
                    btn.addActionListener(e -> {
                        selectedBoardCard = card;
                        selectedReservedCard = null;
                        reservedList.clearSelection();
                        updateLegalUi();
                    });
                } else {
                    btn.setText("<html><b>" + key + "</b><br/>Empty</html>");
                    btn.setBackground(new Color(230, 230, 230));
                    btn.setEnabled(false);
                }
                cardButtons.put(key, btn);
                marketPanel.add(btn);
            }
        }
        marketPanel.revalidate();
        marketPanel.repaint();
    }

    private void switchMode(Mode newMode) {
        this.mode = newMode;
        this.selectedBoardCard = null;
        this.selectedReservedCard = null;
        reservedList.clearSelection();
        clearTokenSelection();
        updateLegalUi();
    }

    private void onConfirm() {
        if (finalGameOver) {
            return;
        }
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
            JOptionPane.showMessageDialog(this,
                    "Game over.\nWinner: " + winner.getName() + " (" + winner.getPrestigePoints() + " points)",
                    "Winner",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        switchMode(Mode.IDLE);
        refreshAll();
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
            for (Map.Entry<GemColor, Integer> e : working.entrySet()) {
                if (e.getValue() > max) {
                    max = e.getValue();
                    candidate = e.getKey();
                }
            }
            if (candidate == null || max == 0) break;
            discard.put(candidate, discard.getOrDefault(candidate, 0) + 1);
            working.put(candidate, max - 1);
            excess--;
        }
        return discard;
    }

    private void resolveNobleAttraction(Player player, boolean automaticChoice) {
        List<NobleTile> eligible = nobleAssigner.findEligibleNobles(state, player);
        if (eligible.isEmpty()) {
            return;
        }
        NobleTile chosen;
        if (eligible.size() == 1) {
            chosen = eligible.get(0);
        } else if (automaticChoice) {
            chosen = eligible.stream()
                    .max((a, b) -> {
                        int points = Integer.compare(a.getPrestigePoints(), b.getPrestigePoints());
                        if (points != 0) return points;
                        int aReq = a.getRequirement().asMap().values().stream().mapToInt(Integer::intValue).sum();
                        int bReq = b.getRequirement().asMap().values().stream().mapToInt(Integer::intValue).sum();
                        return Integer.compare(bReq, aReq);
                    })
                    .orElse(eligible.get(0));
        } else {
            Object pick = JOptionPane.showInputDialog(
                    this,
                    "Choose a noble",
                    "Noble Choice",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    eligible.toArray(),
                    eligible.get(0)
            );
            chosen = pick instanceof NobleTile ? (NobleTile) pick : eligible.get(0);
        }
        nobleAssigner.assignNoble(state, player, chosen);
        log(player.getName() + " attracted noble: " + chosen);
    }

    private void refreshAll() {
        rebuildMarketButtons();
        rebuildNobleCards();
        Player current = state.getCurrentPlayer();
        statusLabel.setText("Current Player: " + current.getName());
        reservedLabel.setText(current.getName() + " Reserved Cards");

        for (GemColor color : GemColor.values()) {
            JLabel label = bankLabels.computeIfAbsent(color, c -> {
                JLabel created = new JLabel();
                bankCountPanel.add(created);
                return created;
            });
            label.setText(color.name() + ": " + state.getBank().getTokenCount(color));
        }
        bankCountPanel.revalidate();
        bankCountPanel.repaint();

        for (Player player : state.getPlayers()) {
            JEditorPane area = playerCardAreas.get(player);
            if (area == null) {
                continue;
            }

            area.setText(buildPlayerHtml(player));
            area.setCaretPosition(0);

            JPanel panel = playerCardPanels.get(player);
            if (panel != null) {
                if (player == current) {
                    panel.setBorder(BorderFactory.createTitledBorder(
                            new LineBorder(new Color(36, 92, 194), 2),
                            player.getName() + " (Active)"
                    ));
                } else {
                    panel.setBorder(BorderFactory.createTitledBorder(player.getName()));
                }
            }
        }

        reservedModel.clear();
        for (DevelopmentCard card : current.getReservedCards()) {
            reservedModel.addElement(card);
        }
        reservedList.setCellRenderer((list, value, index1, isSelected, cellHasFocus) -> {
            JLabel cell = new JLabel("r" + (index1 + 1) + " | P:" + value.getPrestigePoints() + " " + value.getBonusColor()
                    + " | Cost " + value.getCost());
            cell.setOpaque(true);
            if (isSelected) {
                cell.setBackground(new Color(207, 229, 255));
            } else {
                cell.setBackground(Color.WHITE);
            }
            return cell;
        });

        updateLegalUi();
        scheduleComputerTurnIfNeeded();
    }

    private void updateLegalUi() {
        if (finalGameOver) {
            disableAllInputs();
            return;
        }

        Player current = state.getCurrentPlayer();
        statusLabel.setText("Current Player: " + current.getName());
        phaseLabel.setText("Phase: " + mode.name().replace('_', ' '));
        helpArea.setText(helpTextForMode());

        actionTakeThree.setEnabled(hasAnyLegalTakeThree(current));
        actionTakeTwo.setEnabled(hasAnyLegalTakeTwo(current));
        actionReserve.setEnabled(hasAnyLegalReserve(current));
        actionBuy.setEnabled(hasAnyLegalBuy(current));

        boolean takeMode = mode == Mode.TAKE_THREE || mode == Mode.TAKE_TWO;
        boolean cardMode = mode == Mode.RESERVE || mode == Mode.BUY;

        if (isComputerTurn() || computerThinking) {
            helpArea.setText("Computer is thinking...");
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
            }
            reservedList.setEnabled(false);
            return;
        }

        for (Map.Entry<GemColor, JButton> entry : tokenButtons.entrySet()) {
            GemColor color = entry.getKey();
            JButton button = entry.getValue();
            button.setEnabled(takeMode && color != GemColor.GOLD);
        }

        // Reserved list only for buy mode.
        reservedList.setEnabled(mode == Mode.BUY);

        // Disable all card buttons first.
        for (JButton btn : cardButtons.values()) {
            btn.setEnabled(false);
        }

        if (cardMode) {
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
                            btn.setBorder(new LineBorder(new Color(36, 92, 194), 4));
                        } else if (mode == Mode.BUY && legal) {
                            btn.setBorder(new LineBorder(new Color(22, 140, 67), 3));
                        } else if (mode == Mode.BUY) {
                            btn.setBorder(new LineBorder(new Color(140, 32, 32), 2));
                        } else {
                            btn.setBorder(new LineBorder(new Color(120, 120, 120), 2));
                        }
                    }
                }
            }
        } else {
            for (JButton btn : cardButtons.values()) {
                btn.setBorder(new LineBorder(new Color(120, 120, 120), 2));
            }
        }

        applyTokenModeRules();
        Move pending = buildPendingMove();
        actionConfirm.setEnabled(mode != Mode.IDLE && pending != null && validator.validate(state, current, pending) == null);
    }

    private void disableAllInputs() {
        actionTakeThree.setEnabled(false);
        actionTakeTwo.setEnabled(false);
        actionReserve.setEnabled(false);
        actionBuy.setEnabled(false);
        actionCancel.setEnabled(false);
        actionConfirm.setEnabled(false);
        for (JButton button : tokenButtons.values()) {
            button.setEnabled(false);
        }
        for (JButton b : cardButtons.values()) {
            b.setEnabled(false);
        }
        reservedList.setEnabled(false);
    }

    private void clearTokenSelection() {
        for (GemColor color : GemColor.values()) {
            selectedTokenCounts.put(color, 0);
        }
        refreshTokenButtonLabels();
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

    private String cardHtml(String key, DevelopmentCard card) {
        StringBuilder cost = new StringBuilder();
        for (Map.Entry<GemColor, Integer> e : card.getCost().entrySet()) {
            if (cost.length() > 0) cost.append(", ");
            cost.append(e.getValue()).append(" ").append(e.getKey().name());
        }
        return "<html><b>Card</b><br/>P:" + card.getPrestigePoints() +
                " Bonus:" + card.getBonusColor().name() +
                "<br/>Cost: " + cost + "</html>";
    }

    private String cardTooltip(String key, DevelopmentCard card) {
        return "Bonus " + card.getBonusColor() + " | Prestige " + card.getPrestigePoints() +
                " | Cost " + card.getCost();
    }

    private String buildPlayerHtml(Player player) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:sans-serif;font-size:12px;'>");
        html.append("<b>Prestige:</b> ").append(player.getPrestigePoints()).append("<br/>");
        html.append("<b>Tokens + Bonuses</b><br/>");

        GemColor[] order = {GemColor.RED, GemColor.BLUE, GemColor.GREEN, GemColor.WHITE, GemColor.BLACK, GemColor.GOLD};
        for (GemColor color : order) {
            int tokens = player.getTokenCount(color);
            int bonus = color == GemColor.GOLD ? 0 : player.getBonusCount(color);
            html.append(color.name().toLowerCase()).append(": ").append(tokens);
            if (bonus > 0) {
                html.append(" <span style='color:")
                        .append(hexColor(color))
                        .append(";font-weight:bold;'>+ ")
                        .append(bonus)
                        .append("</span>");
            }
            html.append("<br/>");
        }

        html.append("<b>Purchased:</b> ").append(player.getPurchasedCards().size())
                .append(" | <b>Nobles:</b> ").append(player.getNobles().size())
                .append(" | <b>Reserved:</b> ").append(player.getReservedCards().size())
                .append("<br/>");
        html.append("<b>Token cap:</b> ").append(player.getTotalTokens())
                .append("/").append(config.getMaxTokensPerPlayer()).append("<br/>");
        html.append("<b>Reserved cards:</b> ").append(compactReserved(player));
        html.append("</body></html>");
        return html.toString();
    }

    private String compactReserved(Player player) {
        if (player.getReservedCards().isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (DevelopmentCard card : player.getReservedCards()) {
            if (index > 1) sb.append(", ");
            sb.append("r").append(index)
                    .append("(P").append(card.getPrestigePoints())
                    .append(" ").append(card.getBonusColor().name().toLowerCase()).append(")");
            index++;
        }
        return sb.toString();
    }

    private String hexColor(GemColor color) {
        return switch (color) {
            case WHITE -> "#8A8A8A";
            case BLUE -> "#1E5BB8";
            case GREEN -> "#1E8C3A";
            case RED -> "#B3261E";
            case BLACK -> "#111111";
            case GOLD -> "#B88700";
        };
    }

    private void rebuildNobleCards() {
        noblesPanel.removeAll();
        List<NobleTile> nobles = state.getBoard().getAvailableNobles();
        for (int i = 0; i < 5; i++) {
            JPanel nobleCard = new JPanel(new BorderLayout());
            nobleCard.setBorder(new LineBorder(new Color(120, 120, 120), 2));
            nobleCard.setBackground(new Color(250, 244, 230));

            if (i < nobles.size()) {
                NobleTile noble = nobles.get(i);
                JLabel label = new JLabel(
                        "<html><b>N" + (i + 1) + "</b><br/>P:" + noble.getPrestigePoints()
                                + "<br/>Req:" + noble.getRequirement().asMap() + "</html>"
                );
                label.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
                nobleCard.add(label, BorderLayout.CENTER);
                nobleCard.setToolTipText("Noble " + (i + 1) + " | Prestige " + noble.getPrestigePoints()
                        + " | Requirements " + noble.getRequirement().asMap());
            } else {
                JLabel label = new JLabel("<html><b>Empty</b></html>", SwingConstants.CENTER);
                nobleCard.add(label, BorderLayout.CENTER);
                nobleCard.setBackground(new Color(235, 235, 235));
            }
            noblesPanel.add(nobleCard);
        }
        noblesPanel.revalidate();
        noblesPanel.repaint();
    }

    private Color colorForBonus(GemColor color) {
        return switch (color) {
            case WHITE -> new Color(245, 245, 245);
            case BLUE -> new Color(206, 225, 255);
            case GREEN -> new Color(208, 242, 214);
            case RED -> new Color(255, 214, 214);
            case BLACK -> new Color(222, 222, 222);
            case GOLD -> new Color(255, 244, 173);
        };
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
                Path.of("assets", "tokens", filename),
                Path.of("resources", "tokens", filename),
                Path.of("src", "assets", "tokens", filename),
                Path.of("media", "tokens", filename)
        };

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return loadScaledIcon(candidate);
            }
        }

        // Fallback: if token images were dropped as unnamed screenshots in media/tokens,
        // map by file order to common colors used in this project.
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
                    return loadScaledIcon(pngs.get(idx));
                }
            }
        } catch (Exception ignored) {
            // Fall through to colored button fallback.
        }
        return null;
    }

    private ImageIcon loadScaledIcon(Path imagePath) {
        ImageIcon icon = new ImageIcon(imagePath.toString());
        Image src = icon.getImage();
        int size = 76;

        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(src, 0, 0, size, size, null);
        g2.dispose();
        return new ImageIcon(out);
    }

    private void applyTokenModeRules() {
        Map<GemColor, Integer> selected = selectedTokens();
        int total = selected.values().stream().mapToInt(Integer::intValue).sum();

        for (Map.Entry<GemColor, JButton> entry : tokenButtons.entrySet()) {
            GemColor color = entry.getKey();
            JButton button = entry.getValue();

            if (mode == Mode.TAKE_THREE) {
                boolean canUse = color != GemColor.GOLD && (selectedTokenCounts.get(color) > 0 || total < 3);
                button.setEnabled(canUse);
            } else if (mode == Mode.TAKE_TWO) {
                GemColor chosen = selected.size() == 1 ? selected.keySet().iterator().next() : null;
                boolean canUse = color != GemColor.GOLD && (chosen == null || chosen == color);
                button.setEnabled(canUse);
            } else {
                button.setEnabled(false);
            }
        }
        refreshTokenButtonLabels();
    }

    private void onTokenButtonClicked(GemColor color) {
        if (color == GemColor.GOLD) {
            return;
        }

        if (mode == Mode.TAKE_THREE) {
            int current = selectedTokenCounts.getOrDefault(color, 0);
            if (current == 0) {
                int total = selectedTokenCounts.values().stream().mapToInt(Integer::intValue).sum();
                if (total < 3) {
                    selectedTokenCounts.put(color, 1);
                }
            } else {
                selectedTokenCounts.put(color, 0);
            }
            return;
        }

        if (mode == Mode.TAKE_TWO) {
            if (selectedTokenCounts.getOrDefault(color, 0) == 2) {
                selectedTokenCounts.put(color, 0);
            } else {
                for (GemColor c : GemColor.values()) {
                    selectedTokenCounts.put(c, 0);
                }
                selectedTokenCounts.put(color, 2);
            }
        }
    }

    private void refreshTokenButtonLabels() {
        for (Map.Entry<GemColor, JButton> entry : tokenButtons.entrySet()) {
            GemColor color = entry.getKey();
            JButton button = entry.getValue();
            int selected = selectedTokenCounts.getOrDefault(color, 0);
            int bank = state.getBank().getTokenCount(color);

            if (tokenIcons.containsKey(color)) {
                button.setText("");
                button.setToolTipText(color.name() + " | bank: " + bank + " | selected: " + selected);
            } else {
                button.setText(color.name() + " (" + bank + ")" + (selected > 0 ? " x" + selected : ""));
            }

            if (selected > 0) {
                button.setBorder(new LineBorder(new Color(36, 92, 194), 3));
            } else {
                button.setBorder(new LineBorder(new Color(120, 120, 120), 1));
            }
        }
    }

    private String helpTextForMode() {
        return switch (mode) {
            case IDLE -> "Select an action to begin this turn. Legal options are enabled; illegal ones stay disabled.";
            case TAKE_THREE -> "Pick exactly 3 different colors (1 each). Confirm will enable only when valid.";
            case TAKE_TWO -> "Pick one color and set its spinner to 2. Confirm enables only if bank/rules allow.";
            case RESERVE -> "Click a legal market card to reserve it. You receive gold if available.";
            case BUY -> "Click a legal market card or choose a reserved card from the list. Green border means affordable.";
        };
    }

    private boolean hasAnyLegalTakeThree(Player player) {
        List<GemColor> normalColors = List.of(GemColor.WHITE, GemColor.BLUE, GemColor.GREEN, GemColor.RED, GemColor.BLACK);
        for (int i = 0; i < normalColors.size(); i++) {
            for (int j = i + 1; j < normalColors.size(); j++) {
                for (int k = j + 1; k < normalColors.size(); k++) {
                    Map<GemColor, Integer> tokens = new EnumMap<>(GemColor.class);
                    tokens.put(normalColors.get(i), 1);
                    tokens.put(normalColors.get(j), 1);
                    tokens.put(normalColors.get(k), 1);
                    if (validator.validate(state, player, Move.takeDifferent(tokens)) == null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasAnyLegalTakeTwo(Player player) {
        for (GemColor color : List.of(GemColor.WHITE, GemColor.BLUE, GemColor.GREEN, GemColor.RED, GemColor.BLACK)) {
            Map<GemColor, Integer> tokens = new EnumMap<>(GemColor.class);
            tokens.put(color, 2);
            if (validator.validate(state, player, Move.takeSame(tokens)) == null) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyLegalReserve(Player player) {
        for (DevelopmentCard card : state.getBoard().getFaceUpCards()) {
            if (validator.validate(state, player, Move.reserveFaceUp(card)) == null) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyLegalBuy(Player player) {
        for (DevelopmentCard card : state.getBoard().getFaceUpCards()) {
            Move move = Move.buy(card, computePaymentTokens(player, card.getCost()), false);
            if (validator.validate(state, player, move) == null) {
                return true;
            }
        }
        for (DevelopmentCard card : player.getReservedCards()) {
            Move move = Move.buy(card, computePaymentTokens(player, card.getCost()), true);
            if (validator.validate(state, player, move) == null) {
                return true;
            }
        }
        return false;
    }

    private String promptName(String prompt) {
        String name = JOptionPane.showInputDialog(this, prompt, "Player Setup", JOptionPane.QUESTION_MESSAGE);
        if (name == null || name.isBlank()) {
            return "Player";
        }
        return name.trim();
    }

    private boolean askYesNo(String prompt) {
        int result = JOptionPane.showConfirmDialog(
                this,
                prompt,
                "Game Setup",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    private AiController.Level askAiLevel() {
        Object choice = JOptionPane.showInputDialog(
                this,
                "Choose computer level",
                "Computer Difficulty",
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"HIGH", "LOW"},
                "HIGH"
        );
        if (choice == null) {
            return AiController.Level.HIGH;
        }
        return "LOW".equalsIgnoreCase(choice.toString())
                ? AiController.Level.LOW
                : AiController.Level.HIGH;
    }

    private int askPlayerCount() {
        Object choice = JOptionPane.showInputDialog(
                this,
                "How many human players?",
                "Player Count",
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"2", "3", "4"},
                "2"
        );
        if (choice == null) {
            return 2;
        }
        try {
            int count = Integer.parseInt(choice.toString());
            if (count < 2) return 2;
            if (count > 4) return 4;
            return count;
        } catch (NumberFormatException e) {
            return 2;
        }
    }

    private Config buildConfig() {
        java.nio.file.Path here = java.nio.file.Path.of(".");
        return new Config(
                15, 10, 3, 1, 2, 4, 3, 4,
                3, 4, 5,
                4, 5, 7, 5,
                3, 2, 2, 1,
                here, here, here, here,
                here, here, here
        );
    }

    private Map<Integer, List<DevelopmentCard>> buildDecks() {
        Path csv = Path.of("temporaryFolder", "Splendor Cards.csv");
        try {
            return new CardLoader().load(csv);
        } catch (IOException | IllegalArgumentException e) {
            log("Failed to load cards from CSV. Falling back to sample deck. Reason: " + e.getMessage());
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

    private List<NobleTile> buildNobles() {
        return List.of(
                noble(3, mapCost(3, 3, 3, 0, 0)),
                noble(3, mapCost(0, 3, 3, 3, 0)),
                noble(3, mapCost(0, 0, 3, 3, 3)),
                noble(3, mapCost(3, 0, 0, 3, 3))
        );
    }

    private DevelopmentCard card(int level, int points, GemColor bonus, Map<GemColor, Integer> costs) {
        model.Cost cost = new model.Cost();
        for (Map.Entry<GemColor, Integer> entry : costs.entrySet()) {
            cost.set(entry.getKey(), entry.getValue());
        }
        return new DevelopmentCard(level, points, bonus, cost);
    }

    private NobleTile noble(int points, Map<GemColor, Integer> reqs) {
        model.Cost cost = new model.Cost();
        for (Map.Entry<GemColor, Integer> entry : reqs.entrySet()) {
            cost.set(entry.getKey(), entry.getValue());
        }
        return new NobleTile(points, cost);
    }

    private Map<GemColor, Integer> mapCost(int w, int b, int g, int r, int k) {
        Map<GemColor, Integer> m = new EnumMap<>(GemColor.class);
        if (w > 0) m.put(GemColor.WHITE, w);
        if (b > 0) m.put(GemColor.BLUE, b);
        if (g > 0) m.put(GemColor.GREEN, g);
        if (r > 0) m.put(GemColor.RED, r);
        if (k > 0) m.put(GemColor.BLACK, k);
        return m;
    }

    private List<DevelopmentCard> shuffled(List<DevelopmentCard> cards) {
        List<DevelopmentCard> copy = new ArrayList<>(cards);
        Collections.shuffle(copy);
        return copy;
    }

    private void log(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private boolean isComputerTurn() {
        return computerPlayers.contains(state.getCurrentPlayer());
    }

    private void scheduleComputerTurnIfNeeded() {
        if (finalGameOver || !isComputerTurn() || computerThinking) {
            return;
        }
        computerThinking = true;
        phaseLabel.setText("Phase: COMPUTER TURN");

        Timer timer = new Timer(600, e -> {
            ((Timer) e.getSource()).stop();
            computerThinking = false;
            doComputerTurn();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void doComputerTurn() {
        if (finalGameOver || !isComputerTurn()) {
            return;
        }

        Player current = state.getCurrentPlayer();
        try {
            Move move = aiStrategy.chooseMove(state, current, validator, aiLevel);
            String err = validator.validate(state, current, move);
            if (err != null) {
                log("Computer produced illegal move: " + err);
                return;
            }

            executor.execute(state, current, move);
            String moveSummary = current.getName() + " (Computer-" + aiLevel + ") played: " + summarizeMove(move);
            latestComputerMoveLabel.setText("Latest Computer Move: " + moveSummary);
            log(moveSummary);
            JOptionPane.showMessageDialog(
                    this,
                    moveSummary,
                    "Computer Move",
                    JOptionPane.INFORMATION_MESSAGE
            );
            resolveTokenCapIfNeeded(current);
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
                JOptionPane.showMessageDialog(this,
                        "Game over.\nWinner: " + winner.getName() + " (" + winner.getPrestigePoints() + " points)",
                        "Winner",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            switchMode(Mode.IDLE);
            refreshAll();
        } catch (Exception ex) {
            log("Computer move failed: " + ex.getMessage());
        }
    }

    private String summarizeMove(Move move) {
        return switch (move.getType()) {
            case TAKE_THREE_DIFFERENT -> "Take 3 different " + move.getTokens();
            case TAKE_TWO_SAME -> "Take 2 same " + move.getTokens();
            case RESERVE -> "Reserve card " + move.getCard();
            case BUY -> "Buy card " + move.getCard() + (move.isFromReserved() ? " (reserved)" : " (market)");
        };
    }
}
