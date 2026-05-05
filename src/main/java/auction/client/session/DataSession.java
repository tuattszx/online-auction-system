package auction.client.session;

import auction.common.model.items.Item;
import auction.common.model.users.User;

import java.util.ArrayList;
import java.util.List;

public class DataSession {
    private static DataSession instance;

    private List<Item> favoriteItems = new ArrayList<>(); // Danh sách lưu đồ yêu thích

    private User loggedInUser;

    private Item selectedItem;

    private DataSession() {}

    public static DataSession getInstance() {
        if (instance == null) {
            instance = new DataSession();
        }
        return instance;
    }

    public User getLoggedInUser() { return loggedInUser; }
    public void setLoggedInUser(User user) { this.loggedInUser = user; }

    // Getter và Setter cho Item
    public Item getSelectedItem() { return selectedItem; }
    public void setSelectedItem(Item item) { this.selectedItem = item; }

    public void clear() {
        loggedInUser = null;
        selectedItem = null;
    }
    // đẩy Item vào danh sách để đưa lên Favorite_view
    public List<Item> getFavoriteItems() {
        return favoriteItems;
    }

    public void addFavorite(Item item) {
        if (!favoriteItems.contains(item)) {
            favoriteItems.add(item);
        }
    }

    public void removeFavorite(Item item) {
        favoriteItems.remove(item);
    }
}