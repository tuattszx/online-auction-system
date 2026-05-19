package auction.common.model.notifications;

public class ItemNotification extends Notification {
    private static final long serialVersionUID = 1L;

    private int itemId;
    private String itemStatus;
    private String adminNote;

    public ItemNotification() {
        super();
    }

    public ItemNotification(int id, int userId, String title, String message, int itemId, String itemStatus, String adminNote) {
        super(id, userId, "ITEM_STATUS", title, message);
        this.itemId = itemId;
        this.itemStatus = itemStatus;
        this.adminNote = adminNote;
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public String getItemStatus() { return itemStatus; }
    public void setItemStatus(String itemStatus) { this.itemStatus = itemStatus; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
}