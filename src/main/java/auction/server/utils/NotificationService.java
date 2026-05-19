package auction.server.utils;

import auction.common.model.bid.Bid;
import auction.common.model.items.Item;
import auction.common.model.notifications.BidNotification;
import auction.server.ClientHandler;
import auction.server.ClientManager;
import auction.server.dao.*;
import auction.server.dao.impl.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NotificationService {
    private static final FavouriteDao favouriteDao = new FavouriteDaoImpl();
    private static final BidDao bidDao = new BidDaoImpl();
    private static final NotificationDAO notificationDao = new NotificationDaoImpl();

    public static void sendOpenNotifications(Item item, LocalDateTime now) {
        try {
            // --- A. Gửi cho NGƯỜI BÁN ---
            BidNotification sellerNotif = new BidNotification(
                    0, item.getSellerId(), "Sản phẩm của bạn đã lên sàn!",
                    String.format("Sản phẩm '%s' của bạn đã chính thức được hệ thống mở phòng đấu giá!", item.getName()),
                    item.getId(), item.getCurrentPrice(), "Hệ thống"
            );
            sellerNotif.setCreatedAt(now);
            saveAndSendRealtime(sellerNotif);

            // --- B. Gửi cho NHỮNG NGƯỜI THẢ TIM ---
            List<Integer> interestedUserIds = favouriteDao.getUserIdsByFavoriteItem(item.getId());
            // Loại bỏ ID người bán ra khỏi danh sách thả tim nếu lỡ tự thích hàng của mình
            interestedUserIds.remove(Integer.valueOf(item.getSellerId()));

            if (!interestedUserIds.isEmpty()) {
                BidNotification loverNotif = new BidNotification(
                        0, 0, "Sản phẩm bạn thích đã mở cửa!",
                        String.format("Sản phẩm '%s' bạn yêu thích đang mở cửa tự do! Hãy vào đặt giá ngay thôi.", item.getName()),
                        item.getId(), item.getCurrentPrice(), "Hệ thống"
                );
                loverNotif.setCreatedAt(now);

                // Lưu DB hàng loạt
                notificationDao.insertNotificationsBatch(Set.copyOf(interestedUserIds), loverNotif);

                // Bắn Socket real-time cho những ai đang online
                for (ClientHandler client : ClientManager.getActiveClients()) {
                    if (client.getLoggedInUser() != null && interestedUserIds.contains(client.getLoggedInUser().getId())) {
                        loverNotif.setUserId(client.getLoggedInUser().getId());
                        client.sendObject(loverNotif);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo OPEN: " + e.getMessage());
        }
    }

    /**
     * 🔥 2. XỬ LÝ KHI KẾT THÚC PHIÊN (CLOSED)
     */
    public static void sendClosedNotifications(Item item, LocalDateTime now) {
        try {
            // Lấy toàn bộ lịch sử bid của sản phẩm này để tìm người tham gia
            List<Bid> allBids = bidDao.getBidsByItemId(item.getId());

            // TRƯỜNG HỢP 1: Có người tham gia đặt giá (Có người thắng cuộc)
            if (allBids != null && !allBids.isEmpty()) {
                // Sắp xếp lấy lượt bid cao nhất để tìm Winner (Đề phòng lịch sử chưa sắp xếp)
                Bid highestBid = allBids.stream()
                        .max((b1, b2) -> Long.compare(b1.getBidAmount(), b2.getBidAmount()))
                        .get();

                int winnerId = highestBid.getIdUser();
                String winnerName = highestBid.getBidderName();
                long finalPrice = highestBid.getBidAmount();

                // --- A. Thông báo cho NGƯỜI THẮNG ---
                BidNotification winnerNotif = new BidNotification(
                        0, winnerId, "Bạn đã đấu giá THẮNG!",
                        String.format("Chúc mừng! Bạn đã đấu giá thành công và sở hữu món hàng '%s' với mức giá %,d $.", item.getName(), finalPrice),
                        item.getId(), finalPrice, winnerName
                );
                winnerNotif.setCreatedAt(now);
                saveAndSendRealtime(winnerNotif);

                // --- B. Thông báo cho NGƯỜI BÁN ---
                BidNotification sellerNotif = new BidNotification(
                        0, item.getSellerId(), "Sản phẩm đã bán thành công!",
                        String.format("Tin vui! Sản phẩm '%s' của bạn đã được bán cho người dùng %s với giá cuối cùng là %,d $.", item.getName(), winnerName, finalPrice),
                        item.getId(), finalPrice, winnerName
                );
                sellerNotif.setCreatedAt(now);
                saveAndSendRealtime(sellerNotif);

                // --- C. Thông báo cho NHỮNG NGƯỜI THUA CUỘC ---
                Set<Integer> losers = allBids.stream()
                        .map(Bid::getIdUser)
                        .filter(id -> id != winnerId)
                        .collect(Collectors.toSet());

                if (!losers.isEmpty()) {
                    BidNotification loserNotif = new BidNotification(
                            0, 0, "Phiên đấu giá kết thúc!",
                            String.format("Phiên đấu giá sản phẩm '%s' đã kết thúc. Người dùng %s đã sở hữu món hàng với giá %,d $.", item.getName(), winnerName, finalPrice),
                            item.getId(), finalPrice, winnerName
                    );
                    loserNotif.setCreatedAt(now);

                    notificationDao.insertNotificationsBatch(losers, loserNotif);

                    // Gửi Socket real-time cho đội thua đang online
                    for (ClientHandler client : ClientManager.getActiveClients()) {
                        if (client.getLoggedInUser() != null && losers.contains(client.getLoggedInUser().getId())) {
                            loserNotif.setUserId(client.getLoggedInUser().getId());
                            client.sendObject(loserNotif);
                        }
                    }
                }

            } else {
                // TRƯỜNG HỢP 2: Hết giờ nhưng KHÔNG có ai thèm đặt giá
                BidNotification looseSellerNotif = new BidNotification(
                        0, item.getSellerId(), "Phiên đấu giá thất bại",
                        String.format("Phiên đấu giá sản phẩm '%s' của bạn đã kết thúc nhưng rất tiếc không có lượt đặt giá nào.", item.getName()),
                        item.getId(), item.getCurrentPrice(), "Hệ thống"
                );
                looseSellerNotif.setCreatedAt(now);
                saveAndSendRealtime(looseSellerNotif);
            }

        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo CLOSED: " + e.getMessage());
        }
    }

    /**
     * Hàm trợ giúp: Tiết kiệm code, vừa lưu DB lẻ vừa check để bắn Socket real-time
     */
    private static void saveAndSendRealtime(BidNotification notif) {
        // 1. Lưu xuống DB
        notificationDao.add(notif);

        // 2. Quét tìm luồng Socket xem User nhận hiện tại có đang online không
        for (ClientHandler client : ClientManager.getActiveClients()) {
            if (client.getLoggedInUser() != null && client.getLoggedInUser().getId() == notif.getUserId()) {
                client.sendObject(notif);
                break;
            }
        }
    }
}