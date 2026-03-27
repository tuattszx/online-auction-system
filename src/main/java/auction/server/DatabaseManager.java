package auction.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;



public class DatabaseManager {
    private static final String HOST = "b99ipdmf7qqs8ydf6oel-mysql.services.clever-cloud.com";
    private static final String DB_NAME = "b99ipdmf7qqs8ydf6oel";
    private static final String USER = "undbtnmtrkodctzy";
    private static final String PASS = "kHMJ9qPTCTiPkEuE3Max";
    private static final String URL = "jdbc:mysql://" + HOST + ":3306/" + DB_NAME;
    private static Connection connection = null;
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Không tìm thấy MySQL Driver: " + e.getMessage());
        }
    }
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Kết nối cơ sở dữ liệu thành công!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}