package auction.server.dao;

import auction.common.model.bid.Bid;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static auction.server.DatabaseManager.getConnection;

public class BidDao {
    public static boolean addBid(Bid bid){
        String sql="INSERT INTO BIDS (id_item,id_user,bid_amount) VALUES (?,?,?)";
        try (Connection conn= getConnection();
             PreparedStatement pstmt=conn.prepareStatement(sql)){
            pstmt.setInt(1,bid.getIdItem());
            pstmt.setInt(2,bid.getIdUser());
            pstmt.setLong(3,bid.getBidAmount());

            return pstmt.executeUpdate()>0;
        }
        catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    private static Bid mapResultSetToBid(ResultSet rs) throws SQLException {
        Bid bid = new Bid();
        bid.setId(rs.getInt("id"));
        bid.setIdItem(rs.getInt("id_item"));
        bid.setIdUser(rs.getInt("id_user"));
        bid.setBidAmount(rs.getLong("bid_amount"));
        
        java.sql.Timestamp ts = rs.getTimestamp("bid_time");
        if (ts != null) {
            bid.setBidTime(ts.toLocalDateTime());
        }
        return bid;
    }

    public static List<Bid> getBidsByItemId(int itemId) {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT * FROM BIDS WHERE id_item = ? ORDER BY bid_time DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapResultSetToBid(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bids;
    }

    public static List<Bid> getBidsByUserId(int userId) {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT * FROM BIDS WHERE id_user = ? ORDER BY bid_time DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapResultSetToBid(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bids;
    }

}