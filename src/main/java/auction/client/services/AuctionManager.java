package auction.client.services;

import auction.client.ClientNetwork;
import auction.common.message.Message;
import auction.common.model.bid.Bid;
import auction.common.model.items.Item;
import java.util.List;

public class AuctionManager {
    private static AuctionManager instance;
    private AuctionManager() {}
    public static AuctionManager getInstance() {
        if (instance == null) instance = new AuctionManager();
        return instance;
    }

    public Item getLatestItem(int itemId) {
        Message res = ClientNetwork.getInstance().sendRequest(new Message("GET_ITEM_BY_ID", itemId));
        return (res != null && "SUCCESS".equals(res.getStatus())) ? (Item) res.getData() : null;
    }

    public List<Bid> getBidHistory(int itemId) {
        Message res = ClientNetwork.getInstance().sendRequest(new Message("GET_BID_BY_ITEM_ID", itemId));
        return (res != null && "SUCCESS".equals(res.getStatus())) ? (List<Bid>) res.getData() : null;
    }

    public List<Object[]> getPriceChart(int itemId) {
        Message res = ClientNetwork.getInstance().sendRequest(new Message("GET_PRICE_CHART", itemId));
        return (res != null && "SUCCESS".equals(res.getStatus())) ? (List<Object[]>) res.getData() : null;
    }

    public Message placeBid(int itemId, int userId, long amount) {
        Bid newBid = new Bid();
        newBid.setIdItem(itemId);
        newBid.setIdUser(userId);
        newBid.setBidAmount(amount);
        return ClientNetwork.getInstance().sendRequest(new Message("PLACE_BID", newBid));
    }
}