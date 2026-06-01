package auction.client.services;

import auction.client.ClientNetwork;
import auction.common.message.Message;
import auction.common.model.items.Item;
import auction.common.model.items.Transaction;
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

    public CompletableFuture<Message> confirmItemAsync(int itemId, boolean isApproved,String reason) {
        Object[] payload = new Object[]{itemId, isApproved,reason};
        return ClientNetwork.getInstance().sendRequestAsync(new Message("CONFIRM_ITEM", payload));
    }

    public CompletableFuture<List<Transaction>> getAllTransactionsAsync(){
        return ClientNetwork.getInstance().sendRequestAsync(new Message("GET_ALL_TRANSACTIONS",null))
                .thenApply(rs -> (rs != null && "SUCCESS".equals(rs.getStatus()) ? (List<Transaction>) rs.getData() : new ArrayList<>()));
    }

    public CompletableFuture<Message> cancelAuctionAsync(int itemId,String reason) {
        return ClientNetwork.getInstance().sendRequestAsync(new Message("CANCEL_AUCTION", new Object[]{itemId,reason}));
    }
}
