package auction.server.dao;

import java.util.List;

public interface FavouriteDao {
    boolean addFavourite(int userId, int itemId);
    boolean removeFavourite(int userId, int itemId);
    List<Integer> getFavoriteItemIdsByUserId(int userId);
    List<Integer> getUserIdsByFavoriteItem(int itemId);
}
