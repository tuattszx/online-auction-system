package auction.common.message;

import java.io.Serializable;
import java.time.LocalDateTime;


public class BidUpdateNotification implements Serializable {
    private static final long serialVersionUID = 1L;

    private int itemId;
    private long newPrice;
    private String bidderName;
    private LocalDateTime bidTime;
    private LocalDateTime newEndTime;

    public BidUpdateNotification() {
    }

    public BidUpdateNotification(int itemId, long newPrice, String bidderName, LocalDateTime bidTime) {
        this.itemId = itemId;
        this.newPrice = newPrice;
        this.bidderName = bidderName;
        this.bidTime = bidTime;
    }

    public BidUpdateNotification(int itemId, long newPrice, String bidderName, LocalDateTime bidTime, LocalDateTime newEndTime) {
        this.itemId = itemId;
        this.newPrice = newPrice;
        this.bidderName = bidderName;
        this.bidTime = bidTime;
        this.newEndTime = newEndTime;
    }

    // Getter & Setter
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public long getNewPrice() { return newPrice; }
    public void setNewPrice(long newPrice) { this.newPrice = newPrice; }

    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }

    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }

    public LocalDateTime getNewEndTime() { return newEndTime; }
    public void setNewEndTime(LocalDateTime newEndTime) { this.newEndTime = newEndTime; }

    /**
     * Kiểm tra xem phiên đấu giá có vừa được gia hạn hay không
     */
    public boolean isExtended() {
        return newEndTime != null;
    }

    @Override
    public String toString() {
        return "BidUpdateNotification{" +
                "itemId=" + itemId +
                ", newPrice=" + newPrice +
                ", bidderName='" + bidderName + '\'' +
                ", extended=" + isExtended() +
                '}';
    }
}