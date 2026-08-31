import java.io.IOException;
import java.nio.file.*;
import java.io.InputStream;

/** Stores employee documents in a predictable folder on the signed-in user's Desktop. */
public final class EmployeeDocumentStore {
    private static final long MAX_DOCUMENT_BYTES=10L*1024*1024;
    private static final Path ROOT=Paths.get(System.getProperty("user.home"),"Desktop","payroll software documents").toAbsolutePath().normalize();
    private EmployeeDocumentStore() { }
    public static Path folder(String employeeId, String employeeName) throws IOException {
        if(employeeId==null||employeeId.trim().isEmpty()||employeeName==null||employeeName.trim().isEmpty()) throw new IOException("Enter Employee ID and Employee Name before uploading documents.");
        String name=(employeeId.trim()+"-"+employeeName.trim()).replaceAll("[\\\\/:*?\"<>|]","_");
        Path target=ROOT.resolve(name).normalize(); if(!target.startsWith(ROOT))throw new IOException("Invalid document location.");
        return Files.createDirectories(target);
    }
    public static Path save(Path source, String employeeId, String employeeName, int type) throws IOException {
        if(source==null||!Files.isRegularFile(source)||Files.size(source)>MAX_DOCUMENT_BYTES)throw new IOException("Document must be a file no larger than 10 MB.");
        String sourceName=source.getFileName().toString().toLowerCase();
        if(!validType(source,type))throw new IOException("The file contents do not match the selected document type.");
        String targetName=type==0?(sourceName.endsWith(".jpeg")?"Photo.jpeg":"Photo.jpg"):(type==1?"PAN Card.pdf":"Aadhaar Card.pdf");
        Path target=folder(employeeId,employeeName).resolve(targetName);
        return Files.copy(source,target,StandardCopyOption.REPLACE_EXISTING);
    }
    public static void remove(String path) throws IOException { if(path==null||path.isEmpty())return;Path target=Paths.get(path).toAbsolutePath().normalize();if(!target.startsWith(ROOT))throw new IOException("Refusing to remove a file outside Payroll documents.");Files.deleteIfExists(target); }
    private static boolean validType(Path source,int type)throws IOException{try(InputStream in=Files.newInputStream(source)){byte[] b=in.readNBytes(8);if(type==0)return b.length>=3&&(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255;return b.length>=5&&b[0]=='%'&&b[1]=='P'&&b[2]=='D'&&b[3]=='F'&&b[4]=='-';}}
}
