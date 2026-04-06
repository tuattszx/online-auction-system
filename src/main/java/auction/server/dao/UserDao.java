package auction.server.dao;

import auction.common.model.users.User;

import java.sql.*;

import static auction.server.DatabaseManager.getConnection;

public class UserDao{

    public static boolean CheckLogin(String userName,String password){
        String sql="SELECT * FROM users WHERE username=? AND password=?";
        try(Connection conn=getConnection();
            PreparedStatement pstmt= conn.prepareStatement(sql)) {
            pstmt.setString(1, userName);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
            catch (SQLException e){
            System.err.println("Lỗi checkLogin: " + e.getMessage());
            return false;
        }
    }

    public static boolean registerUser(User user) {
        String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getEmail());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateBalance(int id,long amount){
        String sql= "UPDATE users SET balance = balance + ? WHERE ID = ?";
        try (Connection conn= getConnection();
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

    private static User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();

        user.setId(rs.getInt("ID"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setBalance(rs.getLong("balance"));
        user.setRole(rs.getString("ROLE"));
        user.setAddress(rs.getString("ADDRESS"));

        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            user.setCreatTime(timestamp.toLocalDateTime());
        }
        return user;
    }

    public static User findUserById(int id){
        String sql="SELECT * FROM users WHERE id=?";
        try (Connection conn= getConnection();
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

    public static long getBalance(int id) {
        User user = findUserById(id);
        return (user != null) ? user.getBalance() : -1;
    }

    public static boolean isUsernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE USERNAME = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) { return false; }
    }

    public static boolean isEmailExists(String email) {
        String sql = "SELECT 1 FROM users WHERE EMAIL = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) { return false; }
    }

    public static boolean updatePassword(int userId, String newPassword) {
        String sql = "UPDATE users SET PASSWORD = ? WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPassword);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi updatePassword: " + e.getMessage());
            return false;
        }
    }

    public static boolean updateProfile(int userId, String newEmail, String newAddress) {
        String sql = "UPDATE users SET EMAIL = ?, ADDRESS=? WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newEmail);
            pstmt.setString(2, newAddress);
            pstmt.setInt(3,userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi updateProfile: " + e.getMessage());
            return false;
        }
    }
}
