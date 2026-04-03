package auction.common.model;

import java.io.Serializable;

public abstract class BaseEntity implements Serializable {
    protected int id;

    public BaseEntity(){}

    public BaseEntity(int id){
        this.id=id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}