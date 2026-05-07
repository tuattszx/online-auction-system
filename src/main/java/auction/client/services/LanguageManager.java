package auction.client.services;

import java.util.Locale;
import java.util.ResourceBundle;

public class LanguageManager {
    // Mặc định là tiếng Việt ("vi")
    private static Locale currentLocale = new Locale("en");
    private static ResourceBundle bundle = ResourceBundle.getBundle("auction.messages", currentLocale);

    public static void setLocale(String langCode) {
        currentLocale = new Locale(langCode);
        // Load lại file properties tương ứng (messages_vi hoặc messages_en)
        bundle = ResourceBundle.getBundle("auction.messages", currentLocale);
    }

    public static ResourceBundle getBundle() {
        return bundle;
    }

    public static String getString(String key) {
        return bundle.getString(key);
    }
}