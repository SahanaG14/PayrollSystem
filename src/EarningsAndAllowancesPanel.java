import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.*;
import java.util.*;

public final class EarningsAndAllowancesPanel extends JPanel {
    private static final String[] COLUMNS = {"Employee ID", "Employee Name", "HRA", "Attendance Bonus", "Conveyance", "Performance Pay", "Medical", "Special", "Fixed", "Total Allowances", "OT Pay", "Others", "Total Other Earnings"};
    private final EmployeeDAO employees = new EmployeeDAO();
    private final JTabbedPane months = new JTabbedPane();
    private final JPanel current = new JPanel(new BorderLayout());
    private int financialYear = FinancialYear.currentStart();

    public EarningsAndAllowancesPanel() {
        super(new BorderLayout());
        JComboBox<String> year = FinancialYear.selector(financialYear); JButton open = new JButton("Open Year");
        open.addActionListener(e -> { financialYear = FinancialYear.parse(String.valueOf(year.getSelectedItem())); buildMonths(); });
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT)); top.add(new JLabel("Financial Year:")); top.add(year); top.add(open); add(top, BorderLayout.NORTH);
        months.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT); months.setPreferredSize(new Dimension(0, 48)); months.setMinimumSize(new Dimension(0, 48)); months.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        JPanel center = new JPanel(new BorderLayout()); center.add(months, BorderLayout.NORTH); center.add(current, BorderLayout.CENTER); add(center, BorderLayout.CENTER);
        months.addChangeListener(e -> { styleMonthLabels(); loadView(); }); PayrollEvents.onAttendanceSaved(() -> { if (isShowing()) loadView(); }); buildMonths();
    }

    public void refresh() { buildMonths(); }

    private void buildMonths() {
        int selected = Math.max(0, months.getSelectedIndex()); months.removeAll();
        for (int i = 0; i < 12; i++) { String label = FinancialYear.shortName(FinancialYear.month(financialYear, i)); JPanel blank = new JPanel(); blank.setPreferredSize(new Dimension(0, 0)); months.addTab(label, blank); JLabel tab = new JLabel(label, SwingConstants.CENTER); tab.setOpaque(false); tab.setPreferredSize(new Dimension(80, 34)); tab.setFont(new Font("SansSerif", Font.BOLD, 15)); tab.setForeground(Color.WHITE); months.setTabComponentAt(i, tab); }
        months.setSelectedIndex(Math.min(selected, 11)); styleMonthLabels(); loadView();
    }

    private YearMonth period() { return FinancialYear.month(financialYear, Math.max(0, months.getSelectedIndex())); }
    private void styleMonthLabels() { for (int i = 0; i < months.getTabCount(); i++) if (months.getTabComponentAt(i) instanceof JLabel tab) tab.setForeground(Color.WHITE); }
    private void loadView() { if (months.getSelectedIndex() < 0) return; current.removeAll(); current.add(grid(period()), BorderLayout.CENTER); current.revalidate(); current.repaint(); }

    private JComponent grid(YearMonth period) {
        java.util.List<Employee> list = employees.listForMonth("", period); Object[][] rows = new Object[list.size()][COLUMNS.length];
        for (int r = 0; r < list.size(); r++) {
            Employee e = list.get(r); double[] values = displayedValues(e, period);
            rows[r] = new Object[]{e.getId(), e.getName(), values[0], values[1], values[2], values[3], values[4], values[5], values[6], Money.round(sum(values, 0, 6)), values[7], values[8], Money.round(values[7] + values[8])};
        }
        final boolean[] editing = {false}, restoring = {false}; final Object[][][] snapshot = {copyRows(rows)};
        DefaultTableModel model = new DefaultTableModel(rows, COLUMNS) { public boolean isCellEditable(int row, int column) { if (!editing[0] || column < 2 || column == 9 || column == 12) return false; Employee employee = list.get(row); return employee.isWagesStructure() || (column != 3 && column != 10 && column != 11); } };
        JTable table = new JTable(model); table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); ExcelTableSupport.enableNumericMultiFill(table, false);
        int[] widths = {180, 220, 180, 220, 180, 220, 180, 180, 180, 220, 180, 180, 220};
        for (int c = 0; c < COLUMNS.length; c++) { table.getColumnModel().getColumn(c).setPreferredWidth(widths[c]); if (c >= 2) table.getColumnModel().getColumn(c).setCellRenderer(Money.renderer()); }
        UIStyleUtility.applyProfessionalTableStyle(table);
        for (int c = 2; c < COLUMNS.length; c++) table.getColumnModel().getColumn(c).setCellRenderer(overrideRenderer(calculatedColumn(rows, c), c));
        final boolean[] updating = {false};
        JLabel overrideWarning = new JLabel("Warning: Value is not as per global settings."); overrideWarning.setForeground(new Color(190, 70, 70)); overrideWarning.setVisible(hasOverrides(model, rows));
        model.addTableModelListener(e -> { if (e.getType() != TableModelEvent.UPDATE || restoring[0] || updating[0] || e.getColumn() < 2) return; int row = e.getFirstRow(); updating[0] = true; try { recalculateRow(model, row); overrideWarning.setVisible(hasOverrides(model, rows)); } finally { updating[0] = false; } });
        JTextField search = new JTextField(20); TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model); table.setRowSorter(sorter);
        search.getDocument().addDocumentListener(new DocumentListener() { private void filter() { String q = search.getText().trim(); sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q), 0, 1)); } public void insertUpdate(DocumentEvent e) { filter(); } public void removeUpdate(DocumentEvent e) { filter(); } public void changedUpdate(DocumentEvent e) { filter(); } });
        JButton editCancel = new JButton("Edit"), save = new JButton("Save"), export = new JButton("Export to Excel"), imported = new JButton("Import Excel"); save.setEnabled(false); editCancel.addActionListener(e -> { if (!editing[0]) { snapshot[0] = copyModel(model); editing[0] = true; editCancel.setText("Cancel"); save.setEnabled(true); } else { restoring[0] = true; UIStyleUtility.restoreTableValues(model,snapshot[0]); restoring[0] = false; editing[0] = false; editCancel.setText("Edit"); save.setEnabled(false); } table.repaint(); }); save.addActionListener(e -> { for(int row=0;row<model.getRowCount();row++) saveRow(model,row,period); snapshot[0] = copyModel(model); editing[0] = false; editCancel.setText("Edit"); save.setEnabled(false); JOptionPane.showMessageDialog(this,"Earnings & Allowances saved."); }); export.addActionListener(e -> exportExcel(model, period)); imported.addActionListener(e -> importExcel(model, period)); TabStyle.styleActionButton(editCancel); TabStyle.styleActionButton(save); TabStyle.styleActionButton(export); TabStyle.styleActionButton(imported);
        JPanel controls = new JPanel(new BorderLayout()); JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); searchPanel.add(new JLabel("Search Employee ID / Name:")); searchPanel.add(search); JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT)); actions.add(imported); actions.add(export); controls.add(searchPanel, BorderLayout.WEST); controls.add(actions, BorderLayout.EAST); JPanel bottom = new JPanel(new BorderLayout()); JPanel editActions = new JPanel(new FlowLayout(FlowLayout.RIGHT)); editActions.add(editCancel); editActions.add(save); bottom.add(editActions,BorderLayout.EAST); bottom.add(overrideWarning,BorderLayout.WEST);
        JPanel out = new JPanel(new BorderLayout()); out.add(controls, BorderLayout.NORTH); out.add(UIStyleUtility.frozenEmployeeColumns(table), BorderLayout.CENTER); out.add(bottom, BorderLayout.SOUTH); return out;
    }

    public static double[] displayedValues(Employee e, YearMonth period) { MonthlyEarningsStore.Value saved = MonthlyEarningsStore.get(e.getId(), period.toString()); double[] calculated = proratedValues(e, period); double[] values = saved == null || zeroAllowances(saved) ? calculated : new double[]{saved.hra, calculated[1], saved.conveyance, saved.performance, saved.medical, saved.special, saved.fixed, saved.overtime, saved.reimbursements}; if (!e.isWagesStructure()) values[1] = values[7] = values[8] = 0.0; return values; }
    public static double epfGrossWages(Employee e, YearMonth period) { double[] values=displayedValues(e,period); MonthlyEarningsStore.Value saved=MonthlyEarningsStore.get(e.getId(),period.toString()); double performance=saved==null?values[3]:saved.performance; return Money.round(sum(values,0,6)-performance-values[1]); }

    private static double[] proratedValues(Employee e, YearMonth period) {
        AttendanceDAO attendance = new AttendanceDAO(); AttendanceRecord record = attendance.load(e.getId(), period.toString());
        double workingDays = attendance.workingDays(period.toString()), payableDays = Math.max(0, record.daysPayable);
        if (workingDays <= 0) workingDays = 1;
        if (!attendance.hasSavedAttendance(e.getId(), period.toString())) payableDays = workingDays;
        else payableDays = LeaveBalanceService.payableDays(e, period, workingDays, record.absentDays, record.paidLeaveDays, record.unpaidLeaveDays, new AttendanceSettingsDAO().load());
        double ratio = Math.min(1, payableDays / workingDays);
        double proratedBasic = Money.round(SalaryRevisionStore.basicFor(e, period) * ratio);
        CTCStore.Value ctc = CTCStore.get(e.getId());
        double hra = ctc.hraOverride ? Money.round(proratedBasic * ctc.hraOverridePercent / 100.0) : allowance("HRA", proratedBasic, 40);
        boolean fullAttendance = payableDays >= workingDays && workingDays > 0;
        double configuredBonus = CompanyPolicyStore.hasAllowance("Attendance Bonus") ? CompanyPolicyStore.allowance("Attendance Bonus") : 0.0;
        double attendanceBonus = e.isWagesStructure() && fullAttendance ? Money.round(CompanyPolicyStore.percentage("Attendance Bonus") ? proratedBasic * configuredBonus / 100.0 : configuredBonus) : 0.0;
        double conveyance = allowance("Conveyance Allowance", proratedBasic, PayrollRulesStore.conveyance());
        double performance = allowance("Performance Pay", proratedBasic, 0);
        double medical = allowance("Medical Allowance", proratedBasic, PayrollRulesStore.medical());
        double special = allowance("Special Allowance", proratedBasic, 15);
        double fixed = allowance("Fixed Allowance", proratedBasic, 0);
        double hourlyRate = PayrollCalculator.effectiveOtRate(SalaryRevisionStore.basicFor(e, period), workingDays * PayrollRulesStore.workingHours());
        return new double[]{hra, attendanceBonus, conveyance, performance, medical, special, fixed, e.isWagesStructure() ? Money.round(hourlyRate * Math.max(0, record.overtimeHours)) : 0.0, e.isWagesStructure() ? Money.round(hourlyRate * Math.max(0, record.otherHours)) : 0.0};
    }

    private static boolean zeroAllowances(MonthlyEarningsStore.Value value) { return value.hra == 0 && value.attendance == 0 && value.conveyance == 0 && value.performance == 0 && value.medical == 0 && value.special == 0 && value.fixed == 0; }
    private static Object[][] copyRows(Object[][] source) { Object[][] copy=new Object[source.length][]; for(int row=0;row<source.length;row++) copy[row]=java.util.Arrays.copyOf(source[row],source[row].length); return copy; }
    private static Object[][] copyModel(DefaultTableModel model) { Object[][] copy=new Object[model.getRowCount()][model.getColumnCount()]; for(int row=0;row<copy.length;row++) for(int column=0;column<copy[row].length;column++) copy[row][column]=model.getValueAt(row,column); return copy; }
    private static double allowance(String name, double proratedBasic, double fallback) { boolean configured = CompanyPolicyStore.hasAllowance(name); double value = configured ? CompanyPolicyStore.allowance(name) : fallback; boolean percentage = configured ? CompanyPolicyStore.percentage(name) : "HRA".equals(name) || "Special Allowance".equals(name); return Money.round(percentage ? proratedBasic * value / 100.0 : value); }

    private void recalculateRow(DefaultTableModel model, int row) { model.setValueAt(Money.round(sum(model, row, 2, 8)), row, 9); model.setValueAt(Money.round(number(model, row, 10) + number(model, row, 11)), row, 12); }
    private void saveRow(DefaultTableModel model, int row, YearMonth period) {
        MonthlyEarningsStore.Value v = MonthlyEarningsStore.getOrCreate(String.valueOf(model.getValueAt(row, 0)), period.toString());
        v.hra = number(model, row, 2); v.attendance = number(model, row, 3); v.conveyance = number(model, row, 4); v.performance = number(model, row, 5); v.medical = number(model, row, 6); v.special = number(model, row, 7); v.fixed = number(model, row, 8); v.overtime = number(model, row, 10); v.reimbursements = number(model, row, 11);
        MonthlyEarningsStore.save(); AutoSaveService.markDirty(); PayrollEvents.attendanceSaved();
    }

    private void exportExcel(DefaultTableModel model, YearMonth period) { java.util.List<Object[]> rows = new ArrayList<>(); for (int r = 0; r < model.getRowCount(); r++) { Object[] row = new Object[COLUMNS.length]; for (int c = 0; c < COLUMNS.length; c++) row[c] = model.getValueAt(r, c); rows.add(row); } PayrollExcel.export(this, "Earnings & Allowances", period + "-Earnings.xlsx", COLUMNS, rows); }
    private void importExcel(DefaultTableModel model, YearMonth period) { try { PayrollExcel.Sheet sheet = PayrollExcel.importSheet(this); if (sheet == null) return; PayrollExcel.requireHeaders(sheet, COLUMNS); for (int i = 1; i < sheet.rows.size(); i++) { java.util.List<String> data = sheet.rows.get(i); for (int r = 0; r < model.getRowCount(); r++) if (String.valueOf(model.getValueAt(r, 0)).equals(PayrollExcel.cell(data, 0))) { for (int c = 2; c <= 11; c++) if (c != 9) model.setValueAt(PayrollExcel.number(PayrollExcel.cell(data, c)), r, c); recalculateRow(model, r); saveRow(model, r, period); break; } } } catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage()); } }
    private static double number(DefaultTableModel model, int row, int column) { try { return Money.round(Double.parseDouble(String.valueOf(model.getValueAt(row, column)))); } catch (Exception ignored) { return 0; } }
    private static double sum(DefaultTableModel model, int row, int from, int to) { double value = 0; for (int c = from; c <= to; c++) value += number(model, row, c); return value; }
    private static double sum(double[] values, int from, int to) { double value = 0; for (int i = from; i <= to; i++) value += values[i]; return value; }
    private static Object[] calculatedColumn(Object[][] rows, int column) { Object[] values = new Object[rows.length]; for (int row = 0; row < rows.length; row++) values[row] = rows[row][column]; return values; }
    private static boolean hasOverrides(DefaultTableModel model, Object[][] expected) { for (int row = 0; row < model.getRowCount(); row++) for (int column = 2; column < model.getColumnCount(); column++) if (column != 9 && column != 12 && !same(model.getValueAt(row, column), expected[row][column])) return true; return false; }
    private static boolean same(Object left, Object right) { try { return Math.abs(Double.parseDouble(String.valueOf(left)) - Double.parseDouble(String.valueOf(right))) < .005; } catch (Exception ignored) { return String.valueOf(left).equals(String.valueOf(right)); } }
    private static TableCellRenderer overrideRenderer(Object[] expected, int column) { return new DefaultTableCellRenderer() { @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int viewColumn) { super.getTableCellRendererComponent(table, value instanceof Number ? Money.text(((Number)value).doubleValue()) : value, selected, focused, row, viewColumn); int modelRow = table.convertRowIndexToModel(row); boolean totalColumn = "Total Allowances".equals(table.getColumnName(viewColumn)) || "Total Other Earnings".equals(table.getColumnName(viewColumn)); boolean overridden = modelRow < expected.length && !same(value, expected[modelRow]); setOpaque(true); if (!selected) setBackground(totalColumn ? new Color(230, 240, 255) : overridden ? new Color(255, 230, 230) : Color.WHITE); setFont(getFont().deriveFont(totalColumn ? Font.BOLD : Font.PLAIN)); setHorizontalAlignment(SwingConstants.RIGHT); return this; } }; }
}
