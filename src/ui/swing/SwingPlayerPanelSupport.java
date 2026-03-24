package ui.swing;

import config.Config;
import model.GameState;
import model.Player;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;

final class SwingPlayerPanelSupport {
    private SwingPlayerPanelSupport() {
    }

    static void initialiseSinglePlayerPanels(
            AbstractSwingSplendorFrame frame,
            JPanel playersPanel,
            GameState state,
            Map<Player, JPanel> playerCardPanels,
            Map<Player, JEditorPane> playerCardAreas
    ) {
        playersPanel.removeAll();
        playerCardPanels.clear();
        playerCardAreas.clear();
        playersPanel.setLayout(new java.awt.GridLayout(state.getPlayers().size(), 1, 8, 8));

        for (Player player : state.getPlayers()) {
            JPanel panel = frame.createPlayerPanel(player.getName(), false);
            JEditorPane area = frame.createPlayerHtmlArea();
            JScrollPane areaScroll = new JScrollPane(area);
            SwingUiTheme.styleScrollPane(areaScroll, SwingUiTheme.PANEL_BG_ALT);
            panel.add(areaScroll, BorderLayout.CENTER);
            playerCardPanels.put(player, panel);
            playerCardAreas.put(player, area);
            playersPanel.add(panel);
        }
    }

    static void refreshSinglePlayerPanels(
            Config config,
            Player current,
            Map<Player, JPanel> playerCardPanels,
            Map<Player, JEditorPane> playerCardAreas
    ) {
        for (Map.Entry<Player, JEditorPane> entry : playerCardAreas.entrySet()) {
            Player player = entry.getKey();
            JEditorPane area = entry.getValue();
            area.setText(SwingPlayerSummaryFormatter.buildRichHtml(player, config));
            area.setCaretPosition(0);

            JPanel panel = playerCardPanels.get(player);
            if (panel == null) {
                continue;
            }
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

    static void renderLobbyPlayers(
            AbstractSwingSplendorFrame frame,
            JPanel playersPanel,
            List<String> lobbyPlayers,
            int hostPlayerIndex,
            int myPlayerIndex,
            int minPlayersToStart
    ) {
        playersPanel.removeAll();
        playersPanel.setLayout(new java.awt.GridLayout(Math.max(1, lobbyPlayers.size()), 1, 8, 8));
        for (int i = 0; i < lobbyPlayers.size(); i++) {
            String title = lobbyPlayers.get(i);
            if (i == hostPlayerIndex) {
                title += " (Host)";
            }
            if (i == myPlayerIndex) {
                title += " (You)";
            }
            JPanel panel = frame.createPlayerPanel(title, false);
            JTextArea area = new JTextArea();
            area.setEditable(false);
            area.setOpaque(false);
            area.setForeground(SwingUiTheme.TEXT_PRIMARY);
            area.setText(i == hostPlayerIndex
                    ? "Can start the match when at least " + minPlayersToStart + " players are connected."
                    : "Waiting in lobby for the host to start the match.");
            panel.add(area, BorderLayout.CENTER);
            playersPanel.add(panel);
        }
        playersPanel.revalidate();
        playersPanel.repaint();
    }

    static void renderMultiplayerPlayers(AbstractSwingSplendorFrame frame, JPanel playersPanel, GameState state) {
        playersPanel.removeAll();
        playersPanel.setLayout(new java.awt.GridLayout(state.getPlayers().size(), 1, 8, 8));
        for (int i = 0; i < state.getPlayers().size(); i++) {
            Player player = state.getPlayer(i);
            String title = (i == state.getCurrentPlayerIndex() ? "* " : "") + player.getName();
            JPanel panel = frame.createPlayerPanel(title, i == state.getCurrentPlayerIndex());

            JEditorPane area = new JEditorPane();
            area.setEditable(false);
            area.setText(SwingPlayerSummaryFormatter.buildCompactText(player));
            area.setBackground(SwingUiTheme.PANEL_BG_ALT);
            area.setForeground(SwingUiTheme.TEXT_PRIMARY);
            JScrollPane scrollPane = new JScrollPane(area);
            SwingUiTheme.styleScrollPane(scrollPane, SwingUiTheme.PANEL_BG_ALT);
            panel.add(scrollPane, BorderLayout.CENTER);
            playersPanel.add(panel);
        }
        playersPanel.revalidate();
        playersPanel.repaint();
    }
}
