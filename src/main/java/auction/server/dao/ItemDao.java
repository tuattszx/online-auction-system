package auction.server.dao;


import auction.common.model.items.Item;

import java.sql.*;
import java.util.*;

import static auction.server.DatabaseManager.getConnection;

public class ItemDao {
    public static boolean addItem(Item item) throws SQLException {
        String sql= "INSERT INTO ITEMS (name, description, start_price, current_price, start_time, end_time, id_seller) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try(Connection conn= getConnection();
            PreparedStatement pstmt=conn.prepareStatement(sql)){

            pstmt.setString(1,item.getName());
            pstmt.setString(2, item.getDescription());
            pstmt.setLong(3, item.getStartingPrice());
            pstmt.setLong(4, item.getStartingPrice());
            pstmt.setTimestamp(5, item.getStartTime() != null ? Timestamp.valueOf(item.getStartTime()) : null);
            pstmt.setTimestamp(6, item.getEndTime() != null ? Timestamp.valueOf(item.getEndTime()) : null);
            pstmt.setInt(7, item.getSellerId());

            return pstmt.executeUpdate() > 0;
        }
        catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    private static Item mapResultSetToItem(ResultSet rs) throws SQLException {
        Item item = new Item();

        item.setId(rs.getInt("id"));
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

        int categoryId=rs.getInt("id_category");
        if (categoryId >0) item.setCategoryId(categoryId);

        return item;
    }

    public static List<Item> getAllItems() {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM ITEMS ORDER BY created_time DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Item item = mapResultSetToItem(rs);
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
                    return mapResultSetToItem(rs);
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
        String sql = "SELECT * FROM ITEMS WHERE id_category = ? ORDER BY created_time DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, catId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToItem(rs));
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
                    list.add(mapResultSetToItem(rs));
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
        String sql = "UPDATE ITEMS SET name = ?, description = ?,start_price=?, id_category = ?,start_time=?, end_time = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getDescription());
            pstmt.setLong(3, item.getStartingPrice());
            pstmt.setInt(4, item.getCategoryId());
            pstmt.setTimestamp(5,item.getStartTime() != null ? Timestamp.valueOf(item.getStartTime()) : null);
            pstmt.setTimestamp(6, item.getEndTime() != null ? Timestamp.valueOf(item.getEndTime()) : null);
            pstmt.setInt(7, item.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
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
                    Item item = mapResultSetToItem(rs);
                    itemList.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy danh sách sản phẩm của người bán (ID: " + sellerId + "): " + e.getMessage());
            e.printStackTrace();
        }

        return itemList;
    }

}