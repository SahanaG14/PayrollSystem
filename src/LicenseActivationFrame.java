import javax.swing.*;
import java.awt.*;

/** One-time local activation UI shown before account creation and login. */
public final class LicenseActivationFrame extends JFrame {
    private final JTextField key = new JTextField(30);
    private final JLabel status = new JLabel(" ");

    public LicenseActivationFrame() {
        super(Branding.APPLICATION_NAME + " - License Activation");
        Branding.applyWindowIcon(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 8, 7, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        panel.add(new JLabel(Branding.logoIcon(440, 228), SwingConstants.CENTER), c);

        c.gridy++;
        JLabel title = new JLabel(Branding.APPLICATION_NAME, SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        panel.add(title, c);

        c.gridy++;
        JLabel activation = new JLabel("License Activation", SwingConstants.CENTER);
        activation.setFont(activation.getFont().deriveFont(Font.BOLD, 18f));
        panel.add(activation, c);

        c.gridwidth = 1;
        c.gridy++;
        panel.add(new JLabel("License Key"), c);
        c.gridx = 1;
        panel.add(key, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        status.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(status, c);

        c.gridy++;
        JButton activate = new JButton("Activate");
        activate.addActionListener(e -> activate());
        panel.add(activate, c);
        getRootPane().setDefaultButton(activate);

        add(panel);
        pack();
        setMinimumSize(new Dimension(640, 480));
        setLocationRelativeTo(null);
    }

    private void activate() {
        LicenseService.Result result = LicenseService.activate(key.getText());
        if (!result.allowed()) {
            status.setText(result.message());
            return;
        }
        dispose();
        Main.startApplication();
    }
}
