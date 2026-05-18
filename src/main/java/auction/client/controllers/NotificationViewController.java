package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.services.AuctionManager;
import auction.client.services.NotificationSubscriptionManager;
import auction.client.session.DataSession;
import auction.common.message.Message;
import auction.common.model.notifications.BidNotification;
import auction.common.model.notifications.ItemNotification;
import auction.common.model.notifications.Notification;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class NotificationViewController {
    @FXML private HBox searchBar; // Liên kết với thanh tìm kiếm
    @FXML
    private HeaderMenuController headerMenuController;
    @FXML
    private VBox vboxMainNotifications;
    private Consumer<Notification> realtimeCallback;

    @FXML
    public void initialize(){
        loadNotifications();
        if (headerMenuController != null) {
            headerMenuController.hideSearchBar();
        }
        headerMenuController.setBalance(DataSession.getInstance().getLoggedInUser() != null ? DataSession.getInstance().getLoggedInUser().getBalance() + " $" : "0 $");
    }

    private HBox createNotificationItem(String avatarUrl, String titleText, String actionText, String timeStr, boolean isUnread) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 16, 8, 16));
        row.setSpacing(12);
        row.setStyle("-fx-cursor: hand;");

        // Hiệu ứng hover giống Facebook
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #f2f2f2; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent;"));

        // 1. Ảnh đại diện hình tròn
        ImageView avatar = new ImageView(new Image(avatarUrl));
        avatar.setFitWidth(56);
        avatar.setFitHeight(56);
        // Bo tròn ảnh đại diện
        Circle clip = new Circle(28, 28, 28);
        avatar.setClip(clip);

        // 2. Phần nội dung chữ (Tên + hành động + thời gian)
        VBox textContainer = new VBox();
        textContainer.setSpacing(4);
        HBox.setHgrow(textContainer, Priority.ALWAYS);

        // Dùng TextFlow để có thể bôi đậm tên, viết thường hành động
        Text textTitle = new Text(titleText);
        textTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-fill: #050505;");

        Text textAction = new Text(actionText);
        textAction.setStyle("-fx-font-size: 14px;");

        TextFlow titleFlow = new TextFlow(textTitle);
        TextFlow messageFlow = new TextFlow(textAction);

        Label lblTime = new Label(timeStr);
        lblTime.setStyle("-fx-font-size: 12px; -fx-text-fill: #65676b;");
        if(isUnread) {
            lblTime.setStyle("-fx-font-size: 12px; -fx-text-fill: #1877f2; -fx-font-weight: bold;");
        }

        textContainer.getChildren().addAll(titleFlow,messageFlow, lblTime);

        row.getChildren().addAll(avatar, textContainer);

        // 3. Nếu chưa đọc -> Thêm chấm xanh thông báo ở góc phải
        if (isUnread) {
            Circle unreadDot = new Circle(6, javafx.scene.paint.Color.web("#1877f2"));
            row.getChildren().add(unreadDot);
        }

        return row;
    }

    private HBox createNotificationItemRow(Notification note) {
        String defaultAvatar = "https://www.w3schools.com/howto/img_avatar.png";
        String titleText = note.getTitle();
        String messageText = note.getMessage();

        // 1. 🔥 BÓC TÁCH NỘI DUNG CHI TIẾT THEO TỪNG LOẠI THÔNG BÁO
        if (note instanceof BidNotification bidNote) {
            defaultAvatar = "https://www.w3schools.com/howto/img_avatar.png";

            messageText = String.format("Món hàng mã số #%d đã bị %s đè giá lên %,d $.",
                    bidNote.getItemId(), bidNote.getBidderName(), bidNote.getNewPrice());

        } else if (note instanceof ItemNotification itemNote) {
            defaultAvatar = "https://cdn-icons-png.flaticon.com/512/9425/9425832.png";

            messageText = String.format("Sản phẩm #%d có trạng thái mới: %s. \nGhi chú từ Admin: %s",
                    itemNote.getItemId(), itemNote.getItemStatus(), itemNote.getAdminNote());
        }

        String timeDisplay = formatRelativeTime(note.getCreatedAt());

        // Gọi hàm build giao diện thô với dữ liệu đã được bóc tách riêng biệt
        HBox row = createNotificationItem(defaultAvatar, titleText, messageText, timeDisplay, !note.isRead());

        // XỬ LÝ SỰ KIỆN CLICK VÀ ĐIỀU HƯỚNG CHÍNH XÁC THEO FILE FXML
        row.setOnMouseClicked(e -> {
            note.setRead(true);
            if (row.getChildren().get(row.getChildren().size() - 1) instanceof Circle) {
                row.getChildren().remove(row.getChildren().size() - 1);
            }

            try {
                // Trường hợp A: Thông báo liên quan đến ĐẤU GIÁ (BidNotification) -> Vào item-view.fxml
                if (note instanceof BidNotification bidNote) {
                    System.out.println("Điều hướng tới sản phẩm ID: " + bidNote.getItemId());

                    AuctionManager.getInstance().getLatestItemAsync(bidNote.getItemId())
                            .thenAccept(item -> {
                                        Platform.runLater(() -> {
                                            if (item != null) {
                                                DataSession.getInstance().setSelectedItem(item);
                                                ViewManager.switchScene(e, "item-view.fxml", "Chi tiết: " + item.getName());
                                            } else {
                                                System.err.println("Không tìm thấy sản phẩm!");
                                            }
                                        });
                            });
                }

                // Trường hợp B: Thông báo liên quan đến SẢN PHẨM (ItemNotification) -> Vào seller-view.fxml
                else if (note instanceof ItemNotification itemNote) {
                    ViewManager.switchScene(e, "seller-view.fxml", "Quản lý bán hàng");
                }
            } catch (Exception ex) {
                System.err.println("Lỗi khi chuyển màn hình từ thông báo: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        return row;
    }

    private void setupRealtimeNotification() {
        this.realtimeCallback = newNotif -> {
            Platform.runLater(() -> {
                HBox newNotificationItem = createNotificationItemRow(newNotif);

                // Chèn lên đầu danh sách (dưới chữ "Mới")
                if (vboxMainNotifications.getChildren().size() > 1) {
                    vboxMainNotifications.getChildren().add(1, newNotificationItem);
                } else {
                    vboxMainNotifications.getChildren().add(newNotificationItem);
                }
            });
        };
        NotificationSubscriptionManager.getInstance().subscribe(realtimeCallback);
    }

    public void loadNotifications() {
        // 1. Xóa danh sách cũ đi để tải mới
        vboxMainNotifications.getChildren().clear();

        // 2. Giữ nguyên tiêu đề "Mới" như thiết kế ban đầu
        Label lblMoi = new Label("Mới");
        lblMoi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #050505;");
        VBox.setMargin(lblMoi, new Insets(10, 0, 5, 16));
        vboxMainNotifications.getChildren().add(lblMoi);

        // 3. Gọi background task lấy dữ liệu từ Server
        Task<Message> getMessageTask = new Task<>() {
            @Override
            protected Message call() throws Exception {
                Message request = new Message("GET_MESSAGE", DataSession.getInstance().getLoggedInUser().getId());
                return ClientNetwork.getInstance().sendRequest(request);
            }
        };

        getMessageTask.setOnSucceeded(event -> {
            Message response = getMessageTask.getValue();
            if (response != null && "SUCCESS".equals(response.getStatus())) {
                List<Notification> notifications = (List<Notification>) response.getData();
                Platform.runLater(() -> {
                    if (notifications != null) {
                        for (Notification note : notifications) {
                            HBox notificationItem = createNotificationItemRow(note);
                            vboxMainNotifications.getChildren().add(notificationItem);
                        }
                    }
                });
            }
        });

        new Thread(getMessageTask).start();
    }

    private String formatRelativeTime(LocalDateTime createdAt) {
        if (createdAt == null) return "Không rõ";

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(createdAt, now);

        long seconds = duration.getSeconds();
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (seconds < 60) {
            return "Vừa xong";
        } else if (minutes < 60) {
            return minutes + " phút trước";
        } else if (hours < 24) {
            return hours + " giờ trước";
        } else if (days < 7) {
            return days + " ngày trước";
        } else {
            // Nếu quá lâu rồi (trên 7 ngày) thì hiển thị ngày tháng cụ thể luôn
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return createdAt.format(formatter);
        }
    }
}
