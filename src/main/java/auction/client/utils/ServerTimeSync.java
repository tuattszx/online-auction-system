package auction.client.utils;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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

    public static String formatRelativeTime(LocalDateTime createdAt) {
        if (createdAt == null) return "Không rõ";

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(createdAt, now);

        long seconds = duration.getSeconds();
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (seconds < 60) {
            return "Vừa xong";
        } else if (minutes < 60) {
            return minutes + " phút trước";
        } else if (hours < 24) {
            return hours + " giờ trước";
        } else if (days < 7) {
            return days + " ngày trước";
        } else {
            // Nếu quá lâu rồi (trên 7 ngày) thì hiển thị ngày tháng cụ thể luôn
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return createdAt.format(formatter);
        }
    }
}