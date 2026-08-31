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
import java.time.Instant;
import java.util.*;

/** Validates a lifetime license and binds it to this computer through the licensing API. */
public final class LicenseService {
    private static final String ENDPOINT = System.getProperty("payroll.license.url", "").replaceAll("/$", "");
    private static final Path STATE = Paths.get(System.getProperty("user.home"), ".payrollsystem", "license.properties");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private LicenseService() { }

    public static boolean isConfigured() { return ENDPOINT.startsWith("https://"); }
    public static String machineId() { return sha256(machineMaterial()); }

    /** A license is verified online at every launch so a local state file cannot extend use. */
    public static Result validateSavedLicense() {
        Properties state = load();
        String key = state.getProperty("licenseKey", "");
        if (key.isBlank()) return Result.denied("Activate this copy of Payroll System to continue.");
        if (!isConfigured()) return Result.denied("Licensing server URL has not been configured.");
        try { return validate(key, false); }
        catch (Exception e) { return Result.denied("Cannot reach the licensing server. Connect to the internet and try again."); }
    }

    public static Result activate(String licenseKey) {
        if (!isConfigured()) return Result.denied("Licensing server URL has not been configured.");
        try { return validate(licenseKey.trim(), true); }
        catch (Exception e) { return Result.denied("Could not contact the licensing server: " + e.getMessage()); }
    }

    private static Result validate(String key, boolean activate) throws Exception {
        String body = "{\"licenseKey\":\"" + json(key) + "\",\"machineId\":\"" + machineId() + "\",\"machineName\":\"" + json(System.getProperty("os.name") + " / " + System.getProperty("user.name")) + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT + (activate ? "/v1/activate" : "/v1/validate")))
            .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2 || !response.body().contains("\"valid\":true"))
            return Result.denied(message(response.body(), "License was not accepted."));
        Properties state = load(); state.setProperty("licenseKey", key); state.setProperty("lastVerified", Long.toString(System.currentTimeMillis())); save(state);
        return Result.allowed("Licensed");
    }

    private static String machineMaterial() {
        StringBuilder material = new StringBuilder(System.getProperty("os.name", "") + "|" + System.getProperty("os.arch", ""));
        try { Enumeration<NetworkInterface> all = NetworkInterface.getNetworkInterfaces(); while (all.hasMoreElements()) { NetworkInterface network = all.nextElement(); byte[] mac = network.getHardwareAddress(); if (mac != null && !network.isLoopback() && !network.isVirtual()) material.append('|').append(Base64.getEncoder().encodeToString(mac)); } }
        catch (Exception ignored) { }
        return material.toString();
    }
    private static Properties load() { Properties p = new Properties(); try (InputStream in = Files.newInputStream(STATE)) { p.load(in); } catch (IOException ignored) { } return p; }
    private static void save(Properties p) { try { Files.createDirectories(STATE.getParent()); try (OutputStream out = Files.newOutputStream(STATE)) { p.store(out, "Payroll System license state"); } } catch (IOException ignored) { } }
    private static String sha256(String value) { try { byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); StringBuilder out = new StringBuilder(); for (byte b : bytes) out.append(String.format("%02x", b)); return out.toString(); } catch (Exception e) { throw new IllegalStateException(e); } }
    private static String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"); }
    private static String message(String response, String fallback) { int mark = response.indexOf("\"message\":\""); if (mark < 0) return fallback; int start = mark + 11, end = response.indexOf('"', start); return end > start ? response.substring(start, end) : fallback; }
    public record Result(boolean allowed, String message) { static Result allowed(String message) { return new Result(true, message); } static Result denied(String message) { return new Result(false, message); } }
}
