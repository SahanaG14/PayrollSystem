import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Main application shell. Card changes are deliberately lightweight and never wait on an active table editor. */
public class PayrollSystemFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final EmployeeDAO employees = new EmployeeDAO();
    private final Map<String, JButton> menuButtons = new LinkedHashMap<>();
    private JPanel sidebar;
    private JButton sidebarToggle;
    private boolean sidebarCollapsed;

    private JTextField search;
    private JTable employeeTable;
    private JLabel moduleTitle;
    private PayrollPanel payroll;
    private AttendancePanel attendance;
    private CTCPanel ctc;
    private EarningsAndAllowancesPanel earningsAndAllowances;
    private SalaryPanel salary;
    private DeductionsPanel deductions;
    private MasterDataPanel masterData;
    private DashboardPanel dashboard;
    private BackupRestorePanel backupRestore;
    private ReportsPanel reports;
    private HelpPanel help;
    private JComponent masterForm;
    private boolean switching;
    private String activeCard = "Employee";
    private final Set<String> staleModules = new HashSet<>();

    public PayrollSystemFrame() {
        super(Branding.APPLICATION_NAME);
        Branding.applyWindowIcon(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1280, 720));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        RepaintManager.currentManager(this).setDoubleBufferingEnabled(true);
        installShutdownHandler();
        addWindowFocusListener(new WindowFocusListener() {
            @Override public void windowGainedFocus(WindowEvent event) { Main.revalidateLicenseWhenFocused(); }
            @Override public void windowLostFocus(WindowEvent event) { }
        });
        BackupScheduler.start();
        PayrollEvents.onAttendanceSaved(() -> {
            if (ctc != null) ctc.refresh();
            if (salary != null) salary.refresh();
            if (earningsAndAllowances != null) earningsAndAllowances.refresh();
        });
        PayrollEvents.onDataChanged(this::markDependentModulesStale);

        add(nav(), BorderLayout.WEST);
        add(top(), BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);

        content.add(employeeView(), "Employee");
        content.add(settingsView(), "Settings");
        ActivityLogger.log("Security", "APPLICATION START", Branding.COMPANY_NAME+" started", "LOGIN");
        showCard("Dashboard");
    }

    private void installShutdownHandler() {
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                Main.applicationClosing();
                stopActiveEditing(content);
                for (Window window : Window.getWindows()) {
                    if (window != PayrollSystemFrame.this && window.isDisplayable()) window.dispose();
                }
                ApplicationTasks.shutdown();
                BackupScheduler.shutdown();
            }
        });
    }

    private JComponent settingsView() {
        JTabbedPane settings = new JTabbedPane();
        settings.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        settings.setFont(new Font("SansSerif", Font.BOLD, 14));
        ChangePasswordPanel passwordSettings = new ChangePasswordPanel(this::openCredentialRecovery);
        settings.addTab("Company Details", new SettingsEditGuard(new CompanyDetailsPanel()));
        settings.addTab("Allowance Defaults", new SettingsEditGuard(new AllowanceSettingsPanel()));
        settings.addTab("OT & Other Earnings", new SettingsEditGuard(new OtherEarningsSettingsPanel()));
        settings.addTab("Attendance & Leave Rules", new SettingsEditGuard(new AttendanceSettingsPanel()));
        settings.addTab("Deduction Rules", new SettingsEditGuard(new DeductionSettingsPanel()));
        settings.addTab("Revised Salary Settings", new SettingsEditGuard(new RevisedSalarySettingsPanel()));
        settings.addTab("Password management", new SettingsEditGuard(passwordSettings));
        settings.addChangeListener(event -> passwordSettings.refresh());
        return settings;
    }

    private JComponent nav() {
        JPanel panel = new JPanel(); sidebar = panel;
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(220, 0));
        panel.setBackground(new Color(22, 35, 55));
        panel.setDoubleBuffered(true);

        sidebarToggle=new JButton("\u2630  MENU");sidebarToggle.setFont(new Font("SansSerif",Font.BOLD,20));sidebarToggle.setForeground(Color.WHITE);sidebarToggle.setOpaque(false);sidebarToggle.setContentAreaFilled(false);sidebarToggle.setBorderPainted(false);sidebarToggle.setFocusPainted(false);sidebarToggle.setToolTipText("Collapse sidebar");sidebarToggle.setMaximumSize(new Dimension(220,45));sidebarToggle.setPreferredSize(new Dimension(220,45));sidebarToggle.setMinimumSize(new Dimension(220,45));sidebarToggle.setAlignmentX(Component.CENTER_ALIGNMENT);sidebarToggle.setHorizontalAlignment(SwingConstants.LEFT);sidebarToggle.setBorder(BorderFactory.createEmptyBorder(0,20,0,16));sidebarToggle.addActionListener(e->toggleSidebar());panel.add(sidebarToggle);

        for (String name : new String[]{"Dashboard", "Master Data", "Employee", "Attendance", "Cost to Company (CTC)", "Earnings & Allowances", "Deductions", "Salary", "Payslip", "Reports"}) {
            JButton button = navButton(name);
            menuButtons.put(name, button);
            button.addActionListener(event -> requestNavigation(name));
            panel.add(button);
        }
        panel.add(Box.createVerticalGlue());
        JButton settings=navButton("Settings");menuButtons.put("Settings",settings);settings.addActionListener(e->requestNavigation("Settings"));panel.add(settings);
        JButton backup=navButton("Backup & Restore");menuButtons.put("Backup & Restore",backup);backup.addActionListener(e->requestNavigation("Backup & Restore"));panel.add(backup);
        JButton helpButton=navButton("Help");menuButtons.put("Help",helpButton);helpButton.addActionListener(e->requestNavigation("Help"));panel.add(helpButton);
        JButton logout=navButton("Logout");menuButtons.put("Logout",logout);logout.addActionListener(e->logout());panel.add(logout);
        return panel;
    }

    private JButton navButton(String text) { JButton button = new RoundedNavButton(text); button.putClientProperty("navButton", Boolean.TRUE); button.setMaximumSize(new Dimension(218, 48)); button.setPreferredSize(new Dimension(218, 48)); button.setAlignmentX(Component.CENTER_ALIGNMENT); button.setHorizontalAlignment(SwingConstants.LEFT); button.setFont(new Font("SansSerif", Font.BOLD, 15)); button.setForeground(Color.WHITE); button.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 16)); button.setFocusPainted(false); button.setOpaque(false); button.setContentAreaFilled(false); button.setBorderPainted(false); return button; }

    private void toggleSidebar(){sidebarCollapsed=!sidebarCollapsed;updateSidebarLayout();}
    private void updateSidebarLayout(){int targetWidth=sidebarCollapsed?60:220;Dimension slotSize=new Dimension(targetWidth,45);sidebarToggle.setText(sidebarCollapsed?"\u2630":"\u2630  MENU");sidebarToggle.setPreferredSize(slotSize);sidebarToggle.setMinimumSize(slotSize);sidebarToggle.setMaximumSize(slotSize);sidebarToggle.setHorizontalAlignment(sidebarCollapsed?SwingConstants.CENTER:SwingConstants.LEFT);sidebarToggle.setToolTipText(sidebarCollapsed?"Expand sidebar":"Collapse sidebar");for(JButton button:menuButtons.values()){Dimension size=new Dimension(targetWidth,48);button.setPreferredSize(size);button.setMinimumSize(size);button.setMaximumSize(size);}updateSidebarIcons();int from=sidebar.getPreferredSize().width;new Timer(12,new java.awt.event.ActionListener(){int width=from;public void actionPerformed(java.awt.event.ActionEvent e){width+=Integer.compare(targetWidth,width)*Math.min(20,Math.abs(targetWidth-width));sidebar.setPreferredSize(new Dimension(width,0));sidebar.revalidate();sidebar.repaint();if(width==targetWidth)((Timer)e.getSource()).stop();}}).start();}
    private void updateSidebarIcons(){for(Map.Entry<String,JButton> entry:menuButtons.entrySet()){JButton button=entry.getValue();if(button.getClientProperty("menuText")==null)button.putClientProperty("menuText",button.getText());String text=String.valueOf(button.getClientProperty("menuText"));button.setText(sidebarCollapsed?sidebarIcon(entry.getKey()):text);button.setToolTipText(sidebarCollapsed?text:null);button.setHorizontalAlignment(sidebarCollapsed?SwingConstants.CENTER:SwingConstants.LEFT);}}
    private String sidebarIcon(String name){return switch(name){case "Dashboard"->"\uD83D\uDCCA";case "Master Data"->"\uD83D\uDCC2";case "Employee"->"\uD83D\uDC65";case "Attendance"->"\uD83D\uDCC5";case "Cost to Company (CTC)"->"\uD83D\uDCB0";case "Earnings & Allowances"->"\uD83D\uDCC8";case "Deductions"->"\uD83D\uDCC9";case "Salary"->"\uD83D\uDCBB";case "Payslip"->"\uD83D\uDCC4";case "Reports"->"\uD83D\uDCCA";case "Settings"->"\u2699";case "Backup & Restore"->"\uD83D\uDCBE";case "Help"->"\u2753";case "Logout"->"\u279C";default->"";};}

    private void logout(){String username=Session.currentUser;AuditLogDAO.logActivity(username,"USER_LOGOUT","User logged out of system",new java.sql.Timestamp(System.currentTimeMillis()));Session.logout();dispose();SwingUtilities.invokeLater(()->new LoginFrame().setVisible(true));}

    private JComponent top() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));
        moduleTitle = new JLabel("Employee");
        moduleTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        panel.add(moduleTitle, BorderLayout.WEST);
        JButton refresh = new JButton("↻");
        refresh.setFont(new Font("SansSerif", Font.BOLD, 24));
        refresh.setToolTipText("Refresh current module");
        refresh.setFocusPainted(false);
        refresh.setMargin(new Insets(1, 8, 3, 8));
        refresh.addActionListener(event -> refreshActiveModule());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.add(refresh);
        panel.add(actions, BorderLayout.EAST);
        return panel;
    }

    /** Reloads the visible module without requiring the user to leave and re-open its tab. */
    private void refreshActiveModule() {
        Component visible = visibleModule();
        if (hasUnsavedEdits(visible) && JOptionPane.showConfirmDialog(this, "Unsaved changes are present. Refreshing will discard them. Do you want to continue?", "Refresh", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        stopActiveEditing(visible);
        refreshModule(activeCard);
        staleModules.remove(activeCard);
        content.revalidate();
        content.repaint();
    }

    private void markDependentModulesStale(String event) {
        switch (event) {
            case "employee" -> staleModules.addAll(java.util.List.of("Employee", "Attendance", "CTC", "Earnings & Allowances", "Deductions", "Salary", "Payslip", "Reports", "Dashboard"));
            case "attendance", "ctc", "earnings", "deductions", "settings" -> staleModules.addAll(java.util.List.of("CTC", "Earnings & Allowances", "Deductions", "Salary", "Payslip", "Reports", "Dashboard"));
            case "restore" -> staleModules.addAll(java.util.List.of("Employee", "Master Data", "Attendance", "CTC", "Earnings & Allowances", "Deductions", "Salary", "Payslip", "Reports", "Dashboard", "Backup & Restore"));
            default -> { return; }
        }
        if (staleModules.contains(activeCard) && !hasUnsavedEdits(content)) { refreshModule(activeCard); staleModules.remove(activeCard); }
    }

    private void requestNavigation(String menu) {
        if (switching) return;
        if ("Master Data".equals(menu)) {
            openMasterData();
            return;
        }
        if ("Deductions".equals(menu)) {
            openDeductions();
            return;
        }
        String card = menu.startsWith("Cost") ? "CTC" : menu;
        showCard(card);
    }

    private void openDeductions() {
        try {
            if (deductions == null) {
                DeductionsPanel panel = new DeductionsPanel();
                content.add(panel, "Deductions");
                deductions = panel;
            }
            cards.show(content, "Deductions");
            activeCard = "Deductions";
            selectMenu("Deductions");
            content.revalidate();
            content.repaint();
        } catch (RuntimeException exception) {
            deductions = null;
            JOptionPane.showMessageDialog(this, "Unable to open Deductions: " + exception.getMessage(), "Deductions", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openCredentialRecovery() {
        showCard("Help");
        help.showCredentialRecovery();
    }

    private JComponent employeeView() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(UIStyleUtility.compactModuleBorder());
        JLabel heading = new JLabel("Active Employees (Master Data)");
        heading.setFont(new Font("SansSerif", Font.BOLD, 24));
        search = new JTextField(16);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void load() { if (employeeTable != null && !switching) loadEmployees(search.getText().trim()); }
            public void insertUpdate(javax.swing.event.DocumentEvent event) { load(); }
            public void removeUpdate(javax.swing.event.DocumentEvent event) { load(); }
            public void changedUpdate(javax.swing.event.DocumentEvent event) { load(); }
        });
        JPanel north = new JPanel(new BorderLayout());
        north.add(heading, BorderLayout.NORTH);
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filter.add(new JLabel("Search Employee ID:"));
        filter.add(search);
        north.add(filter, BorderLayout.SOUTH);
        panel.add(north, BorderLayout.NORTH);
        employeeTable = new JTable();
        TabStyle.configureTable(employeeTable);
        employeeTable.setRowHeight(32);
        employeeTable.setDoubleBuffered(true);
        panel.add(new JScrollPane(employeeTable), BorderLayout.CENTER);
        loadEmployees("");
        return panel;
    }

    private void loadEmployees(String query) {
        java.util.List<Employee> list = employees.listActive(query);
        list.sort(Comparator.comparing(Employee::getId, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        Object[][] rows = new Object[list.size()][6];
        for (int i = 0; i < list.size(); i++) {
            Employee employee = list.get(i);
            rows[i] = new Object[]{employee.getId(), employee.getName(), employee.getDepartment(), employee.getDesignation(), employee.getPhone(), employee.getEmail()};
        }
        employeeTable.setModel(new DefaultTableModel(rows, new String[]{"Employee ID", "Employee Name", "Department", "Designation", "Phone Number", "Email ID"}) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        employeeTable.clearSelection();
        employeeTable.revalidate();
        employeeTable.repaint();
    }

    private void openMasterData() {
        if (!SecurityService.hasMasterPassword()) {
            SecurityService.offerMasterDataSetup(this);
            return;
        }
        if (!SecurityService.promptForPassword("Enter Master Data Password")) {
            JOptionPane.showMessageDialog(this, "Master Data access was not granted.");
            return;
        }
        if (masterData == null) {
            masterData = new MasterDataPanel(this);
            content.add(masterData, "Master Data");
        }
        showCard("Master Data");
        SwingUtilities.invokeLater(masterData::refreshTable);
    }

    void openMasterDataForDashboard() {
        if (!SecurityService.hasMasterPassword()) { SecurityService.offerMasterDataSetup(this); return; }
        if (!SecurityService.promptForPassword("Enter Master Data Password")) { JOptionPane.showMessageDialog(this,"Master Data access was not granted."); return; }
        if (masterData == null) { masterData=new MasterDataPanel(this); content.add(masterData,"Master Data"); }
        showMasterForm(null);
    }

    void showMasterForm(Employee employee) {
        stopActiveEditing(content);
        if (masterForm != null) content.remove(masterForm);
        EmployeeFormPanel form = new EmployeeFormPanel(this);
        if (employee != null) form.edit(employee);
        masterForm = form;
        content.add(form, "Master Form");
        showCard("Master Form");
    }

    void openEmployeeForm(Employee employee) { showMasterForm(employee); }

    void showCard(String name) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showCard(name));
            return;
        }
        if (switching) return;
        switching = true;
        try {
            stopActiveEditing(content);
            ensureModule(name);
            if (staleModules.contains(name) && !hasUnsavedEdits(content)) { refreshModule(name); staleModules.remove(name); }
            TabStyle.apply(content);
            cards.show(content, name);
            activeCard = name;
            selectMenu(name);
            content.revalidate();
            content.repaint();
        } finally {
            switching = false;
        }
        // Do not rebuild every module after every click. Some registers contain twelve
        // month sheets, and rebuilding them on the EDT made the menu appear locked.
        // New modules load when created; explicit saves refresh the affected view.
    }

    private void refreshModule(String name) {
        if (!isDisplayable()) return;
        try {
            if ("Payslip".equals(name) && payroll != null) payroll.refreshEmployees();
            else if ("Attendance".equals(name) && attendance != null) attendance.refreshEmployees();
            else if ("CTC".equals(name) && ctc != null) ctc.refresh();
            else if ("Earnings & Allowances".equals(name) && earningsAndAllowances != null) earningsAndAllowances.refresh();
            else if ("Salary".equals(name) && salary != null) salary.refresh();
            else if ("Deductions".equals(name) && deductions != null) deductions.refresh();
            else if ("Employee".equals(name) && employeeTable != null) loadEmployees(search == null ? "" : search.getText());
            else if ("Master Data".equals(name) && masterData != null) masterData.refreshTable();
            else if ("Reports".equals(name) && reports != null) reports.refresh();
            else if ("Dashboard".equals(name) && dashboard != null) dashboard.refresh();
        } catch (RuntimeException ignored) {
            // A failed module refresh must never lock the navigation shell.
        }
    }

    private void ensureModule(String name) {
        if ("Dashboard".equals(name) && dashboard == null) {
            dashboard = new DashboardPanel(this); content.add(dashboard, "Dashboard");
        } else if ("Attendance".equals(name) && attendance == null) {
            attendance = new AttendancePanel(); content.add(attendance, "Attendance");
        } else if ("CTC".equals(name) && ctc == null) {
            ctc = new CTCPanel(); content.add(ctc, "CTC");
        } else if ("Earnings & Allowances".equals(name) && earningsAndAllowances == null) {
            earningsAndAllowances = new EarningsAndAllowancesPanel(); content.add(earningsAndAllowances, "Earnings & Allowances");
        } else if ("Salary".equals(name) && salary == null) {
            salary = new SalaryPanel(); content.add(salary, "Salary");
        } else if ("Deductions".equals(name) && deductions == null) {
            deductions = new DeductionsPanel(); content.add(deductions, "Deductions");
        } else if ("Payslip".equals(name) && payroll == null) {
            payroll = new PayrollPanel(this); content.add(payroll, "Payslip");
        } else if ("Reports".equals(name) && reports == null) {
            reports = new ReportsPanel(); content.add(reports, "Reports");
        } else if ("Help".equals(name) && help == null) {
            help = new HelpPanel(this::logoutAfterCredentialRecovery); content.add(help, "Help");
        } else if ("Backup & Restore".equals(name) && backupRestore == null) {
            backupRestore = new BackupRestorePanel(); content.add(backupRestore, "Backup & Restore");
        }
    }

    private void logoutAfterCredentialRecovery() {
        Session.logout();
        dispose();
        SwingUtilities.invokeLater(Main::openLogin);
    }

    private void selectMenu(String card) {
        String menu = "CTC".equals(card) ? "Cost to Company (CTC)" : "Master Form".equals(card) ? "Master Data" : card;
        Color defaultBackground = new Color(30, 41, 59), activeBackground = new Color(71, 85, 105);
        for (JButton button : menuButtons.values()) {
            button.putClientProperty("active", Boolean.FALSE);
            button.setBackground(defaultBackground);
            button.setForeground(Color.WHITE);
            button.setOpaque(false);
            button.setContentAreaFilled(false);
        }
        JButton activeButton = menuButtons.get(menu);
        if (activeButton != null) {
            activeButton.putClientProperty("active", Boolean.TRUE);
            activeButton.setBackground(activeBackground);
            activeButton.setForeground(Color.WHITE);
            activeButton.setOpaque(false);
            activeButton.setContentAreaFilled(false);
        }
        menuButtons.values().forEach(button -> { button.revalidate(); button.repaint(); });
        if (moduleTitle != null) moduleTitle.setText(menu);
    }

    private static final class RoundedNavButton extends JButton {
        RoundedNavButton(String text) { super(text); setOpaque(false); setContentAreaFilled(false); setRolloverEnabled(true); }
        @Override protected void paintComponent(Graphics graphics) { Graphics2D g = (Graphics2D) graphics.create(); try { g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); boolean active = Boolean.TRUE.equals(getClientProperty("active")), hover = getModel().isRollover(); g.setColor(active ? new Color(71, 85, 105) : hover ? new Color(36, 60, 84) : new Color(30, 41, 59)); g.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24); if(active){g.setColor(new Color(203, 213, 225));g.fillRoundRect(0, 9, 5, Math.max(0,getHeight()-18), 5, 5);} g.setColor(Color.WHITE);g.setFont(getFont());FontMetrics metrics=g.getFontMetrics();g.drawString(getText(),20,(getHeight()-metrics.getHeight())/2+metrics.getAscent()); } finally { g.dispose(); } }
    }

    void returnToMasterData() {
        if (employeeTable != null) loadEmployees(search == null ? "" : search.getText());
        refreshEmployeeDependentModules();
        if (masterData != null) {
            masterData.refreshTable();
            showCard("Master Data");
        } else showCard("Employee");
    }

    private void refreshEmployeeDependentModules() {
        if (attendance != null) attendance.refreshEmployees();
        if (ctc != null) ctc.refresh();
        if (earningsAndAllowances != null) earningsAndAllowances.refresh();
        if (deductions != null) deductions.refresh();
        if (salary != null) salary.refresh();
        if (payroll != null) payroll.refreshEmployees();
        if (reports != null) reports.refresh();
        if (dashboard != null) dashboard.refresh();
    }

    private static void stopActiveEditing(Component component) {
        if (component instanceof JTable table && table.isEditing()) {
            TableCellEditor editor = table.getCellEditor();
            if (editor != null && !editor.stopCellEditing()) editor.cancelCellEditing();
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) stopActiveEditing(child);
        }
    }
    private static boolean hasUnsavedEdits(Component component) {
        if (component instanceof JTable table && table.isEditing()) return true;
        if (component instanceof SettingsEditGuard guard && guard.isEditing()) return true;
        if (component instanceof Container container) for (Component child : container.getComponents()) if (hasUnsavedEdits(child)) return true;
        return false;
    }
    private Component visibleModule() { for (Component child : content.getComponents()) if (child.isVisible()) return child; return content; }
}
