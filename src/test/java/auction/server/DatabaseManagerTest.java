package auction.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.sql.Connection;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    @Test
    @DisplayName("Test kết nối: Phải lấy được Connection từ Pool")
    void testGetConnection() throws SQLException {
        // Lấy kết nối từ DatabaseManager (Singleton)
        DatabaseManager manager = DatabaseManager.getInstance();

        try (Connection conn = manager.getConnection()) {
            // 1. Kiểm tra kết nối không được null
            assertNotNull(conn, "Connection trả về không được null");

            // 2. Kiểm tra kết nối thực sự đang mở
            assertTrue(conn.isValid(5), "Kết nối tới TiDB phải còn hiệu lực");

            System.out.println("Test Connection thành công: " + conn.getMetaData().getDatabaseProductName());
        }
    }

    @Test
    @DisplayName("Test Singleton: Chỉ được có một Instance duy nhất")
    void testSingleton() {
        DatabaseManager instance1 = DatabaseManager.getInstance();
        DatabaseManager instance2 = DatabaseManager.getInstance();

        assertSame(instance1, instance2, "Cả 2 instance phải là một (Singleton)");
    }

    @Test
    @DisplayName("Test Hiệu năng Pool: Lần thứ 2 lấy kết nối phải cực nhanh")
    void testPoolPerformance() throws SQLException {
        DatabaseManager manager = DatabaseManager.getInstance();

        // Lần 1: Có thể chậm vì phải khởi tạo Pool
        Connection conn1 = manager.getConnection();
        conn1.close(); // Trả lại vào pool

        // Lần 2: Phải lấy từ Pool nên tốc độ phải tính bằng mili giây (rất nhỏ)
        long start = System.currentTimeMillis();
        Connection conn2 = manager.getConnection();
        long end = System.currentTimeMillis();

        conn2.close();

        assertTrue((end - start) < 100, "Lần 2 lấy kết nối phải < 100ms nhờ HikariCP Pool");
    }
}