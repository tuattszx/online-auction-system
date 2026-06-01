package auction.server.dao.impl;

import auction.common.model.items.Transaction;
import auction.server.DatabaseManager;
import auction.server.dao.TransactionDao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDaoImpl implements TransactionDao {

    public List<Transaction> getAll() {
        List<Transaction> transactions = new ArrayList<>();

        String sql = "SELECT t.id, t.id_item, t.id_seller, t.id_bidder, t.amount, t.created_at, " +
                "       i.name AS item_name, " +
                "       b.username AS bidder_name, " +
                "       s.username AS seller_name " +
                "FROM bid_transaction t " +
                "INNER JOIN items i ON t.id_item = i.id " +
                "INNER JOIN users b ON t.id_bidder = b.id " +
                "INNER JOIN users s ON t.id_seller = s.id " +
                "ORDER BY t.created_at DESC";

        // Thực thi kết nối bằng DatabaseManager của bạn
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                // Gọi hàm map dữ liệu cho từng dòng kết quả
                Transaction trans = mapResultSetToTransaction(rs);
                transactions.add(trans);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return transactions;
    }

    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        Transaction transaction = new Transaction();

        transaction.setId(rs.getInt("id"));
        transaction.setItemId(rs.getInt("id_item"));
        transaction.setUserId(rs.getInt("id_bidder"));
        transaction.setReceiverId(rs.getInt("id_seller"));

        transaction.setItemName(rs.getString("item_name"));
        transaction.setUserName(rs.getString("bidder_name"));
        transaction.setReceiverName(rs.getString("seller_name"));

        transaction.setAmount(rs.getLong("amount"));

        Timestamp time = rs.getTimestamp("created_at");
        if (time != null) {
            transaction.setTime(time.toLocalDateTime());
        }
        return transaction;
    }
}
