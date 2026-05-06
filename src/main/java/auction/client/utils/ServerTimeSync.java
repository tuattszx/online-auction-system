package auction.client.utils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ServerTimeSync {
    private static long timeOffset = 0; // Độ lệch tính bằng mili giây

    // 1. Gọi hàm này ngay khi ứng dụng khởi động hoặc sau khi login
    // serverTimeMillis là giá trị System.currentTimeMillis() lấy từ Server
    public static void sync(long serverTimeMillis) {
        long clientTimeMillis = System.currentTimeMillis();
        timeOffset = serverTimeMillis - clientTimeMillis;
        System.out.println("Đã đồng bộ. Độ lệch: " + timeOffset + "ms");
    }

    // 2. Thay thế hoàn toàn cho LocalDateTime.now()
    public static LocalDateTime getNow() {
        long synchronizedMillis = System.currentTimeMillis() + timeOffset;
        return Instant.ofEpochMilli(synchronizedMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}