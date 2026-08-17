import java.util.prefs.Preferences;

/** Accountant-entered adjustments for the editable monthly Annual Salary view. */
public final class AnnualSalaryOverrideStore {
  private static final Preferences P=Preferences.userNodeForPackage(AnnualSalaryOverrideStore.class);
  private AnnualSalaryOverrideStore(){}
  private static String key(String id,String month,int column){return "annual.override."+id+"."+month+"."+column;}
  public static double value(String id,String month,int column,double fallback){String raw=P.get(key(id,month,column),null);if(raw==null)return fallback;try{return Double.parseDouble(raw);}catch(Exception ignored){return fallback;}}
  public static void save(String id,String month,int column,double value){P.put(key(id,month,column),String.valueOf(Money.round(value)));}
  public static void purgeEmployee(String id){try{for(String key:P.keys())if(key.startsWith("annual.override."+id+"."))P.remove(key);}catch(Exception ignored){}}
}
