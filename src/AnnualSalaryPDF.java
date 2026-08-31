import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** A4-landscape Annual Salary Breakup preview and PDF exporter. */
public final class AnnualSalaryPDF {
    private static final String[] COLUMNS = {"MONTH", "BASIC", "HRA", "SPL ALLOW.", "FIXED ALLOW.", "MEDICAL EXP.", "CONVEYANCE", "BONUS / OTHER", "GROSS SALARY", "PF-EE", "PT", "LWF", "ESIC", "TDS", "NET SALARY"};
    private static final int[] WIDTHS = {55, 56, 55, 55, 60, 61, 61, 61, 68, 44, 37, 37, 38, 37, 69};

    public static String name(Employee person, String fy) { return safe(person.getId()) + "-" + safe(person.getName()) + "-Annual-" + fy + ".pdf"; }

    public static File create(Employee person, int start, String fy, File folder) {
        try {
            if (folder == null) return null;
            folder.mkdirs();
            File out = new File(folder, name(person, fy));
            PayslipPDF.Logo logo = PayslipPDF.Logo.load();
            String stream = pdfStream(person, start, fy) + (logo == null ? "" : "q 30 0 0 30 30 539 cm /Logo Do Q\n");
            byte[] body = stream.getBytes(StandardCharsets.ISO_8859_1);
            String resources = "/Font<</F1 4 0 R/F2 5 0 R>>" + (logo == null ? "" : "/XObject<</Logo 7 0 R>>");
            ByteArrayOutputStream pdf = new ByteArrayOutputStream();
            pdf.write(("%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj\n3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 842 595]/Resources<<" + resources + ">>/Contents 6 0 R>>endobj\n4 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj\n5 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica-Bold>>endobj\n6 0 obj<</Length " + body.length + ">>stream\n" + stream + "\nendstream endobj\n").getBytes(StandardCharsets.ISO_8859_1));
            if (logo != null) { pdf.write(("7 0 obj<</Type/XObject/Subtype/Image/Width " + logo.width + "/Height " + logo.height + "/ColorSpace/DeviceRGB/BitsPerComponent 8/Filter/DCTDecode/Length " + logo.data.length + ">>stream\n").getBytes(StandardCharsets.ISO_8859_1)); pdf.write(logo.data); pdf.write("\nendstream endobj\n".getBytes(StandardCharsets.ISO_8859_1)); }
            pdf.write("trailer<</Root 1 0 R>>\n%%EOF".getBytes(StandardCharsets.ISO_8859_1));
            Files.write(out.toPath(), pdf.toByteArray());
            return out;
        } catch (Exception ignored) { return null; }
    }

