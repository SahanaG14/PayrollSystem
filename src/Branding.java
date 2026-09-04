import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.List;

/** Central, user-facing YASL branding. */
public final class Branding {
    public static final String COMPANY_NAME="Yashasvi Accounting Solutions LLP";
    public static final String COMPANY_EMAIL="connect@yasl.co.in";
    public static final String APPLICATION_NAME="YASL Payroll";
    public static final String COMPANY_ADDRESS="1530/2, GROUND FLOOR, YASHASWI, 6TH CROSS,\n4TH BLOCK, DODDABOMASANDRA,\nVIDYARANYAPURA POST, BENGALURU - 560097";
    private static BufferedImage logo;
    private static BufferedImage applicationIcon;
    private Branding() { }
    private static synchronized BufferedImage logo(){if(logo!=null)return logo;try(InputStream input=Branding.class.getResourceAsStream("/yasl-logo.png")){if(input!=null)logo=ImageIO.read(input);if(logo==null)logo=ImageIO.read(new File("assets/yasl-logo.png"));}catch(Exception ignored){}return logo;}
    private static synchronized BufferedImage applicationIcon(){if(applicationIcon!=null)return applicationIcon;try(InputStream input=Branding.class.getResourceAsStream("/yasl-app-icon.png")){if(input!=null)applicationIcon=ImageIO.read(input);if(applicationIcon==null)applicationIcon=ImageIO.read(new File("assets/yasl-app-icon.png"));}catch(Exception ignored){}return applicationIcon;}
    public static ImageIcon logoIcon(int maximumWidth,int maximumHeight){BufferedImage source=logo();if(source==null)return new ImageIcon();double scale=Math.min((double)maximumWidth/source.getWidth(),(double)maximumHeight/source.getHeight());int width=Math.max(1,(int)Math.round(source.getWidth()*scale)),height=Math.max(1,(int)Math.round(source.getHeight()*scale));return new ImageIcon(source.getScaledInstance(width,height,Image.SCALE_SMOOTH));}
    public static void applyWindowIcon(JFrame frame){BufferedImage source=applicationIcon();if(source==null)return;frame.setIconImages(List.of(scaled(source,16),scaled(source,32),scaled(source,48),scaled(source,64),scaled(source,128),scaled(source,256)));}
    private static Image scaled(BufferedImage source,int size){return source.getScaledInstance(size,size,Image.SCALE_SMOOTH);}
}
