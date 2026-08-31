import javax.swing.*;
import java.awt.*;

public final class LicenseActivationFrame extends JFrame {
    private final JTextField key = new JTextField(28);
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    public LicenseActivationFrame() {
        super("Activate Payroll System"); setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel panel = new JPanel(new GridBagLayout()); panel.setBorder(BorderFactory.createEmptyBorder(36, 48, 36, 48));
        GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(8, 8, 8, 8); c.fill = GridBagConstraints.HORIZONTAL; c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        JLabel title = new JLabel("Activate Payroll System", SwingConstants.CENTER); title.setFont(title.getFont().deriveFont(Font.BOLD, 24f)); panel.add(title, c);
        c.gridy++; panel.add(new JLabel("Enter the lifetime license key supplied with your purchase.", SwingConstants.CENTER), c);
        c.gridy++; c.gridwidth = 1; panel.add(new JLabel("License key"), c); c.gridx = 1; panel.add(key, c);
        c.gridx = 0; c.gridy++; c.gridwidth = 2; status.setForeground(Color.RED); panel.add(status, c);
        c.gridy++; JButton activate = new JButton("Activate"); activate.addActionListener(e -> activate()); panel.add(activate, c); getRootPane().setDefaultButton(activate);
        add(panel); pack(); setMinimumSize(new Dimension(630, 260)); setLocationRelativeTo(null);
    }
    private void activate() {
        String enteredKey = key.getText(); key.setEnabled(false); status.setForeground(Color.DARK_GRAY); status.setText("Contacting licensing server...");
        new Thread(() -> { LicenseService.Result result = LicenseService.activate(enteredKey); SwingUtilities.invokeLater(() -> {
            if (result.allowed()) { dispose(); new Thread(Main::startApplication, "Payroll-Startup").start(); }
            else { key.setEnabled(true); status.setForeground(Color.RED); status.setText(result.message()); }
        }); }, "License-Activation").start();
    }
}
