package auction.server.dao;

import auction.common.model.notifications.Notification;

import java.util.List;
import java.util.Set;

public interface NotificationDAO extends GenericDAO<Notification, Integer> {
        boolean markAsRead(int notificationId);
        List<Notification> getNotificationsByUserId(int userId);
        boolean insertNotificationsBatch(Set<Integer> userIds, Notification notification);
}
