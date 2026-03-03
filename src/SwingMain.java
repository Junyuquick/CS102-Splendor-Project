import javax.swing.SwingUtilities;

public class SwingMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ui.swing.SwingSplendorApp app = new ui.swing.SwingSplendorApp();
            app.setVisible(true);
        });
    }
}
