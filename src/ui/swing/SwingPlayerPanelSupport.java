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

/**
 * Helper methods for building and refreshing player-summary panels in the Swing UI.
 */
final class SwingPlayerPanelSupport {
    /**
     * Utility class; not instantiable.
     */
    private SwingPlayerPanelSupport() {
    }

    /**
     * Creates the player panels used by the single-player interface.
     *
     * @param frame owning frame that supplies shared panel factories
     * @param playersPanel container that holds all player panels
     * @param state current game state
     * @param playerCardPanels map that stores each player's panel
     * @param playerCardAreas map that stores each player's summary area
     */
    static void initialiseSinglePlayerPanels(
            AbstractSwingSplendorFrame frame,
            JPanel playersPanel,
            GameState state,
            Map<Player, JPanel> playerCardPanels,
            Map<Player, JEditorPane> playerCardAreas
    ) {
        playersPanel.removeAll(); // Remove existing child components before rebuilding.
        playerCardPanels.clear();
        playerCardAreas.clear();
        playersPanel.setLayout(new java.awt.GridLayout(state.getPlayers().size(), 1, 8, 8)); // Arrange player panels in a vertical grid.

        for (Player player : state.getPlayers()) {
            JPanel panel = frame.createPlayerPanel(player.getName(), false);
            JEditorPane area = frame.createPlayerHtmlArea();
            JScrollPane areaScroll = new JScrollPane(area);
            SwingUiTheme.styleScrollPane(areaScroll, SwingUiTheme.PANEL_BG_ALT);
            panel.add(areaScroll, BorderLayout.CENTER); // Place scroll area in panel center region.
            playerCardPanels.put(player, panel);
            playerCardAreas.put(player, area);
            playersPanel.add(panel);
        }
    }

    /**
     * Refreshes the single-player summary panels after game-state changes.
     *
     * @param config game configuration used when formatting summaries
     * @param current player whose turn is active
     * @param playerCardPanels map of players to their panels
     * @param playerCardAreas map of players to their formatted text areas
     */
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
            area.setCaretPosition(0); // Scroll text view back to top.

            JPanel panel = playerCardPanels.get(player);
            if (panel == null) {
                continue;
            }
            if (player == current) {
                panel.setBorder(BorderFactory.createTitledBorder( // Build a titled border around the player panel.
                        new LineBorder(SwingUiTheme.ACCENT_BLUE, 2),
                        player.getName() + " (Active)"
                ));
                SwingUiTheme.styleTitledBorder((TitledBorder) panel.getBorder());
            } else {
                panel.setBorder(SwingUiTheme.createTitledBorder(player.getName()));
            }
        }
    }

    /**
     * Renders the multiplayer lobby view before a game has started.
     *
     * @param frame owning frame that supplies shared panel factories
     * @param playersPanel container that holds lobby panels
     * @param lobbyPlayers connected player names in lobby order
     * @param hostPlayerIndex index of the host player
     * @param myPlayerIndex index of the local player
     * @param minPlayersToStart minimum players required to start
     */
    static void renderLobbyPlayers(
            AbstractSwingSplendorFrame frame,
            JPanel playersPanel,
            List<String> lobbyPlayers,
            int hostPlayerIndex,
            int myPlayerIndex,
            int minPlayersToStart
    ) {
        playersPanel.removeAll(); // Clear old lobby cards.
        playersPanel.setLayout(new java.awt.GridLayout(Math.max(1, lobbyPlayers.size()), 1, 8, 8)); // One row per lobby player.
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
            area.setEditable(false); // Read-only status text.
            area.setOpaque(false);
            area.setForeground(SwingUiTheme.TEXT_PRIMARY);
            area.setText(i == hostPlayerIndex
                    ? "Can start the match when at least " + minPlayersToStart + " players are connected."
                    : "Waiting in lobby for the host to start the match.");
            panel.add(area, BorderLayout.CENTER);
            playersPanel.add(panel);
        }
        playersPanel.revalidate(); // Re-run layout after rebuilding lobby panels.
        playersPanel.repaint(); // Request redraw.
    }

}
