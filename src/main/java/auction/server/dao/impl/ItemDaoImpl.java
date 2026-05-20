package auction.server.dao.impl;

import auction.common.model.categories.Category;
import auction.common.model.items.AuctionItem;
import auction.common.model.items.Item;
import auction.common.model.items.ItemImage;
import auction.server.DatabaseManager;
import auction.server.dao.ItemDao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class ItemDaoImpl implements ItemDao {

    @Override
    public Item getById(Integer id){
        return getItemById(id);
    }

    @Override
    public List<Item> getAll(){
        return getAllItems();
    }

    @Override
    public boolean add(Item item) {
        try {
            return addItem(item);
        }
        catch (Exception e){
            return false;
        }
    }

    @Override
    public boolean update(Item newItem){
        return updateItem(newItem);
    }

    @Override
    public boolean delete(Integer id){
        return  deleteItem(id);
    }

    private void insertImages(int itemId, List<ItemImage> images, Connection conn) throws SQLException {
        if (images == null || images.isEmpty()) return;

        String sqlImg = "INSERT INTO images (id_item, url_image, is_default) VALUES (?, ?, ?)";
        try (PreparedStatement pImg = conn.prepareStatement(sqlImg)) {
            for (ItemImage img : images) {
                pImg.setInt(1, itemId);
                pImg.setString(2, img.getUrlImage());
                pImg.setBoolean(3, img.isDefault()); // Lưu 0 hoặc 1 vào tinyint
                pImg.addBatch();
            }
            pImg.executeBatch();
        }
    }

    private void insertCategories(int itemId, List<Category> categories, Connection conn) throws SQLException {
        if (categories == null || categories.isEmpty()) return;

        String sqlCat = "INSERT INTO ITEM_CATEGORIES (id_item, id_category) VALUES (?, ?)";
        try (PreparedStatement pCat = conn.prepareStatement(sqlCat)) {
            for (Category cat : categories) {
                pCat.setInt(1, itemId);
                pCat.setInt(2, cat.getId());
                pCat.addBatch();
            }
            pCat.executeBatch();
        }
    }

    public boolean addItem(Item item) throws SQLException {
        String sql = "INSERT INTO ITEMS (name, description, start_price, current_price, start_time, end_time, id_seller, length, width, height) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, item.getName());
                pstmt.setString(2, item.getDescription());
                pstmt.setLong(3, item.getStartingPrice());
                pstmt.setLong(4, item.getStartingPrice());
                pstmt.setTimestamp(5, item.getStartTime() != null ? Timestamp.valueOf(item.getStartTime()) : null);
                pstmt.setTimestamp(6, item.getEndTime() != null ? Timestamp.valueOf(item.getEndTime()) : null);
                pstmt.setInt(7, item.getSellerId());
                pstmt.setDouble(8, item.getLength());
                pstmt.setDouble(9, item.getWidth());
                pstmt.setDouble(10, item.getHeight());

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    conn.rollback();
                    return false;
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int newItemId = generatedKeys.getInt(1);
                        item.setId(newItemId);

                        insertImages(newItemId, item.getImages(), conn);
                        insertCategories(newItemId, item.getCategories(), conn);
                    }
                }
                conn.commit();
                return true;
            }
        } catch (SQLException e) {
            if (conn != null) {
                System.err.println("Lỗi hệ thống, đang hoàn tác dữ liệu (Rollback)...");
                conn.rollback();
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    private List<ItemImage> getImagesByItemId(int itemId, Connection conn) throws SQLException {
        List<ItemImage> images = new ArrayList<>();
        String sql = "SELECT * FROM images WHERE id_item = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    images.add(new ItemImage(rs.getInt("id"), rs.getInt("id_item"),
                            rs.getString("url_image"), rs.getBoolean("is_default")));
                }
            }
        }
        return images;
    }

    private List<Category> getCategoriesByItemId(int itemId, Connection conn) throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT c.* FROM CATEGORIES c JOIN ITEM_CATEGORIES ic ON c.id = ic.id_category WHERE ic.id_item = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    categories.add(new Category(rs.getInt("id"), rs.getString("name"), rs.getString("description")));
                }
            }
        }
        return categories;
    }

    private Item mapResultSetToItem(ResultSet rs,Connection conn) throws SQLException {
        Item item = new Item();

        int id=rs.getInt("id");
        item.setId(id);
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));

        item.setStartingPrice(rs.getLong("start_price"));
        item.setCurrentPrice(rs.getLong("current_price"));

        Timestamp createTs = rs.getTimestamp("created_time");
        if (createTs != null) item.setCreatedTime(createTs.toLocalDateTime());

        Timestamp startTs = rs.getTimestamp("start_time");
        if (startTs != null) item.setStartTime(startTs.toLocalDateTime());

        Timestamp endTs = rs.getTimestamp("end_time");
        if (endTs != null) item.setEndTime(endTs.toLocalDateTime());

        item.setSellerId(rs.getInt("id_seller"));
        item.setStatus(rs.getString("status"));

        item.setLength(rs.getDouble("length"));
        item.setWidth(rs.getDouble("width"));
        item.setHeight(rs.getDouble("height"));

        int bidderId = rs.getInt("id_current_bidder");
        if (rs.wasNull()) {
            item.setCurrentBidderId(null);
        } else {
            item.setCurrentBidderId(bidderId);
        }

        item.setImages(getImagesByItemId(id, conn));
        item.setCategories(getCategoriesByItemId(id, conn));

        return item;
    }

    public List<Item> getAllItems() {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM ITEMS WHERE status NOT IN ('UNAPPROVED', 'DELETED') ORDER BY created_time DESC";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Item item = mapResultSetToItem(rs,conn);
                itemList.add(item);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy danh sách Item: " + e.getMessage());
            e.printStackTrace();
        }

        return itemList;
    }

    public Item getItemById(int id){
        String sql="SELECT * FROM ITEMS WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1,id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToItem(rs,conn);
                }
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Item> getItemsByCategory(int catId) {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT i.* FROM ITEMS i " +
                "JOIN ITEM_CATEGORIES ic ON i.id = ic.id_item " +
                "WHERE ic.id_category = ? ORDER BY i.created_time DESC";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, catId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToItem(rs, conn));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lọc hàng theo danh mục: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Item> searchItems(String keyword) {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT * FROM ITEMS WHERE (name LIKE ? OR description LIKE ?) " +
                "AND  status = 'OPEN' ORDER BY created_time DESC";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToItem(rs,conn));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi tìm kiếm Item: " + e.getMessage());
        }
        return list;
    }

    @Override
    public boolean updateCurrentPrice(int itemId, long newPrice) {
        String sql = "UPDATE ITEMS SET current_price = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, newPrice);
            pstmt.setInt(2, itemId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi cập nhật giá Item: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateStatus(int itemId, String status) {
        String sql = "UPDATE ITEMS SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, itemId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteItem(int id){
        return  updateStatus(id,"DELETED");
    }

    public boolean updateItem(Item item) {
        String sql = "UPDATE ITEMS SET name = ?, description = ?, start_price = ?, " +
                "start_time = ?, end_time = ?, length = ?, width = ?, height = ?, current_price = ? " +
                "WHERE id = ? AND status = 'UNAPPROVED'";

        Connection conn = null;
        int affectedRows=0;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, item.getName());
                pstmt.setString(2, item.getDescription());
                pstmt.setLong(3, item.getStartingPrice());
                pstmt.setTimestamp(4, item.getStartTime() != null ? Timestamp.valueOf(item.getStartTime()) : null);
                pstmt.setTimestamp(5, item.getEndTime() != null ? Timestamp.valueOf(item.getEndTime()) : null);
                pstmt.setDouble(6, item.getLength());
                pstmt.setDouble(7, item.getWidth());
                pstmt.setDouble(8, item.getHeight());
                pstmt.setLong(9, item.getStartingPrice());
                pstmt.setInt(10, item.getId());

                affectedRows=pstmt.executeUpdate();
            }

            if (affectedRows == 0) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement delCat = conn.prepareStatement("DELETE FROM ITEM_CATEGORIES WHERE id_item = ?")) {
                delCat.setInt(1, item.getId());
                delCat.executeUpdate();
            }
            insertCategories(item.getId(), item.getCategories(), conn);

            if (item.getImages() != null && !item.getImages().isEmpty()) {
                try (PreparedStatement delImg = conn.prepareStatement("DELETE FROM images WHERE id_item = ?")) {
                    delImg.setInt(1, item.getId());
                    delImg.executeUpdate();
                }
                insertImages(item.getId(), item.getImages(), conn);
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    @Override
    public List<Item> getItemsBySeller(int sellerId) {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM ITEMS WHERE id_seller = ? ORDER BY created_time DESC";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sellerId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Item item = mapResultSetToItem(rs,conn);
                    itemList.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy danh sách sản phẩm của người bán (ID: " + sellerId + "): " + e.getMessage());
            e.printStackTrace();
        }

        return itemList;
    }

    @Override
    public boolean placeBid(int idItem, long newPrice, int id_bidder) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);

            // 1. Lấy thông tin hiện tại của Item
            String sqlItem = "SELECT current_price, id_current_bidder FROM ITEMS WHERE id = ? FOR UPDATE";
            long oldPrice = 0;
            Integer oldBidderId = null;

            try (PreparedStatement psItem = conn.prepareStatement(sqlItem)) {
                psItem.setInt(1, idItem);
                try (ResultSet rs = psItem.executeQuery()) {
                    if (rs.next()) {
                        oldPrice = rs.getLong("current_price");
                        oldBidderId = rs.getInt("id_current_bidder");
                        if (rs.wasNull()) oldBidderId = null;
                    } else {
                        conn.rollback();
                        return false; // Item không tồn tại
                    }
                }
            }

            // Kiểm tra xem giá mới có thực sự cao hơn giá cũ không (tránh race condition)
            if (newPrice <= oldPrice) {
                conn.rollback();
                return false;
            }

            // 2. Kiểm tra số dư của người đặt giá mới
            String sqlCheckBalance = "SELECT BALANCE, frozen_balance FROM users WHERE ID = ? FOR UPDATE";
            try (PreparedStatement psCheck = conn.prepareStatement(sqlCheckBalance)) {
                psCheck.setInt(1, id_bidder);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        long balance = rs.getLong("BALANCE");
                        long frozen = rs.getLong("frozen_balance");
                        if (newPrice > (balance - frozen)) {
                            System.out.println("User " + id_bidder + " không đủ tiền khả dụng!");
                            conn.rollback();
                            return false;
                        }
                    }
                }
            }

            // 3. Giải phóng tiền cho người bị đè giá
            if (oldBidderId != null && oldBidderId != id_bidder) {
                String sqlUnfreeze = "UPDATE users SET frozen_balance = frozen_balance - ? WHERE ID = ?";
                try (PreparedStatement psUnfreeze = conn.prepareStatement(sqlUnfreeze)) {
                    psUnfreeze.setLong(1, oldPrice);
                    psUnfreeze.setInt(2, oldBidderId);
                    psUnfreeze.executeUpdate();
                }
            }

            // 4. Đóng băng tiền cho người mới
            // Lưu ý: Nếu người mới cũng là người cũ (tự nâng giá mình), ta chỉ đóng băng phần chênh lệch
            String sqlFreeze = "UPDATE users SET frozen_balance = frozen_balance + ? WHERE ID = ?";
            try (PreparedStatement psFreeze = conn.prepareStatement(sqlFreeze)) {
                long amountToFreeze = (oldBidderId != null && oldBidderId == id_bidder)
                        ? (newPrice - oldPrice) : newPrice;
                psFreeze.setLong(1, amountToFreeze);
                psFreeze.setInt(2, id_bidder);
                psFreeze.executeUpdate();
            }

            // 5. Cập nhật thông tin món hàng
            String sqlUpdateItem = "UPDATE ITEMS SET current_price = ?, id_current_bidder = ? WHERE id = ?";
            try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdateItem)) {
                psUpdate.setLong(1, newPrice);
                psUpdate.setInt(2, id_bidder);
                psUpdate.setInt(3, idItem);
                int updated = psUpdate.executeUpdate();
                if (updated == 0) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit(); // HOÀN TẤT TRANSACTION
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    public List<Item> getUnapprovedItems() {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM ITEMS WHERE status = 'UNAPPROVED' ORDER BY created_time DESC";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Item item = mapResultSetToItem(rs, conn);
                itemList.add(item);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching unapproved items: " + e.getMessage());
            e.printStackTrace();
        }

        return itemList;
    }

    public boolean approveItem(int itemId, boolean isApproved) {
        if (isApproved) {
            return updateStatus(itemId, "PENDING");
        } else {
            return deleteItem(itemId);
        }
    }

    public boolean updateEndTime(int id, LocalDateTime newEndTime) {
        String sql = "UPDATE ITEMS SET end_time = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(newEndTime));
            pstmt.setInt(2, id);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating end time for item ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<AuctionItem> getMyAuctions(int userId) {
        List<AuctionItem> list = new ArrayList<>();
        // Câu truy vấn lấy thông tin Item và mức giá cao nhất người dùng này từng trả
        String sql = "SELECT i.id, i.name, i.current_price, i.end_time, " +
                "MAX(b.bid_amount) as your_max_bid " +
                "FROM ITEMS i " +
                "JOIN BIDS b ON i.id = b.id_item " +
                "WHERE b.id_user = ? " +
                "GROUP BY i.id, i.name, i.current_price, i.end_time";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    long currentPrice = rs.getLong("current_price");
                    long yourMaxBid = rs.getLong("your_max_bid");
                    LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();

                    // 1. Logic xác định trạng thái đơn giản:
                    // Nếu giá hiện tại của món hàng đúng bằng giá mình đã trả -> Winning
                    String status = (currentPrice <= yourMaxBid) ? "Winning" : "Losing";

                    // 2. Tính toán thời gian còn lại (Dạng String để hiển thị lên bảng)
                    String remaining = calculateRemainingTime(endTime);
                    list.add(new AuctionItem(
                            id,
                            name,
                            remaining,
                            (long) currentPrice,
                            (long) yourMaxBid,
                            status
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy danh sách đấu giá của tôi: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // Hàm phụ trợ để tính thời gian còn lại
    private String calculateRemainingTime(LocalDateTime endTime) {
        java.time.Duration duration = java.time.Duration.between(LocalDateTime.now(), endTime);
        if (duration.isNegative() || duration.isZero()) {
            return "Closed";
        }
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void processExpiredItems() {
        String sqlSelect = "SELECT id FROM ITEMS WHERE end_time <= NOW() AND (status = 'OPEN' OR status = 'PENDING')";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlSelect);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int itemId = rs.getInt("id");
                System.out.println("Đang kết toán cho sản phẩm ID: " + itemId);

                Item closedItem = finalizeAuctionAndGetItem(itemId);

                if (closedItem != null) {
                    auction.server.utils.NotificationService.sendClosedNotifications(closedItem, java.time.LocalDateTime.now());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void processIncomingItems() {
        String sqlSelect = "SELECT id, name, id_seller, current_price FROM ITEMS WHERE start_time <= NOW() AND status = 'PENDING'";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlSelect);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int itemId = rs.getInt("id");
                String itemName = rs.getString("name");
                int sellerId = rs.getInt("id_seller");
                int currentPrice = rs.getInt("current_price");

                String sqlLock = "UPDATE ITEMS SET status = 'OPEN' WHERE id = ? AND status = 'PENDING'";
                int rowsAffected = 0;

                try (PreparedStatement psLock = conn.prepareStatement(sqlLock)) {
                    psLock.setInt(1, itemId);
                    rowsAffected = psLock.executeUpdate();
                }

                if (rowsAffected > 0) {
                    Item item = new Item();
                    item.setId(itemId);
                    item.setName(itemName);
                    item.setSellerId(sellerId);
                    item.setCurrentPrice(currentPrice);

                    auction.server.utils.NotificationService.sendOpenNotifications(item, java.time.LocalDateTime.now());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Hàm finalizeAuction thực hiện trừ BALANCE và frozen_balance
    public Item finalizeAuctionAndGetItem(int itemId) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);

            String sqlLock = "UPDATE ITEMS SET status = 'CLOSED' WHERE id = ? AND (status = 'OPEN' OR status = 'PENDING')";
            int rowsAffected = 0;
            try (PreparedStatement ps = conn.prepareStatement(sqlLock)) {
                ps.setInt(1, itemId);
                rowsAffected = ps.executeUpdate();
            }

            if (rowsAffected == 0) {
                conn.rollback();
                return null;
            }

            String sqlInfo = "SELECT name, id_current_bidder, current_price, id_seller FROM ITEMS WHERE id = ?";
            String itemName = "";
            int winnerId = 0, sellerId = 0;
            long finalPrice = 0;

            try (PreparedStatement ps = conn.prepareStatement(sqlInfo)) {
                ps.setInt(1, itemId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    itemName = rs.getString("name");
                    winnerId = rs.getInt("id_current_bidder");
                    finalPrice = rs.getLong("current_price");
                    sellerId = rs.getInt("id_seller");
                }
            }

            if (winnerId != 0) {
                String sqlWinner = "UPDATE users SET BALANCE = BALANCE - ?, frozen_balance = frozen_balance - ? WHERE ID = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlWinner)) {
                    ps.setLong(1, finalPrice);
                    ps.setLong(2, finalPrice);
                    ps.setInt(3, winnerId);
                    ps.executeUpdate();
                }

                String sqlSeller = "UPDATE users SET BALANCE = BALANCE + ? WHERE ID = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlSeller)) {
                    ps.setLong(1, finalPrice);
                    ps.setInt(2, sellerId);
                    ps.executeUpdate();
                }
            }

            conn.commit();

            Item item = new Item();
            item.setId(itemId);
            item.setName(itemName);
            item.setSellerId(sellerId);
            item.setCurrentPrice((int) finalPrice);

            return item;

        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }
}
