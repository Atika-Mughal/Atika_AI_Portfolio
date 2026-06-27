import java.sql.*;


public class DatabaseConnection {
    private static final String HOST     = "localhost";
    private static final int    PORT     = 3306;
    private static final String DATABASE = "sctms";
    private static final String USER     = "root";
    private static final String PASSWORD = "pakistan123";

    private static final String URL =
        "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE +
        "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean validateLogin(String username, String password, String userType) {
        String query = "SELECT 1 FROM users WHERE username=? AND password=? AND user_type=?";
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, username);
            pst.setString(2, password);
            pst.setString(3, userType);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
