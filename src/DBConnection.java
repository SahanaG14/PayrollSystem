import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.io.File;

public class DBConnection {
    private static final File DATABASE_FILE = existingDatabaseFile();
    private static final String URL = "jdbc:sqlite:"+DATABASE_FILE.getAbsolutePath();
    static {
        File parent=DATABASE_FILE.getParentFile();if(parent!=null&&!parent.exists())parent.mkdirs();
        try { Class.forName("org.sqlite.JDBC"); try(Connection ignored=DriverManager.getConnection(URL)) { System.out.println("Successfully connected to SQLite DB!"); } }
        catch (Exception ignored) { }
    }
    private static File existingDatabaseFile(){String configured=System.getProperty("payroll.db.path","").trim();return configured.isEmpty()?new File(System.getProperty("user.dir"),"payroll.db"):new File(configured);}
    public static Connection getConnection() {
        try { System.out.println("SQLite DB Absolute Path: " + DATABASE_FILE.getAbsolutePath()); Class.forName("org.sqlite.JDBC"); Connection connection=DriverManager.getConnection(URL); System.out.println("Successfully connected to SQLite DB!"); try(Statement statement=connection.createStatement()){statement.execute("PRAGMA foreign_keys = ON");statement.execute("PRAGMA busy_timeout = 5000");} return connection; }
        catch (Exception ignored) { return null; }
    }
}
