import java.sql.*;
import java.time.YearMonth;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** SQLite persistence for one employee-month attendance record. */
public class AttendanceDAO {
    private static final Map<String,AttendanceRecord> memory=new HashMap<>();
    private static final Map<String,Double> workingDaysMemory=new HashMap<>();

    public AttendanceDAO(){ }

    public double workingDays(String month){
        Connection c=DBConnection.getConnection();
        if(c==null)return workingDaysMemory.getOrDefault(month,0.0);
        String sql="SELECT a.total_working_days FROM attendance a JOIN employee_master_data e ON e.employee_id=a.employee_id WHERE a.month=? AND a.fy=? AND e.is_deleted=0 LIMIT 1";
        try(c;PreparedStatement p=c.prepareStatement(sql)){p.setString(1,month);p.setString(2,fy(month));ResultSet r=p.executeQuery();return r.next()?r.getDouble(1):0.0;}catch(SQLException ignored){return 0.0;}
    }

    public void saveWorkingDays(String month,double days)throws SQLException{
        if(days<0)throw new SQLException("Working Days cannot be negative.");
        Connection c=DBConnection.getConnection();
        if(c==null){workingDaysMemory.put(month,days);return;}
        String sql="UPDATE attendance SET total_working_days=? WHERE month=? AND fy=? AND employee_id IN (SELECT employee_id FROM employee_master_data WHERE is_deleted=0)";
        try(c;PreparedStatement p=c.prepareStatement(sql)){p.setDouble(1,days);p.setString(2,month);p.setString(3,fy(month));p.executeUpdate();}
    }

    public void save(AttendanceRecord record)throws SQLException{
        saveAll(java.util.List.of(record));
    }

    public void saveAll(Collection<AttendanceRecord> records)throws SQLException{
        if(records==null||records.isEmpty())return;
        Connection c=DBConnection.getConnection();
        if(c==null){for(AttendanceRecord r:records)memory.put(key(r.employeeId,r.month),r);return;}
        boolean originalAutoCommit=c.getAutoCommit();
        try(c){c.setAutoCommit(false);for(AttendanceRecord r:records)save(c,r);c.commit();}
        catch(SQLException e){try{c.rollback();}catch(SQLException ignored){}throw e;}
        finally{try{c.setAutoCommit(originalAutoCommit);}catch(SQLException ignored){}}
    }

    public AttendanceRecord load(String employeeId,String month){
        Connection c=DBConnection.getConnection();
        if(c==null)return memory.getOrDefault(key(employeeId,month),AttendanceRecord.blank(employeeId,month));
        String sql="SELECT a.* FROM attendance a JOIN employee_master_data e ON e.employee_id=a.employee_id WHERE a.employee_id=? AND a.month=? AND a.fy=? AND e.is_deleted=0";
        try(c;PreparedStatement p=c.prepareStatement(sql)){p.setString(1,employeeId);p.setString(2,month);p.setString(3,fy(month));ResultSet r=p.executeQuery();return r.next()?read(r):AttendanceRecord.blank(employeeId,month);}catch(SQLException ignored){return AttendanceRecord.blank(employeeId,month);}
    }

    public boolean hasSavedAttendance(String employeeId,String month){
        Connection c=DBConnection.getConnection();
        if(c==null)return memory.containsKey(key(employeeId,month));
        String sql="SELECT 1 FROM attendance a JOIN employee_master_data e ON e.employee_id=a.employee_id WHERE a.employee_id=? AND a.month=? AND a.fy=? AND e.is_deleted=0";
        try(c;PreparedStatement p=c.prepareStatement(sql)){p.setString(1,employeeId);p.setString(2,month);p.setString(3,fy(month));return p.executeQuery().next();}catch(SQLException ignored){return false;}
    }

    /** Employee deletion is soft; attendance history is retained. */
    public void purgeEmployee(String employeeId) { }

    private void save(Connection c,AttendanceRecord r)throws SQLException{
        if(!active(c,r.employeeId))throw new SQLException("Cannot save attendance for a deleted employee.");
        String sql="INSERT INTO attendance (employee_id,employee_name,month,fy,total_working_days,absent_days,cl,el,days_payable,ot_hours,other) VALUES(?,(SELECT employee_name FROM employee_master_data WHERE employee_id=?),?,?,?,?,?,?,?,?,?) ON CONFLICT(employee_id,month,fy) DO UPDATE SET employee_name=excluded.employee_name,total_working_days=excluded.total_working_days,absent_days=excluded.absent_days,cl=excluded.cl,el=excluded.el,days_payable=excluded.days_payable,ot_hours=excluded.ot_hours,other=excluded.other";
        try(PreparedStatement p=c.prepareStatement(sql)){p.setString(1,r.employeeId);p.setString(2,r.employeeId);p.setString(3,r.month);p.setString(4,fy(r.month));p.setDouble(5,r.workingDays);p.setDouble(6,r.absentDays);p.setDouble(7,r.paidLeaveDays);p.setDouble(8,r.unpaidLeaveDays);p.setDouble(9,r.daysPayable);p.setDouble(10,r.overtimeHours);p.setDouble(11,r.otherHours);p.executeUpdate();}
    }

    private boolean active(Connection c,String employeeId)throws SQLException{try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM employee_master_data WHERE employee_id=? AND is_deleted=0")){p.setString(1,employeeId);return p.executeQuery().next();}}
    private AttendanceRecord read(ResultSet r)throws SQLException{AttendanceRecord a=new AttendanceRecord();a.employeeId=r.getString("employee_id");a.month=r.getString("month");a.workingDays=r.getDouble("total_working_days");a.absentDays=r.getDouble("absent_days");a.paidLeaveDays=r.getDouble("cl");a.unpaidLeaveDays=r.getDouble("el");a.daysPayable=r.getDouble("days_payable");a.overtimeHours=r.getDouble("ot_hours");a.otherHours=r.getDouble("other");return a;}
    private static String key(String employeeId,String month){return employeeId+"|"+month;}
    private static String fy(String month){YearMonth value=YearMonth.parse(month);int start=value.getMonthValue()<4?value.getYear()-1:value.getYear();return start+"-"+(start+1);}
}
