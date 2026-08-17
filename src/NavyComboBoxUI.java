import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;

public final class NavyComboBoxUI extends BasicComboBoxUI {
    private static final Color NAVY = new Color(26, 46, 64);

    @Override public void installUI(JComponent component) {
        super.installUI(component);
        comboBox.setBackground(NAVY);
        comboBox.setForeground(Color.WHITE);
        comboBox.setBorder(BorderFactory.createLineBorder(NAVY.darker(), 1, true));
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                boolean display = index < 0;
                label.setOpaque(true);
                label.setBackground(display ? NAVY : selected ? new Color(36, 60, 84) : Color.WHITE);
                label.setForeground(display || selected ? Color.WHITE : Color.BLACK);
                label.setBorder(BorderFactory.createEmptyBorder(4, 9, 4, 9));
                return label;
            }
        });
    }

    @Override protected JButton createArrowButton() {
        JButton arrow = new JButton("\u25BE");
        arrow.setFont(new Font("Segoe UI Symbol", Font.BOLD, 14));
        arrow.setForeground(Color.WHITE);
        arrow.setBackground(NAVY);
        arrow.setOpaque(true);
        arrow.setContentAreaFilled(true);
        arrow.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        arrow.setFocusPainted(false);
        return arrow;
    }
}
