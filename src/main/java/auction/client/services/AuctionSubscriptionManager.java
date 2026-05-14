package auction.client.services;

import auction.common.message.BidUpdateNotification;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public class AuctionSubscriptionManager {
    private static AuctionSubscriptionManager instance;

    // Map lưu trữ: Key là itemId, Value là danh sách các hàm callback để cập nhật UI
    private final Map<Integer, Set<Consumer<BidUpdateNotification>>> subscribers = new ConcurrentHashMap<>();

    public static synchronized AuctionSubscriptionManager getInstance() {
        if (instance == null) instance = new AuctionSubscriptionManager();
        return instance;
    }

    /**
     * Đăng ký lắng nghe cập nhật cho một món hàng cụ thể
     */
    public void subscribe(int itemId, Consumer<BidUpdateNotification> callback) {
        subscribers.computeIfAbsent(itemId, k -> new CopyOnWriteArraySet<>()).add(callback);
    }

    /**
     * Hủy đăng ký khi đóng màn hình hoặc chuyển món hàng khác
     */
    public void unsubscribe(int itemId, Consumer<BidUpdateNotification> callback) {
        if (subscribers.containsKey(itemId)) {
            subscribers.get(itemId).remove(callback);
        }
    }

    /**
     * Hàm này được gọi từ ClientNetwork khi nhận được BidUpdateNotification từ Server
     */
    public void notifyUpdate(BidUpdateNotification notification) {
        Set<Consumer<BidUpdateNotification>> callbacks = subscribers.get(notification.getItemId());
        if (callbacks != null) {
            for (Consumer<BidUpdateNotification> callback : callbacks) {
                callback.accept(notification);
            }
        }
    }
}