package auction.server.dao;

import auction.common.model.bid.Bid;

import java.util.List;

public interface BidDao extends GenericDAO<Bid, Integer> {
    List<Bid> getBidsByItemId(int itemId);
    List<Bid> getBidsByUserId(int userId);
}