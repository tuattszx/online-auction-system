package auction.server.dao;

import auction.common.model.categories.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


public interface CategoryDao extends GenericDAO<Category, Integer> {
    List<Category> getCategoryByName(List<String> name) throws SQLException;
}
