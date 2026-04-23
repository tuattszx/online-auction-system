package auction.client.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Utility class để xử lý upload và lưu hình ảnh
 */
public class ImageService {
    
    // Đường dẫn lưu hình ảnh
    private static final String UPLOAD_DIR = "src/main/resources/uploads/items/";
    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "webp"};
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * Upload hình ảnh từ một file vào upload directory
     * @param sourceFile file được chọn từ FileChooser
     * @return đường dẫn tương đối của file đã upload (ví dụ: "uploads/items/product-123.jpg")
     * @throws IllegalArgumentException nếu file không hợp lệ
     * @throws IOException nếu có lỗi khi upload
     */
    public static String uploadImage(File sourceFile) throws IllegalArgumentException, IOException {
        // ✅ Validation
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IllegalArgumentException("File không tồn tại: " + sourceFile);
        }

        // ✅ Check file size
        if (sourceFile.length() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Kích thước file quá lớn! Tối đa " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }

        // ✅ Check file extension
        String fileExtension = getFileExtension(sourceFile);
        if (!isAllowedExtension(fileExtension)) {
            throw new IllegalArgumentException("Định dạng file không được phép! Chấp nhận: jpg, jpeg, png, gif, webp");
        }

        // ✅ Tạo upload directory nếu chưa tồn tại
        createUploadDirectoryIfNotExists();

        // ✅ Generate filename duy nhất (tránh trùng tên)
        String uniqueFilename = generateUniqueFilename(fileExtension);
        
        // ✅ Copy file vào upload directory
        Path uploadPath = Paths.get(UPLOAD_DIR, uniqueFilename);
        Files.copy(sourceFile.toPath(), uploadPath);

        // ✅ Return relative path
        String relativePath = "uploads/items/" + uniqueFilename;
        System.out.println("✅ Image uploaded: " + relativePath);
        return relativePath;
    }

    /**
     * Xóa hình ảnh đã upload
     * @param imagePath đường dẫn tương đối của image (ví dụ: "uploads/items/product-123.jpg")
     * @return true nếu xóa thành công
     */
    public static boolean deleteImage(String imagePath) {
        try {
            if (imagePath == null || imagePath.isEmpty()) {
                return false;
            }
            
            // Convert relative path to absolute path
            Path filePath;
            if (imagePath.startsWith("uploads/")) {
                filePath = Paths.get("src/main/resources/" + imagePath);
            } else {
                filePath = Paths.get("src/main/resources/uploads/items/" + imagePath);
            }

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                System.out.println("✅ Image deleted: " + imagePath);
                return true;
            }
        } catch (IOException e) {
            System.err.println("❌ Error deleting image: " + e.getMessage());
        }
        return false;
    }

    /**
     * Lấy extension của file
     */
    private static String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf(".");
        return lastDot > 0 ? name.substring(lastDot + 1).toLowerCase() : "";
    }

    /**
     * Kiểm tra extension có được phép không
     */
    private static boolean isAllowedExtension(String extension) {
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tạo filename duy nhất (tránh trùng tên)
     */
    private static String generateUniqueFilename(String extension) {
        String timestamp = System.currentTimeMillis() + "";
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return "product-" + timestamp + "-" + uuid + "." + extension;
    }

    /**
     * Tạo upload directory nếu chưa tồn tại
     */
    private static void createUploadDirectoryIfNotExists() throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("✅ Upload directory created: " + UPLOAD_DIR);
        }
    }

    /**
     * Lấy full path của image từ relative path (dùng khi display)
     */
    public static String getImageFullPath(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        
        // File path để load resource
        if (imagePath.startsWith("uploads/")) {
            return imagePath;
        } else {
            return "uploads/items/" + imagePath;
        }
    }

    /**
     * Kiểm tra image file có hợp lệ không
     */
    public static boolean isValidImageFile(File file) {
        try {
            if (file == null || !file.exists()) {
                return false;
            }
            
            String extension = getFileExtension(file);
            if (!isAllowedExtension(extension)) {
                return false;
            }
            
            if (file.length() > MAX_FILE_SIZE) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

