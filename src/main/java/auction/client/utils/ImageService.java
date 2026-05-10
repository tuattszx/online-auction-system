package auction.client.utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ImageService {

    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "webp"};
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String CLOUD_NAME = "dubcflcfg";
    private static final String UPLOAD_PRESET = "wdgl5qek";

    public static boolean isValidImage(File file) {
        if (file == null || !file.exists()) return false;
        if (file.length() > MAX_FILE_SIZE) return false;

        String name = file.getName().toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (name.endsWith("." + ext)) return true;
        }
        return false;
    }

    /**
     * Hàm nén ảnh thủ công bằng Java ImageIO
     * Giảm chất lượng xuống còn 0.7 (70%) để tối ưu tốc độ upload
     */
    private static File compressImage(File inputFile) throws IOException {
        BufferedImage image = ImageIO.read(inputFile);

        // Tạo file tạm để chứa ảnh đã nén
        File compressedFile = File.createTempFile("temp_upload_", ".jpg");

        OutputStream os = new FileOutputStream(compressedFile);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();

        ImageOutputStream ios = ImageIO.createImageOutputStream(os);
        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.7f); // Giảm chất lượng xuống 70%
        }

        writer.write(null, new IIOImage(image, null, null), param);

        os.close();
        ios.close();
        writer.dispose();

        return compressedFile;
    }

    public static String uploadToCloud(File file) {
        if (file == null || !file.exists()) {
            System.err.println("File không tồn tại!");
            return null;
        }

        File fileToUpload = file;
        boolean isTempFile = false;

        try {
            // Thực hiện nén trước khi upload để tăng tốc độ
            try {
                System.out.println("Đang nén ảnh...");
                fileToUpload = compressImage(file);
                isTempFile = true;
                System.out.println("Nén xong. Dung lượng mới: " + (fileToUpload.length() / 1024) + " KB");
            } catch (Exception e) {
                System.err.println("Không nén được ảnh, sẽ upload file gốc: " + e.getMessage());
                fileToUpload = file;
            }

            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", CLOUD_NAME);
            Cloudinary cloudinary = new Cloudinary(config);

            System.out.println("Đang upload lên Cloudinary...");
            Map uploadResult = cloudinary.uploader().upload(fileToUpload, ObjectUtils.asMap(
                    "upload_preset", UPLOAD_PRESET,
                    "unsigned", true
            ));

            // Xóa file tạm sau khi upload thành công để tránh rác bộ nhớ máy khách
            if (isTempFile && fileToUpload.exists()) {
                fileToUpload.delete();
            }

            return (String) uploadResult.get("secure_url");

        } catch (Exception e) {
            System.err.println("Lỗi khi upload: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}