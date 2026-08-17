import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

public class SalaryPanel extends JPanel {
    private final EmployeeDAO employees = new EmployeeDAO();
    private final JTabbedPane sections = new JTabbedPane(), months = new JTabbedPane(), financialMonths = new JTabbedPane();
    private int year = FinancialYear.currentStart();
    private JComboBox<String> financialYear;

    public SalaryPanel() {
        setLayout(new BorderLayout());
        sections.addTab("Salary Calculation", monthlyView());
        sections.addTab("Annual Salary", annualView());
        sections.addTab("IT Computation", itComputationView());
        sections.addChangeListener(e -> { if (sections.getSelectedIndex() == 0) loadMonthlySelected(); else if (sections.getSelectedIndex() == 1) loadFinancialSelected(); });
        add(sections, BorderLayout.CENTER);
    }
    public void refresh() { if (sections.getSelectedIndex() == 0) buildMonthly(); else if (sections.getSelectedIndex() == 1) buildFinancial(); }

    private JComponent itComputationView(){return new ITComputationPanel();}
    private JComponent legacyItComputationView() {
        JPanel panel = new JPanel(new BorderLayout()); JComboBox<String> fy = FinancialYear.selector(FinancialYear.currentStart()); JTabbedPane tabs = new JTabbedPane();
        tabs.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        final int[] activeFinancialYear = {FinancialYear.parse((String) fy.getSelectedItem())};
        Runnable load = () -> { if (tabs.getTabCount() == 0) return; int start = activeFinancialYear[0]; int index = Math.max(0, tabs.getSelectedIndex()); YearMonth period = FinancialYear.month(start, index); java.util.List<Employee> list = employees.listForMonth("", period); Object[][] rows = new Object[list.size()][3]; for (int i = 0; i < list.size(); i++) rows[i] = new Object[]{list.get(i).getId(), list.get(i).getName(), list.get(i)};
            DefaultTableModel model = new DefaultTableModel(rows, new String[]{"Employee ID", "Employee Name", "IT Computation"}) { public boolean isCellEditable(int r, int c) { return c == 2; } };
            JTable table = new JTable(model); UIStyleUtility.applyProfessionalTableStyle(table); table.getColumnModel().getColumn(2).setPreferredWidth(220); table.getColumnModel().getColumn(2).setCellRenderer(new ITActionsRenderer()); table.getColumnModel().getColumn(2).setCellEditor(new ITActionsEditor(start, period));
            JPanel page = new JPanel(new BorderLayout()); page.add(EmployeeIdSearch.create(table, model), BorderLayout.NORTH); page.add(new JScrollPane(table), BorderLayout.CENTER); tabs.setComponentAt(index, page); };
        Runnable rebuild = () -> { int selected = Math.max(0, tabs.getSelectedIndex()); tabs.removeAll(); for (int i = 0; i < 12; i++) { YearMonth period = FinancialYear.month(activeFinancialYear[0], i); String name = FinancialYear.shortName(period); tabs.addTab(name, new JPanel()); JLabel label = new JLabel(name, SwingConstants.CENTER); label.setPreferredSize(new Dimension(80, 30)); label.setForeground(Color.WHITE); tabs.setTabComponentAt(i, label); } tabs.setSelectedIndex(Math.min(selected, 11)); load.run(); tabs.revalidate(); tabs.repaint(); };
        tabs.addChangeListener(e -> load.run()); JButton open = new JButton("Open Year"); open.addActionListener(e -> { activeFinancialYear[0] = FinancialYear.parse((String) fy.getSelectedItem()); rebuild.run(); }); JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT)); top.add(new JLabel("Financial Year:")); top.add(fy); top.add(open); panel.add(top, BorderLayout.NORTH); panel.add(tabs, BorderLayout.CENTER); rebuild.run(); return panel;
    }

    private JComponent monthlyView() {
        JPanel panel = new JPanel(new BorderLayout()); JPanel top = new JPanel(); JComboBox<String> y = FinancialYear.selector(year); JButton open = new JButton("Open Year"); months.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        open.addActionListener(e -> { year = FinancialYear.parse((String)y.getSelectedItem()); buildMonthly(); });
        top.add(new JLabel("Salary Register - Financial Year:")); top.add(y); top.add(open); panel.add(top, BorderLayout.NORTH);
        months.addChangeListener(e -> loadMonthlySelected()); panel.add(months, BorderLayout.CENTER); buildMonthly(); return panel;
    }
    private void buildMonthly() {
        int selected = Math.max(0, months.getSelectedIndex()); months.removeAll();
        for (int i = 0; i < 12; i++) { YearMonth period = FinancialYear.month(year, i); months.addTab(shortMonth(period), new JPanel()); monthTab(months, i, period); }
        months.setSelectedIndex(Math.min(selected, 11)); loadMonthlySelected(); months.revalidate(); months.repaint();
    }
    private void loadMonthlySelected() {
        int index = months.getSelectedIndex(); if (index < 0 || !(months.getComponentAt(index) instanceof JPanel)) return;
        JComponent current = (JComponent) months.getComponentAt(index); if (current.getComponentCount() != 0 || Boolean.TRUE.equals(current.getClientProperty("loading"))) return;
        current.putClientProperty("loading", Boolean.TRUE); current.setLayout(new GridBagLayout()); current.add(new JLabel("Loading salary register…"));
        int activeYear = year; YearMonth month = FinancialYear.month(activeYear, index);
        ApplicationTasks.execute(() -> { JComponent sheet = monthlySheet(month); SwingUtilities.invokeLater(() -> { if (year == activeYear && months.getSelectedIndex() == index) { TabStyle.apply(sheet); months.setComponentAt(index, sheet); } }); });
    }
    private JComponent monthlySheet(YearMonth month) {
        java.util.List<Employee> list = employees.listForMonth("", month); Object[][] rows = new Object[list.size()][8];
        for (int i = 0; i < list.size(); i++) { Employee e = list.get(i); PayrollCalculator.Result r = SalaryCalculationEngine.calculate(e, month); rows[i] = new Object[]{e.getId(), e.getName(), money(r.earnings.get("Monthly Basic Pay")), money(r.earnings.get("Total Allowances")), money(r.earnings.get("Total Other Earnings")), Money.round(r.gross), money(r.deductions.get("Total Deductions")), Money.round(r.net)}; }
        DefaultTableModel model = new DefaultTableModel(rows, new String[]{"Employee ID", "Employee Name", "Monthly Basic Pay", "Total Allowances", "Total Other Earnings", "Gross Salary", "Total Deductions", "Net Salary"}) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable table = new JTable(model); table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); table.putClientProperty("salaryGrossNetOnly", Boolean.TRUE); for (int c = 2; c < 8; c++) table.getColumnModel().getColumn(c).setCellRenderer(Money.renderer()); int[] widths={180,220,190,210,240,190,230,190}; for(int c=0;c<widths.length;c++)table.getColumnModel().getColumn(c).setPreferredWidth(widths[c]); UIStyleUtility.applyProfessionalTableStyle(table);
        JButton export = new JButton("Export to Excel"); export.addActionListener(e -> exportMonthlyExcel(model, month));
        JPanel top = new JPanel(new BorderLayout()); top.add(EmployeeIdSearch.create(table, model),BorderLayout.WEST); JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT));actions.add(export);top.add(actions,BorderLayout.EAST);
        JScrollPane scroll=UIStyleUtility.frozenEmployeeColumns(table); scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); scroll.getHorizontalScrollBar().setUnitIncrement(24);
        JPanel panel = new JPanel(new BorderLayout()); panel.add(top, BorderLayout.NORTH); panel.add(scroll, BorderLayout.CENTER); return panel;
    }
    private void exportMonthlyExcel(DefaultTableModel model, YearMonth month) {
        String[] headers = new String[model.getColumnCount()]; for (int c = 0; c < headers.length; c++) headers[c] = model.getColumnName(c);
        java.util.List<Object[]> rows = new ArrayList<>(); for (int r = 0; r < model.getRowCount(); r++) { Object[] row = new Object[headers.length]; for (int c = 0; c < headers.length; c++) row[c] = model.getValueAt(r, c); rows.add(row); }
        PayrollExcel.exportBoldHeader(this, "Salary Register", month + "-Salary-Register.xlsx", headers, rows);
    }

    private JComponent annualView() {
        JPanel panel = new JPanel(new BorderLayout()); financialYear = FinancialYear.selector(FinancialYear.currentStart());
        JButton open = new JButton("Open Year"); open.addActionListener(e -> buildFinancial());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT)); top.add(new JLabel("Financial Year:")); top.add(financialYear); top.add(open); panel.add(top, BorderLayout.NORTH);
        // The annual selector is a single APR-to-MAR navigation strip.  Scroll
        // horizontally on unusually narrow screens rather than wrapping tabs.
        financialMonths.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        financialMonths.addChangeListener(e -> { styleFinancialLabels(); loadFinancialSelected(); }); panel.add(financialMonths, BorderLayout.CENTER); buildFinancial(); return panel;
    }
    private void buildFinancial() {
        if (financialYear == null) return; int selected = Math.max(0, financialMonths.getSelectedIndex()); financialMonths.removeAll();
        int start = financialStart(); financialMonths.addTab("Annual View", new JPanel());
        JLabel annual = new JLabel("Annual View", SwingConstants.CENTER); annual.setPreferredSize(new Dimension(100, 30)); annual.setForeground(Color.WHITE); financialMonths.setTabComponentAt(0, annual);
        for (int i = 0; i < 12; i++) { int month = i + 4; YearMonth period = month <= 12 ? YearMonth.of(start, month) : YearMonth.of(start + 1, month - 12); financialMonths.addTab(shortMonth(period), new JPanel()); monthTab(financialMonths, i + 1, period); }
        financialMonths.setSelectedIndex(Math.min(selected, 12)); styleFinancialLabels(); loadFinancialSelected(); financialMonths.revalidate(); financialMonths.repaint();
    }
    private void loadFinancialSelected() {
        if (financialYear == null) return; int index = financialMonths.getSelectedIndex(); if (index < 0) return; JComponent current = (JComponent) financialMonths.getComponentAt(index); if (current.getComponentCount() > 0) return;
        int start = financialStart(); current.setLayout(new GridBagLayout()); current.add(new JLabel("Loading salary data…"));
        ApplicationTasks.execute(() -> { JComponent sheet=index == 0 ? annualSheet(start) : monthlyAnnualSheet(start, financialPeriod(start,index-1)); SwingUtilities.invokeLater(() -> { if(financialStart()==start&&financialMonths.getSelectedIndex()==index){TabStyle.apply(sheet);financialMonths.setComponentAt(index,sheet);} }); });
    }
    private int financialStart() { return Integer.parseInt(((String) financialYear.getSelectedItem()).substring(0, 4)); }
    private YearMonth financialPeriod(int start, int index) { int month = index + 4; return month <= 12 ? YearMonth.of(start, month) : YearMonth.of(start + 1, month - 12); }

    private JComponent annualSheet(int startYear) {
        String fy = (String) financialYear.getSelectedItem(); java.util.List<Employee> list = employees.listForFinancialYear("", startYear); Object[][] rows = new Object[list.size()][6];
        for (int i = 0; i < list.size(); i++) { Employee person = list.get(i); rows[i] = new Object[]{Boolean.FALSE, person.getId(), person.getName(), Money.round(annualNet(person, startYear)), person, person}; }
        DefaultTableModel model = new DefaultTableModel(rows, new String[]{"Select", "Employee ID", "Employee Name", "Net Salary of Year", "Salary File", "Export to Excel"}) {
            public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : (c == 3 ? Double.class : Object.class); }
            public boolean isCellEditable(int r, int c) { return c == 0 || c == 4 || c == 5; }
        };
        JTable table = new JTable(model); table.setShowGrid(true); table.setShowHorizontalLines(true); table.setShowVerticalLines(true); table.setGridColor(new Color(180,180,180)); table.setBorder(BorderFactory.createLineBorder(new Color(160,160,160))); UIStyleUtility.applyProfessionalTableStyle(table); table.getColumnModel().getColumn(3).setCellRenderer(Money.renderer());
        table.getColumnModel().getColumn(4).setCellRenderer(new AnnualActionsRenderer()); table.getColumnModel().getColumn(4).setCellEditor(new AnnualActionsEditor(startYear, fy));
        table.getColumnModel().getColumn(5).setCellRenderer(new Form16ActionsRenderer()); table.getColumnModel().getColumn(5).setCellEditor(new Form16ActionsEditor(startYear, fy));
        JCheckBox all = new JCheckBox("Select All"); all.addActionListener(e -> { for (int r = 0; r < model.getRowCount(); r++) model.setValueAt(all.isSelected(), r, 0); });
        JButton selected = new JButton("Download Selected"), downloadAll = new JButton("Download All"), exportAll=new JButton("Export All to Excel"); selected.addActionListener(e -> downloadBulk(model, startYear, fy, false, 4)); downloadAll.addActionListener(e -> downloadBulk(model, startYear, fy, true, 4)); exportAll.addActionListener(e->exportAnnualExcel(startYear,fy));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT)); top.add(EmployeeIdSearch.create(table, model)); top.add(all); top.add(selected); top.add(downloadAll);top.add(exportAll); return sheet(top, table);
    }

    private JComponent monthlyAnnualSheet(int startYear, YearMonth period) {
        String fy = (String) financialYear.getSelectedItem(); java.util.List<Employee> list = employees.listForMonth("", period); Object[][] rows = new Object[list.size()][19];
        for (int i = 0; i < list.size(); i++) {
            Employee e = list.get(i); PayrollCalculator.Result r = SalaryCalculationEngine.calculate(e, period); CTCStore.Value c = CTCStore.get(e.getId()); CTCStore.Value other = CTCStore.getOther(e.getId(), period.toString()); DeductionStore.Value d = DeductionStore.get(e.getId(), period.toString());
            Object[] values=new Object[]{Boolean.FALSE, e.getId(), e.getName(), money(r.earnings.get("Monthly Basic Pay")), Money.round(c.hra), Money.round(c.special), Money.round(c.fixed), Money.round(c.medical), Money.round(c.conveyance), Money.round(c.attendance + other.other()), Money.round(r.gross), Money.round(d.epf), Money.round(d.pt), 0.0, Money.round(d.esic), Money.round(d.tds), Money.round(r.net), e, e};
            for(int col=3;col<=16;col++) values[col]=AnnualSalaryOverrideStore.value(e.getId(),period.toString(),col,(Double)values[col]); rows[i]=values;
        }
        String[] headers = {"Select", "Employee ID", "Employee Name", "Basic", "HRA", "Spl Allow", "Fixed Allow", "Medical Exp", "Conveyance", "Bonus / Other", "Gross Salary", "PF-EE", "PT", "LWF", "ESIC", "Income Tax / TDS", "Net Salary", "Salary File", "Export to Excel"};
        DefaultTableModel model = new DefaultTableModel(rows, headers) { public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : (c >= 3 && c <= 16 ? Double.class : Object.class); } public boolean isCellEditable(int r, int c) { return c == 0 || (c >= 3 && c <= 16) || c == 17 || c == 18; } };
        JTable table = new JTable(model); table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); UIStyleUtility.applyProfessionalTableStyle(table);
        for (int c = 3; c <= 16; c++) { table.getColumnModel().getColumn(c).setCellRenderer(Money.renderer()); table.getColumnModel().getColumn(c).setPreferredWidth(c == 10 || c == 16 ? 130 : 105); }
        table.getColumnModel().getColumn(17).setPreferredWidth(150); table.getColumnModel().getColumn(18).setPreferredWidth(180);
        table.getColumnModel().getColumn(17).setCellRenderer(new MonthlyActionsRenderer(period)); table.getColumnModel().getColumn(17).setCellEditor(new MonthlyActionsEditor(period));
        table.getColumnModel().getColumn(18).setCellRenderer(new Form16ActionsRenderer()); table.getColumnModel().getColumn(18).setCellEditor(new Form16ActionsEditor(startYear, fy));
        final boolean[] savingEdit={false}; model.addTableModelListener(e->{if(savingEdit[0]||e.getType()!=javax.swing.event.TableModelEvent.UPDATE||e.getColumn()<3||e.getColumn()>16)return;int row=e.getFirstRow();try{double value=Money.round(Double.parseDouble(String.valueOf(model.getValueAt(row,e.getColumn()))));savingEdit[0]=true;model.setValueAt(value,row,e.getColumn());savingEdit[0]=false;AnnualSalaryOverrideStore.save(String.valueOf(model.getValueAt(row,1)),period.toString(),e.getColumn(),value);AutoSaveService.markDirty();}catch(Exception ignored){savingEdit[0]=false;}});
        JCheckBox all = new JCheckBox("Select All"); all.addActionListener(e -> { for (int r = 0; r < model.getRowCount(); r++) model.setValueAt(all.isSelected(), r, 0); });
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT)); top.add(new JLabel("Monthly details: " + period.getMonth() + " " + period.getYear())); top.add(EmployeeIdSearch.create(table, model)); top.add(all); return sheet(top, table);
    }
    private JComponent sheet(JPanel top, JTable table) { JPanel panel = new JPanel(new BorderLayout()); panel.add(top, BorderLayout.NORTH); panel.add(new JScrollPane(table), BorderLayout.CENTER); return panel; }
    private double annualNet(Employee person, int start) { double total = 0; for (int i = 0; i < 12; i++) total += SalaryCalculationEngine.calculate(person, financialPeriod(start, i)).net; return Money.round(total); }
    private void exportAnnualExcel(int start,String fy){JFileChooser chooser=new JFileChooser();chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;File folder=new File(chooser.getSelectedFile(),"Annual-Salary-"+fy);String[] headers={"Financial Year","Month","Employee ID","Employee Name","Basic Pay","HRA","Special Allowance","Fixed Allowance","Medical","Conveyance","Bonus / Other","Gross Salary","EPF","PT","ESIC","TDS","Net Salary"};java.util.List<Employee> people=employees.listForFinancialYear("",start);ApplicationTasks.execute(()->{int written=0;try{folder.mkdirs();for(Employee e:people){java.util.List<Object[]> rows=new ArrayList<>();for(int i=0;i<12;i++){YearMonth p=financialPeriod(start,i);PayrollCalculator.Result r=PayslipPDF.result(e,p);CTCStore.Value c=CTCStore.get(e.getId());CTCStore.Value o=CTCStore.getOther(e.getId(),p.toString());DeductionStore.Value d=DeductionStore.get(e.getId(),p.toString());rows.add(new Object[]{fy,p.getMonth()+"-"+p.getYear(),e.getId(),e.getName(),money(r.earnings.get("Monthly Basic Pay")),Money.round(c.hra),Money.round(c.special),Money.round(c.fixed),Money.round(c.medical),Money.round(c.conveyance),Money.round(c.attendance+o.other()),Money.round(r.gross),Money.round(d.epf),Money.round(d.pt),Money.round(d.esic),Money.round(d.tds),Money.round(r.net)});}String name=(e.getId()+"-"+e.getName()+"-Annual-"+fy).replaceAll("[\\\\/:*?\"<>|]","_")+".xlsx";PayrollExcel.write(new File(folder,name),"Annual Salary",headers,rows);written++;}int count=written;SwingUtilities.invokeLater(()->JOptionPane.showMessageDialog(this,count+" separate annual Excel files saved in:\n"+folder.getAbsolutePath()));}catch(Exception ex){SwingUtilities.invokeLater(()->JOptionPane.showMessageDialog(this,"Annual Excel export failed: "+ex.getMessage()));}});}
    private static double money(Double value) { return Money.round(value == null ? 0 : value); }

    private void downloadBulk(DefaultTableModel model, int start, String fy, boolean all, int employeeColumn) {
        java.util.List<Employee> picked = new ArrayList<>(); for (int r = 0; r < model.getRowCount(); r++) if (all || Boolean.TRUE.equals(model.getValueAt(r, 0))) picked.add((Employee) model.getValueAt(r, employeeColumn));
        if (picked.isEmpty()) { JOptionPane.showMessageDialog(this, "Select at least one employee."); return; }
        JFileChooser chooser = new JFileChooser(); chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File folder = new File(chooser.getSelectedFile(), "Annual-Salary-" + fy); ApplicationTasks.execute(() -> { folder.mkdirs(); for (Employee person : picked) AnnualSalaryPDF.create(person, start, fy, folder); SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Annual salary files saved in " + folder.getAbsolutePath())); });
    }
    private class AnnualActionsRenderer extends JPanel implements TableCellRenderer { AnnualActionsRenderer() { add(new JButton("View")); add(new JButton("Download")); } public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) { return this; } }
    private class AnnualActionsEditor extends AbstractCellEditor implements TableCellEditor { private final int start; private final String fy; private Employee person; private final JPanel panel = new JPanel(); AnnualActionsEditor(int start, String fy) { this.start = start; this.fy = fy; JButton view = new JButton("View"), download = new JButton("Download"); view.addActionListener(e -> { showAnnual(person, start, fy); fireEditingStopped(); }); download.addActionListener(e -> { downloadAnnual(person, start, fy); fireEditingStopped(); }); panel.add(view); panel.add(download); } public Component getTableCellEditorComponent(JTable t, Object value, boolean s, int row, int col) { person = (Employee) value; return panel; } public Object getCellEditorValue() { return person; } }
    private class MonthlyActionsRenderer extends JPanel implements TableCellRenderer { MonthlyActionsRenderer(YearMonth period) { add(new JButton("View")); add(new JButton("Download")); } public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) { return this; } }
    private class MonthlyActionsEditor extends AbstractCellEditor implements TableCellEditor { private final YearMonth period; private Employee person; private final JPanel panel = new JPanel(); MonthlyActionsEditor(YearMonth period) { this.period = period; JButton view = new JButton("View"), download = new JButton("Download"); view.addActionListener(e -> { showMonthly(person, period); fireEditingStopped(); }); download.addActionListener(e -> { downloadMonthly(person, period); fireEditingStopped(); }); panel.add(view); panel.add(download); } public Component getTableCellEditorComponent(JTable t, Object value, boolean s, int row, int col) { person = (Employee) value; return panel; } public Object getCellEditorValue() { return person; } }
    private class ITActionsRenderer extends JPanel implements TableCellRenderer { ITActionsRenderer() { add(new JButton("View")); add(new JButton("Download")); } public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) { return this; } }
    private class ITActionsEditor extends AbstractCellEditor implements TableCellEditor { private final int start; private final YearMonth period; private Employee person; private final JPanel panel = new JPanel(); ITActionsEditor(int start, YearMonth period) { this.start = start; this.period = period; JButton view = new JButton("View"), download = new JButton("Download"); view.addActionListener(e -> { showIT(person, start, period); fireEditingStopped(); }); download.addActionListener(e -> { downloadIT(person, start, period); fireEditingStopped(); }); panel.add(view); panel.add(download); } public Component getTableCellEditorComponent(JTable t, Object value, boolean s, int row, int col) { person = (Employee) value; return panel; } public Object getCellEditorValue() { return person; } }
    private class Form16ActionsRenderer extends JPanel implements TableCellRenderer { Form16ActionsRenderer() { add(new JButton("View / Edit")); add(new JButton("Download Excel")); } public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) { return this; } }
    private class Form16ActionsEditor extends AbstractCellEditor implements TableCellEditor { private final int start; private final String fy; private Employee person; private final JPanel panel = new JPanel(); Form16ActionsEditor(int start, String fy) { this.start = start; this.fy = fy; JButton edit = new JButton("View / Edit"), download = new JButton("Download Excel"); edit.addActionListener(e -> { openForm16(person, start, fy); fireEditingStopped(); }); download.addActionListener(e -> { downloadForm16(person, start, fy); fireEditingStopped(); }); panel.add(edit); panel.add(download); } public Component getTableCellEditorComponent(JTable t, Object value, boolean s, int row, int col) { person = (Employee) value; return panel; } public Object getCellEditorValue() { return person; } }
    private void showAnnual(Employee person, int start, String fy) { if (person == null) return; JScrollPane preview = new JScrollPane(AnnualSalaryPDF.previewPanel(person, start, fy)); preview.setPreferredSize(new Dimension(1100, 620)); JOptionPane.showMessageDialog(this, preview, "Annual Salary Breakup - " + fy, JOptionPane.PLAIN_MESSAGE); }
    private void downloadAnnual(Employee person, int start, String fy) { if (person == null) return; JFileChooser chooser = new JFileChooser(); chooser.setSelectedFile(new File(AnnualSalaryPDF.name(person, fy))); if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) { File temp = AnnualSalaryPDF.create(person, start, fy, chooser.getSelectedFile().getParentFile()); if (temp != null) try { Files.move(temp.toPath(), chooser.getSelectedFile().toPath(), StandardCopyOption.REPLACE_EXISTING); } catch (Exception e) { JOptionPane.showMessageDialog(this, "Download failed."); } } }
    private void showMonthly(Employee person, YearMonth period) { if (person == null) return; PayrollCalculator.Result result = PayslipPDF.result(person, period); JOptionPane.showMessageDialog(this, PayslipPDF.previewText(person, result, period), "Payslip - " + period, JOptionPane.INFORMATION_MESSAGE); }
    private void downloadMonthly(Employee person, YearMonth period) { if (person == null) return; JFileChooser chooser = new JFileChooser(); chooser.setSelectedFile(new File(person.getId() + "-" + person.getName() + "-" + period + ".pdf")); if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) { File output = PayslipPDF.create(person, PayslipPDF.result(person, period), chooser.getSelectedFile().getParentFile(), period); if (output != null) try { Files.move(output.toPath(), chooser.getSelectedFile().toPath(), StandardCopyOption.REPLACE_EXISTING); } catch (Exception ignored) {} } }
    private void showIT(Employee person, int start, YearMonth period) { if (person == null) return; JScrollPane preview = new JScrollPane(ITComputationPDF.previewPanel(person, start, period)); preview.setPreferredSize(new Dimension(900, 650)); JOptionPane.showMessageDialog(this, preview, "IT Computation - " + period.getMonth() + " " + period.getYear(), JOptionPane.PLAIN_MESSAGE); }
    private void downloadIT(Employee person, int start, YearMonth period) { if (person == null) return; JFileChooser chooser = new JFileChooser(); chooser.setSelectedFile(new File(person.getId() + "-IT-Computation-" + period + ".pdf")); if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return; ApplicationTasks.execute(() -> { File generated = ITComputationPDF.create(person, start, period, chooser.getSelectedFile().getParentFile()); if (generated != null) try { Files.move(generated.toPath(), chooser.getSelectedFile().toPath(), StandardCopyOption.REPLACE_EXISTING); } catch (Exception ignored) {} SwingUtilities.invokeLater(() -> { if (chooser.getSelectedFile().isFile()) JOptionPane.showMessageDialog(this, "IT Computation PDF saved."); else JOptionPane.showMessageDialog(this, "IT PDF download failed."); }); }); }
    private void openForm16(Employee person, int start, String fy) { if (person == null) return; try { File folder = new File(System.getProperty("user.home"), "Documents/Payroll Form 16"); File file = Form16Excel.create(person, start, fy, folder); if (file != null && Desktop.isDesktopSupported()) Desktop.getDesktop().open(file); } catch (Exception e) { JOptionPane.showMessageDialog(this, "Unable to open the Form 16 Excel file."); } }
    private void downloadForm16(Employee person, int start, String fy) { if (person == null) return; JFileChooser chooser = new JFileChooser(); chooser.setSelectedFile(new File(Form16Excel.name(person, fy))); if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) { File generated = Form16Excel.create(person, start, fy, chooser.getSelectedFile().getParentFile()); if (generated != null) try { Files.move(generated.toPath(), chooser.getSelectedFile().toPath(), StandardCopyOption.REPLACE_EXISTING); } catch (Exception e) { JOptionPane.showMessageDialog(this, "Excel download failed."); } } }
    private String shortMonth(YearMonth period) { return period.getMonth().toString().substring(0, 3); }
    private void monthTab(JTabbedPane pane, int index, YearMonth period) { JLabel label = new JLabel(shortMonth(period), SwingConstants.CENTER); label.setPreferredSize(new Dimension(80, 30)); label.setForeground(Color.WHITE); pane.setTabComponentAt(index, label); }
    private void styleFinancialLabels() { for (int i = 0; i < financialMonths.getTabCount(); i++) if (financialMonths.getTabComponentAt(i) instanceof JLabel label) label.setForeground(Color.WHITE); }
}
