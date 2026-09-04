import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Read-only in-app view of the application's SQLite database. */
public final class DatabaseViewerPanel extends JPanel {
    private static final int ROW_LIMIT = 1_000;
    private final JComboBox<String> tables = new JComboBox<>();
    private final JTable rows = new JTable(readOnlyModel(new String[0], new Object[0][0]));
    private final JLabel status = new JLabel("Unlock with the Master Data password to view database records.");
    private boolean unlocked;

    public DatabaseViewerPanel() {
        super(new BorderLayout(10, 10));
        setBorder(UIStyleUtility.compactModuleBorder());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton unlock = new JButton("Open SQLite Database (Read-only)");
        unlock.addActionListener(e -> unlock());
        tables.setEnabled(false);
        tables.addActionListener(e -> loadSelectedTable());
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> loadTables());
        controls.add(unlock); controls.add(new JLabel("Table:")); controls.add(tables); controls.add(refresh);
        add(controls, BorderLayout.NORTH);
        rows.setAutoCreateRowSorter(true);
        add(new JScrollPane(rows), BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(4, 2, 2, 2));
        add(status, BorderLayout.SOUTH);
    }

    private void unlock() {
        if (!SecurityService.promptForPassword("Master Data Password Required")) return;
        unlocked = true;
        loadTables();
    }

    private void loadTables() {
        if (!unlocked) return;
        status.setText("Loading database tables…");
        new SwingWorker<List<String>, Void>() {
            @Override protected List<String> doInBackground() throws Exception {
                List<String> names = new ArrayList<>();
                try (Connection c = DBConnection.getConnection(); Statement s = c == null ? null : c.createStatement()) {
                    if (s == null) throw new SQLException("Database is unavailable.");
                    try (ResultSet result = s.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")) {
                        while (result.next()) names.add(result.getString(1));
                    }
                }
                return names;
            }
            @Override protected void done() {
                try {
                    List<String> names = get();
                    tables.removeAllItems(); for (String name : names) tables.addItem(name);
                    tables.setEnabled(!names.isEmpty());
                    status.setText(names.isEmpty() ? "No application tables were found." : "Read-only view — " + DBConnection.databaseFile().getAbsolutePath());
                    if (!names.isEmpty()) loadSelectedTable();
                } catch (Exception error) { status.setText("Could not read database tables: " + message(error)); }
            }
        }.execute();
    }

    private void loadSelectedTable() {
        if (!unlocked || tables.getSelectedItem() == null) return;
        String table = String.valueOf(tables.getSelectedItem());
        status.setText("Loading " + table + "…");
        new SwingWorker<TableData, Void>() {
            @Override protected TableData doInBackground() throws Exception {
                List<String> names = new ArrayList<>(); List<Object[]> values = new ArrayList<>();
                try (Connection c = DBConnection.getConnection(); Statement s = c == null ? null : c.createStatement()) {
                    if (s == null) throw new SQLException("Database is unavailable.");
                    try (ResultSet result = s.executeQuery("SELECT * FROM \"" + table.replace("\"", "\"\"") + "\" LIMIT " + ROW_LIMIT)) {
                        ResultSetMetaData meta = result.getMetaData();
                        for (int column = 1; column <= meta.getColumnCount(); column++) names.add(meta.getColumnLabel(column));
                        while (result.next()) { Object[] row = new Object[names.size()]; for (int column = 0; column < row.length; column++) row[column] = result.getObject(column + 1); values.add(row); }
                    }
                }
                return new TableData(names.toArray(String[]::new), values.toArray(Object[][]::new));
            }
            @Override protected void done() {
                try { TableData data = get(); rows.setModel(readOnlyModel(data.columns, data.values)); status.setText("Read-only view of " + table + " — " + data.values.length + (data.values.length == ROW_LIMIT ? "+" : "") + " row(s)."); }
                catch (Exception error) { status.setText("Could not read " + table + ": " + message(error)); }
            }
        }.execute();
    }

    private static DefaultTableModel readOnlyModel(String[] columns, Object[][] values) { return new DefaultTableModel(values, columns) { @Override public boolean isCellEditable(int row, int column) { return false; } }; }
    private static String message(Exception error) { Throwable cause = error.getCause() == null ? error : error.getCause(); return cause.getMessage() == null ? "Database error." : cause.getMessage(); }
    private record TableData(String[] columns, Object[][] values) { }
}
