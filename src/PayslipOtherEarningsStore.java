import java.sql.*;import java.time.*;
public final class PayslipOtherEarningsStore {
 private static YearMonth p(String v){try{return YearMonth.parse(v);}catch(Exception e){return YearMonth.now();}}private static String fy(YearMonth p){return FinancialYear.text(p.getMonthValue()>=4?p.getYear():p.getYear()-1);}
 public static synchronized double get(String id,String month){YearMonth p=p(month);try(Connection c=DBConnection.getConnection();PreparedStatement s=c==null?null:c.prepareStatement("SELECT manual_other_earnings FROM payslip WHERE employee_id=? AND month=? AND fy=?")){if(s==null)return 0;s.setString(1,id);s.setString(2,p.getMonth().toString().substring(0,3));s.setString(3,fy(p));ResultSet r=s.executeQuery();return r.next()?r.getDouble(1):0;}catch(SQLException e){return 0;}}
 public static synchronized boolean has(String id,String month){return get(id,month)!=0;}
 public static synchronized void clear(String id,String month){put(id,month,0);}
 public static synchronized void put(String id,String month,double amount){YearMonth p=p(month);try(Connection c=DBConnection.getConnection();PreparedStatement s=c==null?null:c.prepareStatement("INSERT INTO payslip(employee_id,employee_name,month,fy,manual_other_earnings) SELECT ?,employee_name,?,?,? FROM employee_master_data WHERE employee_id=? AND is_deleted=0 ON CONFLICT(employee_id,month,fy) DO UPDATE SET manual_other_earnings=excluded.manual_other_earnings")){if(s==null)return;s.setString(1,id);s.setString(2,p.getMonth().toString().substring(0,3));s.setString(3,fy(p));s.setDouble(4,Money.round(Math.max(0,amount)));s.setString(5,id);s.executeUpdate();}catch(SQLException ignored){}AutoSaveService.markDirty();}
 public static synchronized void flush(){}public static synchronized void purgeEmployee(String id){}
 private PayslipOtherEarningsStore(){}
}
