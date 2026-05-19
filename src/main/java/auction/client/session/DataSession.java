package auction.client.session;

import auction.common.model.items.Item;
import auction.common.model.users.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

public class DataSession {
    private static DataSession instance;

    private ObservableList<Item> favoriteItems = FXCollections.observableArrayList();

    private User loggedInUser;

    private Item selectedItem;

    private int unreadNotificationCount;

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
    public ObservableList<Item> getFavoriteItems() {
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

    public int getUnreadNotificationCount() {
        return unreadNotificationCount;
    }
    public void setUnreadNotificationCount(int count) {
        this.unreadNotificationCount = count;
    }
    public void incrementUnreadNotificationCount() {
        this.unreadNotificationCount++;
    }
}