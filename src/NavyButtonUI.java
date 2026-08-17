import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

public final class NavyButtonUI extends BasicButtonUI {
    private static final Color NAVY = new Color(26, 46, 64);
    private static final Color HOVER = new Color(36, 60, 84);

    public static ComponentUI createUI(JComponent component) { return new NavyButtonUI(); }

    @Override public void installDefaults(AbstractButton button) {
        super.installDefaults(button);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
    }

    @Override public void paint(Graphics graphics, JComponent component) {
        AbstractButton button = (AbstractButton) component;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ButtonModel model = button.getModel();
            g.setColor(!button.isEnabled() ? new Color(92, 106, 120)
                    : model.isPressed() ? NAVY.darker()
                    : model.isRollover() ? HOVER : NAVY);
            g.fillRoundRect(0, 0, component.getWidth() - 1, component.getHeight() - 1, 16, 16);
        } finally { g.dispose(); }
        super.paint(graphics, component);
    }
}
