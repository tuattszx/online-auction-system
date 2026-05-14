package auction.client.services;

import auction.client.ClientNetwork;
import auction.common.message.Message;
import auction.common.model.bid.Bid;
import auction.common.model.items.Item;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AuctionManager {
    private static AuctionManager instance;
    private AuctionManager() {}
    public static AuctionManager getInstance() {
        if (instance == null) instance = new AuctionManager();
        return instance;
    }

    public CompletableFuture<Item> getLatestItemAsync(int itemId) {
        return ClientNetwork.getInstance().sendRequestAsync(new Message("GET_ITEM_BY_ID", itemId))
                .thenApply(res -> (res != null && "SUCCESS".equals(res.getStatus())) ? (Item) res.getData() : null);
    }

    public CompletableFuture<List<Bid>> getBidHistoryAsync(int itemId) {
        return ClientNetwork.getInstance().sendRequestAsync(new Message("GET_BID_BY_ITEM_ID", itemId))
                .thenApply(res -> (res != null && "SUCCESS".equals(res.getStatus())) ? (List<Bid>) res.getData() : null);
    }

    public CompletableFuture<List<Object[]>> getPriceChartAsync(int itemId) {
        return ClientNetwork.getInstance().sendRequestAsync(new Message("GET_PRICE_CHART", itemId))
                .thenApply(res -> (res != null && "SUCCESS".equals(res.getStatus())) ? (List<Object[]>) res.getData() : null);
    }

    public CompletableFuture<Message> getItemImagesAsync(int itemId) {
        return ClientNetwork.getInstance().sendRequestAsync(new Message("GET_ITEM_IMAGES", itemId));
    }

    public CompletableFuture<Message> placeBidAsync(int itemId, int userId, long amount) {
        Bid newBid = new Bid();
        newBid.setIdItem(itemId);
        newBid.setIdUser(userId);
        newBid.setBidAmount(amount);

        return ClientNetwork.getInstance().sendRequestAsync(new Message("PLACE_BID", newBid));
    }
}