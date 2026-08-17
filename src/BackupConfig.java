import java.nio.file.*;import java.util.prefs.Preferences;
public final class BackupConfig{
 private static final Preferences P=Preferences.userNodeForPackage(BackupConfig.class);
 public static Path folder(){String p=P.get("folder","");return p.isBlank()?null:Paths.get(p);}public static void folder(Path p){P.put("folder",p.toAbsolutePath().toString());}
 public static int intervalDays(){return P.getInt("intervalDays",7);}public static void intervalDays(int v){P.putInt("intervalDays",v);}
 public static long lastBackup(){return P.getLong("lastBackup",0);}public static void completed(){P.putLong("lastBackup",System.currentTimeMillis());}
 private BackupConfig(){}
}
