package auction.server.dao.impl;

import auction.common.message.BidUpdateNotification;
import auction.common.model.bid.Bid;
import auction.common.model.categories.Category;
import auction.common.model.items.AuctionItem;
import auction.common.model.items.Item;
import auction.common.model.items.ItemImage;
import auction.common.model.users.User;
import auction.server.ClientManager;
import auction.server.DatabaseManager;
import auction.server.dao.ItemDao;
import auction.server.utils.NotificationService;

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
                    else {
                        conn.rollback();
                        return false;
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
        item.setWeight(rs.getDouble("weight"));

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
                "WHERE ic.id_category = ? and i.status NOT IN ('UNAPPROVED', 'DELETED') ORDER BY i.created_time DESC";

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

    public Object[] getDashboardStats(){
        try (Connection conn= DatabaseManager.getInstance().getConnection()) {
            int liveAuctions=0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM ITEMS WHERE status = 'OPEN'")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) liveAuctions = rs.getInt(1);
            }

            double successRate=0.0;
            String rateSql = "SELECT COUNT(*), SUM(CASE WHEN id_current_bidder IS NOT NULL AND id_current_bidder > 0 THEN 1 ELSE 0 END) " +
                    "FROM items WHERE status IN ('CLOSED', 'DELETED')";
            try (PreparedStatement ps = conn.prepareStatement(rateSql)) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int totalClosed = rs.getInt(1);
                    int successfulClosed = rs.getInt(2);
                    successRate = (totalClosed == 0) ? 0.0 : ((double) successfulClosed / totalClosed) * 100.0;
                }
            }

            return new Object[]{liveAuctions,successRate};
        } catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateItem(Item item) {
        String sql = "UPDATE ITEMS SET name = ?, description = ?, start_price = ?, " +
                "start_time = ?, end_time = ?, length = ?, width = ?, height = ?,weight =?, current_price = ? " +
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
                pstmt.setDouble(9, item.getWeight());
                pstmt.setLong(10, item.getStartingPrice());
                pstmt.setInt(11, item.getId());

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
                long currentPrice = rs.getLong("current_price");

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
                String sqlWinner = "UPDATE users SET BALANCE = BALANCE - ?, frozen_balance = frozen_balance - ?, actual_expenses = actual_expenses + ? WHERE ID = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlWinner)) {
                    ps.setLong(1, finalPrice);
                    ps.setLong(2, finalPrice);
                    ps.setLong(3, finalPrice);
                    ps.setInt(4, winnerId);
                    ps.executeUpdate();
                }

                String sqlSeller = "UPDATE users SET BALANCE = BALANCE + ?, total_expenses = total_expenses + ? WHERE ID = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlSeller)) {
                    ps.setLong(1, finalPrice);
                    ps.setLong(2, finalPrice);
                    ps.setInt(3, sellerId);
                    ps.executeUpdate();
                }
            }

            conn.commit();

            Item item = new Item();
            item.setId(itemId);
            item.setName(itemName);
            item.setSellerId(sellerId);
            item.setCurrentPrice(finalPrice);

            return item;

        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }
    public long getTotalRevenueBySellerId(int sellerId) {
        long totalRevenue = 0;
        // Truy vấn trực tiếp trường total_expenses từ bảng users theo ID của Seller
        String sql = "SELECT total_expenses FROM users WHERE ID = ?";

        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, sellerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalRevenue = rs.getLong("total_expenses");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi truy vấn total_expenses cho Seller: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
        return totalRevenue;
    }

    @Override
    public boolean setupAutoBid(int itemId, int userId, long maxBid, long increment, String username) {
        String sqlInsertConfig = "INSERT INTO AUTOMATIC_BIDS (id_item, id_user, max_bid, increment) " +
                "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE max_bid = ?, increment = ?";

        boolean isConfigSaved = false;
        long currentPrice = 0;

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sqlInsertConfig)) {
                ps.setInt(1, itemId);
                ps.setInt(2, userId);
                ps.setLong(3, maxBid);
                ps.setLong(4, increment);
                ps.setLong(5, maxBid);
                ps.setLong(6, increment);
                ps.executeUpdate();
            }

            String sqlGetPrice = "SELECT current_price FROM ITEMS WHERE id = ? FOR UPDATE";
            try (PreparedStatement ps = conn.prepareStatement(sqlGetPrice)) {
                ps.setInt(1, itemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentPrice = rs.getLong("current_price");
                    }
                }
            }
            conn.commit();
            isConfigSaved = true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        if (isConfigSaved) {
            long initialBidAmount = currentPrice + increment;
            if (initialBidAmount <= maxBid) {

                boolean isBidSuccess = placeBid(itemId, initialBidAmount, userId);

                if (isBidSuccess) {
                    String sqlInsertBid = "INSERT INTO BIDS (id_item, id_user, bid_amount) VALUES (?, ?, ?)";
                    try (Connection conn = DatabaseManager.getInstance().getConnection();
                         PreparedStatement ps = conn.prepareStatement(sqlInsertBid)) {
                        ps.setInt(1, itemId);
                        ps.setInt(2, userId);
                        ps.setLong(3, initialBidAmount);
                        ps.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    // Lấy thông tin Item độc lập không lồng connection
                    Item updateItem = getItemById(itemId);
                    if (updateItem != null) {
                        // Gửi tin real-time đích danh phòng
                        ClientManager.broadcast(new BidUpdateNotification(
                                itemId,
                                initialBidAmount,
                                username,
                                LocalDateTime.now(),
                                updateItem.getEndTime()
                        ));

                        Bid bidRequest = new Bid();
                        bidRequest.setIdItem(itemId);
                        bidRequest.setIdUser(userId);
                        bidRequest.setBidderName(username);
                        bidRequest.setBidAmount(initialBidAmount);
                        NotificationService.handleSendMessageBid(updateItem, bidRequest, LocalDateTime.now());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void checkAndTriggerAutomaticBids(int itemId) {
        String sqlFindNextAuto = "SELECT a.id_user, a.max_bid, a.increment, u.username " +
                "FROM AUTOMATIC_BIDS a " +
                "JOIN users u ON a.id_user = u.id " +
                "WHERE a.id_item = ? AND a.id_user != ? " +
                "ORDER BY a.max_bid DESC, a.created_at ASC LIMIT 1";
        String sqlGetCurrentState = "SELECT current_price, id_current_bidder, status FROM ITEMS WHERE id = ?";

        while (true) {
            long currentPrice = 0;
            int idCurrentBidder = 0;
            String itemStatus = "";

            try ( Connection conn= DatabaseManager.getInstance().getConnection();
                    PreparedStatement ps = conn.prepareStatement(sqlGetCurrentState)) {
                ps.setInt(1, itemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentPrice = rs.getLong("current_price");
                        idCurrentBidder = rs.getInt("id_current_bidder");
                        itemStatus = rs.getString("status");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                break;
            }

            if (!"OPEN".equals(itemStatus)) {
                break;
            }
            int nextUserId = 0;
            long maxBid = 0;
            long increment = 0;
            String nextUsername = "";

            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlFindNextAuto)) {
                ps.setInt(1, itemId);
                ps.setInt(2, idCurrentBidder);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        nextUserId = rs.getInt("id_user");
                        maxBid = rs.getLong("max_bid");
                        increment = rs.getLong("increment");
                        nextUsername = rs.getString("username");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                break;
            }

            // Kết thúc vòng lặp khi không còn ai cạnh tranh giá
            if (nextUserId == 0) {
                break;
            }

            long nextBidAmount = currentPrice + increment;

            if (nextBidAmount <= maxBid) {
                boolean success = placeBid(itemId, nextBidAmount, nextUserId);

                if (success) {
                    // Lưu lịch sử đấu giá cho tài khoản chạy tự động ngầm
                    String insertBid = "INSERT INTO BIDS (id_item, id_user, bid_amount) VALUES (?, ?, ?)";
                    try (Connection conn= DatabaseManager.getInstance().getConnection();
                            PreparedStatement ps = conn.prepareStatement(insertBid)) {
                        ps.setInt(1, itemId);
                        ps.setInt(2, nextUserId);
                        ps.setLong(3, nextBidAmount);
                        ps.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    Item updateItem= getItemById(itemId);
                    if (updateItem != null) {
                        ClientManager.broadcast(new BidUpdateNotification(
                                itemId,
                                nextBidAmount,
                                nextUsername,
                                LocalDateTime.now(),
                                updateItem.getEndTime()
                        ));

                        Bid bidRequest = new Bid();
                        bidRequest.setIdItem(itemId);
                        bidRequest.setIdUser(nextUserId);
                        bidRequest.setBidderName(nextUsername);
                        bidRequest.setBidAmount(nextBidAmount);
                        NotificationService.handleSendMessageBid(updateItem,bidRequest,LocalDateTime.now());
                    }

                    String sqlCheckTime = "SELECT end_time FROM ITEMS WHERE id = ?";
                    try (Connection conn= DatabaseManager.getInstance().getConnection();
                            PreparedStatement psTime = conn.prepareStatement(sqlCheckTime)) {
                        psTime.setInt(1, itemId);
                        try (ResultSet rsTime = psTime.executeQuery()) {
                            if (rsTime.next()) {
                                Timestamp endTs = rsTime.getTimestamp("end_time");
                                if (endTs != null) {
                                    LocalDateTime endTime = endTs.toLocalDateTime();
                                    if (java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds() < 30) {
                                        LocalDateTime newEndTime = endTime.plusMinutes(2);
                                        String sqlUpdateBox = "UPDATE ITEMS SET end_time = ? WHERE id = ?";
                                        try (PreparedStatement psUp = conn.prepareStatement(sqlUpdateBox)) {
                                            psUp.setTimestamp(1, Timestamp.valueOf(newEndTime));
                                            psUp.setInt(2, itemId);
                                            psUp.executeUpdate();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                        // Thất bại (Ví dụ: hết tiền khả dụng thực tế) -> Hủy cấu hình
                        deleteAutoBidAndNotify(itemId, nextUserId, nextUsername, "Place bid failed! Canceled auto bid.", conn);
                        try { Thread.sleep(1000); } catch (InterruptedException ie) { break; }
                        break;
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try (Connection conn= DatabaseManager.getInstance().getConnection()) {
                    // Vượt quá mức Max Bid của người này -> Xóa cấu hình khỏi hàng đợi
                    deleteAutoBidAndNotify(itemId, nextUserId, nextUsername, "Giá sản phẩm (" + nextBidAmount + " $) đã vượt quá mức giá trần bạn cài đặt.", conn);
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { break; }
                    break;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void deleteAutoBidAndNotify(int itemId, int userId, String username, String reason, Connection conn) {
        try {
            boolean isDeleted= cancelAutoBid(itemId, userId);
            if (isDeleted) {
                System.out.println("[AUTO-BID CANCELLED] Gỡ cấu hình của User ID: " + userId);
                // Đẩy thông báo thời gian thực về Client
                auction.server.utils.NotificationService.sendAutoBidCancelledNotification(itemId, userId, reason, LocalDateTime.now());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean cancelAutoBid(int itemId, int userId) {
        String sql = "DELETE FROM AUTOMATIC_BIDS WHERE id_item = ? AND id_user = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            pstmt.setInt(2, userId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0; // Trả về true nếu thực sự có cấu hình bị xóa
        } catch (SQLException e) {
            System.err.println("Lỗi khi hủy Auto Bid từ User: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean checkAutoBidExists(int itemId, int userId) {
        String sql = "SELECT 1 FROM AUTOMATIC_BIDS WHERE id_item = ? AND id_user = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            pstmt.setInt(2, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Nếu có dòng trả về -> true (đã đặt), ngược lại -> false
            }
        } catch (SQLException e) {
            System.err.println("Lỗi check trạng thái AutoBid: " + e.getMessage());
            return false;
        }
    }
    @Override
    public List<User> getCustomersBySellerId(int sellerId) {
        List<User> customers = new ArrayList<>();

        // Câu lệnh SQL dựa trên cấu trúc bảng ITEMS thực tế của bạn
        String sql = "SELECT DISTINCT u.* FROM users u " +
                "JOIN ITEMS i ON u.ID = i.id_current_bidder " +
                "WHERE i.id_seller = ? AND i.status = 'CLOSED'";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sellerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Khởi tạo đối tượng User (Lưu ý: Tên cột 'ID', 'username'.. phải khớp với bảng users của bạn)
                    User user = new User(
                            rs.getInt("ID"),             // id lấy từ class cha Account hoặc users
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email"),
                            rs.getString("address"),
                            rs.getLong("balance"),
                            rs.getString("shippingPhone")
                    );

                    // Gán các thuộc tính mở rộng của class User như trong ảnh code của bạn
                    user.setFirstName(rs.getString("firstName"));
                    user.setLastName(rs.getString("lastName"));
                    user.setPhoneNumber(rs.getString("phoneNumber"));
                    user.setCountry(rs.getString("country"));
                    user.setLanguage(rs.getString("language"));

                    customers.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customers;
    }
}
