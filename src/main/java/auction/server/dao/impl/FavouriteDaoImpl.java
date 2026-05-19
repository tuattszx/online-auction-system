package auction.server.dao.impl;

import auction.server.DatabaseManager;
import auction.server.dao.FavouriteDao;

import javax.xml.transform.Result;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class FavouriteDaoImpl implements FavouriteDao {

    @Override
    public boolean addFavourite(int userId, int itemId) {
        String sql= "INSERT IGNORE INTO user_favourites (user_id,item_id) VALUES (?,?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement pstmt= conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, itemId);
            return pstmt.executeUpdate() > 0;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeFavourite(int userId, int itemId) {
        String sql= "DELETE FROM user_favourites WHERE user_id = ? AND item_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt= conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, itemId);
            return pstmt.executeUpdate() > 0;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

     @Override
     public List<Integer> getFavoriteItemIdsByUserId(int userId){
        String sql = "SELECT item_id FROM user_favourites WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Integer> itemIds = new ArrayList<>();
                while (rs.next()) {
                    itemIds.add(rs.getInt("item_id"));
                }
                return itemIds;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
     }

     @Override
    public List<Integer> getUserIdsByFavoriteItem(int itemId){
        String sql= "SELECT user_id FROM user_favourites WHERE item_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt= conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Integer> userIds = new ArrayList<>();
                while (rs.next()) {
                    userIds.add(rs.getInt("user_id"));
                }
                return userIds;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
     }
}
