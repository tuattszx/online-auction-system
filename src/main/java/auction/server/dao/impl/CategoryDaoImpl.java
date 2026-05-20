package auction.server.dao.impl;

import auction.common.model.categories.Category;
import auction.server.DatabaseManager;
import auction.server.dao.CategoryDao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class CategoryDaoImpl implements CategoryDao {

    @Override
    public Category getById(Integer id) {
        String sql = "SELECT * FROM CATEGORIES WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToCategory(rs);
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Category> getAll() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM CATEGORIES ORDER BY name ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToCategory(rs));
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public boolean add(Category category){
        String sql = "INSERT INTO CATEGORIES (name, description) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category.getName());
            pstmt.setString(2, category.getDescription());
            return pstmt.executeUpdate() > 0;
        }
        catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(Category category){
        String sql = "UPDATE CATEGORIES SET name = ?, description = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category.getName());
            pstmt.setString(2, category.getDescription());
            pstmt.setInt(3, category.getId());
            return pstmt.executeUpdate() > 0;
        }
        catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM CATEGORIES WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
        catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Category> getCategoryByName(List<String> name) throws SQLException {
        List<Category> list = new ArrayList<>();
        if (name == null || name.isEmpty()) return list;

        String placeholders = String.join(",", name.stream().map(n -> "?").toArray(String[]::new));
        String sql = "SELECT * FROM CATEGORIES WHERE name IN (" + placeholders + ") ORDER BY name ASC";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < name.size(); i++) {
                pstmt.setString(i + 1, name.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToCategory(rs));
                }
            }
        }
        return list;
    }

    private Category mapResultSetToCategory(ResultSet rs) throws SQLException {
        Category cat = new Category();
        cat.setId(rs.getInt("id"));
        cat.setName(rs.getString("name"));
        cat.setDescription(rs.getString("description"));
        return cat;
    }
}
