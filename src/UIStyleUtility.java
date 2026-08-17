import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;

public final class UIStyleUtility {
    private static final Color TOTAL = new Color(220, 235, 252);
    public static final Color NAVY = new Color(26, 46, 64), ZEBRA = new Color(246, 248, 250), GRID = new Color(203, 210, 217);
    public static void installGlobalButtonDefaults() {
        Border rounded = BorderFactory.createCompoundBorder(new LineBorder(new Color(180, 190, 200), 1, true), BorderFactory.createEmptyBorder(6, 10, 6, 10));
        UIManager.put("ButtonUI", "NavyButtonUI");
        UIManager.put("ComboBoxUI", "NavyComboBoxUI");
        UIManager.put("TabbedPaneUI", "NavyTabbedPaneUI");
        UIManager.put("Button.background", NAVY);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.select", NAVY.brighter());
        UIManager.put("Button.focus", NAVY);
        UIManager.put("Button.disabledText", new Color(220, 228, 236));
        UIManager.put("Button.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(NAVY.darker()),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        UIManager.put("Button.opaque", Boolean.TRUE);
        UIManager.put("Button.contentAreaFilled", Boolean.TRUE);
        UIManager.put("TextField.border", rounded);
        UIManager.put("PasswordField.border", rounded);
        UIManager.put("TextArea.border", rounded);
        UIManager.put("ComboBox.border", rounded);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("PasswordField.background", Color.WHITE);
        UIManager.put("TextArea.background", Color.WHITE);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("PasswordField.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("TextArea.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("ComboBox.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("TabbedPane.foreground", Color.WHITE);
        UIManager.put("TabbedPane.selectedForeground", Color.WHITE);
    }
    public static void applyProfessionalTableStyle(JTable table) {
        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new HeaderRenderer());
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 42));
        header.setResizingAllowed(true);
        table.setFont(new Font("SansSerif", Font.PLAIN, 16)); table.setRowHeight(36); table.setAutoResizeMode(Boolean.TRUE.equals(table.getClientProperty("fitAllColumns")) ? JTable.AUTO_RESIZE_ALL_COLUMNS : JTable.AUTO_RESIZE_OFF); table.setGridColor(GRID); table.setShowGrid(true); table.setShowHorizontalLines(true); table.setShowVerticalLines(true); table.setIntercellSpacing(new Dimension(0, 0));
        table.setDefaultRenderer(Object.class, new DataRenderer()); table.setDefaultRenderer(Number.class, new DataRenderer());
        table.setDefaultRenderer(Boolean.class, new CheckboxRenderer());
        boolean salaryOnly = Boolean.TRUE.equals(table.getClientProperty("salaryGrossNetOnly"));
        for (int column = 0; column < table.getColumnCount(); column++) {
            String name = table.getColumnName(column);
            if (isTotalColumn(name, salaryOnly)) table.getColumnModel().getColumn(column).setCellRenderer(new TotalColumnRenderer());
        }
        autoFitColumnWidths(table);
    }
    public static void autoFitColumnWidths(JTable table) {
        Font headerFont = new Font("SansSerif", Font.BOLD, 17), dataFont = new Font("SansSerif", Font.PLAIN, 16);
        FontMetrics headerMetrics = table.getTableHeader().getFontMetrics(headerFont), dataMetrics = table.getFontMetrics(dataFont);
        for (int column = 0; column < table.getColumnCount(); column++) {
            int headerWidth = headerMetrics.stringWidth(table.getColumnName(column)) + 44;
            int dataWidth = 0;
            for (int row = 0; row < table.getRowCount(); row++) dataWidth = Math.max(dataWidth, dataMetrics.stringWidth(String.valueOf(table.getValueAt(row, column))) + 32);
            TableColumn tableColumn = table.getColumnModel().getColumn(column); int resolved = Math.max(Math.max(headerWidth, dataWidth), 160); tableColumn.setMinWidth(resolved); tableColumn.setPreferredWidth(resolved); tableColumn.setMaxWidth(Integer.MAX_VALUE);
        }
    }
    public static JScrollPane frozenEmployeeColumns(JTable table) {
        if(table.getColumnCount()<2||!"Employee ID".equalsIgnoreCase(table.getColumnName(0))||!"Employee Name".equalsIgnoreCase(table.getColumnName(1)))return new JScrollPane(table);
        TableColumn id=table.getColumnModel().getColumn(0),name=table.getColumnModel().getColumn(1);
        DefaultTableColumnModel frozenColumns=new DefaultTableColumnModel();
        frozenColumns.addColumn(copyColumn(id));frozenColumns.addColumn(copyColumn(name));
        table.removeColumn(id);table.removeColumn(name);
        JTable frozen=new JTable(table.getModel(),frozenColumns);frozen.setRowSorter(table.getRowSorter());frozen.setSelectionModel(table.getSelectionModel());frozen.setRowHeight(table.getRowHeight());frozen.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);applyProfessionalTableStyle(frozen);
        JScrollPane scroll=new JScrollPane(table,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setRowHeaderView(frozen);scroll.setCorner(JScrollPane.UPPER_LEFT_CORNER,frozen.getTableHeader());scroll.getRowHeader().setPreferredSize(new Dimension(frozen.getColumnModel().getTotalColumnWidth(),0));scroll.getViewport().setBorder(null);
        return scroll;
    }
    public static void restoreTableValues(DefaultTableModel model,Object[][] snapshot){for(int row=0;row<Math.min(model.getRowCount(),snapshot.length);row++)for(int column=0;column<Math.min(model.getColumnCount(),snapshot[row].length);column++)model.setValueAt(snapshot[row][column],row,column);}
    private static TableColumn copyColumn(TableColumn source){TableColumn copy=new TableColumn(source.getModelIndex());copy.setHeaderValue(source.getHeaderValue());copy.setPreferredWidth(source.getPreferredWidth());copy.setMinWidth(source.getMinWidth());copy.setMaxWidth(source.getMaxWidth());copy.setCellRenderer(source.getCellRenderer());copy.setCellEditor(source.getCellEditor());return copy;}
    private static boolean isTotalColumn(String name, boolean salaryOnly) { String value = name.toLowerCase(); return salaryOnly ? value.equals("gross salary") || value.equals("net salary") : value.contains("total") || value.contains("gross salary") || value.contains("net salary") || value.equals("net pay") || value.equals("net payable salary"); }
    private static boolean isCheckboxColumn(JTable table, int column) { return table.getColumnClass(column) == Boolean.class || table.getColumnName(column).equalsIgnoreCase("select"); }
    private static boolean isFinancialOrQuantityColumn(String name) { String value = name.toLowerCase(); if (value.equals("heads of income") || value.equals("income") || value.contains("description")) return false; return value.matches(".*(basic|pay|salary|allowance|deduction|gross|net|total|ot|overtime|esic|epf|pt|tds|advance|ctc|tax|income|actual|projected|amount|arrears|bonus|conveyance|hra|medical|special|fixed|absent|casual|earned|payable|days|hours|leave).*" ); }
    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() { setOpaque(true); setHorizontalAlignment(SwingConstants.CENTER); }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            boolean numeric = isFinancialOrQuantityColumn(table.getColumnName(column)), checkbox = isCheckboxColumn(table, column);
            setOpaque(true); setBackground(NAVY); setForeground(Color.WHITE); setFont(new Font("SansSerif", Font.BOLD, 17)); setBorder(new CompoundBorder(new LineBorder(NAVY.darker(), 1), BorderFactory.createEmptyBorder(0, 12, 0, numeric ? 14 : 12))); setHorizontalAlignment(checkbox ? SwingConstants.CENTER : numeric ? SwingConstants.RIGHT : SwingConstants.LEFT); return this;
        }
    }
    private static final class DataRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value instanceof Number ? Money.text(((Number) value).doubleValue()) : value, selected, focused, row, column);
            boolean numeric = value instanceof Number || isFinancialOrQuantityColumn(table.getColumnName(column));
            if (!selected) setBackground(row % 2 == 0 ? Color.WHITE : ZEBRA); setForeground(Color.BLACK); setFont(new Font("SansSerif", Font.PLAIN, 16)); setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 1, GRID), BorderFactory.createEmptyBorder(0, 12, 0, numeric ? 14 : 12))); setHorizontalAlignment(numeric ? SwingConstants.RIGHT : SwingConstants.LEFT); return this;
        }
    }
    private static final class TotalColumnRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value instanceof Number ? Money.text(((Number)value).doubleValue()) : value, selected, focused, row, column);
            if (!selected) setBackground(TOTAL); setForeground(Color.BLACK); setFont(new Font("SansSerif", Font.BOLD, 16)); setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 1, GRID), BorderFactory.createEmptyBorder(0, 12, 0, 14))); setHorizontalAlignment(SwingConstants.RIGHT);
            return this;
        }
    }
    private static final class CheckboxRenderer extends JCheckBox implements TableCellRenderer {
        CheckboxRenderer() { setHorizontalAlignment(SwingConstants.CENTER); setOpaque(true); }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            setSelected(Boolean.TRUE.equals(value)); setBackground(selected ? table.getSelectionBackground() : Color.WHITE); setBorder(new MatteBorder(0, 0, 1, 1, new Color(180, 180, 180))); return this;
        }
    }
    private UIStyleUtility() { }
}
