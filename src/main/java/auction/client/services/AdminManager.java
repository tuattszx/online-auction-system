package auction.client.services;

import auction.client.ClientNetwork;
import auction.common.message.Message;
import auction.common.model.items.Item;
import auction.common.model.users.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AdminManager {
    private static AdminManager instance;

    private AdminManager() {}

    public static synchronized AdminManager getInstance() {
        if (instance == null) {
            instance = new AdminManager();
        }
        return instance;
    }

    public CompletableFuture<Map<String, Object>> getDashboardStatsAsync() {
        return ClientNetwork.getInstance().sendRequestAsync(new Message("GET_DASHBOARD_STATS", null))
                .thenApply(res -> (res != null && "SUCCESS".equals(res.getStatus())) ? (Map<String, Object>) res.getData() : null);
    }

    public CompletableFuture<List<User>> getAllUsersAsync() {
        return ClientNetwork.getInstance().sendRequestAsync(new Message("GET_ALL_USERS", null))
                .thenApply(res -> (res != null && "SUCCESS".equals(res.getStatus())) ? (List<User>) res.getData() : new ArrayList<>());
    }

    public CompletableFuture<List<Item>> getUnapprovedItemsAsync() {
        return ClientNetwork.getInstance().sendRequestAsync(new Message("GET_UNAPPROVED_ITEMS", null))
                .thenApply(res -> (res != null && "SUCCESS".equals(res.getStatus())) ? (List<Item>) res.getData() : new ArrayList<>());
    }

    public CompletableFuture<Message> warnUserAsync(int userId, String reason) {
        Object[] payload = new Object[]{userId, reason};
        return ClientNetwork.getInstance().sendRequestAsync(new Message("WARN_USER", payload));
    }

    public CompletableFuture<Message> banUserAsync(int userId) {
        return ClientNetwork.getInstance().sendRequestAsync(new Message("DELETE_USER", userId));
    }

    public CompletableFuture<Message> unbanUserAsync(int userId) {
        return ClientNetwork.getInstance().sendRequestAsync(new Message("UNBAN_USER", userId));
    }

    public CompletableFuture<Message> confirmItemAsync(int itemId, boolean isApproved) {
        Object[] payload = new Object[]{itemId, isApproved};
        return ClientNetwork.getInstance().sendRequestAsync(new Message("CONFIRM_ITEM", payload));
    }
}
