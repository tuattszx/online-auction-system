package auction.client.session;

import auction.common.model.items.Item;
import auction.common.model.users.User;

public class DataSession {
    private static DataSession instance;

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
}