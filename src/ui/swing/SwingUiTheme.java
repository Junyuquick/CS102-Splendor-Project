package ui.swing;

import model.GemColor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Font;

/**
 * Centralized colors, dimensions, and styling helpers for the Swing user interface.
 */
final class SwingUiTheme {
    static final int DEV_CARD_ICON_WIDTH = 96;
    static final int DEV_CARD_ICON_HEIGHT = 134;
    static final int DEV_CARD_BUTTON_WIDTH = 108;
    static final int DEV_CARD_BUTTON_HEIGHT = 146;
    static final int NOBLE_ICON_SIZE = 110;
    static final int TOKEN_BUTTON_SIZE = 60;
    static final int TOKEN_ICON_SIZE = 48;
    static final int NOBLE_CARD_WIDTH = 122;
    static final int NOBLE_CARD_HEIGHT = 122;

    static final Color APP_BG = new Color(82, 87, 94);
    static final Color PANEL_BG = new Color(100, 106, 114);
    static final Color PANEL_BG_ALT = new Color(116, 123, 132);
    static final Color SURFACE_BG = new Color(132, 139, 148);
    static final Color EMPTY_BG = new Color(148, 154, 162);
    static final Color TEXT_PRIMARY = new Color(232, 236, 241);
    static final Color TEXT_MUTED = new Color(182, 190, 200);
    static final Color BORDER_COLOR = new Color(92, 102, 114);
    static final Color ACCENT_BLUE = new Color(78, 144, 255);
    static final Color ACCENT_GREEN = new Color(60, 179, 113);
    static final Color ACCENT_RED = new Color(212, 88, 88);
    static final Color SELECTED_BG = new Color(52, 71, 96);

    private SwingUiTheme() {
    }

    /**
     * Creates a standard titled border used throughout the application.
     *
     * @param title border title text
     * @return styled titled border
     */
    static TitledBorder createTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(new LineBorder(BORDER_COLOR, 1), title);
        styleTitledBorder(border);
        return border;
    }

    /**
     * Creates the border used for player summary panels.
     *
     * @param title panel title
     * @param active whether the player is currently active
     * @return styled border
     */
    static TitledBorder createPlayerBorder(String title, boolean active) {
        return BorderFactory.createTitledBorder(
                new LineBorder(active ? ACCENT_BLUE : BORDER_COLOR, active ? 2 : 1),
                title,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 15),
                active ? ACCENT_BLUE : TEXT_PRIMARY
        );
    }

    /**
     * Applies the shared title styling used by themed borders.
     *
     * @param border border to style
     */
    static void styleTitledBorder(TitledBorder border) {
        border.setTitleColor(TEXT_PRIMARY);
    }

    /**
     * Applies the standard action-button appearance.
     *
     * @param button button to style
     */
    static void styleActionButton(JButton button) {
        button.setFocusPainted(false);
        button.setBackground(SURFACE_BG);
        button.setForeground(TEXT_PRIMARY);
        button.setOpaque(true);
        button.setBorder(new LineBorder(BORDER_COLOR, 1, true));
    }

    /**
     * Styles a scroll pane so it matches the application's panel colors.
     *
     * @param scrollPane scroll pane to style
     * @param viewportColor background color for the viewport
     */
    static void styleScrollPane(JScrollPane scrollPane, Color viewportColor) {
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(viewportColor);
        scrollPane.setBackground(viewportColor);
    }

    /**
     * Returns the display color used for a token of the given gem color.
     *
     * @param color gem color
     * @return token display color
     */
    static Color tokenColor(GemColor color) {
        return switch (color) {
            case WHITE -> new Color(240, 240, 240);
            case BLUE -> new Color(66, 133, 244);
            case GREEN -> new Color(52, 168, 83);
            case RED -> new Color(234, 67, 53);
            case BLACK -> new Color(60, 60, 60);
            case GOLD -> new Color(251, 188, 5);
        };
    }

    /**
     * Returns the background color used when rendering a card that grants the given bonus.
     *
     * @param color bonus color
     * @return display color for that bonus type
     */
    static Color colorForBonus(GemColor color) {
        return switch (color) {
            case WHITE -> new Color(245, 245, 245);
            case BLUE -> new Color(206, 225, 255);
            case GREEN -> new Color(208, 242, 214);
            case RED -> new Color(255, 214, 214);
            case BLACK -> new Color(222, 222, 222);
            case GOLD -> new Color(255, 244, 173);
        };
    }
}
