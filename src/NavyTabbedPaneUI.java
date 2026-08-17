import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

public final class NavyTabbedPaneUI extends BasicTabbedPaneUI {
    public static ComponentUI createUI(JComponent component) { return new NavyTabbedPaneUI(); }

    @Override protected void installDefaults() {
        super.installDefaults();
        tabAreaInsets = new Insets(3, 3, 3, 3);
        tabInsets = new Insets(7, 12, 7, 12);
        selectedTabPadInsets = new Insets(7, 12, 7, 12);
    }

    @Override protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
        return Math.max(38, super.calculateTabHeight(tabPlacement, tabIndex, fontHeight));
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
}
