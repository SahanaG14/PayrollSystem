import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.sql.*;

public final class ITComputationPanel extends JPanel {
    private static final String SELECT_EMPLOYEE = "-- Select Employee --";
    private final EmployeeDAO employees = new EmployeeDAO();
    private final JComboBox<Object> employee = new JComboBox<>();
    private final JComboBox<String> financialYear = new JComboBox<>();
    private final JComboBox<String> month = new JComboBox<>(), regime = new JComboBox<>(new String[]{"Old Regime", "New Regime"});
    private final JTextField id = ro(), name = ro(), pan = ro(), gender = ro(), tan = ro(), dob = ro(), taxYear = ro(), companyPan = ro(), joining = ro();
    private boolean recalculating, refreshingEmployees;
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Heads of Income", "Actual", "Projected", "Total"}, 0) {
        public boolean isCellEditable(int row, int column) { return column > 0 && column < 3 && editableRow(row); }
    };
    private final JTable table = new JTable(model);
    private final JButton save = new JButton("Save IT Computation"), pdf = new JButton("Download as PDF"), excel = new JButton("Export to Excel"), print = new JButton("Print");

    public ITComputationPanel() {
        super(new BorderLayout(8, 8));
        employee.addItem(SELECT_EMPLOYEE);
        for (Employee e : employees.listActive("")) employee.addItem(e);
        populateFinancialYears();
        for (int i = 0; i < 12; i++) month.addItem(FinancialYear.month(FinancialYear.currentStart(), i).getMonth().toString());
        month.setSelectedItem("APRIL");
        regime.setSelectedItem("New Regime");

        JPanel top = new JPanel(new BorderLayout());
        JPanel choose = new JPanel(new FlowLayout(FlowLayout.LEFT));
        choose.add(new JLabel("Select Employee:")); choose.add(employee);
        choose.add(new JLabel("Financial Year:")); choose.add(financialYear);
        top.add(choose, BorderLayout.NORTH);
        JPanel meta = new JPanel(new GridLayout(4, 6, 8, 5));
        addMeta(meta, "Employee ID", id, "Employee Name", name, "", new JLabel());
        addMeta(meta, "Employee PAN", pan, "Gender", gender, "Company TAN", tan);
        addMeta(meta, "Date of Birth", dob, "Tax Year", taxYear, "Company PAN", companyPan);
        addMeta(meta, "Date of Joining", joining, "Month", month, "Regime Type", regime);
        top.add(meta, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        table.setRowHeight(27);
        model.addTableModelListener(e -> {
            if (!recalculating && e.getType() == TableModelEvent.UPDATE && e.getColumn() >= 1 && e.getColumn() <= 2) recalculateEditedValues(e.getFirstRow());
        });
        for (int c = 1; c < 4; c++) table.getColumnModel().getColumn(c).setCellRenderer(Money.renderer());
        UIStyleUtility.applyProfessionalTableStyle(table);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton all = new JButton("Select All"), printAll = new JButton("Print All");
        save.addActionListener(e -> saveComputation());
        pdf.addActionListener(e -> pdf()); excel.addActionListener(e -> excel()); print.addActionListener(e -> print());
        all.addActionListener(e -> { if (employee.getItemCount() > 1) employee.setSelectedIndex(1); });
        printAll.addActionListener(e -> printAll());
        for (JButton b : new JButton[]{save, pdf, excel, print, all, printAll}) actions.add(b);
        add(actions, BorderLayout.SOUTH);
        employee.addActionListener(e -> { if (!refreshingEmployees) refreshIT(); }); financialYear.addActionListener(e -> { refreshEligibleEmployees(); refreshIT(); }); month.addActionListener(e -> { refreshEligibleEmployees(); refreshIT(); }); regime.addActionListener(e -> refreshIT());
        refreshEligibleEmployees();
        employee.setSelectedIndex(0);
        refreshIT();
    }

    private void refreshEligibleEmployees() { if (financialYear.getSelectedItem()==null || month.getSelectedItem()==null) return; Employee selected=current();int start=FinancialYear.parse(String.valueOf(financialYear.getSelectedItem()));int value=Month.valueOf(String.valueOf(month.getSelectedItem())).getValue();YearMonth period=FinancialYear.month(start,value>=4?value-4:value+8);refreshingEmployees=true;employee.removeAllItems();employee.addItem(SELECT_EMPLOYEE);for(Employee person:employees.listForMonth("",period))employee.addItem(person);if(selected!=null)for(int i=1;i<employee.getItemCount();i++)if(((Employee)employee.getItemAt(i)).getId().equals(selected.getId())){employee.setSelectedIndex(i);break;}refreshingEmployees=false; }
    private static void addMeta(JPanel p, String l1, JComponent f1, String l2, JComponent f2, String l3, JComponent f3) { for (Object value : new Object[]{new JLabel(l1), f1, new JLabel(l2), f2, new JLabel(l3), f3}) { JComponent component = (JComponent) value; component.setFont(new Font("SansSerif", component instanceof JLabel ? Font.BOLD : Font.PLAIN, 16)); p.add(component); } }
    private static JTextField ro() { JTextField f = new JTextField(); f.setEditable(false); return f; }
    private void populateFinancialYears() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        for (int year = currentYear + 3; year >= currentYear - 5; year--) financialYear.addItem(year + "-" + (year + 1));
        int activeStart = today.getMonthValue() >= 4 ? currentYear : currentYear - 1;
        financialYear.setSelectedItem(activeStart + "-" + (activeStart + 1));
    }

    private void refreshIT() {
        Employee e = current();
        boolean active = e != null && "New Regime".equals(regime.getSelectedItem());
        for (JButton b : new JButton[]{save, pdf, excel, print}) b.setEnabled(active);
        model.setRowCount(0);
        if (e == null) {
            clearMetadata();
            if ("New Regime".equals(regime.getSelectedItem())) populateNewRegimeTemplate();
            return;
        }
        updateMetadata(e);
        if (active) recalculateITTable(e);
    }

    private void saveComputation(){Employee e=current();if(e==null)return;try(Connection c=DBConnection.getConnection();PreparedStatement p=c==null?null:c.prepareStatement("INSERT INTO it_computation(employee_id,employee_name,month,financial_year,tax_regime,actual_amount,projected_amount,total_amount,gross_salary,less_exemption_u_s_10,net_salary,less_deduction_u_s_16,standard_deduction,tax_on_employment,net_taxable_salary,tax_payable_on_total_income,less_relief_u_s_87a,tax_payable,add_surcharge,add_cess,total_tax_payable,total_tax_payable_rounded,tax_recovered_till_previous_month,tax_deducted_current_month,total_tax_deducted,balance_tax_payable_remaining_months) SELECT ?,employee_name,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,? FROM employee_master_data WHERE employee_id=? AND is_deleted=0")){if(p==null)return;c.setAutoCommit(false);int i=1;p.setString(i++,e.getId());p.setString(i++,String.valueOf(month.getSelectedItem()));p.setString(i++,String.valueOf(financialYear.getSelectedItem()));p.setString(i++,String.valueOf(regime.getSelectedItem()));double actual=0,projected=0,total=0;for(int r=0;r<model.getRowCount();r++){actual+=amount(model.getValueAt(r,1));projected+=amount(model.getValueAt(r,2));total+=amount(model.getValueAt(r,3));}p.setDouble(i++,actual);p.setDouble(i++,projected);p.setDouble(i++,total);for(int r:new int[]{13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30})p.setDouble(i++,r<model.getRowCount()?amount(model.getValueAt(r,3)):0);p.setString(i,e.getId());p.executeUpdate();c.commit();ActivityLogger.log("Salary","IT COMPUTATION SAVED",e.getId(),"SUCCESS");JOptionPane.showMessageDialog(this,"IT Computation saved successfully.");}catch(Exception ex){JOptionPane.showMessageDialog(this,"Unable to save IT Computation: "+ex.getMessage());}}

    private void clearMetadata() { for (JTextField f : new JTextField[]{id, name, pan, gender, tan, dob, companyPan, joining}) f.setText("-"); taxYear.setText(String.valueOf(financialYear.getSelectedItem())); }
    private void updateMetadata(Employee e) { id.setText(s(e.getId())); name.setText(s(e.getName())); pan.setText(s(e.getPan())); gender.setText(s(e.getGender())); tan.setText(s(CompanyDetailsStore.tan())); dob.setText(s(e.getDob())); taxYear.setText(String.valueOf(financialYear.getSelectedItem())); companyPan.setText(s(CompanyDetailsStore.pan())); joining.setText(s(e.getJoiningDate())); }

    private void populateNewRegimeTemplate() {
        String[] labels = {"Income from Salary", "  Basic", "  House Rent Allowance", "  Attendance Bonus", "  Conveyance Allowance", "  Performance Pay", "  Medical Allowance", "  Special Allowance", "  Fixed Allowance", "  Arrears", "  Annual Bonus", "Perquisites", "Profits in lieu of salary", "Gross Salary", "Less Exemption U/s 10", "Net Salary", "Less Deduction U/s 16", "  Standard Deduction", "  Tax on Employment", "Net Taxable Salary", "Tax Payable on Total Income", "Less Relief U/s 87 A", "Tax Payable", "Add Surcharge", "Add Cess", "Total Tax Payable", "Total Tax Payable (Rounded Off)", "Tax Recovered Till Previous Month", "Tax Deducted Current Month", "Total Tax Deducted", "Balance Tax Payable for remaining months"};
        for (String label : labels) model.addRow(new Object[]{label, "", "", ""});
    }

    private void recalculateITTable(Employee e) {
        populateNewRegimeTemplate();
        int start = FinancialYear.parse(String.valueOf(financialYear.getSelectedItem())), processed = month.getSelectedIndex() + 1;
        double[] actual = new double[10], projected = new double[10];
        for (int i = 0; i < processed; i++) addActual(actual, e, FinancialYear.month(start, i));
        for (int i = processed; i < 12; i++) addProjected(projected, e, FinancialYear.month(start, i));
        if (processed == 12) actual[9] = CTCStore.get(e.getId()).annualBonus; else projected[9] = CTCStore.get(e.getId()).annualBonus;
        for (int i = 0; i < 10; i++) setAmounts(i + 1, actual[i], projected[i], actual[i] + projected[i]);
        double annualGross = 0; for (int i = 0; i < 10; i++) annualGross += actual[i] + projected[i];
        setAmounts(14, 0, 0, 0); setAmounts(17, 0, 0, 75000); setAmounts(18, 0, 0, PayrollCalculator.annualProfessionalTax(annualGross));
        recalculateEditedValues(-1);
    }

    private void addActual(double[] values, Employee e, YearMonth period) {
        PayrollCalculator.Result result = PayslipPDF.result(e, period); CTCStore.Value c = CTCStore.get(e.getId()); MonthlyEarningsStore.Value saved = MonthlyEarningsStore.get(e.getId(), period.toString());
        values[0] += amount(result.earnings.get("Monthly Basic Pay")); values[1] += c.hra; values[2] += c.attendance; values[3] += c.conveyance; values[4] += c.performance; values[5] += c.medical; values[6] += c.special; values[7] += c.fixed; values[8] += saved == null ? 0 : saved.arrears;
    }

    private void addProjected(double[] values, Employee e, YearMonth period) {
        double basic = SalaryRevisionStore.basicFor(e, period), hra = Money.round(basic * PayrollRulesStore.hraPercent() / 100.0);
        values[0] += basic; values[1] += hra; values[2] += configuredAllowance("Attendance Bonus", basic); values[3] += configuredAllowance("Conveyance Allowance", basic); values[4] += configuredAllowance("Performance Pay", basic); values[5] += configuredAllowance("Medical Allowance", basic); values[6] += configuredAllowance("Special Allowance", basic); values[7] += configuredAllowance("Fixed Allowance", basic);
    }

    private static double configuredAllowance(String allowance, double basic) { double value = CompanyPolicyStore.allowance(allowance); return value <= 0 ? 0 : (CompanyPolicyStore.percentage(allowance) ? Money.round(value * basic / 100.0) : value); }
    private static double amount(Object value) { return value instanceof Number ? ((Number) value).doubleValue() : 0; }
    private static boolean editableRow(int row) { return row >= 1 && row <= 12; }

    private void recalculateEditedValues(int editedRow) {
        if (model.getRowCount() == 0) return;
        recalculating = true;
        try {
            if (editedRow >= 0) setAmounts(editedRow, valueAt(editedRow, 1), valueAt(editedRow, 2), valueAt(editedRow, 1) + valueAt(editedRow, 2));
            for (int row = 0; row < model.getRowCount(); row++) if (row != editedRow) setAmounts(row, valueAt(row, 1), valueAt(row, 2), valueAt(row, 1) + valueAt(row, 2));
            for (int c = 1; c < 4; c++) {
                double gross = sum(c, 1, 12), exemption = valueAt(14, c), netSalary = Math.max(0, gross - exemption), deduction = valueAt(17, c) + valueAt(18, c), netTaxable = Math.max(0, netSalary - deduction), taxable = netTaxable, deducted = valueAt(27, c) + valueAt(28, c);
                TaxCalculator.TaxResult tax = TaxCalculator.calculateNewRegimeTax(taxable);
                double balance = Math.max(0, tax.totalTaxPayable() - deducted);
                setValue(13, c, gross); setValue(15, c, netSalary); setValue(19, c, netTaxable); setValue(20, c, tax.slabTax()); setValue(21, c, tax.rebate87A()); setValue(22, c, tax.taxPayable()); setValue(23, c, tax.surcharge()); setValue(24, c, tax.cess()); setValue(25, c, tax.totalTaxPayable()); setValue(26, c, Math.round(tax.totalTaxPayable())); setValue(29, c, deducted); setValue(30, c, balance);
            }
        } finally { recalculating = false; }
    }

    private double sum(int column, int from, int to) { double total = 0; for (int row = from; row <= to; row++) total += valueAt(row, column); return total; }
    private double valueAt(int row, int column) { return amount(model.getValueAt(row, column)); }
    private void setValue(int row, int column, double value) { model.setValueAt(Money.round(value), row, column); }

    private void setAmounts(int row, double actual, double projected, double total) { model.setValueAt(actual, row, 1); model.setValueAt(projected, row, 2); model.setValueAt(total, row, 3); }
    private Employee current() { return employee.getSelectedItem() instanceof Employee e ? e : null; }
    private static String s(String v) { return v == null ? "" : v; }
    private String downloadFileName(Employee e) { return e.getId() + "_" + e.getName().replaceAll("\\s+", "_") + "_IT_Computation"; }

    private void pdf() {
        Employee e = current(); if (e == null) return;
        JFileChooser fileChooser = new JFileChooser(); fileChooser.setSelectedFile(new File(downloadFileName(e) + ".pdf"));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File selected = fileChooser.getSelectedFile(); if (!selected.getName().toLowerCase().endsWith(".pdf")) selected = new File(selected.getParentFile(), selected.getName() + ".pdf");
        int start = FinancialYear.parse(String.valueOf(financialYear.getSelectedItem()));
        File generated = ITComputationPDF.create(e, start, FinancialYear.month(start, month.getSelectedIndex()), selected.getParentFile());
        if (generated != null) try { Files.move(generated.toPath(), selected.toPath(), StandardCopyOption.REPLACE_EXISTING); } catch (IOException ignored) { }
    }

    private void excel() {
        Employee e = current(); if (e == null) return;
        java.util.List<Object[]> rows = new ArrayList<>(); boolean totalOnly = false;
        for (int r = 0; r < model.getRowCount(); r++) { String label = String.valueOf(model.getValueAt(r, 0)); if (label.trim().equals("Standard Deduction")) totalOnly = true; rows.add(new Object[]{label, totalOnly ? "" : model.getValueAt(r, 1), totalOnly ? "" : model.getValueAt(r, 2), model.getValueAt(r, 3)}); }
        PayrollExcel.exportBoldHeader(this, "IT Computation", downloadFileName(e) + ".xlsx", new String[]{"Heads of Income", "Actual", "Projected", "Total"}, rows);
    }

    private void print() { Employee e = current(); int start = FinancialYear.parse(String.valueOf(financialYear.getSelectedItem())); if (e != null) try { File f = ITComputationPDF.create(e, start, FinancialYear.month(start, month.getSelectedIndex()), new File(System.getProperty("java.io.tmpdir"))); if (f != null && Desktop.isDesktopSupported()) Desktop.getDesktop().print(f); } catch (Exception x) { x.printStackTrace(); } }
    private void printAll() { int start = FinancialYear.parse(String.valueOf(financialYear.getSelectedItem())); for (Employee e : employees.listActive("")) { try { File f = ITComputationPDF.create(e, start, FinancialYear.month(start, month.getSelectedIndex()), new File("IT-Computation")); if (f != null && Desktop.isDesktopSupported()) Desktop.getDesktop().print(f); } catch (Exception x) { x.printStackTrace(); } } }
}
