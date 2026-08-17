import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/** Adds a small, familiar Excel-style fill action to editable Swing tables. */
public final class ExcelTableSupport {
    private ExcelTableSupport() { }
    public static void enableNumericMultiFill(JTable table, boolean wholeNumbersOnly) {
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent event) {
                if(table.isEditing() || table.getSelectedRowCount()*table.getSelectedColumnCount() < 2) return;
                char typed=event.getKeyChar();
                if(Character.isISOControl(typed)) return;
                String value=String.valueOf(typed);
                if(wholeNumbersOnly ? !value.matches("\\d") : !value.matches("[0-9.-]")) return;
                if(wholeNumbersOnly ? !value.matches("\\d+") : !value.matches("-?\\d+(\\.\\d+)?")) return;
                for(int row:table.getSelectedRows()) for(int column:table.getSelectedColumns()) if(table.isCellEditable(row,column)) table.setValueAt(wholeNumbersOnly?Integer.parseInt(value):Double.parseDouble(value),row,column);
                event.consume();
            }
        });
    }
}
