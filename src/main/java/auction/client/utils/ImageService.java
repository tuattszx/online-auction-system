package auction.client.utils;

import auction.server.utils.CloudinaryUtil;

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

    public static String uploadToCloud(File file) {
        try {
            // Gọi thẳng class CloudinaryUtil mà mình đã cấu hình API Key
            String url = CloudinaryUtil.uploadImage(file);
            return url;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}