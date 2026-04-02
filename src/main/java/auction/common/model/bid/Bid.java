package auction.common.model.bid;

import auction.common.model.BaseEntity;
import java.time.LocalDateTime;

public class Bid extends BaseEntity {
    private int idItem;
    private int idUser;
    private long bidAmount;
    private LocalDateTime bidTime;

    public Bid() {
        super();
    }


    public Bid(int id, int idItem, int idUser, long bidAmount, LocalDateTime bidTime) {
        super(id);
        this.idItem = idItem;
        this.idUser = idUser;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    public int getIdItem() {
        return idItem;
    }
    public void setIdItem(int idItem) {
        this.idItem = idItem;
    }

    public int getIdUser() {
        return idUser;
    }
    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public long getBidAmount() {
        return bidAmount;
    }
    public void setBidAmount(long bidAmount) {
        if (bidAmount > 0) {
            this.bidAmount = bidAmount;
        }
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }
    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }


    @Override
    public String toString() {
        return "Bid{" +
                "id=" + getID() +
                ", idItem=" + idItem +
                ", idUser=" + idUser +
                ", bidAmount=" + bidAmount +
                ", bidTime=" + bidTime +
                '}';
    }
}