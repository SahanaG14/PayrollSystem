import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.io.File;

/** Central SQLite location. Packaged builds keep writable data outside Program Files. */
public final class DBConnection {
    private static final File DATABASE_FILE=existingDatabaseFile();
    private static final String URL="jdbc:sqlite:"+DATABASE_FILE.getAbsolutePath();
    private static final Object DATABASE_LOCK=new Object();
    static {File parent=DATABASE_FILE.getParentFile();if(parent!=null&&!parent.exists())parent.mkdirs();try{Class.forName("org.sqlite.JDBC");try(Connection ignored=DriverManager.getConnection(URL)){}}catch(Exception ignored){}}
    private static File existingDatabaseFile(){String configured=System.getProperty("payroll.db.path","").trim();if(!configured.isEmpty())return new File(configured);if(Boolean.getBoolean("payroll.packaged")){String local=System.getenv("LOCALAPPDATA");File base=(local==null||local.isBlank())?new File(System.getProperty("user.home"),"YASL Payroll"):new File(local,"YASL Payroll");File current=new File(new File(base,"data"),"payroll.db");File legacyBase=(local==null||local.isBlank())?new File(System.getProperty("user.home"),"PayrollSystem"):new File(local,"PayrollSystem");File legacy=new File(legacyBase,"payroll.db");return !current.isFile()&&legacy.isFile()?legacy:current;}return new File(System.getProperty("user.dir"),"payroll.db");}
    public static File databaseFile(){return DATABASE_FILE;}
    public static Object databaseLock(){return DATABASE_LOCK;}
    public static Connection getConnection(){try{Class.forName("org.sqlite.JDBC");Connection connection=DriverManager.getConnection(URL);try(Statement statement=connection.createStatement()){statement.execute("PRAGMA foreign_keys = ON");statement.execute("PRAGMA busy_timeout = 5000");}return connection;}catch(Exception ignored){return null;}}
    private DBConnection(){}
}
