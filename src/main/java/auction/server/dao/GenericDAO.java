package auction.server.dao;

import java.sql.SQLException;
import java.util.List;

public interface GenericDAO<T, ID> {
    T getById(ID id) ;
    List<T> getAll() ;
    boolean add(T t) ;
    boolean update(T t) ;
    boolean delete(ID id) ;
}