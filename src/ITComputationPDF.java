import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.YearMonth;

/** A single A4 IT-computation statement: actual FY income through a selected month plus projected balance. */
public final class ITComputationPDF {
    private static final String[] COMPONENTS = {"Basic", "HRA", "Attendance Bonus", "Conv (Conveyance)", "Perf (Performance Pay)", "Medic (Medical Allowance)", "Special (Special Allowance)", "Fixed (Fixed Allowance)"};

    public static File create(Employee employee, int financialYearStart, YearMonth through, File folder) {
        if (folder == null) return null;
        try {
            folder.mkdirs();
            File output = ExportFileName.unique(new File(folder, safe(employee.getId()) + "-IT-Computation-" + through + ".pdf"));
            PayslipPDF.Logo logo = PayslipPDF.Logo.load();
            String content = pdfContent(employee, financialYearStart, through) + (logo == null ? "" : "q 38 0 0 38 44 780 cm /Logo Do Q\n");
            byte[] stream = content.getBytes(StandardCharsets.ISO_8859_1);
            String resources = "/Font<</F1 4 0 R/F2 5 0 R>>" + (logo == null ? "" : "/XObject<</Logo 7 0 R>>");
            ByteArrayOutputStream pdf = new ByteArrayOutputStream();
            pdf.write(("%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj\n3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 595 842]/Resources<<" + resources + ">>/Contents 6 0 R>>endobj\n4 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj\n5 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica-Bold>>endobj\n6 0 obj<</Length " + stream.length + ">>stream\n" + content + "\nendstream endobj\n").getBytes(StandardCharsets.ISO_8859_1));
            if (logo != null) {
                pdf.write(("7 0 obj<</Type/XObject/Subtype/Image/Width " + logo.width + "/Height " + logo.height + "/ColorSpace/DeviceRGB/BitsPerComponent 8/Filter/DCTDecode/Length " + logo.data.length + ">>stream\n").getBytes(StandardCharsets.ISO_8859_1));
                pdf.write(logo.data); pdf.write("\nendstream endobj\n".getBytes(StandardCharsets.ISO_8859_1));
            }
            pdf.write("trailer<</Root 1 0 R>>\n%%EOF".getBytes(StandardCharsets.ISO_8859_1));
            Files.write(output.toPath(), pdf.toByteArray());
            return output;
        } catch (Exception ignored) { return null; }
    }

    public static JComponent previewPanel(Employee employee, int financialYearStart, YearMonth through) {
        Data data = data(employee, financialYearStart, through);
        JPanel page = new JPanel(new BorderLayout(7, 7));
        page.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK), BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        page.setBackground(Color.WHITE);
        page.add(previewHeader(employee, financialYearStart, through), BorderLayout.NORTH);

