package auction.common.model;

import java.io.Serializable;

public abstract class BaseEntity implements Serializable {
    private int id;

    public BaseEntity(){}

    public BaseEntity(int id){
        this.id=id;
    }

    public int getID() {
        return id;
    }
}