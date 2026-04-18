package auction.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;



public class DatabaseManager {
    private static final String HOST = "mysql-1190fe6e-bduc10612-e69b.g.aivencloud.com";
    private static final String PORT = "18689";
    private static final String USER = "avnadmin";
    private static final String PASS = "AVNS_L_eIVZuMWqUZ1W3O9oI";
    private static final String DB_NAME = "defaultdb";

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME + "?useSSL=true&trustServerCertificate=true";

    private static HikariDataSource dataSource;
    static {
        try {
            HikariConfig config = new HikariConfig();

            config.setJdbcUrl(URL);
            config.setUsername(USER);
            config.setPassword(PASS);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            System.err.println("Không tìm thấy MySQL Driver: " + e.getMessage());
        }
    }
    public static Connection getConnection() throws SQLException {
        if (dataSource==null){
            throw new SQLException();
        }
        return dataSource.getConnection();
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        // Lần 1: Tốn thời gian khởi tạo Pool (có thể mất 1-2s)
        try (Connection conn1 = getConnection();) {
            System.out.println("Lần 1 lấy kết nối mất: " + (System.currentTimeMillis() - start) + "ms");

        }
        catch (SQLException e){
            e.printStackTrace();
        }
         // Trả lại vào pool

        // Lần 2: Lấy từ Pool ra dùng tiếp
        long start2 = System.currentTimeMillis();

        try (Connection conn2 = getConnection();){
            System.out.println("Lần 2 lấy kết nối mất: " + (System.currentTimeMillis() - start2) + "ms");
        }
        catch (SQLException e){
            e.printStackTrace();
        }

    }
}