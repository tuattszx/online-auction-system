package auction.client.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

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

    public static byte[] toBytes(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }
}