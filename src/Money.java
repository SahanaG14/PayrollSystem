import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.text.DecimalFormat;
import javax.swing.border.*;
import java.awt.*;

/** Consistent two-decimal display and storage helpers for all payroll amounts. */
public final class Money {
    private static final DecimalFormat FORMAT=new DecimalFormat("0.00");
    private Money() { }
    public static double round(double value){return Math.round(value*100.0)/100.0;}
    public static String text(double value){return FORMAT.format(round(value));}
    public static DefaultTableCellRenderer renderer(){return new DefaultTableCellRenderer(){public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focused,int row,int column){super.getTableCellRendererComponent(table,value instanceof Number?text(((Number)value).doubleValue()):value,selected,focused,row,column);setHorizontalAlignment(SwingConstants.RIGHT);setBorder(new CompoundBorder(new MatteBorder(0,0,1,1,new Color(180,180,180)),BorderFactory.createEmptyBorder(0,8,0,12)));return this;}};}
}
