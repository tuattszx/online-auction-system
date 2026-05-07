package auction.server.dao.impl;

import auction.common.model.bid.Bid;
import auction.server.DatabaseManager;
import auction.server.dao.BidDao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class BidDaoImpl implements BidDao {

    @Override
    public boolean add(Bid bid){
        return addBid(bid);
    }

    @Override
    public Bid getById(Integer id) {
        String sql = "SELECT * FROM BIDS WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToBid(rs);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Bid> getAll() {

        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT * FROM BIDS ORDER BY bid_time DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) bids.add(mapResultSetToBid(rs));
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return bids;
    }

    @Override
    public boolean update(Bid bid) {
        throw new UnsupportedOperationException("Không được phép cập nhật lịch sử đấu giá!");
    }

    @Override
    public boolean delete(Integer id) {
        throw new UnsupportedOperationException("Không được phép xóa lịch sử đấu giá!");
    }

    public boolean addBid(Bid bid){
        String sql="INSERT INTO BIDS (id_item,id_user,bid_amount) VALUES (?,?,?)";
        try (Connection conn= DatabaseManager.getInstance().getConnection();
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

    private Bid mapResultSetToBid(ResultSet rs) throws SQLException {
        Bid bid = new Bid();
        bid.setId(rs.getInt("id"));
        bid.setIdItem(rs.getInt("id_item"));
        bid.setIdUser(rs.getInt("id_user"));
        bid.setBidAmount(rs.getLong("bid_amount"));

        java.sql.Timestamp ts = rs.getTimestamp("bid_time");
        if (ts != null) {
            bid.setBidTime(ts.toLocalDateTime());
        }
        bid.setBidderName(rs.getString("dis_name"));
        return bid;
    }

    @Override
    public List<Bid> getBidsByItemId(int itemId) {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT b.*, u.dis_name FROM BIDS b " +
                "JOIN users u ON b.id_user = u.ID " +
                "WHERE b.id_item = ? ORDER BY b.bid_time DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Bid bid = mapResultSetToBid(rs);
                    bids.add(bid);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bids;
    }

    @Override
    public List<Bid> getBidsByUserId(int userId) {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT * FROM BIDS WHERE id_user = ? ORDER BY bid_time DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
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
