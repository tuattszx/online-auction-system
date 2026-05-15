package auction.common.model.items;

import javafx.beans.property.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Objects;

public class AuctionItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private int idVal;
    private String nameVal;
    private String remainingTimeVal;
    private long currentBidVal;
    private long yourBidVal;
    private String statusVal;

    private transient IntegerProperty id;
    private transient StringProperty name;
    private transient StringProperty remainingTime;
    private transient LongProperty currentBid;
    private transient LongProperty yourBid;
    private transient StringProperty status;

    // 1. Constructor đầy đủ
    public AuctionItem(int id, String name, String remainingTime, long currentBid, long yourBid, String status) {
        this.idVal = id;
        this.nameVal = name;
        this.remainingTimeVal = remainingTime;
        this.currentBidVal = currentBid;
        this.yourBidVal = yourBid;
        this.statusVal = status;
        initializeProperties();
    }

    private void initializeProperties() {
        this.id = new SimpleIntegerProperty(idVal);
        this.name = new SimpleStringProperty(nameVal);
        this.remainingTime = new SimpleStringProperty(remainingTimeVal);
        this.currentBid = new SimpleLongProperty(currentBidVal);
        this.yourBid = new SimpleLongProperty(yourBidVal);
        this.status = new SimpleStringProperty(statusVal);
    }

    // 2. Các hàm Getter cho Property (Để TableView quan sát được sự thay đổi)
    public StringProperty nameProperty() { return name; }
    public StringProperty remainingTimeProperty() { return remainingTime; }
    public LongProperty currentBidProperty() { return currentBid; }
    public LongProperty yourBidProperty() { return yourBid; }
    public StringProperty statusProperty() { return status; }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject(); // Đọc các biến *Val
        initializeProperties(); // Hồi sinh các Property cho UI
    }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Nếu cùng địa chỉ ô nhớ thì là 1
        if (o == null || getClass() != o.getClass()) return false; // Nếu khác loại thì không phải
        AuctionItem that = (AuctionItem) o;
        // So sánh dựa trên ID duy nhất của sản phẩm
        return idVal == that.idVal;
    }

    @Override
    public int hashCode() {
        // Tạo mã băm dựa trên ID để Java nhận diện nhanh trong List/Set
        return Objects.hash(idVal);
    }
}