import javax.swing.*;
import java.util.concurrent.CopyOnWriteArrayList;

/** Small in-process event bridge for recalculating dependent views after a saved payroll action. */
public final class PayrollEvents {
    private static final CopyOnWriteArrayList<Runnable> ATTENDANCE_SAVED = new CopyOnWriteArrayList<>();
    public static void onAttendanceSaved(Runnable listener) { ATTENDANCE_SAVED.add(listener); }
    public static void attendanceSaved() {
        SwingUtilities.invokeLater(() -> ATTENDANCE_SAVED.forEach(listener -> {
            try { listener.run(); } catch (RuntimeException ignored) { }
        }));
    }
    private PayrollEvents() { }
}
