import javax.swing.*;
import java.awt.*;

/** Consistent protection against accidentally closing a top-level YASL window. */
public final class CloseConfirmation {
    public static boolean confirm(Window owner) {
        return JOptionPane.showConfirmDialog(owner, "Are you sure you want to close YASL Payroll?", "Close YASL Payroll", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
    private CloseConfirmation() { }
}
