package network;

import ui.swing.MultiplayerSwingApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Entry point for multiplayer client.
 * Shows connection dialog and launches the multiplayer UI.
 */
public class ClientMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            showConnectionDialog();
        });
    }

    public static void showConnectionDialog() {
        showConnectionDialog("localhost", "12345");
    }

    public static void showConnectionDialog(String defaultHost, String defaultPort) {
        JFrame dialog = new JFrame("Connect to Splendor Game");
        dialog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Host IP:"), gbc);
        gbc.gridx = 1;
        JTextField hostField = new JTextField(defaultHost, 15);
        panel.add(hostField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        JTextField portField = new JTextField(defaultPort, 15);
        panel.add(portField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Your Name:"), gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField("Player", 15);
        panel.add(nameField, gbc);

        JButton connectButton = new JButton("Connect");
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(connectButton, gbc);

        connectButton.addActionListener(e -> {
            String host = hostField.getText().trim();
            String portStr = portField.getText().trim();
            String name = nameField.getText().trim();

            if (host.isEmpty() || portStr.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int port = Integer.parseInt(portStr);
                GameClient client = new GameClient(host, port, name);
                if (client.connect()) {
                    dialog.dispose();
                    new MultiplayerSwingApp(client).setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(dialog, "Failed to connect to server", "Connection Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid port number", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(panel);
        dialog.setVisible(true);
    }
}