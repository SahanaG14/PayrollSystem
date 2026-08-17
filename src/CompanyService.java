import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

public class CompanyService {

    public static String createCompany(String name, String address) {
        try {
            Connection conn = DBConnection.getConnection();

            // Generate company ID automatically
            String companyId = "CMP" + System.currentTimeMillis();

            String query = "INSERT INTO company VALUES (?, ?, ?, NOW())";
            PreparedStatement ps = conn.prepareStatement(query);

            ps.setString(1, companyId);
            ps.setString(2, name);
            ps.setString(3, address);

            ps.executeUpdate();

            return companyId;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}