package auction.common.model.notifications;

public class BidNotification extends Notification {
    private static final long serialVersionUID = 1L;

    private int itemId;
    private long newPrice;
    private String bidderName;

    public BidNotification() {
        super();
    }

    public BidNotification(int id, int userId, String title, String message, int itemId, long newPrice, String bidderName) {
        super(id, userId, "BID_ACTION", title, message);
        this.itemId = itemId;
        this.newPrice = newPrice;
        this.bidderName = bidderName;
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public long getNewPrice() { return newPrice; }
    public void setNewPrice(long newPrice) { this.newPrice = newPrice; }

    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }
}