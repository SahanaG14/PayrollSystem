import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.sql.*;
import java.util.Arrays;
import java.util.Base64;

public final class UserAuthentication {
    private static final int ITERATIONS=120_000, KEY_BITS=256;
    public static boolean isPasswordCompliant(char[] password){String value=new String(password);return value.length()>=8&&value.length()<=16&&value.matches(".*[A-Z].*")&&value.matches(".*[a-z].*")&&value.matches(".*\\d.*")&&value.matches(".*[^A-Za-z0-9].*");}
    public static boolean verify(String username,char[] password){try(Connection c=DBConnection.getConnection()){if(c==null)return false;ensureTable(c);try(PreparedStatement p=c.prepareStatement("SELECT password_hash FROM payroll_users WHERE username=?")){p.setString(1,username);ResultSet r=p.executeQuery();return r.next()&&matches(password,r.getString(1));}}catch(SQLException ignored){return false;}finally{Arrays.fill(password,'\0');}}
    public static boolean login(String username,char[] password){if(!verify(username,password))return false;Session.currentUser=username;Session.role="Admin";return true;}
    public static boolean updateCredentials(String currentUsername,String newUsername,char[] newPassword){try(Connection c=DBConnection.getConnection()){if(c==null)return false;ensureTable(c);String target=newUsername==null||newUsername.isBlank()?currentUsername:newUsername.trim();try(PreparedStatement p=c.prepareStatement("INSERT INTO payroll_users(username,password_hash,role_name) VALUES(?,?,?) ON CONFLICT(username) DO UPDATE SET password_hash=excluded.password_hash,role_name=excluded.role_name")){p.setString(1,target);p.setString(2,encode(newPassword));p.setString(3,"Admin");p.executeUpdate();}Session.currentUser=target;Session.role="Admin";return true;}catch(Exception e){return false;}finally{Arrays.fill(newPassword,'\0');}}
    private static void ensureTable(Connection c)throws SQLException{try(Statement s=c.createStatement()){s.executeUpdate("CREATE TABLE IF NOT EXISTS payroll_users (username TEXT PRIMARY KEY,password_hash TEXT NOT NULL,role_name TEXT NOT NULL)");}}
    private static boolean matches(char[] value,String stored){try{if(stored!=null&&stored.startsWith("pbkdf2$")){String[] parts=stored.split("\\$");if(parts.length!=4)return false;byte[] actual=derive(value,Base64.getDecoder().decode(parts[2]),Integer.parseInt(parts[1]));return MessageDigest.isEqual(actual,Base64.getDecoder().decode(parts[3]));}return MessageDigest.isEqual(legacy(new String(value)).getBytes(StandardCharsets.UTF_8),String.valueOf(stored).getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){return false;}}
    private static String encode(char[] value)throws Exception{byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);return "pbkdf2$"+ITERATIONS+"$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(derive(value,salt,ITERATIONS));}
    private static byte[] derive(char[] value,byte[] salt,int iterations)throws Exception{PBEKeySpec spec=new PBEKeySpec(value,salt,iterations,KEY_BITS);try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}finally{spec.clearPassword();}}
    private static String legacy(String value)throws Exception{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format("%02x",b));return out.toString();}
    private UserAuthentication(){}
}
