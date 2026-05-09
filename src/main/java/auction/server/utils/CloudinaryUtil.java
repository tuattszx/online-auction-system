package auction.server.utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.File;
import java.util.Map;

public class CloudinaryUtil {
    private static Cloudinary cloudinary;

    static {
        // Thay thế bằng thông số từ Dashboard của bạn
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dubcflcfg",
                "api_key", "487825921416994",
                "api_secret", "pfUTdivXubIRw34bBbQtEIb6B8w",
                "secure", true
        ));
    }

    public static String uploadImage(File file) {
        try {
            // Thực hiện upload file
            Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());

            // Trả về URL của ảnh sau khi upload thành công
            return (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}