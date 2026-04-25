package auction.server.dao;

import auction.common.model.categories.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static auction.server.DatabaseManager.getConnection;

public class CategoryDao {

    public static Category getCategoryByName(String name) throws SQLException {
        String sql = "SELECT * FROM CATEGORIES WHERE name = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Category cat = new Category();
                    cat.setId(rs.getInt("id"));
                    cat.setName(rs.getString("name"));
                    cat.setDescription(rs.getString("description"));
                    return cat;
                }
            }
        }
        return null;
    }
}