        Object[][] rows = new Object[COMPONENTS.length + 1][4];
        for (int i = 0; i < COMPONENTS.length; i++) rows[i] = new Object[]{COMPONENTS[i], Money.text(data.actual[i]), Money.text(data.projected[i]), Money.text(data.total[i])};
        rows[COMPONENTS.length] = new Object[]{"GROSS SALARY", Money.text(data.actualGross), Money.text(data.projectedGross), Money.text(data.gross)};
        DefaultTableModel model = new DefaultTableModel(rows, new String[]{"INCOME", "ACTUAL", "PROJECTED", "TOTAL"}) { public boolean isCellEditable(int row, int column) { return false; } };
        JTable table = new JTable(model); table.setRowHeight(25); table.setGridColor(Color.BLACK); table.setShowGrid(true);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13)); table.getTableHeader().setBackground(new Color(221, 237, 211));
        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setPreferredWidth(240);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer(); right.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c = 1; c < 4; c++) table.getColumnModel().getColumn(c).setCellRenderer(right);
        JLabel incomeTitle = new JLabel("INCOME COMPUTATION", SwingConstants.CENTER); incomeTitle.setOpaque(true); incomeTitle.setBackground(new Color(221, 237, 211)); incomeTitle.setFont(new Font("SansSerif", Font.BOLD, 14)); incomeTitle.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        JPanel income = new JPanel(new BorderLayout()); income.add(incomeTitle, BorderLayout.NORTH); income.add(new JScrollPane(table), BorderLayout.CENTER); page.add(income, BorderLayout.CENTER);

        JPanel tax = new JPanel(new GridLayout(3, 2, 0, 0)); tax.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        addTax(tax, "Net Taxable Salary", Money.text(data.netTaxable)); addTax(tax, "Taxable Income", Money.text(data.netTaxable)); addTax(tax, "Tax Payable", Money.text(data.taxPayable));
        page.add(tax, BorderLayout.SOUTH);
        return page;
    }

    private static JComponent previewHeader(Employee e, int fy, YearMonth month) {
        JPanel header = new JPanel(new BorderLayout()); header.setBackground(Color.WHITE);
        JLabel brand = new JLabel("  " + CompanyDetailsStore.name() + "\n", SwingConstants.CENTER); brand.setOpaque(true); brand.setBackground(new Color(221, 237, 211)); brand.setFont(new Font("SansSerif", Font.BOLD, 17));
        JLabel address = new JLabel(CompanyDetailsStore.address(), SwingConstants.CENTER); address.setOpaque(true); address.setBackground(new Color(221, 237, 211));
        JPanel heading = new JPanel(new GridLayout(0, 1)); heading.add(brand); heading.add(address); JLabel title = new JLabel("IT COMPUTATION - FY " + fy(fy), SwingConstants.CENTER); title.setFont(new Font("SansSerif", Font.BOLD, 15)); title.setOpaque(true); title.setBackground(new Color(221, 237, 211)); heading.add(title); header.add(heading, BorderLayout.NORTH);
        JPanel details = new JPanel(new GridLayout(4, 2)); details.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        detail(details, "Employee ID: " + e.getId()); detail(details, "PAN: " + e.getPan()); detail(details, "Name: " + e.getName()); detail(details, "Date of Joining: " + e.getJoiningDate()); detail(details, "Gender: " + e.getGender()); detail(details, "Month: " + month.getMonth() + "-" + month.getYear()); detail(details, "DOB: " + e.getDob()); detail(details, "FY: " + fy(fy));
        header.add(details, BorderLayout.CENTER); return header;
    }

    private static void detail(JPanel panel, String text) { JLabel label = new JLabel("  " + text); label.setBorder(BorderFactory.createLineBorder(Color.BLACK)); panel.add(label); }
    private static void addTax(JPanel panel, String label, String value) { JLabel left = new JLabel("  " + label); left.setFont(new Font("SansSerif", Font.BOLD, 13)); left.setBorder(BorderFactory.createLineBorder(Color.BLACK)); JLabel right = new JLabel(value + "  ", SwingConstants.RIGHT); right.setBorder(BorderFactory.createLineBorder(Color.BLACK)); panel.add(left); panel.add(right); }

    private static Data data(Employee employee, int start, YearMonth through) {
        int elapsed = Math.max(1, Math.min(12, (through.getYear() - start) * 12 + through.getMonthValue() - 3));
        Data data = new Data();
        for (int index = 0; index < 12; index++) {
            YearMonth period = FinancialYear.month(start, index);
            double[] values = index < elapsed ? actual(employee, period) : projected(employee, period);
            for (int column = 0; column < COMPONENTS.length; column++) {
                if (index < elapsed) data.actual[column] += values[column]; else data.projected[column] += values[column];
            }
        }
        for (int i = 0; i < COMPONENTS.length; i++) { data.actual[i] = Money.round(data.actual[i]); data.projected[i] = Money.round(data.projected[i]); data.total[i] = Money.round(data.actual[i] + data.projected[i]); data.actualGross += data.actual[i]; data.projectedGross += data.projected[i]; }
        data.actualGross = Money.round(data.actualGross); data.projectedGross = Money.round(data.projectedGross); data.gross = Money.round(data.actualGross + data.projectedGross); data.netTaxable = Money.round(Math.max(0, data.gross - 75000)); data.taxPayable = 0; return data;
    }

    private static double[] actual(Employee employee, YearMonth period) {
        PayrollCalculator.Result result = PayslipPDF.result(employee, period); CTCStore.Value c = CTCStore.get(employee.getId());
        return new double[]{number(result.earnings.get("Monthly Basic Pay")), c.hra, c.attendance, c.conveyance, c.performance, c.medical, c.special, c.fixed};
    }

    private static double[] projected(Employee employee, YearMonth period) {
        double basic = SalaryRevisionStore.basicFor(employee, period);
        double hra = Money.round(basic * .40); double attendance = PayrollRulesStore.attendanceBonus();
        return new double[]{basic, hra, attendance, configured("Conveyance Allowance", basic), configured("Performance Pay", basic), configured("Medical Allowance", basic), SpecialAllowance.earned(employee, period, period.lengthOfMonth(), period.lengthOfMonth(), basic), configured("Fixed Allowance", basic)};
    }

    private static double configured(String name, double basic) { double amount = CompanyPolicyStore.allowance(name); return amount <= 0 ? 0 : Money.round(CompanyPolicyStore.percentage(name) ? amount * basic / 100 : amount); }
    private static double number(Double value) { return value == null ? 0 : value; }
    private static String fy(int start) { return start + "-" + String.format("%02d", (start + 1) % 100); }
    private static String safe(String value) { return value == null ? "Employee" : value.replaceAll("[^a-zA-Z0-9_-]", "_"); }

    private static String pdfContent(Employee e, int fy, YearMonth through) {
        StringBuilder s = new StringBuilder(); int x = 28, width = 539, y = 826;
        fillBox(s, x, y - 54, width, 54); border(s, x, y - 54, width, 54);
        text(s, 88, y - 18, CompanyDetailsStore.name(), 14, true);
        text(s, 88, y - 34, CompanyDetailsStore.address(), 8, false);
        text(s, 88, y - 47, "INCOME TAX COMPUTATION STATEMENT", 10, true); y -= 66;
        fillBox(s, x, y - 15, width, 15); border(s, x, y - 15, width, 15); centered(s, x + width / 2, y - 11, "EMPLOYEE DETAILS", 7, true); y -= 15;
        y -= 19; cell(s, x, y, 269, 19, "Employee ID: " + e.getId(), 8, false, false); cell(s, x + 269, y, 270, 19, "Employee Name: " + e.getName(), 8, false, false);
        String[][] details = {{"Employee PAN", e.getPan(), "Gender", e.getGender(), "Company TAN", CompanyDetailsStore.tan()}, {"Date of Birth", e.getDob(), "Tax Year", fy(fy), "Company PAN", CompanyDetailsStore.pan()}, {"Date of Joining", e.getJoiningDate(), "Month", through.getMonth().toString(), "Regime Type", "New Regime"}};
        for (String[] row : details) { y -= 19; for (int i = 0; i < 3; i++) cell(s, x + i * 180, y, i == 2 ? 179 : 180, 19, row[i * 2] + ": " + row[i * 2 + 1], 8, false, false); }
        y -= 12; int[] widths = {245, 98, 98, 98}; String[] headers = {"Heads of Income", "Actual", "Projected", "Total"}; int current = x;
        for (int i = 0; i < headers.length; i++) { fillBox(s, current, y - 15, widths[i], 15); border(s, current, y - 15, widths[i], 15); centered(s, current + widths[i] / 2, y - 11, headers[i], 8, true); current += widths[i]; } y -= 15;
        TableData table = tableData(e, fy, through);
        for (int i = 0; i < table.labels.length; i++) { y -= 14; current = x; boolean heading = table.isHeading(i), totalOnly = i >= 17, emphasis = heading || !table.labels[i].startsWith("  "); String[] row = {table.labels[i], heading || totalOnly ? "" : Money.text(table.actual[i]), heading || totalOnly ? "" : Money.text(table.projected[i]), heading ? "" : Money.text(table.total[i])}; for (int c = 0; c < 4; c++) { cell(s, current, y, widths[c], 14, row[c], 8, emphasis && c == 0, c > 0); current += widths[c]; } }
        y -= 22; text(s, x, y, "* Basis, current income projection and declarations available as on date.", 6, false);
        y -= 18; text(s, x + 20, y, "Prepared By", 7, false); text(s, x + 215, y, "Employee Signature", 7, false); text(s, x + 410, y, "Authorised Signatory", 7, false);
        return s.toString();
    }

    private static TableData tableData(Employee e, int start, YearMonth through) {
        int elapsed = Math.max(1, Math.min(12, (through.getYear() - start) * 12 + through.getMonthValue() - 3)), remainingMonths = 13 - elapsed;
        YearMonth previous = elapsed == 1 ? null : FinancialYear.month(start, elapsed - 2);
        String recoveredLabel = previous == null ? "  Tax Recovered Till Previous Month" : "  Tax Recovered Till " + previous.getMonth() + " " + previous.getYear();
        String[] labels = {"Income from Salary", "  Basic", "  House Rent Allowance", "  Attendance Bonus", "  Conveyance Allowance", "  Performance Pay", "  Medical Allowance", "  Special Allowance", "  Fixed Allowance", "  Arrears", "  Annual Bonus", "Perquisites", "Profits in lieu of salary", "Gross Salary", "Less Exemption U/s 10", "Net Salary", "Less Deduction U/s 16", "  Standard Deduction", "  Tax on Employment", "Net Taxable Salary", "Tax Payable on Total Income", "Less Relief U/s 87 A", "Tax Payable", "Add Surcharge", "Add Cess", "Total Tax Payable", "Total Tax Payable (Rounded Off)", recoveredLabel, "Tax Deducted Current Month", "Total Tax Deducted", "Balance Tax Payable for remaining months"};
        TableData result = new TableData(labels, 0, 16);
        double[] actualTotals = new double[10], projectedTotals = new double[10];
        for (int index = 0; index < 12; index++) {
            YearMonth period = FinancialYear.month(start, index);
            double[] values = index < elapsed ? actual(e, period) : projected(e, period);
            for (int component = 0; component < 8; component++) {
                if (index < elapsed) actualTotals[component] += values[component]; else projectedTotals[component] += values[component];
            }
            if (index < elapsed) { MonthlyEarningsStore.Value saved = MonthlyEarningsStore.get(e.getId(), period.toString()); if (saved != null) actualTotals[8] += saved.arrears; }
        }
        CTCStore.Value c = CTCStore.get(e.getId());
        if (elapsed == 12) actualTotals[9] = c.annualBonus; else projectedTotals[9] = c.annualBonus;
        double actual = 0, projected = 0;
        for (int i = 0; i < actualTotals.length; i++) { double a = Money.round(actualTotals[i]), p = Money.round(projectedTotals[i]); result.set(i + 1, a, p, Money.round(a + p)); actual += a; projected += p; }
        double gross = Money.round(actual + projected), standard = 75000, pt = PayrollCalculator.annualProfessionalTax(gross), taxable = Math.max(0, gross - standard - pt), recovered = taxRecoveredBefore(e, start, elapsed), currentTds = DeductionStore.get(e.getId(), through.toString()).tds; TaxCalculator.TaxResult tax = TaxCalculator.calculateNewRegimeTax(taxable); double deducted = Money.round(recovered + currentTds), balance = Math.max(0, tax.totalTaxPayable() - deducted);
        result.set(13, actual, projected, gross); result.set(14, 0, 0, 0); result.set(15, 0, 0, gross); result.set(17, 0, 0, standard); result.set(18, 0, 0, pt); result.set(19, 0, 0, taxable); result.set(20, 0, 0, tax.slabTax()); result.set(21, 0, 0, tax.rebate87A()); result.set(22, 0, 0, tax.taxPayable()); result.set(23, 0, 0, tax.surcharge()); result.set(24, 0, 0, tax.cess()); result.set(25, 0, 0, tax.totalTaxPayable()); result.set(26, 0, 0, Math.round(tax.totalTaxPayable())); result.set(27, recovered, 0, recovered); result.set(28, currentTds, 0, currentTds); result.set(29, deducted, 0, deducted); result.set(30, 0, 0, balance); return result;
    }

    private static double taxRecoveredBefore(Employee employee, int start, int elapsed) { double recovered = 0; for (int index = 0; index < elapsed - 1; index++) recovered += DeductionStore.get(employee.getId(), FinancialYear.month(start, index).toString()).tds; return Money.round(recovered); }

    private static final class TableData { final String[] labels; final double[] actual, projected, total; final java.util.Set<Integer> headings = new java.util.HashSet<>(); TableData(String[] labels, int... headingRows) { this.labels = labels; actual = new double[labels.length]; projected = new double[labels.length]; total = new double[labels.length]; for (int row : headingRows) headings.add(row); } boolean isHeading(int row) { return headings.contains(row); } void set(int row, double a, double p, double t) { actual[row] = a; projected[row] = p; total[row] = t; } }

    private static void fillBox(StringBuilder s, int x, int y, int width, int height) { s.append("0.88 0.94 0.84 rg ").append(x).append(' ').append(y).append(' ').append(width).append(' ').append(height).append(" re f 0 0 0 rg\n"); }
    private static void border(StringBuilder s, int x, int y, int width, int height) { s.append("0 0 0 RG 0.7 w ").append(x).append(' ').append(y).append(' ').append(width).append(' ').append(height).append(" re S\n"); }
    private static void cell(StringBuilder s, int x, int y, int width, int height, String value, int font, boolean bold, boolean right) { border(s, x, y, width, height); if (value == null || value.isEmpty()) return; double point = right ? x + width - 5 - value.length() * font * .48 : x + 5; text(s, (int) Math.max(x + 4, point), y + 6, value, font, bold); }
    private static void text(StringBuilder s, int x, int y, String value, int size, boolean bold) { if (value == null) value = ""; s.append("0 g BT /").append(bold ? "F2" : "F1").append(' ').append(size).append(" Tf ").append(x).append(' ').append(y).append(" Td (").append(escape(value)).append(") Tj ET\n"); }
    private static void centered(StringBuilder s, int center, int y, String value, int size, boolean bold) { text(s, (int) (center - value.length() * size * .26), y, value, size, bold); }
    private static String escape(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)").replaceAll("[^\\x20-\\x7E]", ""); }
    private static final class Data { final double[] actual = new double[COMPONENTS.length], projected = new double[COMPONENTS.length], total = new double[COMPONENTS.length]; double actualGross, projectedGross, gross, netTaxable, taxPayable; }
    private ITComputationPDF() { }
}
