import java.io.IOException;
import java.nio.file.*;

/** Stores employee documents in a predictable folder on the signed-in user's Desktop. */
public final class EmployeeDocumentStore {
    private EmployeeDocumentStore() { }
    public static Path folder(String employeeId, String employeeName) throws IOException {
        if(employeeId==null||employeeId.trim().isEmpty()||employeeName==null||employeeName.trim().isEmpty()) throw new IOException("Enter Employee ID and Employee Name before uploading documents.");
        String name=(employeeId.trim()+"-"+employeeName.trim()).replaceAll("[\\\\/:*?\"<>|]","_");
        Path root=Paths.get(System.getProperty("user.home"),"Desktop","payroll software documents");
        return Files.createDirectories(root.resolve(name));
    }
    public static Path save(Path source, String employeeId, String employeeName, int type) throws IOException {
        String sourceName=source.getFileName().toString().toLowerCase();
        String targetName=type==0?(sourceName.endsWith(".jpeg")?"Photo.jpeg":"Photo.jpg"):(type==1?"PAN Card.pdf":"Aadhaar Card.pdf");
        Path target=folder(employeeId,employeeName).resolve(targetName);
        return Files.copy(source,target,StandardCopyOption.REPLACE_EXISTING);
    }
    public static void remove(String path) throws IOException { if(path!=null&&!path.isEmpty()) Files.deleteIfExists(Paths.get(path)); }
}
