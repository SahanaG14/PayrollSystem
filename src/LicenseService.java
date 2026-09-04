import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

/** Validates a lifetime license and binds it to this computer through the licensing API. */
public final class LicenseService {
    private static final String ENDPOINT = System.getProperty("payroll.license.url", "https://payroll-license-api.yasl-server.workers.dev").replaceAll("/$", "");
    private static final Path STATE = Paths.get(System.getProperty("user.home"), ".payrollsystem", "license.properties");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private LicenseService() { }

    public static boolean isConfigured() { return ENDPOINT.startsWith("https://"); }
    public static String machineId() { return sha256(machineMaterial()); }
    /** Short, stable customer-facing identifier; the full machine hash remains internal. */
    public static String supportId(){String id=machineId().substring(0,12).toUpperCase(Locale.ROOT);return "YASL-"+id.substring(0,4)+"-"+id.substring(4,8)+"-"+id.substring(8,12);}
    static String savedLicenseKey() { return load().getProperty("licenseKey", ""); }

    /** A license is verified online at every launch so a local state file cannot extend use. */
    public static Result validateSavedLicense() {
        RuntimeValidation validation = validateSavedLicenseForRuntime();
        return switch (validation.status()) {
            case VALID -> Result.allowed("Licensed");
            case INVALID -> Result.denied(validation.message());
            case UNAVAILABLE -> Result.denied("Cannot reach the licensing server. Connect to the internet and try again.");
        };
    }

    /** Online-only status for the runtime watchdog. It never treats a network failure as a revocation. */
    public static RuntimeValidation validateSavedLicenseForRuntime() {
        String key = savedLicenseKey();
        if (key.isBlank()) return RuntimeValidation.invalid("Activate this copy of " + Branding.APPLICATION_NAME + " to continue.");
        if (!isConfigured()) return RuntimeValidation.unavailable("Licensing server URL has not been configured.");
        try { return validateRuntime(key, false); }
        catch (Exception ignored) { return RuntimeValidation.unavailable("Cannot reach the licensing server."); }
    }

    static long lastVerifiedAt() {
        try { return Long.parseLong(load().getProperty("lastVerified", "0")); }
        catch (NumberFormatException ignored) { return 0L; }
    }

    public static Result activate(String licenseKey) {
        if (!isConfigured()) return Result.denied("Licensing server URL has not been configured.");
        try {
            RuntimeValidation validation = validateRuntime(licenseKey.trim(), true);
            return validation.status() == ValidationStatus.VALID ? Result.allowed("Licensed") : Result.denied(validation.message());
        }
        catch (Exception e) { return Result.denied("Could not contact the licensing server: " + e.getMessage()); }
    }

    private static RuntimeValidation validateRuntime(String key, boolean activate) throws Exception {
        String body = "{\"licenseKey\":\"" + json(key) + "\",\"machineId\":\"" + machineId() + "\",\"machineName\":\"" + json(System.getProperty("os.name") + " / " + System.getProperty("user.name")) + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT + (activate ? "/v1/activate" : "/v1/validate")))
            .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        // A Worker, proxy, or connection can temporarily fail without changing the licence.
        // Only an explicit client rejection from the licensing service is a revocation signal.
        if (status == 408 || status == 429 || status / 100 == 5)
            return RuntimeValidation.unavailable("Licensing server is temporarily unavailable.");
        if (status / 100 == 2) {
            if (!response.body().contains("\"valid\":true"))
                return RuntimeValidation.unavailable("Licensing server returned an unexpected response.");
        } else if (status / 100 == 4) {
            return RuntimeValidation.invalid(message(response.body(), "License was not accepted."));
        } else {
            return RuntimeValidation.unavailable("Licensing server returned an unexpected response.");
        }
        Properties state = load(); state.setProperty("licenseKey", key); state.setProperty("lastVerified", Long.toString(System.currentTimeMillis())); save(state);
        return RuntimeValidation.valid();
    }

    private static String machineMaterial() {
        StringBuilder material = new StringBuilder(System.getProperty("os.name", "") + "|" + System.getProperty("os.arch", ""));
        try { Enumeration<NetworkInterface> all = NetworkInterface.getNetworkInterfaces(); while (all.hasMoreElements()) { NetworkInterface network = all.nextElement(); byte[] mac = network.getHardwareAddress(); if (mac != null && !network.isLoopback() && !network.isVirtual()) material.append('|').append(Base64.getEncoder().encodeToString(mac)); } }
        catch (Exception ignored) { }
        return material.toString();
    }
    private static Properties load() { Properties p = new Properties(); try (InputStream in = Files.newInputStream(STATE)) { p.load(in); } catch (IOException ignored) { } return p; }
    private static void save(Properties p) { try { Files.createDirectories(STATE.getParent()); try (OutputStream out = Files.newOutputStream(STATE)) { p.store(out, Branding.COMPANY_NAME+" license state"); } } catch (IOException ignored) { } }
    private static String sha256(String value) { try { byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); StringBuilder out = new StringBuilder(); for (byte b : bytes) out.append(String.format("%02x", b)); return out.toString(); } catch (Exception e) { throw new IllegalStateException(e); } }
    private static String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"); }
    private static String message(String response, String fallback) { int mark = response.indexOf("\"message\":\""); if (mark < 0) return fallback; int start = mark + 11, end = response.indexOf('"', start); return end > start ? response.substring(start, end) : fallback; }
    private static final java.util.concurrent.ScheduledExecutorService HEARTBEAT = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "License-Heartbeat");
        t.setDaemon(true);
        return t;
    });

    /** Starts periodic background license checks while the application remains open. */
    public static void startHeartbeat() {
        HEARTBEAT.scheduleAtFixedRate(() -> {
            try {
                Result result = validateSavedLicense();
                if (!result.allowed()) {
                    java.awt.EventQueue.invokeLater(() -> handleLicenseLost(result.message()));
                }
            } catch (Exception ignored) { }
        }, 15, 15, java.util.concurrent.TimeUnit.MINUTES);
    }

    private static void handleLicenseLost(String reason) {
        for (java.awt.Window window : java.awt.Window.getWindows()) {
            if (!(window instanceof LicenseActivationFrame)) {
                window.dispose();
            }
        }
        javax.swing.JOptionPane.showMessageDialog(null,
            "License validation failed: " + reason + "\n\nPlease activate a valid license to continue using Payroll System.",
            "License Revoked or Inactive",
            javax.swing.JOptionPane.ERROR_MESSAGE);
        new LicenseActivationFrame().setVisible(true);
    }

    public record Result(boolean allowed, String message) { static Result allowed(String message) { return new Result(true, message); } static Result denied(String message) { return new Result(false, message); } }
    public enum ValidationStatus { VALID, INVALID, UNAVAILABLE }
    public record RuntimeValidation(ValidationStatus status, String message) {
        static RuntimeValidation valid() { return new RuntimeValidation(ValidationStatus.VALID, "Licensed"); }
        static RuntimeValidation invalid(String message) { return new RuntimeValidation(ValidationStatus.INVALID, message); }
        static RuntimeValidation unavailable(String message) { return new RuntimeValidation(ValidationStatus.UNAVAILABLE, message); }
    }
}
