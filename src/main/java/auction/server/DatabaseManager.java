package auction.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.SQLException;



public class DatabaseManager {
    private static DatabaseManager instance;
    private static HikariDataSource dataSource;
    private DatabaseManager() {
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

            String host = dotenv.get("DB_HOST", System.getenv("DB_HOST"));
            String port = dotenv.get("DB_PORT", System.getenv("DB_PORT"));
            String user = dotenv.get("DB_USER", System.getenv("DB_USER"));
            String pass = dotenv.get("DB_PASS", System.getenv("DB_PASS"));
            String dbName = dotenv.get("DB_NAME", System.getenv("DB_NAME"));

            String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=true&trustServerCertificate=true";
            HikariConfig config = new HikariConfig();

            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(pass);
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
    public Connection getConnection() throws SQLException {
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