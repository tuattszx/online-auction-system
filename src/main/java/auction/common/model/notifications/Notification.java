package auction.common.model.notifications;

import auction.common.model.BaseEntity;

import java.time.LocalDateTime;

public abstract class Notification extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private int userId;         // Người nhận thông báo
    private String type;        // Loại thông báo để phân biệt
    private String title;       // Tiêu đề ngắn hiển thị bôi đậm
    private String message;     // Nội dung text chi tiết
    private boolean isRead;     // Trạng thái đọc
    private LocalDateTime createdAt;

    public Notification() {
        super();
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }

    public Notification(int id, int userId, String type, String title, String message) {
        super(id);
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
