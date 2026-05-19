package auction.client.services;

import auction.common.model.notifications.Notification;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public class NotificationSubscriptionManager {
    private static NotificationSubscriptionManager instance;

    // Danh sách các callback từ UI đăng ký lắng nghe thông báo mới
    private final Set<Consumer<Notification>> listeners = new CopyOnWriteArraySet<>();

    public static synchronized NotificationSubscriptionManager getInstance() {
        if (instance == null) {
            instance = new NotificationSubscriptionManager();
        }
        return instance;
    }

    /**
     * Đăng ký lắng nghe khi một màn hình (UI) mở ra
     */
    public void subscribe(Consumer<Notification> listener) {
        listeners.add(listener);
    }

    /**
     * Hủy đăng ký khi màn hình đó bị đóng đóng/bị hủy
     */
    public void unsubscribe(Consumer<Notification> listener) {
        listeners.remove(listener);
    }

    /**
     * Kích hoạt gọi tất cả giao diện cập nhật khi ClientNetwork nhận được thông báo mới từ mạng
     */
    public void notifyNewNotification(Notification notification) {
        for (Consumer<Notification> listener : listeners) {
            listener.accept(notification);
        }
    }
}
