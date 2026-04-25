package auction.server.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class ImageService {

    // 1. Xác định gốc dự án (C:/.../online-auction-system)
    private static final String PROJECT_DIR = System.getProperty("user.dir");

    // 2. Tạo đường dẫn đến folder 'server-storage/items' ngang hàng với 'src'
    private static final String STORAGE_PATH = PROJECT_DIR + File.separator + "server-storage" + File.separator + "items";

    public static String saveImage(byte[] imageData, String originalName) throws IOException {
        if (imageData == null || imageData.length == 0) {
            return null;
        }

        // --- BƯỚC 1: KHỞI TẠO THƯ MỤC (Dành cho máy Contributor mới pull code) ---
        File storageDir = new File(STORAGE_PATH);
        if (!storageDir.exists()) {
            // mkdirs() sẽ tạo cả folder cha 'server-storage' và folder con 'items'
            boolean created = storageDir.mkdirs();
            if (created) {
                System.out.println("Đã khởi tạo thư mục lưu trữ tại: " + STORAGE_PATH);
            }
        }

        // --- BƯỚC 2: XỬ LÝ TÊN FILE (Chống trùng lặp tuyệt đối) ---
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }

        // Tên file = prefix + thời gian + chuỗi ngẫu nhiên + đuôi file
        String uniqueName = "item-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().substring(0, 8) + extension;

        // --- BƯỚC 3: GHI FILE VẬT LÝ ---
        Path targetPath = Paths.get(STORAGE_PATH, uniqueName);
        Files.write(targetPath, imageData);

        // --- BƯỚC 4: TRẢ VỀ ĐỊA CHỈ CHO DATABASE ---
        // Lưu ý: Ta trả về đường dẫn bắt đầu bằng "/items/" để đồng bộ hóa hiển thị
        String dbPath = "/items/" + uniqueName;

        System.out.println("Ảnh đã được lưu tại: " + targetPath.toString());
        return dbPath;
    }

    public static void deleteImage(String dbPath) {
        try {
            if (dbPath != null) {
                // Chuyển từ "/items/abc.jpg" thành đường dẫn thật trên ổ cứng
                String fileName = dbPath.replace("/items/", "");
                Path path = Paths.get(STORAGE_PATH, fileName);
                Files.deleteIfExists(path);
                System.out.println("🗑Đã xóa file ảnh: " + fileName);
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi xóa ảnh: " + e.getMessage());
        }
    }
}