import config.ConfigSupport;
import java.awt.GraphicsEnvironment;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import network.ClientMain;

/**
 * Launcher that lets the user choose between single-player and multiplayer
 * modes.
 */
public class Main {
    /**
     * Application entry point that starts the Swing launcher flow.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(
                    "Splendor requires a graphical desktop environment to "
                    + "launch the Swing UI."
            );
            System.exit(1);
            return;
        }

        // Run UI creation on Swing's Event Dispatch Thread (EDT) for thread safety.
        SwingUtilities.invokeLater(Main::showModeDialog);
    }

    /**
     * Prompts the user to launch single-player or multiplayer mode.
     */
    private static void showModeDialog() {
        while (true) {
            String[] options = {"Single Player", "Multiplayer", "Exit"};
            int selected = showOptionDialog(
                    "Choose game mode",
                    "Splendor",
                    options
            );

            switch (selected) {
                case 0 -> {
                    ui.swing.SingleplayerSwingApp app =
                            new ui.swing.SingleplayerSwingApp();
                    app.setVisible(true); // Display the JFrame window.
                    return;
                }
                case 1 -> {
                    if (showMultiplayerModeDialog()) {
                        return;
                    }
                }
                case 2 -> {
                    System.exit(0);
                    return;
                }
                default -> {
                    System.exit(0);
                    return;
                }
            }
        }
    }

    /**
     * Shows multiplayer mode choices.
     *
     * @return true when a game flow is launched; false when user goes back
     */
    private static boolean showMultiplayerModeDialog() {
        while (true) {
            String[] options = {
                    "Multiplayer over Network",
                    "Multiplayer on Same Laptop",
                    "Back"
            };
            int selected = showOptionDialog(
                    "Choose multiplayer mode",
                    "Multiplayer",
                    options
            );

            switch (selected) {
                case 0 -> {
                    if (showNetworkModeDialog()) {
                        return true;
                    }
                }
                case 1 -> {
                    launchSameLaptopMultiplayer();
                    return true;
                }
                case 2 -> {
                    return false;
                }
                default -> {
                    System.exit(0);
                    return true;
                }
            }
        }
    }

    /**
     * Shows host/join options for network multiplayer.
     *
     * @return true when a network flow is launched; false when user goes back
     */
    private static boolean showNetworkModeDialog() {
        while (true) {
            String[] options = {"Host Server", "Join Server", "Back"};
            int selected = showOptionDialog(
                    "Choose network mode",
                    "Multiplayer over Network",
                    options
            );

            switch (selected) {
                case 0 -> {
                    ClientMain.showHostConnectionDialog();
                    return true;
                }
                case 1 -> {
                    ClientMain.showConnectionDialog();
                    return true;
                }
                case 2 -> {
                    return false;
                }
                default -> {
                    System.exit(0);
                    return true;
                }
            }
        }
    }

    /**
     * Prompts for same-laptop player count and launches a local all-human game.
     */
    private static void launchSameLaptopMultiplayer() {
        config.Config config = ConfigSupport.loadDefaultConfig();
        int minPlayers = config.getMinPlayers();
        int maxPlayers = config.getMaxPlayers();

        Object[] options = new Object[maxPlayers - minPlayers + 1];
        for (int i = minPlayers; i <= maxPlayers; i++) {
            options[i - minPlayers] = String.valueOf(i);
        }

        Object selected = JOptionPane.showInputDialog( // Shows a dropdown/input dialog and returns the selected value.
                null,
                "How many players will use this laptop?",
                "Multiplayer on Same Laptop",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (selected == null) {
            System.exit(0);
            return;
        }

        try {
            int playerCount = Integer.parseInt(selected.toString());
            ui.swing.SingleplayerSwingApp app =
                    new ui.swing.SingleplayerSwingApp(playerCount);
            app.setVisible(true); // Display the JFrame window.
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog( // Shows a simple popup message dialog.
                    null,
                    "Invalid player count: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Shows a reusable option dialog and returns the selected button index.
     *
     * @param message message shown in the dialog body
     * @param title dialog window title
     * @param options button labels presented to the user
     * @return selected option index, or -1 if the dialog is closed
     */
    private static int showOptionDialog(
            String message,
            String title,
            String[] options
    ) {
        return JOptionPane.showOptionDialog( // Shows custom option buttons and returns clicked index.
                null,
                message,
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );
    }
}
