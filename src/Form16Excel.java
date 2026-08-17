import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Editable Form 16-style annual salary workbook without sheet protection. */
public final class Form16Excel {
    private static final String[] HEADERS = {"Month", "Basic", "HRA", "Spl Allow", "Fixed Allow", "Medical Exp", "Conveyance", "Bonus / Other", "Gross Salary", "PF-EE", "PT", "LWF", "ESIC", "TDS", "Net Salary"};
    public static String name(Employee employee, String fy) { return safe(employee.getId()) + "-" + safe(employee.getName()) + "-Form16-" + fy + ".xlsx"; }

    public static File create(Employee employee, int startYear, String fy, File folder) {
        try {
            if (folder == null) return null;
            folder.mkdirs(); File output = new File(folder, name(employee, fy));
            try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(output))) {
                entry(zip, "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/></Types>");
                entry(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
                entry(zip, "xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Form 16\" sheetId=\"1\" r:id=\"rId1\"/></sheets><calcPr calcMode=\"auto\" fullCalcOnLoad=\"1\" forceFullCalc=\"1\"/></workbook>");
                entry(zip, "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");
                entry(zip, "xl/styles.xml", styles());
                entry(zip, "xl/worksheets/sheet1.xml", sheet(employee, startYear, fy));
            }
            return output;
        } catch (Exception ignored) { return null; }
    }

    private static String sheet(Employee e, int start, String fy) {
        StringBuilder s = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><dimension ref=\"A1:O18\"/><sheetViews><sheetView workbookViewId=\"0\"><selection activeCell=\"A6\" sqref=\"A6\"/></sheetView></sheetViews><sheetFormatPr defaultRowHeight=\"18\"/><cols>");
        for (int c = 1; c <= HEADERS.length; c++) s.append("<col min=\"").append(c).append("\" max=\"").append(c).append("\" width=\"").append(c == 1 ? 14 : 15).append("\" customWidth=\"1\"/>");
        s.append("</cols><sheetData>");
        row(s, 1, text("A1", "FORM 16 - SALARY BREAKDOWN FOR FY " + fy, 2));
        row(s, 2, text("A2", "Employee Name", 1) + text("B2", e.getName(), 0) + text("D2", "PAN Number", 1) + text("E2", e.getPan(), 0));
        row(s, 3, text("A3", "Employee ID", 1) + text("B3", e.getId(), 0) + text("D3", "Financial Year", 1) + text("E3", fy, 0));
        StringBuilder headers = new StringBuilder(); for (int c = 0; c < HEADERS.length; c++) headers.append(text(ref(c, 5), HEADERS[c], 3)); row(s, 5, headers.toString());
        for (int i = 0; i < 12; i++) {
            YearMonth period = period(start, i); PayrollCalculator.Result result = PayslipPDF.result(e, period); CTCStore.Value c = CTCStore.get(e.getId()); CTCStore.Value other = CTCStore.getOther(e.getId(), period.toString()); DeductionStore.Value d = DeductionStore.get(e.getId(), period.toString()); int r = i + 6;
            StringBuilder cells = new StringBuilder(); cells.append(text(ref(0, r), period.getMonth().toString().substring(0, 3) + "-" + String.format("%02d", period.getYear() % 100), 0));
            cells.append(number(ref(1, r), money(result.earnings.get("Monthly Basic Pay")), 4)); cells.append(number(ref(2, r), c.hra, 4)); cells.append(number(ref(3, r), c.special, 4)); cells.append(number(ref(4, r), c.fixed, 4)); cells.append(number(ref(5, r), c.medical, 4)); cells.append(number(ref(6, r), c.conveyance, 4)); cells.append(number(ref(7, r), c.attendance + other.other(), 4));
            cells.append(formula(ref(8, r), "SUM(B" + r + ":H" + r + ")", 4)); cells.append(number(ref(9, r), d.epf, 4)); cells.append(number(ref(10, r), d.pt, 4)); cells.append(number(ref(11, r), 0, 4)); cells.append(number(ref(12, r), d.esic, 4)); cells.append(number(ref(13, r), d.tds, 4)); cells.append(formula(ref(14, r), "I" + r + "-SUM(J" + r + ":N" + r + ")", 4)); row(s, r, cells.toString());
        }
        StringBuilder totals = new StringBuilder(text("A18", "TOTAL", 3)); for (int c = 1; c < HEADERS.length; c++) totals.append(formula(ref(c, 18), "SUM(" + col(c) + "6:" + col(c) + "17)", 5)); row(s, 18, totals.toString());
        s.append("</sheetData><mergeCells count=\"1\"><mergeCell ref=\"A1:O1\"/></mergeCells><pageMargins left=\"0.25\" right=\"0.25\" top=\"0.5\" bottom=\"0.5\" header=\"0.2\" footer=\"0.2\"/></worksheet>");
        return s.toString();
    }
    private static String styles() { return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><fonts count=\"3\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"14\"/><name val=\"Calibri\"/></font></fonts><fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFD9EAD3\"/><bgColor indexed=\"64\"/></patternFill></fill></fills><borders count=\"2\"><border/><border><left style=\"thin\"/><right style=\"thin\"/><top style=\"thin\"/><bottom style=\"thin\"/></border></borders><cellXfs count=\"6\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\"/><xf numFmtId=\"0\" fontId=\"2\" fillId=\"2\" borderId=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf><xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf><xf numFmtId=\"4\" fontId=\"0\" fillId=\"0\" borderId=\"1\" applyAlignment=\"1\"><alignment horizontal=\"right\"/></xf><xf numFmtId=\"4\" fontId=\"1\" fillId=\"2\" borderId=\"1\" applyAlignment=\"1\"><alignment horizontal=\"right\"/></xf></cellXfs></styleSheet>"; }
    private static void entry(ZipOutputStream zip, String path, String text) throws IOException { zip.putNextEntry(new ZipEntry(path)); zip.write(text.getBytes(StandardCharsets.UTF_8)); zip.closeEntry(); }
    private static void row(StringBuilder s, int row, String cells) { s.append("<row r=\"").append(row).append("\">").append(cells).append("</row>"); }
    private static String text(String ref, String value, int style) { return "<c r=\"" + ref + "\" s=\"" + style + "\" t=\"inlineStr\"><is><t>" + xml(value) + "</t></is></c>"; }
    private static String number(String ref, double value, int style) { return "<c r=\"" + ref + "\" s=\"" + style + "\"><v>" + Money.round(value) + "</v></c>"; }
    private static String formula(String ref, String formula, int style) { return "<c r=\"" + ref + "\" s=\"" + style + "\"><f>" + formula + "</f><v>0</v></c>"; }
    private static YearMonth period(int start, int index) { int month = index + 4; return month <= 12 ? YearMonth.of(start, month) : YearMonth.of(start + 1, month - 12); }
    private static String ref(int col, int row) { return col(col) + row; }
    private static String col(int zeroBased) { StringBuilder out = new StringBuilder(); int n = zeroBased + 1; while (n > 0) { int rem = (n - 1) % 26; out.insert(0, (char) ('A' + rem)); n = (n - 1) / 26; } return out.toString(); }
    private static double money(Double value) { return Money.round(value == null ? 0 : value); }
    private static String xml(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    private static String safe(String value) { return value == null ? "Employee" : value.replaceAll("[^a-zA-Z0-9_-]", "_"); }
    private Form16Excel() {}
}
