import java.util.*;
public class PayrollSettings {
    public static class Component { public boolean enabled; public double value; public boolean percentage; Component(boolean e,double v,boolean p){enabled=e;value=v;percentage=p;} }
    private final LinkedHashMap<String,Component> components=new LinkedHashMap<>();
    public PayrollSettings(){ defaults(); }
    public void defaults(){components.clear(); add("HRA",20,true);add("DA",12,true);add("Medical Allowance",0,false);add("Travel Allowance",0,false);add("Special Allowance",0,false);add("Bonus",5,true);add("Attendance Bonus",0,false);add("Overtime Rate",0,false);add("PF (Employee)",12,true);add("Employer PF",12,true);add("ESI (Employee)",0.75,true);add("Employer ESI",3.25,true);add("Professional Tax",0,false);add("Income Tax",0,true);add("Gratuity",0,false);add("Insurance",0,false);add("Leave Salary",0,false);add("Loan",0,false);add("Advance Salary",0,false);add("Other Deduction",0,false);}
    private void add(String n,double v,boolean p){components.put(n,new Component(v>0,v,p));}
    public LinkedHashMap<String,Component> getComponents(){return components;}
}
