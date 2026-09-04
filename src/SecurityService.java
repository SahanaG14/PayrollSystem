import javax.swing.*;
import java.awt.*;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.sql.*;
import java.util.prefs.Preferences;

/** Stores the Master Data password as a PBKDF2 hash only. */
public final class SecurityService {
    private static final Preferences PREFS=Preferences.userNodeForPackage(SecurityService.class); private static final String KEY="master_password_hash", PROMPT_KEY="master_password_setup_prompted"; private static final int ITERATIONS=600_000, KEY_BITS=256;
    private SecurityService(){}
    public static boolean hasMasterPassword(){return readHash()!=null;}
    /** Existing installs may be prompted; fresh installs collect this in Create Account. */
    public static void showInitialSetupPrompt(Component parent){if(hasMasterPassword()||PREFS.getBoolean(PROMPT_KEY,false))return;SwingUtilities.invokeLater(()->showSetPasswordDialog(parent));}
    public static void offerMasterDataSetup(Component parent){showSetPasswordDialog(parent);}
    public static boolean showSetPasswordDialog(Component parent){JPasswordField password=new JPasswordField(18),confirm=new JPasswordField(18);JPanel panel=new JPanel(new GridLayout(2,2,8,8));panel.add(new JLabel("Password"));panel.add(password);panel.add(new JLabel("Confirm Password"));panel.add(confirm);int choice=JOptionPane.showConfirmDialog(parent,panel,"Set Master Data Password",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);char[] first=password.getPassword(),second=confirm.getPassword();try{if(choice!=JOptionPane.OK_OPTION)return false;if(!isMasterPasswordCompliant(first)){JOptionPane.showMessageDialog(parent,"Master Data password must be exactly 6 characters.");return false;}if(!Arrays.equals(first,second)){JOptionPane.showMessageDialog(parent,"Passwords do not match.");return false;}boolean saved=setMasterPassword(first);JOptionPane.showMessageDialog(parent,saved?"Master Data password has been set.":"Could not save the Master Data password.");return saved;}finally{Arrays.fill(first,'\0');Arrays.fill(second,'\0');}}
    public static boolean promptForPassword(String title){JPasswordField input=new JPasswordField();int result=JOptionPane.showConfirmDialog(null,input,title,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);boolean allowed=result==JOptionPane.OK_OPTION&&verifyMasterPassword(input.getPassword());ActivityLogger.log("Security",allowed?"MASTER DATA LOGIN":"MASTER DATA LOGIN FAILED",title,allowed?"LOGIN":"SECURITY WARNING");return allowed;}
    public static boolean changePassword(char[] current,char[] next){return verifyMasterPassword(current)&&isMasterPasswordCompliant(next)&&setMasterPassword(next);}
    public static boolean verifyMasterPassword(char[] password){String stored=readHash();try{return stored!=null&&matches(password,stored);}finally{Arrays.fill(password,'\0');}}
    public static boolean setMasterPassword(char[] password){if(!isMasterPasswordCompliant(password)){Arrays.fill(password,'\0');return false;}String hash;try{hash=hashForStorage(password);}catch(Exception e){Arrays.fill(password,'\0');return false;}try(Connection c=DBConnection.getConnection()){if(c==null)return false;ensureTable(c);try(PreparedStatement p=c.prepareStatement("INSERT INTO application_security(setting_id,password_hash) VALUES(1,?) ON CONFLICT(setting_id) DO UPDATE SET password_hash=excluded.password_hash")){p.setString(1,hash);p.executeUpdate();}}catch(Exception e){return false;}finally{Arrays.fill(password,'\0');}PREFS.put(KEY,hash);markConfigured();return true;}
    public static boolean isMasterPasswordCompliant(char[] password){return password!=null&&password.length==6;}
    static String hashForStorage(char[] value)throws Exception{byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);return "pbkdf2$"+ITERATIONS+"$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(derive(value,salt,ITERATIONS));}
    static void ensureTable(Connection c)throws SQLException{try(Statement s=c.createStatement()){s.executeUpdate("CREATE TABLE IF NOT EXISTS application_security (setting_id INT PRIMARY KEY, password_hash VARCHAR(128) NOT NULL)");}}
    static void markConfigured(){PREFS.putBoolean(PROMPT_KEY,true);}
    static boolean resetForRecoveredInstallation(Connection c)throws Exception{ensureTable(c);try(PreparedStatement p=c.prepareStatement("INSERT INTO application_security(setting_id,password_hash) VALUES(1,?) ON CONFLICT(setting_id) DO UPDATE SET password_hash=excluded.password_hash")){p.setString(1,hashForStorage("123456".toCharArray()));p.executeUpdate();}return true;}
    private static boolean matches(char[] value,String stored){try{if(!stored.startsWith("pbkdf2$"))return false;String[] p=stored.split("\\$",-1);if(p.length!=4)return false;return MessageDigest.isEqual(derive(value,Base64.getDecoder().decode(p[2]),Integer.parseInt(p[1])),Base64.getDecoder().decode(p[3]));}catch(Exception e){return false;}}
    private static byte[] derive(char[] value,byte[] salt,int iterations)throws Exception{PBEKeySpec spec=new PBEKeySpec(value,salt,iterations,KEY_BITS);try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}finally{spec.clearPassword();}}
    private static String readHash(){try(Connection c=DBConnection.getConnection()){if(c!=null){ensureTable(c);try(Statement s=c.createStatement();ResultSet result=s.executeQuery("SELECT password_hash FROM application_security WHERE setting_id=1")){if(result.next())return result.getString(1);}}}catch(SQLException ignored){}return PREFS.get(KEY,null);}
}
