import java.sql.*;import java.util.*;
/** Permanent storage for employee-specific Other Earnings by month. */
public class CTCOtherEarningsDAO {
    private static final Map<String,double[]> MEMORY=new HashMap<>();
    public CTCOtherEarningsDAO(){}
    public double[] load(String employeeId,String month){try{java.time.YearMonth p=java.time.YearMonth.parse(month);Connection c=DBConnection.getConnection();if(c==null)return MEMORY.get(employeeId+"|"+month);try(PreparedStatement s=c.prepareStatement("SELECT ot_pay,other_earnings FROM earnings_allowances WHERE employee_id=? AND month=? AND fy=?")){s.setString(1,employeeId);s.setString(2,p.getMonth().toString().substring(0,3));s.setString(3,FinancialYear.text(p.getMonthValue()>=4?p.getYear():p.getYear()-1));ResultSet r=s.executeQuery();return r.next()?new double[]{0,r.getDouble(2)}:null;}}catch(Exception e){return null;}}
    public void save(String employeeId,String month,double hours,double others)throws SQLException{MonthlyEarningsStore.Value v=MonthlyEarningsStore.getOrCreate(employeeId,month);v.overtime=Money.round(hours);v.reimbursements=Money.round(others);MonthlyEarningsStore.save();}
}
