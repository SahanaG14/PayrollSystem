import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class EmployeeFormPanel extends JPanel {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-uuuu");
    private final PayrollSystemFrame frame;
    private final EmployeeDAO dao = new EmployeeDAO();
    private Employee edit;
    private JTextField id, name, dob, dept, desig, join, leave, pan, aadhaar, esic, uan, bank, account, ifsc, email, phone;
    private JComboBox<String> gender, status, salaryStructure;
    private JCheckBox esicNa, uanNa, epfCeiling;
    private String photo = "", panPdf = "", aadhaarPdf = "";
    private JLabel photoPreview, photoSource, photoDestination, employeeIdPrefix;
    private JButton uploadPhoto, removePhoto, removePan, removeAadhaar;
    private JLabel duplicateIdError;
    private final Map<JTextField, JLabel> errors = new LinkedHashMap<>();
    private boolean formatting;

    public EmployeeFormPanel(PayrollSystemFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        add(build(), BorderLayout.CENTER);
        reset();
    }

    private JComponent build() {
        id=field(); name=field(); dob=field(); dept=field(); desig=field(); join=field(); leave=field();
        pan=field(); aadhaar=field(); esic=field(); uan=field(); bank=field(); account=field(); ifsc=field(); email=field(); phone=field();
        gender=new JComboBox<>(new String[]{"Male", "Female", "Other"}); gender.setSelectedIndex(-1);
        status=new JComboBox<>(new String[]{"Active Employee", "Ex-Employee"}); leave.setEnabled(false); esicNa=new JCheckBox("NA");esicNa.addActionListener(e->{boolean na=esicNa.isSelected();esic.setEnabled(!na);if(na)esic.setText("NA");else if("NA".equalsIgnoreCase(esic.getText()))esic.setText("");}); uanNa=new JCheckBox("NA");uanNa.addActionListener(e->{boolean na=uanNa.isSelected();uan.setEnabled(!na);if(na)uan.setText("NA");else if("NA".equalsIgnoreCase(uan.getText()))uan.setText("");});
        salaryStructure = new JComboBox<>(new String[]{"Salary", "Wages"});
        salaryStructure.setSelectedIndex(-1);

        epfCeiling = new JCheckBox("Restrict EPF Basic to ₹15,000 Ceiling");
        epfCeiling.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        epfCeiling.setPreferredSize(new Dimension(380, 34));
        status.addActionListener(e -> leave.setEnabled("Ex-Employee".equals(status.getSelectedItem())));
        configureRules(); configureEmployeeIdInput(); configureDuplicateIdValidation();
        uploadPhoto=new JButton("Upload Photo (.jpg/.jpeg)"); uploadPhoto.addActionListener(e -> upload(0)); removePhoto=new JButton("Remove"); removePhoto.addActionListener(e -> removeDocument(0)); removePhoto.setVisible(false);
        photoPreview=new JLabel(); photoPreview.setPreferredSize(new Dimension(58,58)); photoPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY)); photoPreview.setVisible(false);
        photoSource=pathLabel(); photoDestination=pathLabel();
        JPanel photoTop=new JPanel(new BorderLayout(8,0)); photoTop.setOpaque(false); photoTop.add(uploadPhoto,BorderLayout.CENTER); photoTop.add(removePhoto,BorderLayout.EAST);
        JPanel photoRow=new JPanel(new BorderLayout(8,0)); photoRow.setOpaque(false); photoRow.add(photoPreview,BorderLayout.WEST); JPanel photoDetails=new JPanel(new BorderLayout(2,2)); photoDetails.setOpaque(false); photoDetails.add(photoTop,BorderLayout.NORTH); JPanel paths=new JPanel();paths.setLayout(new BoxLayout(paths,BoxLayout.Y_AXIS));paths.setOpaque(false);paths.add(photoSource);paths.add(photoDestination);photoDetails.add(paths,BorderLayout.CENTER);photoRow.add(photoDetails,BorderLayout.CENTER);
        JButton uploadPan=new JButton("Upload PAN PDF"); uploadPan.setPreferredSize(new Dimension(112,36)); uploadPan.addActionListener(e -> upload(1)); removePan=new JButton("Remove"); removePan.setPreferredSize(new Dimension(72,36)); removePan.addActionListener(e -> removeDocument(1)); removePan.setVisible(false);
        JButton uploadAadhaar=new JButton("Upload Aadhaar PDF"); uploadAadhaar.setPreferredSize(new Dimension(112,36)); uploadAadhaar.addActionListener(e -> upload(2)); removeAadhaar=new JButton("Remove"); removeAadhaar.setPreferredSize(new Dimension(72,36)); removeAadhaar.addActionListener(e -> removeDocument(2)); removeAadhaar.setVisible(false);
        JPanel grid=new JPanel(new GridBagLayout()); grid.setBorder(BorderFactory.createEmptyBorder(44,34,4,34));
        addPair(grid,0,"Employee ID",employeeIdInput(),id,"Photo Upload",photoRow,null);
        addPair(grid,1,"Employee Name",name,null,"PAN Number",fieldWithButtons(pan,uploadPan,removePan),pan);
        addPair(grid,2,"Gender",gender,null,"Aadhaar Number",fieldWithButtons(aadhaar,uploadAadhaar,removeAadhaar),aadhaar);
        addPair(grid,3,"Date of Birth (DD-MM-YYYY)",dateInput(dob),dob,"ESIC IP Number",fieldWithButton(esic,esicNa),esic);
        addPair(grid,4,"Department",dept,null,"UAN Number",fieldWithButton(uan,uanNa),uan);
        addPair(grid,5,"Designation",desig,null,"Bank Name",bank,null);
        addPair(grid,6,"Date of Joining (DD-MM-YYYY)",dateInput(join),join,"Bank Account Number",account,account);
        addPair(grid,7,"Status",status,null,"Bank IFSC Code",ifsc,ifsc);
        addPair(grid,8,"Date of Leaving (DD-MM-YYYY)",dateInput(leave),leave,"Salary Structure",salaryStructure,null);
        addPair(grid,9,"Email ID",email,email,"Phone Number",phone,phone);GridBagConstraints epf=new GridBagConstraints();epf.gridx=0;epf.gridy=10;epf.gridwidth=4;epf.anchor=GridBagConstraints.WEST;epf.insets=new Insets(5,8,5,8);grid.add(epfCeiling,epf);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.CENTER,10,8));
        for(String label:new String[]{"Save","Delete","Reset","Back"}) { JButton button=new JButton(label); button.addActionListener(e -> action(label)); buttons.add(button); }
        JPanel all=new JPanel(new BorderLayout()); all.add(grid,BorderLayout.CENTER); all.add(buttons,BorderLayout.SOUTH); return all;
    }

    private void configureRules() {
        for(JTextField field:new JTextField[]{name,dept,desig,bank,ifsc})((AbstractDocument)field.getDocument()).setDocumentFilter(new UppercaseInputFilter());
        rule(dob, "Date of Birth must be DD-MM-YYYY and age must be between 18 and 100 years.", this::validDateOfBirth, true, v -> v);
        rule(join, "Date of Joining must be DD-MM-YYYY and no later than next year.", this::validEmploymentDate, true, v -> v);
        rule(leave, "Date of Leaving must be DD-MM-YYYY and no later than next year.", this::validEmploymentDate, false, v -> v);
        ((AbstractDocument)phone.getDocument()).setDocumentFilter(new DigitsLimitFilter(10));
        rule(phone, "Phone number must contain exactly 10 digits.", v -> v.matches("\\d{10}"), true, v -> v);
        rule(email, "Please enter a valid email address.", v -> v.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"), true, v -> v);
        rule(pan, "PAN must be 10 characters (e.g., ABCDE1234F).", v -> v.matches("[A-Z]{5}\\d{4}[A-Z]"), true, v -> upper(v,10));
        rule(aadhaar, "Aadhaar number must be exactly 12 digits.", v -> v.replaceAll("\\D", "").matches("\\d{12}"), true, this::aadhaarFormat);
        rule(esic, "ESIC IP number must be exactly 10 digits or NA.", v -> esicNa.isSelected() || v.isEmpty() || v.matches("\\d{10}"), false, v -> "NA".equalsIgnoreCase(v) ? "NA" : digits(v,10));
        rule(uan, "UAN number must be exactly 12 digits or NA.", v -> uanNa.isSelected() || v.matches("\\d{12}"), true, v -> "NA".equalsIgnoreCase(v) ? "NA" : digits(v,12));
        rule(account, "Account number must be between 9 and 18 digits.", v -> v.matches("\\d{9,18}"), true, v -> digits(v,18));
        rule(ifsc, "IFSC must be exactly 11 characters.", v -> v.length()==11, true, v -> v.trim().substring(0,Math.min(11,v.trim().length())));
        configureDateRelationshipValidation();
    }
    private void configureDateRelationshipValidation(){DocumentListener validation=listener(this::validateDateRelationships);dob.getDocument().addDocumentListener(validation);join.getDocument().addDocumentListener(validation);leave.getDocument().addDocumentListener(validation);}
    private void validateDateRelationships(){validateEmploymentDateAgainstDob(join,"Employee must be at least 18 years old on the Date of Joining.");validateLeavingAfterJoining();}
    private void validateEmploymentDateAgainstDob(JTextField field,String message){if(!date(dob.getText())||!date(field.getText())||!validEmploymentDate(field.getText()))return;LocalDate birth=LocalDate.parse(dob.getText(),DATE),employment=LocalDate.parse(field.getText(),DATE);boolean invalid=employment.isBefore(birth.plusYears(18));JLabel error=errors.get(field);error.setText(message);error.setVisible(invalid);field.setBackground(invalid?new Color(255,239,239):Color.WHITE);revalidate();repaint();}
    private void validateLeavingAfterJoining(){if(!date(join.getText())||!date(leave.getText())||!validEmploymentDate(leave.getText()))return;boolean invalid=LocalDate.parse(leave.getText(),DATE).isBefore(LocalDate.parse(join.getText(),DATE));JLabel error=errors.get(leave);error.setText("Date of Leaving cannot be before Date of Joining");error.setVisible(invalid);leave.setBackground(invalid?new Color(255,239,239):Color.WHITE);revalidate();repaint();}
    private boolean employmentDatesAfterDob(){if(!date(dob.getText()))return false;LocalDate birth=LocalDate.parse(dob.getText(),DATE);return (!date(join.getText())||!LocalDate.parse(join.getText(),DATE).isBefore(birth.plusYears(18)))&&(!date(leave.getText())||(!LocalDate.parse(leave.getText(),DATE).isBefore(birth.plusYears(18))&&(!date(join.getText())||!LocalDate.parse(leave.getText(),DATE).isBefore(LocalDate.parse(join.getText(),DATE)))));}
    private void configureEmployeeIdInput(){((AbstractDocument)id.getDocument()).setDocumentFilter(new NumericSuffixFilter());}
    private JComponent employeeIdInput(){employeeIdPrefix=new JLabel();employeeIdPrefix.setOpaque(true);employeeIdPrefix.setBackground(new Color(235,239,243));employeeIdPrefix.setBorder(BorderFactory.createEmptyBorder(0,10,0,8));id.setBorder(BorderFactory.createEmptyBorder(0,4,0,10));id.setBackground(Color.WHITE);Dimension size=id.getPreferredSize();refreshEmployeeIdPrefix();JPanel input=new JPanel(new BorderLayout(0,0));input.setOpaque(true);input.setBackground(Color.WHITE);input.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(180,190,200),1,true),BorderFactory.createEmptyBorder(1,1,1,1)));input.setPreferredSize(size);input.setMinimumSize(size);input.setMaximumSize(size);input.add(employeeIdPrefix,BorderLayout.WEST);input.add(id,BorderLayout.CENTER);return input;}
    private void refreshEmployeeIdPrefix(){if(employeeIdPrefix==null)return;String prefix=CompanyDetailsStore.employeeIdPrefix().trim();employeeIdPrefix.setText(prefix);employeeIdPrefix.setVisible(!prefix.isEmpty());}
    private void configureDuplicateIdValidation(){duplicateIdError=new JLabel("Employee ID already exists");duplicateIdError.setForeground(new Color(190,20,20));duplicateIdError.setFont(duplicateIdError.getFont().deriveFont(12f));duplicateIdError.setBorder(BorderFactory.createEmptyBorder(3,0,3,0));duplicateIdError.setVisible(false);errors.put(id,duplicateIdError);id.getDocument().addDocumentListener(listener(()->{String fullId=resolvedEmployeeId();boolean duplicate=edit==null&&!id.getText().trim().isEmpty()&&dao.existsMasterDataId(fullId);duplicateIdError.setVisible(duplicate);id.setBackground(duplicate?new Color(255,239,239):Color.WHITE);revalidate();repaint();}));}
    private String resolvedEmployeeId(){String typed=id.getText().trim(),prefix=CompanyDetailsStore.employeeIdPrefix().trim();return prefix.isEmpty()||typed.regionMatches(true,0,prefix,0,prefix.length())?typed:prefix+typed;}

    private interface Formatter { String format(String value); }
    private interface Check { boolean valid(String value); }
    private void rule(JTextField field, String message, Check check, boolean required, Formatter formatter) {
        JLabel error=new JLabel(message); error.setForeground(new Color(190,20,20)); error.setFont(error.getFont().deriveFont(12f)); error.setBorder(BorderFactory.createEmptyBorder(3,0,3,0)); error.setVisible(false); errors.put(field,error);
        field.getDocument().addDocumentListener(listener(() -> {
            if (!formatting) { String formatted=formatter.format(field.getText()); if (!formatted.equals(field.getText())) { formatting=true; SwingUtilities.invokeLater(() -> { try { if(!formatted.equals(field.getText())) { field.setText(formatted); field.setCaretPosition(formatted.length()); } } finally { formatting=false; } }); } }
            String value=field.getText(); boolean invalid=(!value.isEmpty() || required) && !check.valid(value); error.setVisible(invalid); field.setBackground(invalid ? new Color(255,239,239) : Color.WHITE); EmployeeFormPanel.this.revalidate(); EmployeeFormPanel.this.repaint();
        }));
    }
    private DocumentListener listener(Runnable run) { return new DocumentListener(){public void insertUpdate(DocumentEvent e){run.run();} public void removeUpdate(DocumentEvent e){run.run();} public void changedUpdate(DocumentEvent e){run.run();}}; }
    private String digits(String value,int limit) { String valueDigits=value.replaceAll("\\D",""); return valueDigits.substring(0,Math.min(limit,valueDigits.length())); }
    private String upper(String value,int limit) { value=value.toUpperCase().replaceAll("[^A-Z0-9]",""); return value.substring(0,Math.min(limit,value.length())); }
    private String aadhaarFormat(String value) { String digits=digits(value,12); return digits.replaceFirst("(\\d{4})(?=\\d)","$1 ").replaceFirst("(\\d{4} \\d{4})(?=\\d)","$1 "); }
    private String dateFormat(String value) { String digits=value.replaceAll("\\D","");digits=digits.substring(0,Math.min(8,digits.length()));return digits.length()>4?digits.substring(0,2)+"-"+digits.substring(2,4)+"-"+digits.substring(4):digits.length()>2?digits.substring(0,2)+"-"+digits.substring(2):digits; }

    private void addPair(JPanel grid,int row,String leftLabel,JComponent left,JTextField leftError,String rightLabel,JComponent right,JTextField rightError) {
        addGridCell(grid,row,0,leftLabel,left,leftError); addGridCell(grid,row,2,rightLabel,right,rightError);
    }
    private void addGridCell(JPanel grid,int row,int column,String label,JComponent input,JTextField monitored) {
        JLabel caption=new JLabel(label); caption.setPreferredSize(new Dimension(235,34));
        GridBagConstraints c=new GridBagConstraints(); c.gridx=column;c.gridy=row;c.insets=new Insets(3,0,3,10);c.anchor=GridBagConstraints.WEST;grid.add(caption,c);
        c.gridx=column+1;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(3,0,3,column==0?36:0);
        JTextField errorField=monitored!=null?monitored:(input instanceof JTextField?(JTextField)input:null);
        if(errorField!=null&&errors.containsKey(errorField)){JPanel wrapper=new JPanel(new BorderLayout());wrapper.setOpaque(false);wrapper.add(input,BorderLayout.NORTH);wrapper.add(errors.get(errorField),BorderLayout.SOUTH);grid.add(wrapper,c);}else grid.add(input,c);
    }

    private JPanel columnPanel(){JPanel panel=new JPanel(new GridBagLayout());panel.setOpaque(false);return panel;}
    private void addRow(JPanel panel,int row,String label,JComponent input) {
        JLabel caption=new JLabel(label); caption.setPreferredSize(new Dimension(270,36));
        GridBagConstraints c=new GridBagConstraints(); c.gridx=0;c.gridy=row;c.insets=new Insets(3,0,3,12);c.anchor=GridBagConstraints.WEST;panel.add(caption,c);
        c.gridx=1;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(3,0,3,0);
        if(input instanceof JTextField && errors.containsKey(input)) { JPanel wrap=new JPanel(new BorderLayout());wrap.setOpaque(false);wrap.add(input,BorderLayout.NORTH);wrap.add(errors.get(input),BorderLayout.SOUTH);panel.add(wrap,c); } else panel.add(input,c);
    }
    private JTextField field(){JTextField f=new JTextField();f.setPreferredSize(new Dimension(285,36));return f;}
    private JComponent dateInput(JTextField field){((AbstractDocument)field.getDocument()).setDocumentFilter(new DateInputFilter());JButton calendar=new JButton("\uD83D\uDCC5");calendar.setToolTipText("Choose date");calendar.setMargin(new Insets(1,5,1,5));calendar.addActionListener(e->chooseDate(field));return fieldWithButton(field,calendar);}
    private final class NumericSuffixFilter extends DocumentFilter {public void insertString(FilterBypass fb,int offset,String text,AttributeSet attrs)throws BadLocationException{replace(fb,offset,0,text,attrs);}public void replace(FilterBypass fb,int offset,int length,String text,AttributeSet attrs)throws BadLocationException{if(edit!=null){fb.replace(offset,length,text,attrs);return;}String current=fb.getDocument().getText(0,fb.getDocument().getLength());StringBuilder value=new StringBuilder(current);value.replace(offset,offset+length,text==null?"":text);String digits=value.toString().replaceAll("\\D","");fb.replace(0,fb.getDocument().getLength(),digits,attrs);}public void remove(FilterBypass fb,int offset,int length)throws BadLocationException{replace(fb,offset,length,"",null);}}
    private static final class DigitsLimitFilter extends DocumentFilter {private final int limit;DigitsLimitFilter(int limit){this.limit=limit;}public void insertString(FilterBypass fb,int offset,String text,AttributeSet attrs)throws BadLocationException{replace(fb,offset,0,text,attrs);}public void replace(FilterBypass fb,int offset,int length,String text,AttributeSet attrs)throws BadLocationException{String current=fb.getDocument().getText(0,fb.getDocument().getLength());StringBuilder value=new StringBuilder(current);value.replace(offset,offset+length,text==null?"":text);String digits=value.toString().replaceAll("\\D","");if(digits.length()>limit)digits=digits.substring(0,limit);fb.replace(0,fb.getDocument().getLength(),digits,attrs);}public void remove(FilterBypass fb,int offset,int length)throws BadLocationException{replace(fb,offset,length,"",null);}}
    private static final class UppercaseInputFilter extends DocumentFilter {public void insertString(FilterBypass fb,int offset,String text,AttributeSet attrs)throws BadLocationException{fb.insertString(offset,text==null?null:text.toUpperCase(Locale.ROOT),attrs);}public void replace(FilterBypass fb,int offset,int length,String text,AttributeSet attrs)throws BadLocationException{fb.replace(offset,length,text==null?null:text.toUpperCase(Locale.ROOT),attrs);}}
    private static final class DateInputFilter extends DocumentFilter {public void insertString(FilterBypass fb,int offset,String text,AttributeSet attrs)throws BadLocationException{replace(fb,offset,0,text,attrs);}public void replace(FilterBypass fb,int offset,int length,String text,AttributeSet attrs)throws BadLocationException{String current=fb.getDocument().getText(0,fb.getDocument().getLength());StringBuilder value=new StringBuilder(current);value.replace(offset,offset+length,text==null?"":text);String digits=value.toString().replaceAll("\\D","");if(digits.length()>8)digits=digits.substring(0,8);String formatted=digits.length()>4?digits.substring(0,2)+"-"+digits.substring(2,4)+"-"+digits.substring(4):digits.length()>2?digits.substring(0,2)+"-"+digits.substring(2):digits;fb.replace(0,fb.getDocument().getLength(),formatted,attrs);}public void remove(FilterBypass fb,int offset,int length)throws BadLocationException{replace(fb,offset,length,"",null);}}
    private void chooseDate(JTextField field){
        LocalDate initial=LocalDate.now();
        try { initial=LocalDate.parse(field.getText(),DATE); } catch(Exception ignored) { }
        boolean birth=field==dob;LocalDate minimum=birth?LocalDate.now().minusYears(100):LocalDate.of(1900,1,1),maximum=birth?LocalDate.now().minusYears(18):LocalDate.of(LocalDate.now().getYear()+1,12,31);if(initial.isBefore(minimum))initial=minimum;if(initial.isAfter(maximum))initial=maximum;
        DatePickerDialog picker=new DatePickerDialog(SwingUtilities.getWindowAncestor(this),initial,minimum,maximum);
        picker.setVisible(true);
        if(picker.selectedDate()!=null) field.setText(DATE.format(picker.selectedDate()));
    }

    private static final class DatePickerDialog extends JDialog {
        private static final String[] MONTHS={"January","February","March","April","May","June","July","August","September","October","November","December"};
        private final JComboBox<String> month=new JComboBox<>(MONTHS);
        private final JSpinner year;
        private final JPanel days=new JPanel(new GridLayout(0,7,3,3));
        private final LocalDate minimum,maximum;private LocalDate displayed, selected;

        DatePickerDialog(Window owner,LocalDate initial,LocalDate minimum,LocalDate maximum){
            super(owner,"Select Date",ModalityType.APPLICATION_MODAL);
            this.minimum=minimum;this.maximum=maximum;year=new JSpinner(new SpinnerNumberModel(initial.getYear(),minimum.getYear(),maximum.getYear(),1));displayed=YearMonth.from(initial).atDay(1); selected=initial;
            setLayout(new BorderLayout(10,10));
            JPanel controls=new JPanel(new FlowLayout(FlowLayout.CENTER,8,6));
            JButton previous=new JButton("‹"),next=new JButton("›");
            previous.addActionListener(e->changeMonth(-1)); next.addActionListener(e->changeMonth(1));
            Dimension selectorSize=new Dimension(145,34); Font selectorFont=new Font("SansSerif",Font.PLAIN,14);
            month.setPreferredSize(selectorSize); month.setMinimumSize(selectorSize); month.setMaximumSize(selectorSize); month.setFont(selectorFont);
            year.setPreferredSize(selectorSize); year.setMinimumSize(selectorSize); year.setMaximumSize(selectorSize); year.setFont(selectorFont);
            ((JSpinner.DefaultEditor)year.getEditor()).getTextField().setFont(selectorFont);
            month.setSelectedIndex(displayed.getMonthValue()-1); year.setEditor(new JSpinner.NumberEditor(year,"0")); ((JSpinner.NumberEditor)year.getEditor()).getFormat().setGroupingUsed(false); year.setValue(displayed.getYear());
            month.addActionListener(e->refreshFromControls()); year.addChangeListener(e->refreshFromControls());
            controls.add(previous);controls.add(month);controls.add(year);controls.add(next);
            add(controls,BorderLayout.NORTH);
            JPanel calendar=new JPanel(new BorderLayout(3,3));
            JPanel weekdays=new JPanel(new GridLayout(1,7,3,3));
            for(String label:new String[]{"Sun","Mon","Tue","Wed","Thu","Fri","Sat"}){JLabel day=new JLabel(label,SwingConstants.CENTER);day.setFont(day.getFont().deriveFont(Font.BOLD));weekdays.add(day);}
            calendar.add(weekdays,BorderLayout.NORTH);calendar.add(days,BorderLayout.CENTER);add(calendar,BorderLayout.CENTER);
            JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,6));
            JButton cancel=new JButton("Cancel"),confirm=new JButton("Confirm");
            cancel.addActionListener(e->{selected=null;dispose();}); confirm.addActionListener(e->dispose());
            actions.add(cancel);actions.add(confirm);add(actions,BorderLayout.SOUTH);
            rebuildDays(); setSize(430,360); setResizable(false); setLocationRelativeTo(owner);
        }
        private void changeMonth(int amount){LocalDate candidate=displayed.plusMonths(amount).withDayOfMonth(1);if(candidate.getYear()<minimum.getYear()||candidate.getYear()>maximum.getYear())return;displayed=candidate;month.setSelectedIndex(displayed.getMonthValue()-1);year.setValue(displayed.getYear());rebuildDays();}
        private void refreshFromControls(){displayed=LocalDate.of((Integer)year.getValue(),month.getSelectedIndex()+1,1);rebuildDays();}
        private void rebuildDays(){
            days.removeAll(); int offset=displayed.getDayOfWeek().getValue()%7; YearMonth ym=YearMonth.from(displayed);
            for(int i=0;i<offset;i++)days.add(new JLabel());
            for(int day=1;day<=ym.lengthOfMonth();day++){
                LocalDate value=ym.atDay(day); JButton button=new JButton(String.valueOf(day));
                button.setMargin(new Insets(2,2,2,2));
                button.setEnabled(!value.isBefore(minimum)&&!value.isAfter(maximum));if(value.equals(selected)){button.setBackground(new Color(26,46,64));button.setForeground(Color.WHITE);}
                button.addActionListener(e->{selected=value;rebuildDays();});days.add(button);
            }
            while(days.getComponentCount()%7!=0)days.add(new JLabel());
            days.revalidate();days.repaint();
        }
        LocalDate selectedDate(){return selected;}
    }
    private JComponent fieldWithButton(JTextField value,JComponent button){JPanel row=new JPanel(new BorderLayout(8,0));row.setOpaque(false);row.add(value,BorderLayout.CENTER);row.add(button,BorderLayout.EAST);return row;}
    private JComponent fieldWithButtons(JTextField value,JButton upload,JButton remove){JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,4,0));buttons.setOpaque(false);buttons.add(upload);buttons.add(remove);JPanel row=new JPanel(new BorderLayout(6,0));row.setOpaque(false);row.add(value,BorderLayout.CENTER);row.add(buttons,BorderLayout.EAST);return row;}
    private JLabel pathLabel(){JLabel label=new JLabel();label.setFont(label.getFont().deriveFont(10f));label.setForeground(new Color(80,80,80));label.setVisible(false);return label;}

    private void upload(int type) { JFileChooser chooser=new JFileChooser();chooser.setFileFilter(new FileNameExtensionFilter(type==0?"JPEG Images":"PDF Documents",type==0?new String[]{"jpg","jpeg"}:new String[]{"pdf"}));if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;File file=chooser.getSelectedFile();String n=file.getName().toLowerCase();if((type==0&&!(n.endsWith(".jpg")||n.endsWith(".jpeg")))||(type>0&&!n.endsWith(".pdf"))){JOptionPane.showMessageDialog(this,"Select the required file type.");return;}try{String target=EmployeeDocumentStore.save(file.toPath(),id.getText(),name.getText(),type).toString();if(type==0){photo=target;showPhotoDetails(file.getAbsolutePath(),target);uploadPhoto.setText("Re-upload Photo");removePhoto.setVisible(true);}else if(type==1){panPdf=target;removePan.setVisible(true);}else{aadhaarPdf=target;removeAadhaar.setVisible(true);}revalidate();repaint();}catch(Exception e){JOptionPane.showMessageDialog(this,e.getMessage());} }
    private void showPhotoDetails(String source,String destination){Image image=new ImageIcon(destination).getImage().getScaledInstance(54,54,Image.SCALE_SMOOTH);photoPreview.setIcon(new ImageIcon(image));photoPreview.setVisible(true);photoSource.setText("Source: "+source);photoSource.setToolTipText(source);photoSource.setVisible(true);photoDestination.setText("Destination: "+destination);photoDestination.setToolTipText(destination);photoDestination.setVisible(true);}
    private void removeDocument(int type){String value=type==0?photo:type==1?panPdf:aadhaarPdf;if(value==null||value.isEmpty())return;int choice=JOptionPane.showConfirmDialog(this,"Remove this uploaded document?","Remove Document",JOptionPane.YES_NO_OPTION);if(choice!=JOptionPane.YES_OPTION)return;try{EmployeeDocumentStore.remove(value);if(type==0){photo="";photoPreview.setIcon(null);photoPreview.setVisible(false);photoSource.setVisible(false);photoDestination.setVisible(false);uploadPhoto.setText("Upload Photo (.jpg/.jpeg)");removePhoto.setVisible(false);}else if(type==1){panPdf="";removePan.setVisible(false);}else{aadhaarPdf="";removeAadhaar.setVisible(false);}revalidate();repaint();}catch(Exception e){JOptionPane.showMessageDialog(this,"Could not remove document: "+e.getMessage());}}
    private boolean date(String value){try{LocalDate.parse(value,DATE);return true;}catch(DateTimeParseException e){return false;}}
    private boolean validDateOfBirth(String value){try{LocalDate birth=LocalDate.parse(value,DATE);int age=Period.between(birth,LocalDate.now()).getYears();return birth.isBefore(LocalDate.now())&&age>=18&&age<=100;}catch(Exception ignored){return false;}}
    private boolean validEmploymentDate(String value){try{return LocalDate.parse(value,DATE).getYear()<=LocalDate.now().getYear()+1;}catch(Exception ignored){return false;}}
    private boolean validNewEmployeeAge(String value){return validDateOfBirth(value);}
    private boolean hasErrors(){for(JLabel error:errors.values())if(error.isVisible())return true;return false;}
    private void action(String action) { try { if(action.equals("Back")){frame.returnToMasterData();return;} if(action.equals("Reset")){reset();return;} if(action.equals("Delete")){if(edit!=null){int choice=JOptionPane.showConfirmDialog(this,"This employee will be hidden from all modules. Payroll history will be retained. Continue?","Confirm Employee Removal",JOptionPane.YES_NO_OPTION);if(choice==JOptionPane.YES_OPTION){dao.delete(edit.getId());ActivityLogger.log("Master Data","EMPLOYEE DELETED",edit.getId()+" - "+edit.getName(),"DELETE");frame.returnToMasterData();}return;}int choice=JOptionPane.showConfirmDialog(this,"Are you sure you want to clear this entry? All unsaved data entered in this form will be cleared.","Cancel Entry",JOptionPane.YES_NO_OPTION);if(choice==JOptionPane.YES_OPTION){reset();frame.returnToMasterData();}return;}
        if(missingEmployeeDetails()){JOptionPane.showMessageDialog(this,"Enter employee details.");return;} if(salaryStructure.getSelectedItem()==null){JOptionPane.showMessageDialog(this,"Select Salary Structure before saving.","Salary Structure Required",JOptionPane.ERROR_MESSAGE);return;} if(edit==null&&dao.existsMasterDataId(resolvedEmployeeId())){duplicateIdError.setVisible(true);JOptionPane.showMessageDialog(this,"Employee ID already exists. Cannot save duplicate record.","Duplicate Employee ID",JOptionPane.ERROR_MESSAGE);return;} if(hasErrors()){JOptionPane.showMessageDialog(this,"Correct the highlighted fields before saving.");return;} if(!date(dob.getText())||!date(join.getText())||("Ex-Employee".equals(status.getSelectedItem())&&!date(leave.getText()))){JOptionPane.showMessageDialog(this,"Dates must use DD-MM-YYYY.");return;} if(date(join.getText())&&LocalDate.parse(join.getText(),DATE).isBefore(LocalDate.parse(dob.getText(),DATE).plusYears(18))){JOptionPane.showMessageDialog(this,"Employee must be at least 18 years old on the Date of Joining.","Invalid Date of Joining",JOptionPane.ERROR_MESSAGE);return;} if(date(leave.getText())&&LocalDate.parse(leave.getText(),DATE).isBefore(LocalDate.parse(join.getText(),DATE))){JOptionPane.showMessageDialog(this,"Date of Leaving cannot be earlier than Date of Joining.","Invalid Date of Leaving",JOptionPane.ERROR_MESSAGE);return;} if(edit==null&&!validNewEmployeeAge(dob.getText())){JOptionPane.showMessageDialog(this,"Employee age must be strictly between 18 and 100 years.","Invalid Date of Birth",JOptionPane.ERROR_MESSAGE);return;}
        boolean creating=edit==null;Employee employee=creating?new Employee():edit;String cleanPhone=phone.getText().trim().replaceAll("[^0-9+]","");if(cleanPhone.replace("+","").length()>15)throw new IllegalArgumentException("Phone number must not exceed 15 digits.");employee.setId(resolvedEmployeeId());employee.setName(name.getText());employee.setGender((String)gender.getSelectedItem());employee.setDob(dob.getText());employee.setDepartment(dept.getText());employee.setDesignation(desig.getText());employee.setJoiningDate(join.getText());employee.setEmploymentEndDate(leave.getText());employee.setStatus((String)status.getSelectedItem());employee.setPhotoPath(photo);employee.setPan(pan.getText());employee.setAadhar(aadhaar.getText().replaceAll("\\D", ""));employee.setEsicIpNumber(esicNa.isSelected()?"NA":esic.getText());employee.setUanNumber(uanNa.isSelected()?"NA":uan.getText());employee.setBankName(bank.getText());employee.setAccountNumber(account.getText());employee.setIfsc(ifsc.getText());employee.setEmail(email.getText());employee.setPhone(cleanPhone);employee.setSalaryStructure((String)salaryStructure.getSelectedItem());employee.setRestrictEpfCeiling(epfCeiling.isSelected());employee.setDocumentPaths(panPdf+"|"+aadhaarPdf);if(creating)dao.save(employee);else dao.update(employee);ActivityLogger.log("Master Data",creating?"EMPLOYEE CREATED":"EMPLOYEE UPDATED",employee.getId()+" - "+employee.getName(),creating?"CREATE":"UPDATE");frame.returnToMasterData();
    }catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage());}}
    private boolean missingEmployeeDetails(){return id.getText().trim().isEmpty()||name.getText().trim().isEmpty()||gender.getSelectedItem()==null||dob.getText().trim().isEmpty()||dept.getText().trim().isEmpty()||desig.getText().trim().isEmpty()||join.getText().trim().isEmpty()||pan.getText().trim().isEmpty()||aadhaar.getText().trim().isEmpty()||uan.getText().trim().isEmpty()||bank.getText().trim().isEmpty()||account.getText().trim().isEmpty()||ifsc.getText().trim().isEmpty()||email.getText().trim().isEmpty()||phone.getText().trim().isEmpty();}
    private void reset(){edit=null;refreshEmployeeIdPrefix();for(JTextField f:new JTextField[]{id,name,dob,dept,desig,join,leave,pan,aadhaar,esic,uan,bank,account,ifsc,email,phone})f.setText("");id.setEditable(true);gender.setSelectedIndex(-1);status.setSelectedIndex(0);salaryStructure.setSelectedIndex(-1);epfCeiling.setSelected(false);esicNa.setSelected(false);esic.setEnabled(true);uanNa.setSelected(false);uan.setEnabled(true);photo=panPdf=aadhaarPdf="";if(photoPreview!=null){photoPreview.setIcon(null);photoPreview.setVisible(false);photoSource.setVisible(false);photoDestination.setVisible(false);uploadPhoto.setText("Upload Photo (.jpg/.jpeg)");removePhoto.setVisible(false);removePan.setVisible(false);removeAadhaar.setVisible(false);}}
    public void edit(Employee employee){edit=employee;employeeIdPrefix.setText("");employeeIdPrefix.setVisible(false);id.setText(employee.getId());id.setEditable(false);name.setText(employee.getName());gender.setSelectedItem(employee.getGender());dob.setText(employee.getDob());dept.setText(employee.getDepartment());desig.setText(employee.getDesignation());join.setText(employee.getJoiningDate());leave.setText(employee.getEmploymentEndDate());status.setSelectedItem(employee.getStatus());salaryStructure.setSelectedItem(employee.getSalaryStructure());epfCeiling.setSelected(employee.isRestrictEpfCeiling());pan.setText(employee.getPan());aadhaar.setText(aadhaarFormat(employee.getAadhar()));esicNa.setSelected("NA".equalsIgnoreCase(employee.getEsicIpNumber()));esic.setText(esicNa.isSelected()?"NA":employee.getEsicIpNumber());esic.setEnabled(!esicNa.isSelected());uanNa.setSelected("NA".equalsIgnoreCase(employee.getUanNumber()));uan.setText(uanNa.isSelected()?"NA":employee.getUanNumber());uan.setEnabled(!uanNa.isSelected());bank.setText(employee.getBankName());account.setText(employee.getAccountNumber());ifsc.setText(employee.getIfsc());email.setText(employee.getEmail());phone.setText(employee.getPhone());photo=employee.getPhotoPath();String[] docs=(employee.getDocumentPaths()==null?"":employee.getDocumentPaths()).split("\\|",-1);panPdf=docs.length>0?docs[0]:"";aadhaarPdf=docs.length>1?docs[1]:"";if(photo!=null&&!photo.isEmpty()){showPhotoDetails("Existing stored file",photo);uploadPhoto.setText("Re-upload Photo");removePhoto.setVisible(true);}removePan.setVisible(!panPdf.isEmpty());removeAadhaar.setVisible(!aadhaarPdf.isEmpty());leave.setEnabled("Ex-Employee".equals(employee.getStatus()));}
}
