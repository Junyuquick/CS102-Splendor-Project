import javax.swing.SwingUtilities;


//DONT use VSCODE shortcut to run this code
//run from compileRunShortcut.sh file, else will have a lot clutter
public class SwingMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ui.swing.SwingSplendorApp app = new ui.swing.SwingSplendorApp();
            app.setVisible(true);
        });
    }
}
