import javax.swing.*;import java.awt.*;import java.util.prefs.*;

/** Company-wide increment rule used by the CTC Salary Revision tab. */
public class RevisedSalarySettingsPanel extends JPanel {
    private final Preferences prefs=Preferences.userNodeForPackage(RevisedSalarySettingsPanel.class);
    private final JTextField amount=new JTextField(12); private final JComboBox<String> type=new JComboBox<>(new String[]{"Percentage Increment of Basic Salary","Fixed Increment (Rs.)"});
    public RevisedSalarySettingsPanel(){setLayout(new GridBagLayout());GridBagConstraints c=new GridBagConstraints();c.insets=new Insets(9,9,9,9);c.anchor=GridBagConstraints.WEST;c.gridx=0;c.gridy=0;add(new JLabel("Revision Rule:"),c);c.gridx=1;add(type,c);c.gridy++;c.gridx=0;add(new JLabel("Increment Value:"),c);c.gridx=1;add(amount,c);c.gridy++;c.gridx=1;JButton save=new JButton("Save Revised Salary Settings");save.addActionListener(e->save());add(save,c);amount.setText(String.format("%.2f",prefs.getDouble("revisionValue",0)));type.setSelectedIndex(prefs.getBoolean("revisionPercent",true)?0:1);}
    private void save(){try{double v=Double.parseDouble(amount.getText().trim());if(v<0)throw new NumberFormatException();prefs.putDouble("revisionValue",v);prefs.putBoolean("revisionPercent",type.getSelectedIndex()==0);JOptionPane.showMessageDialog(this,"Revised salary rule saved.");}catch(Exception e){JOptionPane.showMessageDialog(this,"Enter a valid non-negative revision value.");}}
    public static double revised(double basic){Preferences p=Preferences.userNodeForPackage(RevisedSalarySettingsPanel.class);double value=p.getDouble("revisionValue",0);return Money.round(p.getBoolean("revisionPercent",true)?basic*(1+value/100):basic+value);}
}
