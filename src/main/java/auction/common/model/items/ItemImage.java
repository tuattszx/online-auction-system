package auction.common.model.items;

import auction.common.model.BaseEntity;

public class ItemImage extends BaseEntity {
    private int itemId;
    private String urlImage;
    private boolean isDefault;

    public ItemImage() {
        super();
    }

    public ItemImage(int id, int itemId, String urlImage, boolean isDefault) {
        super(id);
        this.itemId = itemId;
        this.urlImage = urlImage;
        this.isDefault = isDefault;
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public String getUrlImage() { return urlImage; }
    public void setUrlImage(String urlImage) { this.urlImage = urlImage; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    @Override
    public String toString() {
        return "ItemImage{" +
                "id=" + getId() +
                ", itemId=" + itemId +
                ", isDefault=" + isDefault +
                '}';
    }
}