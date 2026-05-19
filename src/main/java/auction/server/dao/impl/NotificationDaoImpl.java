package auction.server.dao.impl;

import auction.common.model.notifications.BidNotification;
import auction.common.model.notifications.ItemNotification;
import auction.common.model.notifications.Notification;
import auction.server.DatabaseManager;
import auction.server.dao.NotificationDAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NotificationDaoImpl implements NotificationDAO {

    @Override
    public List<Notification> getNotificationsByUserId(int userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM NOTIFICATIONS WHERE user_id = ? ORDER BY id DESC";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("type");
                    Notification notif = null;

                    // Đa hình bóc tách Object con dựa vào cột type
                    if ("BID_ACTION".equals(type)) {
                        notif = new BidNotification(
                                rs.getInt("id"),
                                rs.getInt("user_id"),
                                rs.getString("title"),
                                rs.getString("message"),
                                rs.getInt("item_id"),
                                rs.getLong("new_price"),
                                rs.getString("bidder_name")
                        );
                    } else if ("ITEM_STATUS".equals(type)) {
                        notif = new ItemNotification(
                                rs.getInt("id"),
                                rs.getInt("user_id"),
                                rs.getString("title"),
                                rs.getString("message"),
                                rs.getInt("item_id"),
                                rs.getString("item_status"),
                                rs.getString("admin_note")
                        );
                    }

                    if (notif != null) {
                        notif.setRead(rs.getBoolean("is_read"));
                        notif.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                        list.add(notif);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public boolean insertNotificationsBatch(Set<Integer> userIds, Notification notification) {
        String sql = "INSERT INTO NOTIFICATIONS (user_id, type, title, message, is_read, item_id, new_price, bidder_name, item_status, admin_note) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false); // Bật Batch Update

            for (int userId : userIds) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, notification.getType());
                pstmt.setString(3, notification.getTitle());
                pstmt.setString(4, notification.getMessage());
                pstmt.setBoolean(5, notification.isRead());

                if (notification instanceof BidNotification bidNotif) {
                    pstmt.setInt(6, bidNotif.getItemId());
                    pstmt.setLong(7, bidNotif.getNewPrice());
                    pstmt.setString(8, bidNotif.getBidderName());
                    pstmt.setNull(9, Types.VARCHAR);
                    pstmt.setNull(10, Types.VARCHAR);
                } else if (notification instanceof ItemNotification itemNotif) {
                    pstmt.setInt(6, itemNotif.getItemId());
                    pstmt.setNull(7, Types.BIGINT);
                    pstmt.setNull(8, Types.VARCHAR);
                    pstmt.setString(9, itemNotif.getItemStatus());
                    pstmt.setString(10, itemNotif.getAdminNote());
                } else {
                    pstmt.setNull(6, Types.INTEGER);
                    pstmt.setNull(7, Types.BIGINT);
                    pstmt.setNull(8, Types.VARCHAR);
                    pstmt.setNull(9, Types.VARCHAR);
                    pstmt.setNull(10, Types.VARCHAR);
                }
                pstmt.addBatch();
            }

            pstmt.executeBatch();
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean markAsRead(int notificationId) {
        String sql = "UPDATE NOTIFICATIONS SET is_read = 1 WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, notificationId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int countUnreadByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM NOTIFICATIONS WHERE user_id = ? AND is_read = 0";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public boolean add(Notification notification) {
        // Tận dụng chính hàm Batch chạy cho 1 phần tử đơn lẻ
        return insertNotificationsBatch(Set.of(notification.getUserId()), notification);
    }

    @Override
    public Notification getById(Integer id){
        return null;
    }

    @Override
    public List<Notification> getAll() {
        return new ArrayList<>();
    }

    @Override
    public boolean update(Notification notification) {
        return false;
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM NOTIFICATIONS WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
