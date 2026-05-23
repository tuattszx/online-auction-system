package auction.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;



public class DatabaseManager {
    private static final String HOST = "gateway01.ap-southeast-1.prod.aws.tidbcloud.com";
    private static final String PORT = "4000";
    private static final String USER = "GRmFoHTy82Wuncw.root";
    private static final String PASS = "5lthvbCLcBtIfZ2C";
    private static final String DB_NAME = "test";

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME + "?useSSL=true&trustServerCertificate=true";

    private static DatabaseManager instance;
    private static HikariDataSource dataSource;
    private DatabaseManager() {
        try {
            HikariConfig config = new HikariConfig();

            config.setJdbcUrl(URL);
            config.setUsername(USER);
            config.setPassword(PASS);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            config.setMaximumPoolSize(20);
            config.setMinimumIdle(3);
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

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }

        }
        return instance;
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        try (Connection conn1 = DatabaseManager.getInstance().getConnection();) {
            System.out.println("Lần 1 lấy kết nối mất: " + (System.currentTimeMillis() - start) + "ms");

        }
        catch (SQLException e){
            e.printStackTrace();
        }

        long start2 = System.currentTimeMillis();
        try (Connection conn2 = DatabaseManager.getInstance().getConnection();){
            System.out.println("Lần 2 lấy kết nối mất: " + (System.currentTimeMillis() - start2) + "ms");
        }
        catch (SQLException e){
            e.printStackTrace();
        }

    }
}