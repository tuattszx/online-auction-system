package auction.common.model.items;

import auction.common.model.BaseEntity;
import auction.common.model.categories.Category;

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
    private Integer currentBidderId;
    private String status;
    private LocalDateTime createdTime;
    private List<Category> categories;
    private List<ItemImage> images;
    private double length;
    private double width;
    private double height;

    public Item(){
        super();
        this.categories =new ArrayList<>();
        this.images=new ArrayList<>();
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
        this.images = new ArrayList<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(long startingPrice){this.startingPrice=startingPrice;}

    public long getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(long currentPrice) { this.currentPrice = currentPrice; }

    public List<ItemImage> getImages() {
        return images;
    }
    public void setImages(List<ItemImage> imageUrls) {
        this.images = imageUrls;
    }
    public void addImages(ItemImage itemImage) {
        this.images.add(itemImage);
    }

    public LocalDateTime getStartTime(){ return  startTime;}
    public void setStartTime(LocalDateTime startTime) {this.startTime = startTime;}

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) {this.sellerId=sellerId;}

    public Integer getCurrentBidderId() {return currentBidderId;}
    public void setCurrentBidderId(Integer currentBidderId) {this.currentBidderId = currentBidderId;}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public List<Category> getCategories() {return categories;}
    public void setCategories(List<Category> categories) {this.categories = categories;}
    public void addCategories(Category category){this.categories.add(category);}

    public double getLength() { return length;}
    public void setLength(double length) {this.length = length;}

    public double getWidth() {return width;}
    public void setWidth(double width) {this.width = width;}

    public double getHeight() {return height;}
    public void setHeight(double height) {this.height = height;}

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
