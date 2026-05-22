package auction.server.dao;

import auction.common.model.items.AuctionItem;
import auction.common.model.items.Item;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemDao extends GenericDAO<Item, Integer> {
    List<Item> getItemsByCategory(int catId);
    List<Item> searchItems(String keyword);
    List<Item> getItemsBySeller(int sellerId);
    boolean updateCurrentPrice(int itemId, long newPrice);
    boolean updateStatus(int itemId, String status);
    boolean placeBid(int idItem, long newPrice, int id_bidder);
    List<Item> getUnapprovedItems();
    boolean approveItem(int itemId, boolean isApproved);
    boolean updateEndTime(int id, LocalDateTime newEndTime);
    List<AuctionItem> getMyAuctions(int userId);
    boolean setupAutoBid(int itemId, int userId, long maxBid, long increment, String username);
    boolean cancelAutoBid(int itemId, int userId);
    boolean checkAutoBidExists(int itemId, int userId);
    void checkAndTriggerAutomaticBids(int itemId, long currentPrice);
}