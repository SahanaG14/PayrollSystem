import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/** Statutory settings used by ESIC and the monthly TDS calculation. */
public class DeductionSettingsPanel extends JPanel {
 private final Map<String,JTextField> fields=new LinkedHashMap<>();
 private final Map<String,JComboBox<String>> calculationTypes=new LinkedHashMap<>();
 private final DefaultTableModel slabs,surchargeSlabs;
 private JButton addSlabButton, removeSlabButton, saveButton;

 public DeductionSettingsPanel(){
  setLayout(new BorderLayout(10,10));
  setBorder(BorderFactory.createEmptyBorder(12,18,12,18));
  JPanel body=new JPanel();body.setLayout(new BoxLayout(body,BoxLayout.Y_AXIS));final JScrollPane mainScrollPane=new JScrollPane(body,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);mainScrollPane.setWheelScrollingEnabled(true);mainScrollPane.getVerticalScrollBar().setUnitIncrement(20);mainScrollPane.getVerticalScrollBar().setBlockIncrement(60);

  JPanel configuration=new JPanel(new GridLayout(1,2,32,0));
  configuration.setAlignmentX(Component.LEFT_ALIGNMENT);
  configuration.setMaximumSize(new Dimension(Integer.MAX_VALUE,350));

  JPanel epfPt=new JPanel();epfPt.setLayout(new BoxLayout(epfPt,BoxLayout.Y_AXIS));
  epfPt.setBorder(BorderFactory.createEmptyBorder(0,0,0,12));
  epfPt.add(section("EPF & PT Configuration"));
 addDeductionField(epfPt,"Employee Provident Fund Contribution","EPF",PayrollRulesStore.epfRate());
 addDeductionField(epfPt,"Employer Provident Fund Contribution","Employer EPF",12.0);
 addDeductionField(epfPt,"Professional Tax (PT)","PT",PayrollRulesStore.ptAmount());
  addDeductionField(epfPt,"Employees’ Pension Scheme (EPS)","EPS",8.33);
  addDerivedDifference(epfPt);
  bindEpsDifference();

  JPanel statutory=new JPanel();statutory.setLayout(new BoxLayout(statutory,BoxLayout.Y_AXIS));
  statutory.setBorder(BorderFactory.createEmptyBorder(0,12,0,0));
  statutory.add(section("ESIC Configuration"));
  addField(statutory,"ESIC Wage Threshold Limit","esicCeiling");
  addField(statutory,"ESIC Employee Contribution Rate (% of gross wages)","esicRate");
  addField(statutory,"ESIC Employer Contribution Rate (% of gross wages)","esicEmployerRate");
  statutory.add(Box.createVerticalStrut(10));
  statutory.add(section("TDS / Income Tax Configuration"));
  addField(statutory,"Standard Deduction Amount","standardDeduction");
  addField(statutory,"Section 87A Rebate Threshold Limit","rebate87aLimit");
  addField(statutory,"Health & Education Cess Rate (%)","cessRate");

  configuration.add(epfPt);configuration.add(statutory);body.add(configuration);
  body.add(Box.createVerticalStrut(8));
  body.add(section("Editable Tax Slabs"));
  slabs=new DefaultTableModel(new String[]{"Slab Name","From Amount","To Amount","Tax Rate (%)"},0);
  for(TaxSlabStore.Slab s:TaxSlabStore.list())slabs.addRow(new Object[]{s.name(),Money.text(s.from()),s.to()==Double.MAX_VALUE?"999999999":Money.text(s.to()),Money.text(s.rate())});
  JTable grid=new JTable(slabs);grid.setRowHeight(28);grid.setFillsViewportHeight(true);final boolean[] slabSync={false};slabs.addTableModelListener(e->{if(slabSync[0]||e.getType()!=javax.swing.event.TableModelEvent.UPDATE||e.getFirstRow()<0)return;int row=e.getFirstRow(),column=e.getColumn();try{slabSync[0]=true;if(column==0){double[] range=TaxSlabStore.parseRangeName(String.valueOf(slabs.getValueAt(row,0)));slabs.setValueAt(Money.text(range[0]),row,1);slabs.setValueAt(Money.text(range[1]),row,2);}else if(column==1||column==2){double from=TaxSlabStore.parseAmount(String.valueOf(slabs.getValueAt(row,1))),to=TaxSlabStore.parseAmount(String.valueOf(slabs.getValueAt(row,2)));slabs.setValueAt(TaxSlabStore.displayName(from,to),row,0);}}catch(IllegalArgumentException ignored){}finally{slabSync[0]=false;}});DefaultCellEditor selectAllEditor=new DefaultCellEditor(new JTextField()){public Component getTableCellEditorComponent(JTable table,Object value,boolean selected,int row,int column){JTextField field=(JTextField)super.getTableCellEditorComponent(table,value,selected,row,column);field.selectAll();return field;}};for(int col=0;col<grid.getColumnCount();col++)grid.getColumnModel().getColumn(col).setCellEditor(selectAllEditor);
  JScrollPane tableArea=new JScrollPane(grid);tableArea.setBorder(BorderFactory.createLineBorder(new Color(165,165,165)));tableArea.setPreferredSize(new Dimension(780,240));tableArea.setAlignmentX(Component.LEFT_ALIGNMENT);
  body.add(tableArea);
  body.add(Box.createVerticalStrut(16));
  JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,8,8));actions.setAlignmentX(Component.LEFT_ALIGNMENT);actions.setBorder(BorderFactory.createEmptyBorder(8,0,7,0));
  addSlabButton=new JButton("+ Add Tax Slab");addSlabButton.addActionListener(e->slabs.addRow(new Object[]{"New Slab","0.00","0.00","0.00"}));
  removeSlabButton=new JButton("Remove Selected Slab");removeSlabButton.addActionListener(e->{int r=grid.getSelectedRow();if(r>=0)slabs.removeRow(r);});
  actions.add(addSlabButton);actions.add(removeSlabButton);body.add(actions);
  body.add(section("Editable Surcharge Slabs"));surchargeSlabs=new DefaultTableModel(new String[]{"Slab Name","From Amount","To Amount","Surcharge Rate (%)"},0);for(SurchargeSlabStore.Slab s:SurchargeSlabStore.list())surchargeSlabs.addRow(new Object[]{s.name(),Money.text(s.from()),Money.text(s.to()),Money.text(s.rate())});JTable surchargeGrid=new JTable(surchargeSlabs);surchargeGrid.setRowHeight(28);surchargeGrid.setFillsViewportHeight(true);for(int col=0;col<surchargeGrid.getColumnCount();col++)surchargeGrid.getColumnModel().getColumn(col).setCellEditor(selectAllEditor);JScrollPane surchargeScroll=new JScrollPane(surchargeGrid);surchargeScroll.setPreferredSize(new Dimension(780,180));surchargeScroll.setAlignmentX(Component.LEFT_ALIGNMENT);body.add(surchargeScroll);JPanel surchargeActions=new JPanel(new FlowLayout(FlowLayout.LEFT,8,8));surchargeActions.setAlignmentX(Component.LEFT_ALIGNMENT);JButton addSurcharge=new JButton("+ Add Surcharge Slab"),removeSurcharge=new JButton("Remove Selected Surcharge Slab");addSurcharge.addActionListener(e->surchargeSlabs.addRow(new Object[]{"New Surcharge","0.00","0.00","0.00"}));removeSurcharge.addActionListener(e->{int r=surchargeGrid.getSelectedRow();if(r>=0)surchargeSlabs.removeRow(r);});surchargeActions.add(addSurcharge);surchargeActions.add(removeSurcharge);body.add(surchargeActions);JPanel saveActions=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,12));saveActions.setAlignmentX(Component.LEFT_ALIGNMENT);saveButton=new JButton("Save Deduction Rules");saveButton.addActionListener(e->save());saveActions.add(saveButton);body.add(saveActions);
  MouseWheelListener forwardScrollListener=e->mainScrollPane.getVerticalScrollBar().dispatchEvent(new MouseWheelEvent(mainScrollPane.getVerticalScrollBar(),e.getID(),e.getWhen(),e.getModifiersEx(),e.getX(),e.getY(),e.getClickCount(),e.isPopupTrigger(),e.getScrollType(),e.getScrollAmount(),e.getWheelRotation()));grid.addMouseWheelListener(forwardScrollListener);tableArea.getViewport().addMouseWheelListener(forwardScrollListener);surchargeGrid.addMouseWheelListener(forwardScrollListener);surchargeScroll.getViewport().addMouseWheelListener(forwardScrollListener);
  bindShortcuts(grid);
  add(mainScrollPane,BorderLayout.CENTER);
 }

 private JLabel section(String text){JLabel l=new JLabel(text);l.setFont(l.getFont().deriveFont(Font.BOLD,17f));l.setAlignmentX(Component.LEFT_ALIGNMENT);return l;}
 private void addField(JPanel p,String label,String key){
  JPanel row=new JPanel(new GridBagLayout());row.setAlignmentX(Component.LEFT_ALIGNMENT);row.setMaximumSize(new Dimension(Integer.MAX_VALUE,38));
  GridBagConstraints c=new GridBagConstraints();c.gridy=0;c.insets=new Insets(4,0,4,8);c.anchor=GridBagConstraints.WEST;
  c.gridx=0;JLabel l=new JLabel(label);l.setPreferredSize(new Dimension(310,26));row.add(l,c);
  c.gridx=1;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(4,0,4,0);
  JTextField f=new JTextField(Money.text(PayrollRulesStore.get(key)));f.setPreferredSize(new Dimension(240,28));installUndoRedo(f);fields.put(key,f);row.add(f,c);p.add(row);
 }
 private void addDeductionField(JPanel p,String label,String name,double fallback){
  JPanel row=new JPanel(new GridBagLayout());row.setAlignmentX(Component.LEFT_ALIGNMENT);row.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
  GridBagConstraints c=new GridBagConstraints();c.gridy=0;c.insets=new Insets(4,0,4,8);c.anchor=GridBagConstraints.WEST;
  c.gridx=0;JLabel l=new JLabel(label);l.setPreferredSize(new Dimension(185,26));row.add(l,c);
  double configured=CompanyPolicyStore.deduction(name);JTextField f=new JTextField(Money.text(CompanyPolicyStore.hasDeduction(name)?configured:fallback));f.setPreferredSize(new Dimension(190,28));installUndoRedo(f);fields.put("deduction."+name,f);
  c.gridx=1;row.add(f,c);
  JComboBox<String> type=new JComboBox<>(new String[]{"Fixed (\u20B9)","% of Basic Pay"});type.setPreferredSize(new Dimension(145,28));type.setSelectedIndex(CompanyPolicyStore.hasDeduction(name)?(CompanyPolicyStore.deductionPercentage(name)?1:0):("EPS".equals(name)?1:0));calculationTypes.put(name,type);
  c.gridx=2;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(4,0,4,0);row.add(type,c);p.add(row);
 }
 private void addDerivedDifference(JPanel p){JPanel row=new JPanel(new GridBagLayout());row.setAlignmentX(Component.LEFT_ALIGNMENT);row.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));GridBagConstraints c=new GridBagConstraints();c.gridy=0;c.insets=new Insets(4,0,4,8);c.anchor=GridBagConstraints.WEST;c.gridx=0;JLabel l=new JLabel("EPF–EPS Difference");l.setPreferredSize(new Dimension(185,26));row.add(l,c);JTextField f=new JTextField();f.setEditable(false);f.setPreferredSize(new Dimension(190,28));fields.put("derived.epfEpsDifference",f);c.gridx=1;row.add(f,c);JComboBox<String> type=new JComboBox<>(new String[]{"% of Basic Pay"});type.setEnabled(false);type.setPreferredSize(new Dimension(145,28));c.gridx=2;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(4,0,4,0);row.add(type,c);p.add(row);updateEpsDifference();}
 private void bindEpsDifference(){javax.swing.event.DocumentListener listener=new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){updateEpsDifference();}public void removeUpdate(javax.swing.event.DocumentEvent e){updateEpsDifference();}public void changedUpdate(javax.swing.event.DocumentEvent e){updateEpsDifference();}};fields.get("deduction.EPF").getDocument().addDocumentListener(listener);fields.get("deduction.EPS").getDocument().addDocumentListener(listener);calculationTypes.get("EPF").addActionListener(e->updateEpsDifference());calculationTypes.get("EPS").addActionListener(e->updateEpsDifference());}
 private void updateEpsDifference(){try{double epf=Double.parseDouble(fields.get("deduction.EPF").getText().trim()),eps=Double.parseDouble(fields.get("deduction.EPS").getText().trim());fields.get("derived.epfEpsDifference").setText(Money.text(calculationTypes.get("EPF").getSelectedIndex()==1&&calculationTypes.get("EPS").getSelectedIndex()==1?Math.max(0,epf-eps):0));}catch(Exception ignored){fields.get("derived.epfEpsDifference").setText("0.00");}}
 private void installUndoRedo(JTextField field){UndoManager manager=new UndoManager();field.getDocument().addUndoableEditListener(e->manager.addEdit(e.getEdit()));field.getInputMap().put(KeyStroke.getKeyStroke("control Z"),"undo");field.getActionMap().put("undo",new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){if(manager.canUndo())manager.undo();}});field.getInputMap().put(KeyStroke.getKeyStroke("control Y"),"redo");field.getActionMap().put("redo",new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){if(manager.canRedo())manager.redo();}});}
 private void bindShortcuts(JTable grid){InputMap input=getInputMap(WHEN_IN_FOCUSED_WINDOW);ActionMap actions=getActionMap();input.put(KeyStroke.getKeyStroke("control S"),"saveDeductionRules");actions.put("saveDeductionRules",new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){saveButton.doClick();}});input.put(KeyStroke.getKeyStroke("control N"),"addTaxSlab");actions.put("addTaxSlab",new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){addSlabButton.doClick();}});grid.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DELETE"),"removeTaxSlab");grid.getActionMap().put("removeTaxSlab",new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){if(grid.getSelectedRow()>=0)removeSlabButton.doClick();}});}
 private void save(){try{
  Map<String,Double> values=PayrollRulesStore.all();
  for(Map.Entry<String,JTextField> e:fields.entrySet())if(!e.getKey().startsWith("deduction."))values.put(e.getKey(),Double.parseDouble(e.getValue().getText().trim()));
  PayrollRulesStore.save(values);
  for(String name:calculationTypes.keySet()){double amount=Double.parseDouble(fields.get("deduction."+name).getText().trim());if(amount<0)throw new IllegalArgumentException();CompanyPolicyStore.deduction(name,amount);CompanyPolicyStore.deductionPercentage(name,calculationTypes.get(name).getSelectedIndex()==1);}
  java.util.List<TaxSlabStore.Slab> list=new ArrayList<>();
  for(int r=0;r<slabs.getRowCount();r++){String name=String.valueOf(slabs.getValueAt(r,0)).trim();double[] range=TaxSlabStore.parseRangeName(name);double from=TaxSlabStore.parseAmount(String.valueOf(slabs.getValueAt(r,1))),to=TaxSlabStore.parseAmount(String.valueOf(slabs.getValueAt(r,2))),rate=Double.parseDouble(String.valueOf(slabs.getValueAt(r,3)).replace("%","").trim());list.add(new TaxSlabStore.Slab(name,from,to,rate));}
  TaxSlabStore.save(list);java.util.List<SurchargeSlabStore.Slab> surcharge=new ArrayList<>();for(int r=0;r<surchargeSlabs.getRowCount();r++)surcharge.add(new SurchargeSlabStore.Slab(String.valueOf(surchargeSlabs.getValueAt(r,0)).trim(),Double.parseDouble(String.valueOf(surchargeSlabs.getValueAt(r,1)).trim()),Double.parseDouble(String.valueOf(surchargeSlabs.getValueAt(r,2)).trim()),Double.parseDouble(String.valueOf(surchargeSlabs.getValueAt(r,3)).trim())));SurchargeSlabStore.save(surcharge);PayrollEvents.attendanceSaved();AutoSaveService.markDirty();JOptionPane.showMessageDialog(this,"Deduction rules saved. New deduction values apply to payroll calculations immediately.");
 }catch(Exception ex){JOptionPane.showMessageDialog(this,"Please enter valid non-negative values for all deduction rules and tax slabs.");}}
}
