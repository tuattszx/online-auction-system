package auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
        }catch (SQLException e){
            System.err.println("Lỗi checkLogin: " + e.getMessage());
            return false;
    }
}}
