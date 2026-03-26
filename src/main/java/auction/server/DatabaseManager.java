package auction.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String HOST = "b99ipdmf7qqs8ydf6oel-mysql.services.clever-cloud.com";
    private static final String DB_NAME = "b99ipdmf7qqs8ydf6oel";
    private static final String USER = "undbtnmtrkodctzy";
    private static final String PASS = "kHMJ9qPTCTiPkEuE3Max";
    private static final String URL = "jdbc:mysql://" + HOST + ":3306/" + DB_NAME;

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tìm thấy MySQL Driver!", e);
        }
    }
    public static void main(String[] args) {
    }
}