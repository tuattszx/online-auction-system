package auction.server.dao;


import auction.common.model.categories.Category;
import auction.common.model.items.Item;
import auction.common.model.items.ItemImage;

import java.sql.*;
import java.util.*;

import static auction.server.DatabaseManager.getConnection;

public class ItemDao {
    private static void insertImages(int itemId, List<ItemImage> images, Connection conn) throws SQLException {
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

    private static void insertCategories(int itemId, List<Category> categories, Connection conn) throws SQLException {
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

    public static boolean addItem(Item item) throws SQLException {
        String sql = "INSERT INTO ITEMS (name, description, start_price, current_price, start_time, end_time, id_seller, length, width, height) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = getConnection();
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

    private static List<ItemImage> getImagesByItemId(int itemId, Connection conn) throws SQLException {
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

    private static List<Category> getCategoriesByItemId(int itemId, Connection conn) throws SQLException {
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

    private static Item mapResultSetToItem(ResultSet rs,Connection conn) throws SQLException {
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

    public static List<Item> getAllItems() {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM ITEMS ORDER BY created_time DESC";

        try (Connection conn = getConnection();
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

    public static Item getItemById(int id){
        String sql="SELECT * FROM ITEMS WHERE id=?";
        try (Connection conn = getConnection();
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

    public static List<Item> getItemsByCategory(int catId) {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT i.* FROM ITEMS i " +
                "JOIN ITEM_CATEGORIES ic ON i.id = ic.id_item " +
                "WHERE ic.id_category = ? ORDER BY i.created_time DESC";

        try (Connection conn = getConnection();
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

    public static List<Item> searchItems(String keyword) {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT * FROM ITEMS WHERE (name LIKE ? OR description LIKE ?) " +
                "AND  status = 'OPEN' ORDER BY created_time DESC";

        try (Connection conn = getConnection();
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

    public static boolean updateCurrentPrice(int itemId, long newPrice) {
        String sql = "UPDATE ITEMS SET current_price = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, newPrice);
            pstmt.setInt(2, itemId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi cập nhật giá Item: " + e.getMessage());
            return false;
        }
    }

    public static boolean updateStatus(int itemId, String status) {
        String sql = "UPDATE ITEMS SET status = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, itemId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteItem(int id){
        return  updateStatus(id,"DELETED");
    }

    public static boolean updateItem(Item item) {
        String sql = "UPDATE ITEMS SET name = ?, description = ?, start_price = ?, " +
                "start_time = ?, end_time = ?, length = ?, width = ?, height = ?, current_price = ? " +
                "WHERE id = ?";

        Connection conn = null;
        try {
            conn = getConnection();
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

                pstmt.executeUpdate();
            }

            try (PreparedStatement delCat = conn.prepareStatement("DELETE FROM ITEM_CATEGORIES WHERE id_item = ?")) {
                delCat.setInt(1, item.getId());
                delCat.executeUpdate();
            }
            insertCategories(item.getId(), item.getCategories(), conn);

            try (PreparedStatement delImg = conn.prepareStatement("DELETE FROM images WHERE id_item = ?")) {
                delImg.setInt(1, item.getId());
                delImg.executeUpdate();
            }
            insertImages(item.getId(), item.getImages(), conn);

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

    public static List<Item> getItemsBySeller(int sellerId) {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM ITEMS WHERE id_seller = ? ORDER BY created_time DESC";

        try (Connection conn = getConnection();
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

    public static boolean placeBid(int idItem,long newPrice,int id_bidder){
        String sql="UPDATE ITEMS SET current_price=?, id_current_bidder=? WHERE id=? and current_price<?";

        try(Connection conn=getConnection();
            PreparedStatement pstmt= conn.prepareStatement(sql)) {

            pstmt.setLong(1,newPrice);
            pstmt.setInt(2, id_bidder);
            pstmt.setInt(3,idItem);
            pstmt.setLong(4,newPrice);

            return pstmt.executeUpdate()>0;
        }
        catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }
}