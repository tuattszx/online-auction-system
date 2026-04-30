package auction.server.dao;

import auction.common.model.categories.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static auction.server.DatabaseManager.getConnection;

public interface CategoryDao extends GenericDAO<Category, Integer> {
    Category getCategoryByName(String name) throws SQLException;
}
