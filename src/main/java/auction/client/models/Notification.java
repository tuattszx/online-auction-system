package auction.client.models;

public class Notification {
    private String avatarUrl;
    private String userName;
    private String actionText;
    private String timeStr;
    private boolean isUnread;

    public Notification(String avatarUrl, String userName, String actionText, String timeStr, boolean isUnread) {
        this.avatarUrl = avatarUrl;
        this.userName = userName;
        this.actionText = actionText;
        this.timeStr = timeStr;
        this.isUnread = isUnread;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getUserName() {
        return userName;
    }

    public String getActionText() {
        return actionText;
    }

    public String getTimeStr() {
        return timeStr;
    }

    public boolean isUnread() {
        return isUnread;
    }
}
