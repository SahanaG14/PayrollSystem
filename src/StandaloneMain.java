import java.awt.*;import javax.swing.*;
/** Offline installer entry point: deliberately contains no licensing or network checks. */
public final class StandaloneMain {
    public static void main(String[] args) {
        DatabaseInitializer.initialize(); AutoSaveService.start();
        Runtime.getRuntime().addShutdownHook(new Thread(AutoSaveService::shutdown,"Payroll-Recovery-Flush"));
        EventQueue.invokeLater(() -> { System.setProperty("filechooser.useShellFolder","false");System.setProperty("FileChooser.useShellFolder","false");try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception ignored){}UIManager.put("Button.background",new Color(26,46,64));UIManager.put("Button.foreground",Color.WHITE);UIManager.put("Button.font",new Font("Segoe UI",Font.BOLD,12));UIManager.put("ButtonUI","NavyButtonUI");UIManager.put("ComboBoxUI","NavyComboBoxUI");UIManager.put("TabbedPaneUI","NavyTabbedPaneUI");UIStyleUtility.installGlobalButtonDefaults();UIManager.put("Label.font",new Font("SansSerif",Font.PLAIN,18));UIManager.put("TextField.font",new Font("SansSerif",Font.PLAIN,18));UIManager.put("ComboBox.font",new Font("SansSerif",Font.PLAIN,18));UIManager.put("TabbedPane.font",new Font("SansSerif",Font.BOLD,18));UIManager.put("Table.font",new Font("SansSerif",Font.PLAIN,18));UIManager.put("TableHeader.font",new Font("SansSerif",Font.BOLD,18));new LoginFrame().setVisible(true); });
    }
    private StandaloneMain() { }
}
