import javax.swing.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Small in-process event bridge for recalculating dependent views after a saved payroll action. */
public final class PayrollEvents {
    private static final CopyOnWriteArrayList<Runnable> ATTENDANCE_SAVED = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Consumer<String>> DATA_CHANGED = new CopyOnWriteArrayList<>();
    public static void onAttendanceSaved(Runnable listener) { ATTENDANCE_SAVED.add(listener); }
    public static void attendanceSaved() {
        SwingUtilities.invokeLater(() -> ATTENDANCE_SAVED.forEach(listener -> {
            try { listener.run(); } catch (RuntimeException ignored) { }
        }));
    }
    public static void onDataChanged(Consumer<String> listener) { DATA_CHANGED.add(listener); }
    /** Fires only after a confirmed operation; listeners execute on the Swing EDT. */
    public static void dataChanged(String event) { SwingUtilities.invokeLater(() -> DATA_CHANGED.forEach(listener -> { try { listener.accept(event); } catch (RuntimeException ignored) { } })); }
    private PayrollEvents() { }
}
