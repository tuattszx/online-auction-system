package auction.server.dao.impl;

import auction.common.model.users.User;
import auction.server.DatabaseManager;
import auction.server.dao.UserDao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class UserDaoImpl implements UserDao {

    @Override
    public boolean add(User user) {
        return registerUser(user);
    }

    @Override
    public List<User> getAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return users;
    }

    @Override
    public User getById(Integer id) {
        String sql="SELECT * FROM users WHERE id=?";
        try (Connection conn= DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt=conn.prepareStatement(sql)) {

            pstmt.setInt(1,id);

            try(ResultSet rs =pstmt.executeQuery()) {
                if (rs.next()){
                    return mapResultSetToUser(rs);
                }
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean update(User user) {
        return updateProfile(
                user.getId(),
                user.getDisplayName(),
                user.getFirstName(),    // Thêm trường mới
                user.getLastName(),     // Thêm trường mới
                user.getEmail(),
                user.getAddress(),      // Trường address có sẵn (dùng làm Delivery Address)
                user.getPhoneNumber(),
                user.getCountry()    ,// Thêm trường mới
                user.getShippingPhone(),
                user.getCardHolderName(), // Lấy từ object user
                user.getCardNumber()      // Lấy từ object user
        );
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    @Override
    public User CheckLogin(String userName, String password){
        String sql="SELECT * FROM users WHERE username=? AND password=?";
        try(Connection conn=DatabaseManager.getInstance().getConnection();
            PreparedStatement pstmt= conn.prepareStatement(sql)) {
            pstmt.setString(1, userName);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToUser(rs);
            }
        }
        catch (SQLException e){
            System.err.println("Lỗi checkLogin: " + e.getMessage());
        }
        return null;
    }

    public boolean registerUser(User user) {
        String sql = "INSERT INTO users (username, password, email,dis_name,phone_number) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4,user.getDisplayName());
            pstmt.setString(5,user.getPhoneNumber());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateBalance(int id,long amount){
        String sql= "UPDATE users SET balance = balance + ? WHERE ID = ?";
        try (Connection conn= DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt= conn.prepareStatement(sql)){

            pstmt.setLong(1,amount);
            pstmt.setInt(2,id);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();

        user.setId(rs.getInt("ID"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setBalance(rs.getLong("balance"));
        user.setRole(rs.getString("ROLE"));
        user.setAddress(rs.getString("ADDRESS"));
        user.setDisplayName(rs.getString("dis_name"));
        user.setPhoneNumber(rs.getString("phone_number"));


        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            user.setCreatTime(timestamp.toLocalDateTime());
        }
        return user;
    }

    @Override
    public long getBalance(int id) {
        User user = getById(id);
        return (user != null) ? user.getBalance() : -1;
    }

    @Override
    public boolean isUsernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE USERNAME = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) { return false; }
    }

    @Override
    public boolean isEmailExists(String email) {
        String sql = "SELECT 1 FROM users WHERE EMAIL = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) { return false; }
    }

    @Override
    public boolean updatePassword(int userId, String newPassword) {
        String sql = "UPDATE users SET PASSWORD = ? WHERE ID = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPassword);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi updatePassword: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateProfile(int userId, String newDisName, String newFirstName, String newLastName,
                                 String newEmail, String newAddress, String newPhoneNumber, String newCountry, String newshippingPhone,String cardName, String cardNum) {

        // Câu lệnh UPDATE map chuẩn xác 7 trường thông tin thông qua ID
        String sql = "UPDATE users SET dis_name = ?, first_name = ?, last_name = ?, EMAIL = ?, "
                + "ADDRESS = ?, phone_number = ?, country = ?, shipping_phone = ?, "
                + "card_holder_name = ?, card_number = ? WHERE ID = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newDisName);
            pstmt.setString(2, newFirstName);
            pstmt.setString(3, newLastName);
            pstmt.setString(4, newEmail);
            pstmt.setString(5, newAddress); // Sử dụng giá trị nhập từ ô Delivery Address
            pstmt.setString(6, newPhoneNumber);
            pstmt.setString(7, newCountry);
            pstmt.setString(8,newshippingPhone);
            pstmt.setString(9, cardName);  // Cột card_holder_name
            pstmt.setString(10, cardNum);
            pstmt.setInt(11, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi updateProfile: " + e.getMessage());
            return false;
        }
    }
}