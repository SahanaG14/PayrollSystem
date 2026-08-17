import java.io.File;

public final class ExportFileName {
    public static File unique(File requested) {
        if (requested == null || !requested.exists()) return requested;
        String name = requested.getName(); int dot = name.lastIndexOf('.'); String stem = dot < 0 ? name : name.substring(0, dot), extension = dot < 0 ? "" : name.substring(dot);
        for (int index = 1; ; index++) { File candidate = new File(requested.getParentFile(), stem + " (" + index + ")" + extension); if (!candidate.exists()) return candidate; }
    }
    private ExportFileName() { }
}
