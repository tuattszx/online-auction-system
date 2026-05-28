package auction.common.model.notifications;

public class SystemNotification extends Notification {
    private static final long serialVersionUID = 1L;

    private String adminSender;

    public SystemNotification() {
        super();
    }

    public SystemNotification(int id, int userId, String title, String message, String adminSender) {
        // Định nghĩa type cứng là "SYSTEM" hoặc "ADMIN_WARN" thay vì "BID_ACTION"
        super(id, userId, "SYSTEM", title, message);
        this.adminSender = adminSender;
    }

    public String getAdminSender() { return adminSender; }
    public void setAdminSender(String adminSender) { this.adminSender = adminSender; }
}