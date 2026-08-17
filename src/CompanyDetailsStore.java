import java.sql.*;
public final class CompanyDetailsStore {
 private static String name="",address="",logo="",pan="",tan="",prefix="";private static boolean loaded;
 private static synchronized void load(){if(loaded)return;loaded=true;try(Connection c=DBConnection.getConnection();PreparedStatement p=c==null?null:c.prepareStatement("SELECT company_name,company_address,company_pan,company_tan,employee_id_prefix,company_logo_path FROM settings_company_details ORDER BY id LIMIT 1")){if(p==null)return;ResultSet r=p.executeQuery();if(r.next()){name=s(r.getString(1));address=s(r.getString(2));pan=s(r.getString(3));tan=s(r.getString(4));prefix=s(r.getString(5));logo=s(r.getString(6));}}catch(SQLException ignored){}}
 private static String s(String v){return v==null?"":v;}
 private static synchronized void save(){try(Connection c=DBConnection.getConnection()){if(c==null)return;c.setAutoCommit(false);try(PreparedStatement u=c.prepareStatement("UPDATE settings_company_details SET company_name=?,company_address=?,company_pan=?,company_tan=?,employee_id_prefix=?,company_logo_path=? WHERE id=(SELECT id FROM settings_company_details WHERE is_deleted=0 ORDER BY id LIMIT 1)")){bind(u);if(u.executeUpdate()==0)try(PreparedStatement i=c.prepareStatement("INSERT INTO settings_company_details(company_name,company_address,company_pan,company_tan,employee_id_prefix,company_logo_path,is_deleted) VALUES(?,?,?,?,?,?,0)")){bind(i);i.executeUpdate();}}c.commit();}catch(SQLException ignored){}}private static void bind(PreparedStatement p)throws SQLException{p.setString(1,name);p.setString(2,address);p.setString(3,pan);p.setString(4,tan);p.setString(5,prefix);p.setString(6,logo);}
 public static String name(){load();return name;}public static void name(String v){load();name=s(v);save();}
 public static String address(){load();return address;}public static void address(String v){load();address=s(v);save();}
 public static String logoPath(){load();return logo;}public static void logoPath(String v){load();logo=s(v);save();}
 public static String pan(){load();return pan;}public static void pan(String v){load();pan=s(v);save();}
 public static String tan(){load();return tan;}public static void tan(String v){load();tan=s(v);save();}
 public static String employeeIdPrefix(){load();return prefix;}public static void employeeIdPrefix(String v){load();prefix=s(v).trim();save();}
 private CompanyDetailsStore(){}
}
