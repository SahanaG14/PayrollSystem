import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        if (!LicenseService.isConfigured()) {
            javax.swing.JOptionPane.showMessageDialog(null, "This installation has no licensing server configured.\nSet -Dpayroll.license.url=https://your-worker.workers.dev before distributing it.", "Licensing setup required", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }
        LicenseService.Result license = LicenseService.validateSavedLicense();
        if (!license.allowed()) {
            EventQueue.invokeLater(() -> new LicenseActivationFrame().setVisible(true));
            return;
        }
        startApplication();
    }
    static void startApplication() {
        DatabaseInitializer.initialize();
        AutoSaveService.start();
        Runtime.getRuntime().addShutdownHook(new Thread(AutoSaveService::shutdown,"Payroll-Recovery-Flush"));
        EventQueue.invokeLater(() -> {
            System.setProperty("filechooser.useShellFolder", "false");
            System.setProperty("FileChooser.useShellFolder", "false");
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { }
            UIManager.put("Button.background", new Color(26, 46, 64));
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 12));
            UIManager.put("Button.opaque", Boolean.TRUE);
            UIManager.put("Button.darcula.background", new Color(26, 46, 64));
            UIManager.put("Button.focusedBackground", new Color(36, 60, 84));
            UIManager.put("ButtonUI", "NavyButtonUI");
            UIManager.put("ComboBoxUI", "NavyComboBoxUI");
            UIManager.put("TabbedPaneUI", "NavyTabbedPaneUI");
            UIStyleUtility.installGlobalButtonDefaults();
            UIManager.put("Label.font", new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 18));
            UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 12));
            UIManager.put("TextField.font", new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 18));
            UIManager.put("ComboBox.font", new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 18));
            UIManager.put("TabbedPane.font", new java.awt.Font("SansSerif", java.awt.Font.BOLD, 18));
            UIManager.put("Table.font", new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 18));
            UIManager.put("TableHeader.font", new java.awt.Font("SansSerif", java.awt.Font.BOLD, 18));
            openLogin();
        });
    }
    static void openLogin() { new LoginFrame().setVisible(true); }
}
