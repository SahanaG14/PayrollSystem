import java.time.*;
import java.util.*;

/** Shared Indian financial-year calendar: April through March. */
public final class FinancialYear {
    private FinancialYear() {}
    public static int currentStart() { int y=Year.now().getValue(); return LocalDate.now().getMonthValue()>=4?y:y-1; }
    public static String text(int start) { return start+"-"+String.format("%02d",(start+1)%100); }
    public static int parse(String value) { try { return Integer.parseInt(value.substring(0,4)); } catch(Exception e) { return currentStart(); } }
    public static YearMonth month(int start,int index) { if(index<0||index>11)throw new IllegalArgumentException("Financial-year month index must be 0 to 11"); return YearMonth.of(index<9?start:start+1,index+4>12?index-8:index+4); }
    public static List<YearMonth> months(int start) { List<YearMonth> out=new ArrayList<>();for(int i=0;i<12;i++)out.add(month(start,i));return out; }
    public static String shortName(YearMonth p) { return p.getMonth().toString().substring(0,3); }
    public static javax.swing.JComboBox<String> selector(int selected) {
        javax.swing.JComboBox<String> box=new javax.swing.JComboBox<>();
        int current=currentStart(),active=selected<2022||selected>current?current:selected;
        for(int y=2022;y<=current;y++) box.addItem(text(y));
        box.setSelectedItem(text(active)); return box;
    }
}
