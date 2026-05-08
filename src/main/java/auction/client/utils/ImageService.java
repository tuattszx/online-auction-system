package auction.client.utils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;

public class ImageService {

    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "webp"};
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB


    public static boolean isValidImage(File file) {
        if (file == null || !file.exists()) return false;

        // Check size
        if (file.length() > MAX_FILE_SIZE) return false;

        // Check extension
        String name = file.getName().toLowerCase();
        boolean validExt = false;
        for (String ext : ALLOWED_EXTENSIONS) {
            if (name.endsWith("." + ext)) {
                validExt = true;
                break;
            }
        }
        return validExt;
    }

    public static byte[] toBytesAndCompress(File file) throws IOException {
        // 1. Đọc ảnh từ file
        BufferedImage originalImage = ImageIO.read(file);
        if (originalImage == null) {
            return Files.readAllBytes(file.toPath()); // Trả về byte gốc nếu không đọc được
        }

        // 2. Chuẩn bị ảnh mới (Loại bỏ kênh Alpha/Transparent để nén JPG tốt hơn)
        BufferedImage newImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_INT_RGB);

        // Vẽ ảnh gốc lên ảnh mới (Nền trắng cho các ảnh PNG trong suốt)
        Graphics2D g2d = newImage.createGraphics();
        g2d.setColor(java.awt.Color.WHITE);
        g2d.fillRect(0, 0, newImage.getWidth(), newImage.getHeight());
        g2d.drawImage(originalImage, 0, 0, null);
        g2d.dispose();

        // 3. Thiết lập nén JPG (Chất lượng 0.7f = 70%)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) throw new IllegalStateException("No writers found");

        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.7f); // Điều chỉnh từ 0.0 đến 1.0 (0.7 là cân bằng nhất)
            }

            writer.write(null, new IIOImage(newImage, null, null), param);
        } finally {
            writer.dispose();
        }

        byte[] compressedData = baos.toByteArray();
        System.out.println("Original: " + file.length() / 1024 + "KB -> Compressed: " + compressedData.length / 1024 + "KB");

        return compressedData;
    }
}