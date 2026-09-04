import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.sql.*;

/** Small persistent audit trail that is safe to use from UI or worker threads. */
public final class ActivityLogger {
    public static final class Entry {
        public final LocalDateTime timestamp; public final String module, action, details, user, session, status;
        Entry(LocalDateTime time, String module, String action, String details, String user, String session, String status) {
            this.timestamp=time; this.module=module; this.action=action; this.details=details; this.user=user; this.session=session; this.status=status;
        }
    }
    private static final Path FILE=Path.of(System.getProperty("user.home"), ".payroll-activity.csv");
    private static final DateTimeFormatter FORMAT=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<Entry> ENTRIES=new ArrayList<>();
    private static final String SESSION=Session.getLocalIPAddress();
    static { load(); }
    public static synchronized void log(String module,String action,String details,String status) {
        Entry e=new Entry(LocalDateTime.now(),clean(module),clean(action),clean(details),clean(Session.currentUser==null?"Administrator":Session.currentUser),SESSION,clean(status));
        ENTRIES.add(e); append(e); appendDatabase(e,"");
    }
    public static synchronized void logAs(String user,String module,String action,String details,String status) {Entry e=new Entry(LocalDateTime.now(),clean(module),clean(action),clean(details),clean(user),SESSION,clean(status));ENTRIES.add(e);append(e);appendDatabase(e,"");}
    public static synchronized void logRecord(String module,String action,String details,String recordId,String status){Entry e=new Entry(LocalDateTime.now(),clean(module),clean(action),clean(details),clean(Session.currentUser==null?"Administrator":Session.currentUser),SESSION,clean(status));ENTRIES.add(e);append(e);appendDatabase(e,clean(recordId));}
    public static synchronized List<Entry> find(String query, String range) {
        String q=clean(query).toLowerCase(Locale.ROOT); LocalDateTime cutoff="Today".equals(range)?LocalDate.now().atStartOfDay():"Last 7 Days".equals(range)?LocalDateTime.now().minusDays(7):"Last 30 Days".equals(range)?LocalDateTime.now().minusDays(30):null;
        List<Entry> out=new ArrayList<>(); for(Entry e:ENTRIES) { String text=(e.module+" "+e.action+" "+e.details+" "+e.user+" "+e.status).toLowerCase(Locale.ROOT); if((cutoff==null||!e.timestamp.isBefore(cutoff))&&(q.isEmpty()||text.contains(q))) out.add(e); }
        Collections.reverse(out); return out;
    }
    public static void exportCsv(File file,List<Entry> entries) throws IOException {
        try(BufferedWriter out=Files.newBufferedWriter(file.toPath(),StandardCharsets.UTF_8)) { out.write("Timestamp,Module,Action Performed,Details,Performed By,IP / Session ID,Status\n"); for(Entry e:entries) out.write(csv(e.timestamp.format(FORMAT))+","+csv(e.module)+","+csv(e.action)+","+csv(e.details)+","+csv(e.user)+","+csv(e.session)+","+csv(e.status)+"\n"); }
    }
    private static void load() { if(!Files.isRegularFile(FILE)) return; try(BufferedReader in=Files.newBufferedReader(FILE,StandardCharsets.UTF_8)) { String line; while((line=in.readLine())!=null) { String[] p=line.split("\\|",-1); if(p.length==7) ENTRIES.add(new Entry(LocalDateTime.parse(p[0],FORMAT),un(p[1]),un(p[2]),un(p[3]),un(p[4]),un(p[5]),un(p[6]))); } } catch(Exception ignored) {} }
    private static void append(Entry e) { try { Files.writeString(FILE,e.timestamp.format(FORMAT)+"|"+enc(e.module)+"|"+enc(e.action)+"|"+enc(e.details)+"|"+enc(e.user)+"|"+enc(e.session)+"|"+enc(e.status)+"\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND); } catch(IOException ignored) {} }
    private static void appendDatabase(Entry e,String recordId){try(Connection c=DBConnection.getConnection();Statement s=c==null?null:c.createStatement()){if(c==null||s==null)return;s.executeUpdate("CREATE TABLE IF NOT EXISTS activity_log (id INTEGER PRIMARY KEY AUTOINCREMENT, occurred_at TEXT NOT NULL, user_name TEXT NOT NULL, module TEXT NOT NULL, action_name TEXT NOT NULL, details TEXT, record_id TEXT, result TEXT NOT NULL)");try(PreparedStatement p=c.prepareStatement("INSERT INTO activity_log(occurred_at,user_name,module,action_name,details,record_id,result) VALUES(?,?,?,?,?,?,?)")){p.setString(1,e.timestamp.format(FORMAT));p.setString(2,e.user);p.setString(3,e.module);p.setString(4,e.action);p.setString(5,e.details);p.setString(6,recordId);p.setString(7,e.status);p.executeUpdate();}}catch(Exception ignored){}}
    private static String enc(String s) { return Base64.getUrlEncoder().encodeToString(clean(s).getBytes(StandardCharsets.UTF_8)); }
    private static String un(String s) { try{return new String(Base64.getUrlDecoder().decode(s),StandardCharsets.UTF_8);}catch(Exception e){return "";} }
    private static String csv(String s) { String value=clean(s);if(!value.isEmpty()&&"=+-@".indexOf(value.charAt(0))>=0)value="'"+value;return "\""+value.replace("\"","\"\"")+"\""; }
    private static String clean(String s) {String value=s==null?"":s.replace('\n',' ').replace('\r',' ');value=value.replaceAll("(?i)(password|recovery[ -]?code|hash|encryption key)\\s*[:=]\\s*[^,; ]+","$1: [redacted]");value=value.replaceAll("\\b\\d{12}\\b","XXXX-XXXX-XXXX");value=value.replaceAll("\\b[A-Z]{5}\\d{4}[A-Z]\\b","XXXXX1234X");value=value.replaceAll("\\b\\d{10}\\b","XXXXXX1234");return value; }
    private ActivityLogger() {}
}
