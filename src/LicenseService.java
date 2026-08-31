import java.net.URI;import java.net.http.*;import java.nio.charset.StandardCharsets;import java.util.UUID;import java.util.prefs.Preferences;
/** Desktop client for the Cloudflare licensing Worker; it never contains the admin secret. */
public final class LicenseService {
    private static final Preferences PREFS=Preferences.userNodeForPackage(LicenseService.class);private static final HttpClient HTTP=HttpClient.newBuilder().build();
    public record Result(boolean allowed,String message){}
    public static boolean isConfigured(){return !System.getProperty("payroll.license.url","").trim().isEmpty();}
    public static Result validateSavedLicense(){String key=PREFS.get("license.key","");return key.isBlank()?new Result(false,"Enter your license key."):request("/v1/validate",key);}
    public static Result activate(String key){Result result=request("/v1/activate",key);if(result.allowed)PREFS.put("license.key",key.trim());return result;}
    private static Result request(String path,String key){try{String body="{\"licenseKey\":\""+escape(key.trim())+"\",\"deviceId\":\""+escape(deviceId())+"\"}";HttpRequest request=HttpRequest.newBuilder(URI.create(System.getProperty("payroll.license.url").replaceAll("/$","")+path)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();HttpResponse<String> response=HTTP.send(request,HttpResponse.BodyHandlers.ofString());boolean allowed=response.statusCode()/100==2&&response.body().contains("\"allowed\":true");return new Result(allowed,allowed?"":"Activation was declined or could not be verified.");}catch(Exception e){return new Result(false,"Could not contact the licensing service.");}}
    private static String deviceId(){String value=PREFS.get("license.device.id","");if(value.isBlank()){value=UUID.randomUUID().toString();PREFS.put("license.device.id",value);}return value;}private static String escape(String value){return value.replace("\\","\\\\").replace("\"","\\\"");}private LicenseService(){}
}
