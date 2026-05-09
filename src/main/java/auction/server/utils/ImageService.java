package auction.server.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class ImageService {
    /**
     * Gửi file ảnh lên Cloudinary và lấy về URL.
     * Đây là hàm trung chuyển (Wrapper) giúp code ở Controller sạch hơn.
     */
    public static String uploadToCloud(File file) {
        if (file == null) {
            return null;
        }
        // Gọi trực tiếp công cụ Cloudinary đã cấu hình
        return CloudinaryUtil.uploadImage(file);
    }

    /**
     * (Tùy chọn) Nếu bạn muốn giữ lại hàm này để tránh lỗi ở các nơi khác đang gọi,
     * nhưng bây giờ nó không làm gì cả vì ta dùng URL trực tiếp.
     */
}