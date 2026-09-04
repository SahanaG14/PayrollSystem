import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Central, user-facing YASL branding. */
public final class Branding {
    public static final String COMPANY_NAME="Yashasvi Accounting Solutions LLP";
    public static final String COMPANY_EMAIL="connect@yasl.co.in";
    public static final String APPLICATION_NAME="YASL Payroll";
    public static final String COMPANY_ADDRESS="1530/2, GROUND FLOOR, YASHASWI, 6TH CROSS,\n4TH BLOCK, DODDABOMASANDRA,\nVIDYARANYAPURA POST, BENGALURU - 560097";
    private static BufferedImage logo;
    private static BufferedImage applicationIcon;
    /** Strong references keep navigation images available through card changes and L&F updates. */
    private static final Map<String,ImageIcon> MODULE_ICONS=new HashMap<>();
    private static ImageIcon missingModuleIcon;
    private Branding() { }
    private static synchronized BufferedImage logo(){if(logo!=null)return logo;try(InputStream input=Branding.class.getResourceAsStream("/yasl-logo.png")){if(input!=null)logo=ImageIO.read(input);if(logo==null)logo=ImageIO.read(new File("assets/yasl-logo.png"));}catch(Exception ignored){}return logo;}
    private static synchronized BufferedImage applicationIcon(){if(applicationIcon!=null)return applicationIcon;try(InputStream input=Branding.class.getResourceAsStream("/yasl-app-icon.png")){if(input!=null)applicationIcon=ImageIO.read(input);if(applicationIcon==null)applicationIcon=ImageIO.read(new File("assets/yasl-app-icon.png"));}catch(Exception ignored){}return applicationIcon;}
    public static ImageIcon logoIcon(int maximumWidth,int maximumHeight){BufferedImage source=logo();if(source==null)return new ImageIcon();double scale=Math.min((double)maximumWidth/source.getWidth(),(double)maximumHeight/source.getHeight());int width=Math.max(1,(int)Math.round(source.getWidth()*scale)),height=Math.max(1,(int)Math.round(source.getHeight()*scale));return new ImageIcon(source.getScaledInstance(width,height,Image.SCALE_SMOOTH));}
    /** Packaged PNG icon used where an operating-system font glyph is not reliable. */
    public static ImageIcon applicationIcon(int width,int height){BufferedImage source=applicationIcon();return source==null?new ImageIcon():new ImageIcon(source.getScaledInstance(width,height,Image.SCALE_SMOOTH));}
    /** Distinct portable PNG resources for navigation; never relies on emoji or a platform font. */
    public static synchronized ImageIcon moduleIcon(String name,int width,int height){
        String resourcePath="/icons/"+name+".png";
        String cacheKey=resourcePath+"@"+width+"x"+height;
        ImageIcon cached=MODULE_ICONS.get(cacheKey);
        if(cached!=null)return cached;
        URL resource=Branding.class.getResource(resourcePath);
        if(resource==null){
            System.err.println("Missing icon resource: "+resourcePath);
            return missingModuleIcon(width,height);
        }
        try{
            BufferedImage source=ImageIO.read(resource);
            if(source==null)throw new IllegalStateException("Unreadable PNG");
            ImageIcon icon=new ImageIcon(source.getScaledInstance(width,height,Image.SCALE_SMOOTH));
            MODULE_ICONS.put(cacheKey,icon);
            return icon;
        }catch(Exception exception){
            System.err.println("Unable to load icon resource "+resourcePath+": "+exception.getMessage());
            return missingModuleIcon(width,height);
        }
    }
    private static ImageIcon missingModuleIcon(int width,int height){
        if(missingModuleIcon==null){
            BufferedImage image=new BufferedImage(24,24,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(191,219,254));g.setStroke(new BasicStroke(2f));g.drawOval(3,3,18,18);g.drawLine(7,7,17,17);g.drawLine(17,7,7,17);g.dispose();
            missingModuleIcon=new ImageIcon(image);
        }
        if(width==24&&height==24)return missingModuleIcon;
        return new ImageIcon(missingModuleIcon.getImage().getScaledInstance(width,height,Image.SCALE_SMOOTH));
    }
    public static void applyWindowIcon(JFrame frame){BufferedImage source=applicationIcon();if(source==null)return;frame.setIconImages(List.of(scaled(source,16),scaled(source,32),scaled(source,48),scaled(source,64),scaled(source,128),scaled(source,256)));}
    private static Image scaled(BufferedImage source,int size){return source.getScaledInstance(size,size,Image.SCALE_SMOOTH);}
}
