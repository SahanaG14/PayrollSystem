import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseInitializer {
    public static void initialize(){
        ensureSQLiteTables();
        new EmployeeDAO();new AttendanceDAO();
        PayrollRulesStore.all();TaxSlabStore.list();
    }
    public static void rebuildFromScratch(){try(Connection connection=DBConnection.getConnection();Statement statement=connection==null?null:connection.createStatement()){if(statement==null)return;List<String> tables=new ArrayList<>();try(ResultSet result=statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")){while(result.next())tables.add(result.getString(1));}statement.execute("PRAGMA foreign_keys = OFF");for(String table:tables)if(!"employees".equalsIgnoreCase(table)&&!"employee_master_data".equalsIgnoreCase(table))statement.executeUpdate("DROP TABLE IF EXISTS \""+table.replace("\"","\"\"")+"\"");statement.execute("PRAGMA foreign_keys = ON");}catch(Exception ignored){}initialize();}
    private static void ensureSQLiteTables(){try(Connection connection=DBConnection.getConnection();Statement statement=connection==null?null:connection.createStatement()){if(statement==null)return;for(String table:new String[]{"employee_master_data","attendance","ctc","earnings_allowances","deductions","salary","payslip","it_computation","settings_company_details","settings_allowance_defaults","settings_ot_earnings","settings_attendance_leave_rules","settings_deduction_rules","settings_tax_slabs","settings_revised_salary","settings_passwords"}){try{statement.executeUpdate("ALTER TABLE "+table+" ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0");}catch(Exception ignored){}try{statement.executeUpdate("ALTER TABLE "+table+" ADD COLUMN deleted_at TEXT");}catch(Exception ignored){}}}catch(Exception ignored){}}
    private DatabaseInitializer() { }
}
