import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.regex.Pattern;

/** Shared Employee ID filter displayed above employee-based grids. */
public final class EmployeeIdSearch {
    public static JPanel create(JTable table, DefaultTableModel model) {
        return create(table, model, 0);
    }

    public static JPanel create(JTable table, DefaultTableModel model, int... columns) {
        TabStyle.configureTable(table);
        TableRowSorter<DefaultTableModel> sorter=new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        final int[] searchable = columns.length == 1 && columns[0] == 0
                && model.getColumnClass(0) == Boolean.class ? new int[]{1, 2} : columns;
        JTextField query=new JTextField(16);
        query.getDocument().addDocumentListener(new DocumentListener() {
            private void apply(){String value=query.getText().trim();sorter.setRowFilter(value.isEmpty()?null:RowFilter.regexFilter("(?i)"+Pattern.quote(value),searchable));}
            public void insertUpdate(DocumentEvent e){apply();}
            public void removeUpdate(DocumentEvent e){apply();}
            public void changedUpdate(DocumentEvent e){apply();}
        });
        JPanel bar=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));
        bar.add(new JLabel("Search Employee ID:"));
        bar.add(query);
        return bar;
    }
    private EmployeeIdSearch(){}
}
