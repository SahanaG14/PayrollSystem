import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/** Daemon-backed worker pool: pending non-UI work can never hold the JVM open. */
public final class ApplicationTasks {
    private static final ThreadFactory DAEMON_FACTORY = runnable -> {
        Thread thread = new Thread(runnable, "Payroll-Worker");
        thread.setDaemon(true);
        return thread;
    };
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, DAEMON_FACTORY);

    public static void execute(Runnable work) { EXECUTOR.execute(work); }
    public static void shutdown() { AutoSaveService.shutdown(); EXECUTOR.shutdownNow(); }
    private ApplicationTasks() { }
}
