/** Build-time entry point that creates the schema-only SQLite seed included in the Windows installer. */
public final class SchemaSeedBuilder {
    public static void main(String[] args) { DatabaseInitializer.initialize(); }
    private SchemaSeedBuilder() { }
}
