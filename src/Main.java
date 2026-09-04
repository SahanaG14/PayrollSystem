import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import javax.swing.UIManager;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Window;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    private static final AtomicBoolean LICENSE_LOCKOUT = new AtomicBoolean();
    private static LicenseValidationMonitor licenseMonitor;
    public static void main(String[] args) {
        if (!LicenseService.isConfigured()) {
            javax.swing.JOptionPane.showMessageDialog(null, "This installation has no licensing server configured.\nSet -Dpayroll.license.url=https://your-worker.workers.dev before distributing it.", Branding.APPLICATION_NAME+" Licensing setup required", javax.swing.JOptionPane.ERROR_MESSAGE);
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
        stopLicenseMonitoring();
        LICENSE_LOCKOUT.set(false);
        DatabaseInitializer.initialize();
        AutoSaveService.start();
        LicenseService.startHeartbeat();
        Runtime.getRuntime().addShutdownHook(new Thread(AutoSaveService::shutdown,"Payroll-Recovery-Flush"));
        // Start before any startup window is shown so Login and first-time setup are protected too.
        startLicenseMonitoring();
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
            if(UserAuthentication.hasUsers())openLogin();else new InitialAdminSetupFrame().setVisible(true);
        });
    }
    static void openLogin() { new LoginFrame().setVisible(true); }
    static synchronized void revalidateLicenseWhenFocused() { if (licenseMonitor != null) licenseMonitor.checkNow(); }
    private static synchronized void startLicenseMonitoring() { licenseMonitor = new LicenseValidationMonitor(Main::lockForInvalidLicense, Main::lockForUnavailableLicense); licenseMonitor.start(); }
    private static synchronized void stopLicenseMonitoring() { if (licenseMonitor != null) { licenseMonitor.stop(); licenseMonitor = null; } }
    /** Called by the last visible application window before a normal application exit. */
    static void applicationClosing() { stopLicenseMonitoring(); }
    private static void lockForInvalidLicense() {
        lockAndReturnToActivation("Your licence has been revoked or is no longer active. Please contact YASL Support.");
    }
    private static void lockForUnavailableLicense() {
        lockAndReturnToActivation("Licence verification could not be completed during the permitted offline period. Connect to the internet and activate again.");
    }
    private static void lockAndReturnToActivation(String message) {
        SwingUtilities.invokeLater(() -> {
            if (!LICENSE_LOCKOUT.compareAndSet(false, true)) return;
            stopLicenseMonitoring();
            for (Window window : Window.getWindows()) if (window.isDisplayable()) window.dispose();
            JOptionPane.showMessageDialog(null, message, Branding.APPLICATION_NAME, JOptionPane.ERROR_MESSAGE);
            new LicenseActivationFrame().setVisible(true);
        });
    }
}
