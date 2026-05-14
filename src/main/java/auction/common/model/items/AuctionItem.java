package auction.common.model.items;

import javafx.beans.property.*;

import java.io.Serializable;

public class AuctionItem implements Serializable {
    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty remainingTime;
    private final LongProperty currentBid;
    private final LongProperty yourBid;
    private final StringProperty status;

    // 1. Constructor đầy đủ
    public AuctionItem(int id, String name, String remainingTime, long currentBid, long yourBid, String status) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.remainingTime = new SimpleStringProperty(remainingTime);
        this.currentBid = new SimpleLongProperty(currentBid);
        this.yourBid = new SimpleLongProperty(yourBid);
        this.status = new SimpleStringProperty(status);
    }

    // 2. Các hàm Getter cho Property (Để TableView quan sát được sự thay đổi)
    public StringProperty nameProperty() { return name; }
    public StringProperty remainingTimeProperty() { return remainingTime; }
    public LongProperty currentBidProperty() { return currentBid; }
    public LongProperty yourBidProperty() { return yourBid; }
    public StringProperty statusProperty() { return status; }

    // 3. Các hàm Getter/Setter chuẩn (Để lấy giá trị nhanh)
    public Integer getId() { return id.get(); }
    public void setId(Integer id) { this.id.set(id); }

    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }

    public String getRemainingTime() { return remainingTime.get(); }
    public void setRemainingTime(String remainingTime) { this.remainingTime.set(remainingTime); }

    public long getCurrentBid() { return currentBid.get(); }
    public void setCurrentBid(long currentBid) { this.currentBid.set(currentBid); }

    public long getYourBid() { return yourBid.get(); }
    public void setYourBid(long yourBid) { this.yourBid.set(yourBid); }

    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
}