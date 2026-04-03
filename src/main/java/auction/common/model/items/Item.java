package auction.common.model.items;

import auction.common.model.BaseEntity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Item extends BaseEntity {
    private String name;
    private String description;
    private long startingPrice;
    private long currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int sellerId;
    private String status;
    private LocalDateTime createdTime;
    private int categoryId;
    private List<String> imageUrls;

    public Item(){
        super();
        this.imageUrls=new ArrayList<>();
    }
    public Item(int id, String name, String description, long startingPrice,
                long currentPrice,LocalDateTime startTime, LocalDateTime endTime, int sellerId, String status) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
        this.startTime=startTime;
        this.endTime = endTime;
        this.sellerId = sellerId;
        this.status = status;
        this.imageUrls = new ArrayList<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(long startingPrice){this.startingPrice=startingPrice;}

    public long getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(long currentPrice) { this.currentPrice = currentPrice; }

    public List<String> getImageUrls() {
        return imageUrls;
    }
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
    public void addImageUrl(String url) {
        this.imageUrls.add(url);
    }

    public LocalDateTime getStartTime(){ return  startTime;}
    public void setStartTime(LocalDateTime startTime) {this.startTime = startTime;}

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) {this.sellerId=sellerId;}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public int getCategoryId() {return categoryId;}
    public void setCategoryId(int categoryId) {this.categoryId = categoryId;}

    @Override
    public String toString(){
        return "Item{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", currentPrice=" + currentPrice +
                ", status='" + status + '\'' +
                ", endTime=" + endTime +
                '}';
    }
}
