import java.nio.file.Path;import java.util.concurrent.*;
public final class BackupScheduler{
 private static final ScheduledExecutorService EXECUTOR=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"payroll-backup");t.setDaemon(true);return t;});
 public static void start(){EXECUTOR.scheduleWithFixedDelay(BackupScheduler::run,1,24,TimeUnit.HOURS);}private static void run(){try{Path folder=BackupConfig.folder();if(folder!=null&&System.currentTimeMillis()-BackupConfig.lastBackup()>=TimeUnit.DAYS.toMillis(BackupConfig.intervalDays()))BackupService.backup(folder);}catch(Exception ignored){}}
 public static void shutdown(){EXECUTOR.shutdownNow();}private BackupScheduler(){}
}
