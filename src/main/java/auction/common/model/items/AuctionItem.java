package auction.common.model.items;

import javafx.beans.property.*;

public class AuctionItem {
    private final StringProperty name;
    private final StringProperty remainingTime;
    private final DoubleProperty currentBid;
    private final DoubleProperty yourBid;
    private final StringProperty status;

    // 1. Constructor đầy đủ
    public AuctionItem(String name, String remainingTime, double currentBid, double yourBid, String status) {
        this.name = new SimpleStringProperty(name);
        this.remainingTime = new SimpleStringProperty(remainingTime);
        this.currentBid = new SimpleDoubleProperty(currentBid);
        this.yourBid = new SimpleDoubleProperty(yourBid);
        this.status = new SimpleStringProperty(status);
    }

    // 2. Các hàm Getter cho Property (Để TableView quan sát được sự thay đổi)
    public StringProperty nameProperty() { return name; }
    public StringProperty remainingTimeProperty() { return remainingTime; }
    public DoubleProperty currentBidProperty() { return currentBid; }
    public DoubleProperty yourBidProperty() { return yourBid; }
    public StringProperty statusProperty() { return status; }

    // 3. Các hàm Getter/Setter chuẩn (Để lấy giá trị nhanh)
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }

    public String getRemainingTime() { return remainingTime.get(); }
    public void setRemainingTime(String remainingTime) { this.remainingTime.set(remainingTime); }

    public double getCurrentBid() { return currentBid.get(); }
    public void setCurrentBid(double currentBid) { this.currentBid.set(currentBid); }

    public double getYourBid() { return yourBid.get(); }
    public void setYourBid(double yourBid) { this.yourBid.set(yourBid); }

    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
}