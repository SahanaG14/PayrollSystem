import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.sql.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.prefs.Preferences;

/** Stores the Master Data password hash only; it never gates application launch. */
public final class SecurityService {
    private static final Preferences PREFS=Preferences.userNodeForPackage(SecurityService.class); private static final String KEY="master_password_hash", PROMPT_KEY="master_password_setup_prompted"; private static final int ITERATIONS=600_000, KEY_BITS=256;
    private SecurityService(){}
    public static boolean hasMasterPassword(){return readHash()!=null;}
    public static void showInitialSetupPrompt(Component parent){if(hasMasterPassword()||PREFS.getBoolean(PROMPT_KEY,false))return;SwingUtilities.invokeLater(()->{Object[] options={"Set Master Data Password","Later"};int choice=JOptionPane.showOptionDialog(parent,"Master Data is protected. Set its password now, or continue and configure it later in Settings.","Master Data Password Setup",JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE,null,options,options[0]);PREFS.putBoolean(PROMPT_KEY,true);if(choice==0)showSetPasswordDialog(parent);});}
    public static void offerMasterDataSetup(Component parent){Object[] options={"Set Master Data Password Now","Cancel / Later"};int choice=JOptionPane.showOptionDialog(parent,"Master Data is protected. A Master Data password must be set before it can be opened.","Master Data Protected",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,options,options[0]);if(choice==0)showSetPasswordDialog(parent);}
    public static boolean showSetPasswordDialog(Component parent){JPasswordField password=new JPasswordField(18),confirm=new JPasswordField(18);JPanel panel=new JPanel(new GridLayout(2,2,8,8));panel.add(new JLabel("Password"));panel.add(password);panel.add(new JLabel("Confirm Password"));panel.add(confirm);int choice=JOptionPane.showConfirmDialog(parent,panel,"Set Master Data Password",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);char[] first=password.getPassword(),second=confirm.getPassword();try{if(choice!=JOptionPane.OK_OPTION)return false;if(!UserAuthentication.isPasswordCompliant(first)){JOptionPane.showMessageDialog(parent,"Use 12+ characters with upper/lower case, a number, and a symbol.");return showSetPasswordDialog(parent);}if(!Arrays.equals(first,second)){JOptionPane.showMessageDialog(parent,"Passwords do not match.");return showSetPasswordDialog(parent);}boolean saved=setMasterPassword(first);JOptionPane.showMessageDialog(parent,saved?"Master Data password has been set.":"Could not save the Master Data password.");return saved;}finally{Arrays.fill(first,'\0');Arrays.fill(second,'\0');}}
    public static boolean promptForPassword(String title){JPasswordField input=new JPasswordField();int result=JOptionPane.showConfirmDialog(null,input,title,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);boolean allowed=result==JOptionPane.OK_OPTION&&verifyMasterPassword(input.getPassword());ActivityLogger.log("Security",allowed?"MASTER DATA LOGIN":"MASTER DATA LOGIN FAILED",title,allowed?"LOGIN":"SECURITY WARNING");return allowed;}
    public static boolean changePassword(char[] current,char[] next){return verifyMasterPassword(current)&&next.length>=4&&setMasterPassword(next);}
    public static boolean verifyMasterPassword(char[] password){String stored=readHash();try{return stored!=null&&matches(password,stored);}finally{Arrays.fill(password,'\0');}}
    public static boolean setMasterPassword(char[] password){if(password==null||!UserAuthentication.isPasswordCompliant(password))return false;String hash=hash(password);try(Connection c=DBConnection.getConnection()){if(c!=null){ensureTable(c);try(PreparedStatement p=c.prepareStatement("INSERT INTO application_security(setting_id,password_hash) VALUES(1,?) ON CONFLICT(setting_id) DO UPDATE SET password_hash=excluded.password_hash")){p.setString(1,hash);p.executeUpdate();}}}catch(SQLException e){return false;}finally{Arrays.fill(password,'\0');}PREFS.put(KEY,hash);PREFS.putBoolean(PROMPT_KEY,true);return true;}
    private static String hash(char[] value){try{byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);return "pbkdf2$"+ITERATIONS+"$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(derive(value,salt,ITERATIONS));}catch(Exception e){throw new IllegalStateException(e);}}
    private static boolean matches(char[] value,String stored){try{if(!stored.startsWith("pbkdf2$"))return false;String[] p=stored.split("\\$");if(p.length!=4)return false;return MessageDigest.isEqual(derive(value,Base64.getDecoder().decode(p[2]),Integer.parseInt(p[1])),Base64.getDecoder().decode(p[3]));}catch(Exception e){return false;}}
    private static byte[] derive(char[] value,byte[] salt,int iterations)throws Exception{PBEKeySpec spec=new PBEKeySpec(value,salt,iterations,KEY_BITS);try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}finally{spec.clearPassword();}}
    private static String readHash(){try(Connection c=DBConnection.getConnection()){if(c!=null){ensureTable(c);try(Statement s=c.createStatement();ResultSet result=s.executeQuery("SELECT password_hash FROM application_security WHERE setting_id=1")){if(result.next())return result.getString(1);}}}catch(SQLException ignored){}return PREFS.get(KEY,null);}
    private static void ensureTable(Connection c)throws SQLException{try(Statement s=c.createStatement()){s.executeUpdate("CREATE TABLE IF NOT EXISTS application_security (setting_id INT PRIMARY KEY, password_hash VARCHAR(128) NOT NULL)");}}
}
