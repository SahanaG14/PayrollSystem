import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public final class BackupRestorePanel extends JPanel {
    private static final Font FORM_FONT = preferredFont(Font.PLAIN, 16);
    private static final Font FORM_LABEL_FONT = preferredFont(Font.BOLD, 16);
    private static final Dimension FORM_CONTROL_SIZE = new Dimension(360, 38);
    private final JTextField folder = new JTextField(38);
    private final JComboBox<String> interval = new JComboBox<>(new String[]{"Every 7 Days", "Every 15 Days", "Every 30 Days"});
    private final DefaultTableModel history = new DefaultTableModel(new Object[0][], new String[]{"Backup Date & Time", "File Size", "Status"}) { public boolean isCellEditable(int row, int column) { return false; } };

    public BackupRestorePanel() {
        setLayout(new BorderLayout(8, 8)); setBorder(UIStyleUtility.compactModuleBorder());
        folder.setText(BackupConfig.folder() == null ? "" : BackupConfig.folder().toString()); interval.setSelectedItem("Every " + BackupConfig.intervalDays() + " Days");
        JPanel body = new JPanel(); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS)); body.add(settingsCard()); body.add(Box.createVerticalStrut(8)); body.add(historyCard()); add(body, BorderLayout.CENTER); add(actions(), BorderLayout.SOUTH); refreshHistory();
    }

    private JComponent settingsCard() {
        JPanel card = card("Automatic Backup Settings"), form = new JPanel(new GridBagLayout());
        GridBagConstraints c = constraints();
        folder.setFont(FORM_FONT); folder.setPreferredSize(FORM_CONTROL_SIZE); folder.setMinimumSize(FORM_CONTROL_SIZE);
        interval.setFont(FORM_FONT); interval.setPreferredSize(FORM_CONTROL_SIZE); interval.setMinimumSize(FORM_CONTROL_SIZE); interval.setMaximumRowCount(3);
        interval.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focused){
                JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focused); label.setFont(FORM_FONT); label.setHorizontalAlignment(SwingConstants.LEFT); label.setVerticalAlignment(SwingConstants.CENTER); label.setBorder(BorderFactory.createEmptyBorder(0,10,0,10)); label.setPreferredSize(new Dimension(360,38)); return label;
            }
        });
        JLabel folderLabel = label("Backup Folder Path"), intervalLabel = label("Automatic Backup Interval");
        JButton browse = new JButton("Browse..."); browse.setFont(FORM_FONT); browse.setPreferredSize(new Dimension(108,38)); browse.addActionListener(e -> browseFolder());
        JButton save = new JButton("Save Configuration"); save.setFont(FORM_LABEL_FONT); save.addActionListener(e -> saveConfiguration());
        c.gridx = 0; c.gridy = 0; c.weightx = 0; form.add(folderLabel, c);
        c.gridx = 1; c.weightx = 1; form.add(folder, c);
        c.gridx = 2; c.weightx = 0; form.add(browse, c);
        c.gridx = 0; c.gridy = 1; form.add(intervalLabel, c);
        c.gridx = 1; c.gridwidth = 2; c.weightx = 1; form.add(interval, c);
        c.gridx = 1; c.gridy = 2; c.gridwidth = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.WEST; c.insets = new Insets(12,8,0,8); form.add(save, c);
        card.add(form, BorderLayout.CENTER); return card;
    }

    private JComponent historyCard() { JPanel card = card("Backup History & Manual Operations"); JTable table = new JTable(history); UIStyleUtility.applyProfessionalTableStyle(table); table.setFont(FORM_FONT); table.setRowHeight(38); card.add(new JScrollPane(table), BorderLayout.CENTER); card.setPreferredSize(new Dimension(0, 390)); return card; }
    private JPanel card(String title) { JPanel card = new JPanel(new BorderLayout(10, 10)); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)), BorderFactory.createEmptyBorder(16, 18, 18, 18))); JLabel heading = new JLabel(title); heading.setFont(preferredFont(Font.BOLD, 20)); heading.setForeground(new Color(30, 40, 60)); card.add(heading, BorderLayout.NORTH); return card; }
    private JLabel label(String text){JLabel label=new JLabel(text);label.setFont(FORM_LABEL_FONT);label.setPreferredSize(new Dimension(220,38));label.setVerticalAlignment(SwingConstants.CENTER);return label;}
    private GridBagConstraints constraints() { GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(7, 8, 7, 8); c.fill = GridBagConstraints.HORIZONTAL; c.anchor = GridBagConstraints.WEST; return c; }
    private JComponent actions() { JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0)); JButton open = new JButton("Open Database Folder"), export = new JButton("Export Database Copy"), backup = new JButton("Backup Now"), restore = new JButton("Import & Restore Backup"); for(JButton button:new JButton[]{open,export,backup,restore})button.setFont(FORM_FONT); backup.setFont(FORM_LABEL_FONT); restore.setFont(FORM_LABEL_FONT); open.addActionListener(e -> openDatabaseFolder()); export.addActionListener(e -> exportDatabaseCopy()); backup.addActionListener(e -> backupNow()); restore.addActionListener(e -> restore()); panel.add(open);panel.add(export);panel.add(backup); panel.add(restore); return panel; }
    private static Font preferredFont(int style,int size){Font candidate=new Font("Segoe UI",style,size);return "Segoe UI".equalsIgnoreCase(candidate.getFamily())?candidate:new Font(Font.SANS_SERIF,style,size);}
    private void openDatabaseFolder(){try{File folder=DBConnection.databaseFile().getParentFile();if(folder==null||!folder.isDirectory()||!Desktop.isDesktopSupported())throw new IllegalStateException();Desktop.getDesktop().open(folder);}catch(Exception ex){JOptionPane.showMessageDialog(this,"Could not open the database folder.");}}
    private void exportDatabaseCopy(){JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Export Database Copy");chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;try{Path copy=BackupService.exportDatabaseCopy(chooser.getSelectedFile().toPath());ActivityLogger.log("Backup & Restore","DATABASE EXPORTED",copy.getFileName().toString(),"SUCCESS");JOptionPane.showMessageDialog(this,"Database copy exported:\n"+copy); }catch(Exception ex){ActivityLogger.log("Backup & Restore","DATABASE EXPORT FAILED","Database copy could not be created","FAILED");JOptionPane.showMessageDialog(this,"Database export failed: "+ex.getMessage());}}
    private void browseFolder() { JFileChooser chooser = new JFileChooser(); chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) folder.setText(chooser.getSelectedFile().getAbsolutePath()); }
    private boolean saveConfiguration() { try { Path path = Path.of(folder.getText().trim()); BackupConfig.folder(path); BackupConfig.intervalDays(Integer.parseInt(((String) interval.getSelectedItem()).replaceAll("\\D+", ""))); return true; } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Choose a valid backup folder."); return false; } }
    private void backupNow() { if (!saveConfiguration()) return; try { Path backup = BackupService.backup(BackupConfig.folder());ActivityLogger.log("Backup & Restore","BACKUP CREATED",backup.getFileName().toString(),"SUCCESS"); refreshHistory(); JOptionPane.showMessageDialog(this, "Backup created:\n" + backup); } catch (Exception ex) { ActivityLogger.log("Backup & Restore","BACKUP FAILED","Backup could not be created","FAILED");JOptionPane.showMessageDialog(this, "Backup failed: " + ex.getMessage()); } }
    private void restore() { JFileChooser chooser = new JFileChooser(); chooser.setAcceptAllFileFilterUsed(false); chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Payroll Backup ZIP (*.zip)", "zip")); if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return; if (JOptionPane.showConfirmDialog(this, "Restore replaces the active database and local payroll files. Continue?", "Confirm Restore", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return; try { BackupService.restore(chooser.getSelectedFile().toPath()); ActivityLogger.log("Backup & Restore","BACKUP RESTORED",chooser.getSelectedFile().getName(),"SUCCESS");PayrollEvents.dataChanged("restore"); JOptionPane.showMessageDialog(this, "Restore completed. Restart the application."); } catch (Exception ex) { ActivityLogger.log("Backup & Restore","RESTORE FAILED","Backup restore could not be completed","FAILED");JOptionPane.showMessageDialog(this, "Restore failed: " + ex.getMessage()); } }
    private void refreshHistory() { history.setRowCount(0); Path path = BackupConfig.folder(); if (path == null || !Files.isDirectory(path)) return; try { List<Path> backups = new ArrayList<>(); try (var files = Files.list(path)) { files.filter(file -> file.getFileName().toString().startsWith("Payroll_Full_Backup_") && file.getFileName().toString().endsWith(".zip")).forEach(backups::add); } backups.sort(Comparator.reverseOrder()); for (Path backup : backups) { BasicFileAttributes attributes = Files.readAttributes(backup, BasicFileAttributes.class); history.addRow(new Object[]{new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date(attributes.lastModifiedTime().toMillis())), String.format("%.2f MB", attributes.size() / 1048576.0), "Completed"}); } } catch (Exception ignored) { } }
}
