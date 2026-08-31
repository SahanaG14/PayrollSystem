import javax.swing.*;
import java.awt.*;
import java.time.YearMonth;

/** Fixed visual payslip template based on the approved PAYSLIP Template.xlsx layout. */
public class PayslipTemplatePanel extends JPanel {
    private static final Color BAND=new Color(226,239,218);
    private final GridBagConstraints g=new GridBagConstraints();
    public PayslipTemplatePanel(Employee employee,PayrollCalculator.Result result,YearMonth period){
        setLayout(new GridBagLayout()); setBackground(Color.WHITE); setBorder(BorderFactory.createEmptyBorder(12,12,12,12)); setPreferredSize(new Dimension(756,1058));
        for(int i=0;i<6;i++){g.gridx=i;g.gridy=0;g.weightx=1;g.fill=GridBagConstraints.BOTH;}
        AttendanceRecord attendance=new AttendanceDAO().load(employee.getId(),period.toString());
        double working=new AttendanceDAO().workingDays(period.toString()); AttendanceSettings rules=new AttendanceSettingsDAO().load();
        double payable=LeaveBalanceService.payableDays(employee,period,working,attendance.absentDays,attendance.paidLeaveDays,attendance.unpaidLeaveDays,rules);
        CTCStore.Value allowance=CTCStore.get(employee.getId()), other=CTCStore.getOther(employee.getId(),period.toString());double attendanceBonus=working>0&&payable==working?allowance.attendance:0,totalAllowance=allowance.allowances()-allowance.attendance+attendanceBonus;
        DeductionStore.Value deduction=DeductionStore.get(employee.getId(), period.toString());

        companyHeader(0); companyAddress(1);
        row(2,"Employee Information",6,true,26);
        pair(3,"UID:",employee.getId(),"Department:",employee.getDepartment());
        pair(4,"Name:",employee.getName(),"Designation:",employee.getDesignation());
        attendanceHeader(5);
        attendanceBank(6,"Working Days:",String.valueOf(working),"Payable Days:",String.valueOf(payable),"Bank Name:",employee.getBankName());
        attendanceBank(7,"EL & CL Taken:",attendance.unpaidLeaveDays+" / "+attendance.paidLeaveDays,"ESIC IP Number:",safe(employee.getEsicIpNumber()),"Account No:",safe(employee.getAccountNumber()));
        attendanceBank(8,"Leave Taken:",String.valueOf(attendance.absentDays+attendance.paidLeaveDays+attendance.unpaidLeaveDays),"UAN Number:",safe(employee.getUanNumber()),"IFSC Code:",safe(employee.getIfsc()));
        row(9,"",6,false,16);
        row(10,"Salary Calculation for "+period.getMonth()+","+period.getYear(),6,true,26);
        splitHeader(11,"EARNINGS","DEDUCTIONS");
        earningsDeduction(12,"Basic Pay",money(result.earnings.get("Monthly Basic Pay")),"Professional Tax",money(deduction.pt));
        earningsDeduction(13,"Allowances","","Labour Welfare Fund",money(0));
        earningsDeduction(14,"House Rent Allowance",money(allowance.hra),"ESIC",money(deduction.esic));
        earningsDeduction(15,employee.isWagesStructure()?"Attendance Bonus":"",employee.isWagesStructure()?money(attendanceBonus):"","EPF",money(deduction.epf));
        earningsDeduction(16,"Performance Pay",money(allowance.performance),"Advance",money(deduction.advance));
        earningsDeduction(17,"Special Allowance",money(allowance.special),"Total Deductions",money(deduction.total()));
        earningsDeduction(18,"Total Allowance",money(totalAllowance),"Additional Perks / Bonus",money(0));
        earningsDeduction(19,employee.isWagesStructure()?"OT Pay":"",employee.isWagesStructure()?money(other.otPay):"","", "");
        double manualOther=PayslipOtherEarningsStore.get(employee.getId(),period.toString());
        earningsDeduction(20,"Other Allowance",money(other.others+manualOther),"", "");
        earningsDeduction(21,"Gross Salary",money(result.gross),"Net Payable Salary",money(result.net));
        row(22,"",6,false,16);
        cell(23,0,2,"Amount in Words:",false,SwingConstants.RIGHT); cell(23,2,4,AmountInWords.rupees(result.net),false,SwingConstants.LEFT);
    }
    private static String safe(String value){return value==null?"":value;}
    private static String money(Double value){return Money.text(value==null?0:value);} private static String money(double value){return Money.text(value);}
    private void row(int y,String text,int span,boolean green,int height){cell(y,0,span,text,green,SwingConstants.CENTER,height,true);}
    private void companyHeader(int y){String path=CompanyDetailsStore.logoPath();JLabel logo=new JLabel("",SwingConstants.CENTER);logo.setOpaque(true);logo.setBackground(BAND);logo.setBorder(BorderFactory.createLineBorder(Color.BLACK));logo.setPreferredSize(new Dimension(58,58));if(!path.isEmpty()&&new java.io.File(path).isFile()){Image image=new ImageIcon(path).getImage().getScaledInstance(54,54,Image.SCALE_SMOOTH);logo.setIcon(new ImageIcon(image));}g.gridx=0;g.gridy=y;g.gridwidth=1;g.gridheight=2;g.weightx=0;g.fill=GridBagConstraints.BOTH;add(logo,g);g.gridheight=1;String company=CompanyDetailsStore.name().isEmpty()?"PAYSLIP":CompanyDetailsStore.name();cell(y,1,5,company,true,SwingConstants.CENTER,36,true);}
    private void companyAddress(int y){String address=CompanyDetailsStore.address();cell(y,1,5,address.isEmpty()?"":address,true,SwingConstants.CENTER,22,false);}
    private void splitHeader(int y,String left,String right){cell(y,0,4,left,true,SwingConstants.CENTER,25,true);cell(y,4,2,right,true,SwingConstants.CENTER,25,true);}
    private void attendanceHeader(int y){attendanceCell(y,0,4,"Employee Attendance",true,SwingConstants.LEFT,25,true,false,true);attendanceCell(y,4,2,"Salary Transfered To:",true,SwingConstants.LEFT,25,true,true,false);}
    private void pair(int y,String l1,String v1,String l2,String v2){cell(y,0,1,l1,false,SwingConstants.RIGHT);cell(y,1,3,safe(v1),false,SwingConstants.LEFT);cell(y,4,1,l2,false,SwingConstants.RIGHT);cell(y,5,1,safe(v2),false,SwingConstants.LEFT);}
    private void attendanceBank(int y,String l1,String v1,String l2,String v2,String l3,String v3){attendanceCell(y,0,1,l1,false,SwingConstants.RIGHT,23,false,false,false);attendanceCell(y,1,1,safe(v1),false,SwingConstants.LEFT,23,false,false,false);attendanceCell(y,2,1,l2,false,SwingConstants.RIGHT,23,false,false,false);attendanceCell(y,3,1,safe(v2),false,SwingConstants.LEFT,23,false,false,false);attendanceCell(y,4,1,l3,false,SwingConstants.RIGHT,23,false,true,false);attendanceCell(y,5,1,safe(v3),false,SwingConstants.LEFT,23,false,false,false);}
    private void earningsDeduction(int y,String earning,String earningValue,String deduction,String deductionValue){cell(y,0,3,earning,false,SwingConstants.LEFT);cell(y,3,1,earningValue,false,SwingConstants.RIGHT);cell(y,4,1,deduction,false,SwingConstants.LEFT);cell(y,5,1,deductionValue,false,SwingConstants.RIGHT);}
    private void cell(int y,int x,int width,String text,boolean green,int align){cell(y,x,width,text,green,align,23,false);}
    private void cell(int y,int x,int width,String text,boolean green,int align,int height,boolean bold){JLabel label=new JLabel(text,align);label.setOpaque(true);label.setBackground(green?BAND:Color.WHITE);label.setBorder(BorderFactory.createLineBorder(Color.BLACK));label.setFont(new Font("Serif",bold?Font.BOLD:Font.PLAIN,bold?17:12));label.setPreferredSize(new Dimension(0,height));g.gridx=x;g.gridy=y;g.gridwidth=width;g.weightx=width;g.weighty=0;g.fill=GridBagConstraints.HORIZONTAL;add(label,g);}
    private void attendanceCell(int y,int x,int width,String text,boolean green,int align,int height,boolean bold,boolean noLeft,boolean noRight){JLabel label=new JLabel(text,align);label.setOpaque(true);label.setBackground(green?BAND:Color.WHITE);label.setBorder(BorderFactory.createMatteBorder(1,noLeft?0:1,1,noRight?0:1,Color.BLACK));label.setFont(new Font("Serif",bold?Font.BOLD:Font.PLAIN,bold?16:11));label.setPreferredSize(new Dimension(0,height));g.gridx=x;g.gridy=y;g.gridwidth=width;g.weightx=width;g.weighty=0;g.fill=GridBagConstraints.HORIZONTAL;add(label,g);}
}
