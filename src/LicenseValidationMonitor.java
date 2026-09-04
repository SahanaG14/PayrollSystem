import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Performs one non-blocking online validation every two minutes and on safe focus changes. */
public final class LicenseValidationMonitor {
    private static final Duration INTERVAL = Duration.ofMinutes(2);
    private static final Duration OFFLINE_GRACE = Duration.ofHours(Long.getLong("payroll.license.offline.grace.hours", 72L));
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "payroll-license-validator");
        thread.setDaemon(true);
        return thread;
    });
    private final Runnable invalidLockout;
    private final Runnable unavailableLockout;
    private final AtomicBoolean checking = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();

    public LicenseValidationMonitor(Runnable invalidLockout, Runnable unavailableLockout) { this.invalidLockout = invalidLockout; this.unavailableLockout = unavailableLockout; }

    public void start() {
        executor.scheduleWithFixedDelay(this::check, INTERVAL.toMillis(), INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void checkNow() {
        if (!stopped.get()) executor.execute(this::check);
    }

    private void check() {
        if (stopped.get() || !checking.compareAndSet(false, true)) return;
        try {
            LicenseService.RuntimeValidation result = LicenseService.validateSavedLicenseForRuntime();
            if (result.status() == LicenseService.ValidationStatus.INVALID) {
                stop();
                invalidLockout.run();
            } else if (result.status() == LicenseService.ValidationStatus.UNAVAILABLE && graceExpired()) {
                stop();
                unavailableLockout.run();
            }
        } finally {
            checking.set(false);
        }
    }

    private boolean graceExpired() {
        long lastVerified = LicenseService.lastVerifiedAt();
        return lastVerified <= 0 || System.currentTimeMillis() - lastVerified > OFFLINE_GRACE.toMillis();
    }

    public void stop() {
        if (stopped.compareAndSet(false, true)) executor.shutdownNow();
    }
}