    public static JComponent previewPanel(Employee person, int start, String fy) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Salary Breakup for FY " + fy);
        title.setOpaque(true); title.setBackground(new Color(220, 237, 211));
        title.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12)); title.setFont(new Font("SansSerif", Font.BOLD, 20));
        header.add(title, BorderLayout.NORTH);
        JPanel employeeInfo = new JPanel(new BorderLayout());
        JLabel employeeName = new JLabel("Employee Name: " + safe(person.getName()));
        JLabel pan = new JLabel("Employee PAN Number: " + safe(person.getPan()), SwingConstants.RIGHT);
        for (JLabel label : new JLabel[]{employeeName, pan}) label.setFont(new Font("SansSerif", Font.BOLD, 14));
        employeeInfo.add(employeeName, BorderLayout.WEST); employeeInfo.add(pan, BorderLayout.EAST); header.add(employeeInfo, BorderLayout.SOUTH);
        panel.add(header, BorderLayout.NORTH);

        Object[][] rows = rows(person, start);
        DefaultTableModel model = new DefaultTableModel(rows, COLUMNS) { public boolean isCellEditable(int row, int column) { return false; } };
        JTable table = new JTable(model);
        TabStyle.configureTable(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); table.setFont(new Font("SansSerif", Font.PLAIN, 12)); table.setRowHeight(26);
        for (int c = 0; c < COLUMNS.length; c++) {
            table.getColumnModel().getColumn(c).setPreferredWidth(WIDTHS[c] * 2);
            table.getColumnModel().getColumn(c).setCellRenderer(new AnnualCellRenderer(c));
            table.getColumnModel().getColumn(c).setHeaderRenderer(new AnnualHeaderRenderer(c));
        }
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        double annual = total(rows, 14);
        JPanel footer = new JPanel(new GridLayout(1, 1));
        footer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY), BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        for (String text : new String[]{"Net Salary for FY (" + fy + ") in figures: Rs. " + Money.text(annual)}) {
            JLabel line = new JLabel(text); line.setFont(new Font("SansSerif", Font.PLAIN, 13)); footer.add(line);
        }
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    public static String text(Employee person, int start, String fy) {
        StringBuilder text = new StringBuilder("Salary Breakup - ").append(fy).append('\n').append("Employee Name: ").append(safe(person.getName())).append("\nEmployee PAN Number: ").append(safe(person.getPan())).append("\n\n");
        for (Object[] row : rows(person, start)) text.append(Arrays.toString(row)).append('\n');
        double total = total(rows(person, start), 14);
        return text.append("\nNet Salary for Financial Year: Rs. ").append(Money.text(total)).toString();
    }

    private static Object[][] rows(Employee person, int start) {
        Object[][] rows = new Object[13][COLUMNS.length]; double[] sums = new double[COLUMNS.length];
        for (int i = 0; i < 12; i++) {
            int n = i + 4; YearMonth period = n <= 12 ? YearMonth.of(start, n) : YearMonth.of(start + 1, n - 12);
            PayrollCalculator.Result result = PayslipPDF.result(person, period); CTCStore.Value c = CTCStore.get(person.getId());
            CTCStore.Value other = CTCStore.getOther(person.getId(), period.toString()); DeductionStore.Value d = DeductionStore.get(person.getId(), period.toString()); Object[] row = rows[i];
            row[0] = period.getMonth().toString().substring(0, 3) + "-" + String.format("%02d", period.getYear() % 100);
            row[1] = value(result.earnings.get("Monthly Basic Pay")); row[2] = value(c.hra); row[3] = value(c.special); row[4] = value(c.fixed); row[5] = value(c.medical); row[6] = value(c.conveyance);
            row[7] = Money.round(other.other() + c.attendance); row[8] = value(result.gross); row[9] = value(d.epf); row[10] = value(d.pt); row[11] = 0.0; row[12] = value(d.esic); row[13] = value(d.tds); row[14] = value(result.net);
            for (int col = 1; col < COLUMNS.length; col++) sums[col] += ((Number) row[col]).doubleValue();
        }
        rows[12][0] = "TOTAL"; for (int col = 1; col < COLUMNS.length; col++) rows[12][col] = Money.round(sums[col]); return rows;
    }
    private static double value(Double value) { return Money.round(value == null ? 0 : value); }
    private static double value(double value) { return Money.round(value); }
    private static double total(Object[][] rows, int column) { return ((Number) rows[12][column]).doubleValue(); }

    private static String pdfStream(Employee person, int start, String fy) {
        final String[] headers = {"MONTH", "DAYS WORKED", "BASIC", "HRA", "ALLOWANCES", "GROSS EARNINGS", "EPF", "PT", "TDS", "TOTAL DEDUCTIONS", "NET SALARY"};
        final int[] widths = {58, 57, 62, 55, 70, 75, 48, 36, 42, 75, 70};
        String company = CompanyDetailsStore.name(); String address = CompanyDetailsStore.address();
        StringBuilder s = new StringBuilder();
        s.append("0.88 0.94 0.84 rg 24 536 794 34 re f 0 0 0 RG 24 536 794 34 re S 0 0 0 rg ");
        centerLabel(s, 421, 557, safe(company), 15, true); centerLabel(s, 421, 543, safe(address), 8, false);
        s.append("0.88 0.94 0.84 rg 24 507 794 22 re f 0 0 0 RG 24 507 794 22 re S 0 0 0 rg ");
        centerLabel(s, 421, 514, "ANNUAL SALARY STATEMENT - FINANCIAL YEAR " + fy, 13, true);
        label(s, 28, 494, "Employee ID: " + safe(person.getId()) + "    Employee Name: " + safe(person.getName()), 9, true);
        rightLabel(s, 814, 494, "Designation: " + safe(person.getDesignation()), 9, true);
        label(s, 28, 481, "PAN Number: " + safe(person.getPan()) + "    Date of Joining: " + safe(person.getJoiningDate()), 8);
        rightLabel(s, 814, 481, "Department: " + safe(person.getDepartment()), 8, false);
        Object[][] source = rows(person, start); int x = 24, y = 461, h = 18; double scale = 794.0 / Arrays.stream(widths).sum(), current = x;
        for (int c = 0; c < headers.length; c++) { headerCell(s, current, y, widths[c] * scale, h, headers[c], c == 5 || c == 10); current += widths[c] * scale; }
        double[] sums = new double[headers.length];
        for (int r = 0; r < 12; r++) { y -= h; Object[] old = source[r]; double allowances = number(old[3]) + number(old[4]) + number(old[5]) + number(old[6]) + number(old[7]); double deductions = number(old[9]) + number(old[10]) + number(old[12]) + number(old[13]); double payable = daysWorked(person, start, r); Object[] line = {old[0], payable, old[1], old[2], allowances, old[8], old[9], old[10], old[13], deductions, old[14]}; current = x; for (int c = 0; c < line.length; c++) { if (c > 0) sums[c] += number(line[c]); cell(s, current, y, widths[c] * scale, h, c == 0 ? String.valueOf(line[c]) : Money.text(number(line[c])), 6.8, false, c == 0, c == 5 || c == 10); current += widths[c] * scale; } }
        y -= h; current = x; for (int c = 0; c < headers.length; c++) { cell(s, current, y, widths[c] * scale, h, c == 0 ? "TOTAL (YTD)" : Money.text(sums[c]), 6.8, true, c == 0, c == 5 || c == 10); current += widths[c] * scale; }
        y -= 32; s.append("0 0 0 RG 24 ").append(y - 12).append(" 794 28 re S "); label(s, 36, y + 4, "Net Salary for Financial Year " + fy + ": Rs. " + Money.text(sums[10]), 10, true);
        return s.toString();
    }
    private static double number(Object value) { return value instanceof Number number ? number.doubleValue() : 0; }
    private static double daysWorked(Employee person, int start, int index) { int month = index + 4; YearMonth period = month <= 12 ? YearMonth.of(start, month) : YearMonth.of(start + 1, month - 12); AttendanceDAO dao = new AttendanceDAO(); if (!dao.hasSavedAttendance(person.getId(), period.toString())) return 0; AttendanceRecord record = dao.load(person.getId(), period.toString()); AttendanceSettings settings = new AttendanceSettingsDAO().load(); return LeaveBalanceService.payableDays(person,period,dao.workingDays(period.toString()),record.absentDays,record.paidLeaveDays,record.unpaidLeaveDays,settings); }
    private static void headerCell(StringBuilder s, double x, double y, double w, double h, String value, boolean highlight) { s.append(highlight ? "1 0.95 0.70 rg " : "0.82 0.87 0.95 rg ").append(x).append(' ').append(y).append(' ').append(w).append(' ').append(h).append(" re f 0 0 0 RG ").append(x).append(' ').append(y).append(' ').append(w).append(' ').append(h).append(" re S 0 0 0 rg "); centerLabel(s, x + w / 2, y + h / 2 - 2, value, 6.0, true); }
    private static void cell(StringBuilder s, double x, double y, double w, double h, String value, double font, boolean bold, boolean center, boolean highlight) { if (highlight) s.append("1 0.98 0.86 rg ").append(x).append(' ').append(y).append(' ').append(w).append(' ').append(h).append(" re f "); s.append("0 0 0 RG ").append(x).append(' ').append(y).append(' ').append(w).append(' ').append(h).append(" re S 0 0 0 rg "); if (center) centerLabel(s, x + w / 2, y + h / 2 - 2, value, font, bold); else rightLabel(s, x + w - 3, y + h / 2 - 2, value, font, bold); }
    private static boolean highlightColumn(int column) { return column == 8 || column == 14; }
    private static void label(StringBuilder s, double x, double y, String value, double font) { label(s, x, y, value, font, false); }
    private static void label(StringBuilder s, double x, double y, String value, double font, boolean bold) { s.append("BT /").append(bold ? "F2" : "F1").append(' ').append(font).append(" Tf ").append(x).append(' ').append(y).append(" Td (").append(escape(value)).append(") Tj ET "); }
    private static void centerLabel(StringBuilder s, double center, double y, String value, double font, boolean bold) { label(s, center - approx(value, font) / 2, y, value, font, bold); }
    private static void rightLabel(StringBuilder s, double right, double y, String value, double font, boolean bold) { label(s, right - approx(value, font), y, value, font, bold); }
    private static double approx(String value, double font) { return Math.min(790, value.length() * font * .52); }
    private static String escape(String value) { return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)"); }
    private static String safe(String value) { return value == null || value.isBlank() ? "-" : value.replaceAll("[^a-zA-Z0-9 _-]", " "); }

    private static final class AnnualCellRenderer extends DefaultTableCellRenderer {
        private final int column;
        AnnualCellRenderer(int column) { this.column = column; setHorizontalAlignment(column == 0 ? CENTER : RIGHT); }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int col) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, col);
            if (!selected) setBackground(highlightColumn(column) ? new Color(255, 251, 220) : Color.WHITE);
            setFont(table.getFont().deriveFont(row == 12 ? Font.BOLD : Font.PLAIN));
            setText(column == 0 ? String.valueOf(value) : Money.text(value instanceof Number n ? n.doubleValue() : 0)); return this;
        }
    }
    private static final class AnnualHeaderRenderer extends DefaultTableCellRenderer {
        private final int column;
        AnnualHeaderRenderer(int column) { this.column = column; setHorizontalAlignment(CENTER); setFont(new Font("SansSerif", Font.BOLD, 10)); }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int col) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, col); setBackground(highlightColumn(column) ? new Color(255, 239, 150) : new Color(220, 227, 240)); setForeground(Color.BLACK); setText(String.valueOf(value)); return this;
        }
    }
    private AnnualSalaryPDF() {}
}
