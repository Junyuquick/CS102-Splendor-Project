import network.ClientMain;
import network.GameServer;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

// DONT use VSCODE shortcut to run this code
// run from compileRunShortcut.sh file, else will have a lot clutter
public class SwingMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(SwingMain::showModeDialog);
    }

    private static void showModeDialog() {
        String[] options = {"Single Player", "Host Multiplayer", "Join Multiplayer", "Exit"};
        int selected = JOptionPane.showOptionDialog(
                null,
                "Choose game mode",
                "Splendor",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );

        switch (selected) {
            case 0:
                ui.swing.SwingSplendorApp app = new ui.swing.SwingSplendorApp();
                app.setVisible(true);
                break;
            case 1:
                String portStr = JOptionPane.showInputDialog(null, "Multiplayer server port:", "12345");
                if (portStr == null || portStr.isEmpty()) return;
                try {
                    int port = Integer.parseInt(portStr.trim());
                    Thread serverThread = new Thread(() -> {
                        try {
                            new GameServer(port).start();
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(null, "Failed to start server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }, "Splendor-Server");
                    serverThread.setDaemon(true);
                    serverThread.start();
                    // small delay to ensure server is up
                    Thread.sleep(200);
                    ClientMain.showConnectionDialog("localhost", String.valueOf(port));
                } catch (NumberFormatException | InterruptedException e) {
                    JOptionPane.showMessageDialog(null, "Invalid port: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
                break;
            case 2:
                ClientMain.showConnectionDialog();
                break;
            default:
                System.exit(0);
        }
    }
}
