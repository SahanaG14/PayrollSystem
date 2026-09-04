import java.sql.Timestamp;

/** Compatibility facade: all audit events now use the one central ActivityLogger. */
public final class AuditLogDAO {
    public AuditLogDAO(){}
    public void log(String action,String employeeId){ActivityLogger.logRecord("Application",action,"Completed",employeeId,"SUCCESS");}
    public static void logActivity(String username,String action,String details,Timestamp timestamp){ActivityLogger.logAs(username,"Application",action,details,action.contains("FAILED")?"FAILED":"SUCCESS");}
}
