public final class Session {
    public static String currentCompanyId;
    public static String currentUser = "Administrator";
    public static String role = "Admin";
    private Session() { }
    public static void logout() { currentUser = null; role = null; currentCompanyId = null; }
    public static String getLocalIPAddress(){try{return java.net.InetAddress.getLocalHost().getHostAddress();}catch(Exception e){return "127.0.0.1";}}
}
