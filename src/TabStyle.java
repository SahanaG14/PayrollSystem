import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/** Consistent bordered and selected-state tabs for every module and sub-module. */
public final class TabStyle {
    private TabStyle() { }

    public static void apply(Component root) {
        if (root instanceof JTable table) configureTable(table);
        if (root instanceof JButton button && !Boolean.TRUE.equals(button.getClientProperty("navButton"))) styleActionButton(button);
        if (root instanceof JTabbedPane tabs && !(tabs.getUI() instanceof BlackBoxTabsUI)) {
            // Period selectors are deliberately compact, so their tabs remain
            // visible together instead of showing scroll controls.
            tabs.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
            tabs.setUI(new BlackBoxTabsUI());
            tabs.setFont(tabs.getFont().deriveFont(Font.BOLD));
        }
        if (root instanceof Container container) for (Component child : container.getComponents()) apply(child);
    }

    public static void configureTable(JTable table) {
        UIStyleUtility.applyProfessionalTableStyle(table);
        table.setCellSelectionEnabled(true);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(184, 207, 229));
        table.setSelectionForeground(Color.BLACK);
        table.setDefaultRenderer(Number.class, numericRenderer());
    }

    public static DefaultTableCellRenderer numericRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focused, row, column);
                setHorizontalAlignment(SwingConstants.RIGHT);
                return this;
            }
        };
    }

    public static void styleActionButton(JButton button) {
        button.setFont(new Font("SansSerif", Font.BOLD, 15));
        button.setBackground(new Color(26, 46, 64));
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.putClientProperty("JButton.buttonType", "normal");
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyleUtility.NAVY.darker()),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
    }

    private static final class BlackBoxTabsUI extends BasicTabbedPaneUI {
        @Override protected void installDefaults() {
            super.installDefaults();
            tabAreaInsets = new Insets(3, 3, 3, 3);
            tabInsets = new Insets(8, 12, 8, 12);
            selectedTabPadInsets = new Insets(8, 12, 8, 12);
            contentBorderInsets = new Insets(1, 1, 1, 1);
        }

        @Override protected void paintTabBackground(Graphics graphics, int placement, int index, int x, int y, int width, int height, boolean selected) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = getRolloverTab() == index;
                g.setColor(selected ? new Color(45, 91, 145) : hover ? new Color(36, 60, 84) : UIStyleUtility.NAVY);
                g.fillRoundRect(x, y + 1, width - 1, height - 2, 14, 14);
            } finally { g.dispose(); }
        }

        @Override protected void paintTabBorder(Graphics graphics, int placement, int index, int x, int y, int width, int height, boolean selected) {
            graphics.setColor(UIStyleUtility.NAVY.darker());
            graphics.drawRoundRect(x, y + 1, width - 1, height - 2, 14, 14);
        }

        @Override protected void paintText(Graphics graphics, int placement, Font font, FontMetrics metrics, int index, String title, Rectangle textRect, boolean selected) {
            graphics.setFont(font.deriveFont(Font.BOLD));
            graphics.setColor(Color.WHITE);
            BasicGraphicsUtils.drawStringUnderlineCharAt(graphics, title, tabPane.getDisplayedMnemonicIndexAt(index), textRect.x, textRect.y + metrics.getAscent());
        }

        @Override protected boolean shouldPadTabRun(int tabPlacement, int run) {
            // Do not stretch a wrapped run across the full pane width.  That
            // behaviour made APR–MAR split into uneven rows instead of using
            // their compact, one-line month-selector layout.
            return false;
        }

        @Override protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
            // Month selector labels are intentionally fixed-width.  BasicTabbedPane
            // otherwise expands a selected APR/MAY/JUN tab and forces a second row.
            String title = tabPane.getTitleAt(tabIndex);
            if (title != null && title.length() == 3 && tabPane.getTabComponentAt(tabIndex) instanceof JLabel) return 80;
            return super.calculateTabWidth(tabPlacement, tabIndex, metrics);
        }

        @Override protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
            return Math.max(38, super.calculateTabHeight(tabPlacement, tabIndex, fontHeight));
        }

        @Override protected void paintContentBorder(Graphics graphics, int placement, int selectedIndex) {
            graphics.setColor(Color.BLACK);
            Rectangle bounds = tabPane.getBounds();
            Insets insets = tabPane.getInsets();
            int tabBottom = insets.top + calculateTabAreaHeight(placement, runCount, maxTabHeight);
            // A single uninterrupted divider makes the month selector clearly
            // distinct from the controls and grid below it.
            graphics.drawLine(insets.left, tabBottom, bounds.width - insets.right - 1, tabBottom);
            graphics.drawRect(insets.left, tabBottom, bounds.width - insets.left - insets.right - 1, bounds.height - insets.top - insets.bottom - calculateTabAreaHeight(placement, runCount, maxTabHeight) - 1);
        }
    }
}
