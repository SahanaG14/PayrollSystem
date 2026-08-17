import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

public class MasterDataPanel extends JPanel {
    private static final DateTimeFormatter IMPORT_DATE=DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final String[] COLUMNS = {"Employee ID","Employee Name","Gender","Date of Birth","Department","Designation","Date of Joining","Date of Leaving","Status","Salary Structure","Photo","PAN","Aadhaar Card","UAN","ESIC IP Number","Bank Name","Bank Account Number","Bank IFSC Code","Email ID","Phone Number","Actions"};
    private final PayrollSystemFrame frame;
    private final EmployeeDAO dao = new EmployeeDAO();
    private JTable table;
    private List<Employee> employees;
    private JTextField search;

    public MasterDataPanel(PayrollSystemFrame frame) {
        this.frame = frame; setLayout(new BorderLayout()); add(header(), BorderLayout.NORTH);
        table = new JTable(); table.setRowHeight(34); table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.addMouseListener(new java.awt.event.MouseAdapter() { public void mouseClicked(java.awt.event.MouseEvent event) {
            int row = table.rowAtPoint(event.getPoint()), column = table.columnAtPoint(event.getPoint()); if (row < 0 || row >= employees.size()) return;
            Employee employee = employees.get(table.convertRowIndexToModel(row));
            if (column == 10) viewFile(employee.getPhotoPath(), true); else if (column == 11) viewFile(documentPart(employee, 0), false); else if (column == 12) viewFile(documentPart(employee, 1), false);
        }});
        add(new JScrollPane(table), BorderLayout.CENTER); refreshTable();
    }
    private JComponent header() {
        JPanel panel = new JPanel(new BorderLayout()); JPanel left = new JPanel(); left.add(new JLabel("Search Employee ID:")); search = new JTextField(18);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() { public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); } public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); } public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); } });
        left.add(search); JButton export=new JButton("Export Excel"); export.addActionListener(e->exportExcel()); JButton imported=new JButton("Import Excel"); imported.addActionListener(e->importExcel()); left.add(export); left.add(imported); panel.add(left, BorderLayout.WEST); JButton add = new JButton("+ Add Employee"); add.addActionListener(e -> frame.showMasterForm(null)); panel.add(add, BorderLayout.EAST); return panel;
    }
    private static final String[] IMPORT_HEADERS={"Employee ID","Employee Name","Gender","Date of Birth","Department","Designation","Date of Joining","Date of Leaving","Status","Salary Structure","PAN","Aadhaar Number","ESIC IP Number","UAN","Bank Name","Bank Account Number","Bank IFSC Code","Email ID","Phone Number"};
    /** Full profile export: the grid-only Photo and Actions columns are deliberately omitted. */
    private static final String[] EXPORT_HEADERS={"Employee ID","Employee Name","Gender","Date of Birth","Department","Designation","Date of Joining","Date of Leaving","Status","Salary Structure","PAN","Aadhaar Number","ESIC IP Number","UAN","Bank Name","Bank Account Number","Bank IFSC Code","Email ID","Phone Number"};
    private void exportExcel(){List<Object[]> data=new java.util.ArrayList<>();for(Employee e:dao.list(""))data.add(new Object[]{e.getId(),e.getName(),e.getGender(),e.getDob(),e.getDepartment(),e.getDesignation(),e.getJoiningDate(),e.getEmploymentEndDate(),e.getStatus(),e.getSalaryStructure(),e.getPan(),e.getAadhar(),e.getEsicIpNumber(),e.getUanNumber(),e.getBankName(),e.getAccountNumber(),e.getIfsc(),e.getEmail(),e.getPhone()});PayrollExcel.export(this,"Master Data","Master-Data.xlsx",EXPORT_HEADERS,data);}
    private void importExcel(){
        Object[] modes={"Add this data to existing","Replace existing data (data will be lost permanently)"};int selected=JOptionPane.showOptionDialog(this,"Choose how imported employee records should be applied.","Import mode",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,modes,modes[0]);if(selected<0)return;
        boolean replace=selected==1;if(replace&&JOptionPane.showConfirmDialog(this,"Replace permanently deletes all existing employees and associated payroll records. Continue?","Confirm replacement",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
        try {
            PayrollExcel.Sheet sheet=PayrollExcel.importSheet(this);if(sheet==null)return;PayrollExcel.requireHeaders(sheet,IMPORT_HEADERS);
            JDialog loading=importLoadingDialog();loading.setVisible(true);
            new SwingWorker<ImportResult,Void>() {
                protected ImportResult doInBackground(){
                    try {
                        java.util.List<Employee> pending=new java.util.ArrayList<>();java.util.List<String> errors=new java.util.ArrayList<>();
                        for(int i=1;i<sheet.rows.size();i++){java.util.List<String> row=sheet.rows.get(i);if(rowEmpty(row))continue;normalizeImportDates(row);validateImportRow(row,i+1,errors);Employee e=new Employee();String employeeId=PayrollExcel.textCell(row,0).trim();e.setId(employeeId);e.setName(cell(row,1));e.setGender(cell(row,2));e.setDob(cell(row,3));e.setDepartment(cell(row,4));e.setDesignation(cell(row,5));e.setJoiningDate(cell(row,6));e.setEmploymentEndDate(cell(row,7));e.setStatus(cell(row,7).isEmpty()?"Active Employee":"Ex-Employee");e.setSalaryStructure(cell(row,9));e.setPan(cell(row,10));e.setAadhar(cell(row,11).replaceAll("\\D",""));e.setEsicIpNumber(cell(row,12));e.setUanNumber(cell(row,13));e.setBankName(cell(row,14));e.setAccountNumber(cell(row,15));e.setIfsc(cell(row,16));e.setEmail(cell(row,17));e.setPhone(cell(row,18).replaceAll("[^0-9+]",""));pending.add(e);}
                        if(!errors.isEmpty())return new ImportResult(pending.size(),errors,null);
                        if(replace)dao.deleteAll();for(Employee employee:pending)if(!replace&&dao.existsId(employee.getId()))dao.update(employee);else dao.save(employee);
                        return new ImportResult(pending.size(),errors,null);
                    }catch(Exception ex){return new ImportResult(0,null,ex);}
                }
                protected void done(){loading.dispose();try{ImportResult result=get();if(result.failure!=null){JOptionPane.showMessageDialog(MasterDataPanel.this,result.failure.getMessage(),"Import failed",JOptionPane.ERROR_MESSAGE);return;}if(!result.errors.isEmpty()){showImportErrors(result.errors);return;}refreshTable();JOptionPane.showMessageDialog(MasterDataPanel.this,result.count+" employee records imported.");}catch(Exception ex){JOptionPane.showMessageDialog(MasterDataPanel.this,ex.getMessage(),"Import failed",JOptionPane.ERROR_MESSAGE);}}
            }.execute();
        }catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Import failed",JOptionPane.ERROR_MESSAGE);}
    }
    private JDialog importLoadingDialog(){JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor(this),"Importing",Dialog.ModalityType.MODELESS);dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);dialog.add(new JLabel("Importing data, please wait...",SwingConstants.CENTER));dialog.setSize(320,110);dialog.setResizable(false);dialog.setLocationRelativeTo(this);return dialog;}
    private void showImportErrors(java.util.List<String> errors){JTextArea details=new JTextArea(String.join("\n",errors));details.setEditable(false);details.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));JScrollPane scroll=new JScrollPane(details);scroll.setPreferredSize(new Dimension(760,420));JOptionPane.showMessageDialog(this,scroll,"Import validation errors",JOptionPane.ERROR_MESSAGE);}
    private static final class ImportResult { final int count; final java.util.List<String> errors; final Exception failure; ImportResult(int count,java.util.List<String> errors,Exception failure){this.count=count;this.errors=errors==null?java.util.Collections.emptyList():errors;this.failure=failure;} }
    private static String cell(java.util.List<String> row,int column){return PayrollExcel.cell(row,column).trim();}
    private static boolean rowEmpty(java.util.List<String> row){for(String value:row)if(value!=null&&!value.trim().isEmpty())return false;return true;}
    private static void validateImportRow(java.util.List<String> row,int number,java.util.List<String> errors){String[] labels={"Employee ID","Employee Name","Gender","Date of Birth","Department","Designation","Date of Joining","Date of Leaving","Status","Salary Structure","PAN","Aadhaar Number","ESIC IP Number","UAN","Bank Name","Bank Account Number","Bank IFSC Code","Email ID","Phone Number"};for(int i=0;i<labels.length;i++)if(i!=4&&i!=5&&i!=7&&i!=8&&i!=12&&i!=17&&cell(row,i).isEmpty())errors.add("Row "+number+": "+labels[i]+" is required.");String dob=cell(row,3),doj=cell(row,6),doe=cell(row,7),gender=cell(row,2),structure=cell(row,9);if(!date(dob))errors.add("Row "+number+": Date of Birth must be DD-MM-YYYY.");if(!date(doj))errors.add("Row "+number+": Date of Joining must be DD-MM-YYYY.");if(!doe.isEmpty()&&!date(doe))errors.add("Row "+number+": Date of Leaving must be DD-MM-YYYY.");if(!gender.matches("Male|Female|Other"))errors.add("Row "+number+": Gender must be Male, Female, or Other.");if(!structure.matches("Salary|Wages"))errors.add("Row "+number+": Salary Structure must be Salary or Wages.");if(!cell(row,10).matches("[A-Z]{5}\\d{4}[A-Z]"))errors.add("Row "+number+": PAN is invalid.");if(!cell(row,11).replaceAll("\\D","").matches("\\d{12}"))errors.add("Row "+number+": Aadhaar Number must contain 12 digits.");if(!cell(row,12).isEmpty()&&!cell(row,12).equalsIgnoreCase("NA")&&!cell(row,12).matches("\\d{10}"))errors.add("Row "+number+": ESIC IP Number must contain 10 digits or NA.");if(!cell(row,13).equalsIgnoreCase("NA")&&!cell(row,13).matches("\\d{12}"))errors.add("Row "+number+": UAN must contain 12 digits or NA.");if(!cell(row,15).matches("\\d{9,18}"))errors.add("Row "+number+": Bank Account Number must contain 9-18 digits.");if(cell(row,16).length()!=11)errors.add("Row "+number+": Bank IFSC Code must be exactly 11 characters.");if(!cell(row,17).isEmpty()&&!cell(row,17).matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))errors.add("Row "+number+": Email ID is invalid.");if(!cell(row,18).replaceAll("\\D","").matches("\\d{10,15}"))errors.add("Row "+number+": Phone Number must contain 10-15 digits.");}
    private static void normalizeImportDates(java.util.List<String> row){for(int column:new int[]{3,6,7})if(column<row.size()&&!cell(row,column).isEmpty())row.set(column,normalizeImportedDate(cell(row,column)));}
    private static String normalizeImportedDate(String value){
        try { return IMPORT_DATE.format(LocalDate.parse(value,IMPORT_DATE)); } catch(DateTimeParseException ignored) { }
        try { return IMPORT_DATE.format(LocalDate.parse(value,DateTimeFormatter.ISO_LOCAL_DATE)); } catch(DateTimeParseException ignored) { }
        try { return IMPORT_DATE.format(LocalDateTime.parse(value,DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate()); } catch(DateTimeParseException ignored) { }
        try { return IMPORT_DATE.format(OffsetDateTime.parse(value,DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate()); } catch(DateTimeParseException ignored) { }
        try { return IMPORT_DATE.format(LocalDateTime.parse(value,DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")).toLocalDate()); } catch(DateTimeParseException ignored) { }
        try { double serial=Double.parseDouble(value); if(serial>=1&&serial<=100000)return IMPORT_DATE.format(LocalDate.of(1899,12,30).plusDays((long)Math.floor(serial))); } catch(NumberFormatException ignored) { }
        return value;
    }
    private static boolean date(String value){try{LocalDate.parse(value,IMPORT_DATE);return value.matches("\\d{2}-\\d{2}-\\d{4}");}catch(Exception ignored){return false;}}
    public void refreshTable() {
        employees = dao.listByEmployeeId(search == null ? "" : search.getText()); Object[][] rows = new Object[employees.size()][COLUMNS.length];
        for (int i = 0; i < rows.length; i++) { Employee e = employees.get(i); rows[i] = new Object[]{e.getId(),e.getName(),e.getGender(),e.getDob(),e.getDepartment(),e.getDesignation(),e.getJoiningDate(),e.getEmploymentEndDate(),e.getStatus(),e.getSalaryStructure(),e.getPhotoPath() == null || e.getPhotoPath().isEmpty() ? "" : "View",e.getPan() + info(documentPart(e, 0)),mask(e.getAadhar()) + info(documentPart(e, 1)),e.getUanNumber(),e.getEsicIpNumber(),e.getBankName(),e.getAccountNumber(),e.getIfsc(),e.getEmail(),e.getPhone(),e}; }
        table.setModel(new DefaultTableModel(rows, COLUMNS) { public boolean isCellEditable(int row, int column) { return column == 20; } }); table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 0; i < COLUMNS.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(i == 1 || i == 18 ? 230 : (i == 20 ? 110 : 180));
        table.getColumnModel().getColumn(20).setCellRenderer(new EditRenderer()); table.getColumnModel().getColumn(20).setCellEditor(new EditEditor());
    }
    private String info(String path) { return path.isEmpty() ? "" : "  View"; }
    private String documentPart(Employee employee, int index) { String[] parts = (employee.getDocumentPaths() == null ? "" : employee.getDocumentPaths()).split("\\|", -1); return index < parts.length ? parts[index] : ""; }
    private String mask(String value) { String digits = value == null ? "" : value.replaceAll("\\D", ""); return digits.length() == 12 ? "XXXX XXXX " + digits.substring(8) : ""; }
    private void viewFile(String path, boolean image) {
        if (path == null || path.isEmpty() || !new File(path).exists()) { JOptionPane.showMessageDialog(this, "The saved document file is not available."); return; }
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Document Viewer", Dialog.ModalityType.APPLICATION_MODAL); dialog.setLayout(new BorderLayout(8, 8)); dialog.setSize(650, 520); dialog.setLocationRelativeTo(this);
        JButton folder = new JButton("\uD83D\uDCC1 Open Folder"); folder.setToolTipText("Open this document's folder in This PC"); folder.addActionListener(e -> { try { Desktop.getDesktop().open(new File(path).getParentFile()); } catch (Exception x) { JOptionPane.showMessageDialog(dialog, "Could not open the document folder."); } });
        JPanel header = new JPanel(new FlowLayout(FlowLayout.RIGHT)); header.add(folder); dialog.add(header, BorderLayout.NORTH);
        if (image) { Image source = new ImageIcon(path).getImage(); int width = Math.max(1, source.getWidth(null)), height = Math.max(1, source.getHeight(null)); double scale = Math.min(560.0 / width, 380.0 / height); dialog.add(new JScrollPane(new JLabel(new ImageIcon(source.getScaledInstance((int) (width * scale), (int) (height * scale), Image.SCALE_SMOOTH)), SwingConstants.CENTER)), BorderLayout.CENTER); }
        else { JTextArea viewer = new JTextArea("PDF document preview\n\nUse Open Folder to open the saved document location in This PC."); viewer.setEditable(false); viewer.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16)); viewer.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24)); dialog.add(new JScrollPane(viewer), BorderLayout.CENTER); }
        JButton close = new JButton("Close"); close.addActionListener(e -> dialog.dispose()); JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT)); bottom.add(close); dialog.add(bottom, BorderLayout.SOUTH); dialog.setVisible(true);
    }
    private static class EditRenderer extends JPanel implements javax.swing.table.TableCellRenderer { EditRenderer() { add(new JButton("\u270F Edit")); } public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focused, int row, int column) { return this; } }
    private class EditEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor { private Employee employee; private final JButton edit = new JButton("\u270F Edit"); EditEditor() { edit.addActionListener(e -> { fireEditingStopped(); frame.showMasterForm(employee); }); } public Component getTableCellEditorComponent(JTable t, Object value, boolean selected, int row, int column) { employee = (Employee) value; return edit; } public Object getCellEditorValue() { return employee; } }
}
