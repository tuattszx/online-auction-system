package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.session.DataSession;
import auction.common.message.Message;
import auction.server.dao.impl.BidDaoImpl;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;

public class NotificationViewController {
    @FXML private HBox searchBar; // Liên kết với thanh tìm kiếm
    @FXML
    private HeaderMenuController headerMenuController;
    @FXML
    public void initialize(){
        loadNotifications();
        if (headerMenuController != null) {
            headerMenuController.hideSearchBar();
        }
        headerMenuController.setBalance(DataSession.getInstance().getLoggedInUser() != null ? DataSession.getInstance().getLoggedInUser().getBalance() + " $" : "0 $");
    }
    @FXML
    private VBox vboxMainNotifications;
    private HBox createNotificationItem(String avatarUrl, String userName, String actionText, String timeStr, boolean isUnread) {
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
        Text textName = new Text(userName + " ");
        textName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Text textAction = new Text(actionText);
        textAction.setStyle("-fx-font-size: 14px;");

        TextFlow textFlow = new TextFlow(textName, textAction);

        Label lblTime = new Label(timeStr);
        lblTime.setStyle("-fx-font-size: 12px; -fx-text-fill: #65676b;");
        if(isUnread) {
            lblTime.setStyle("-fx-font-size: 12px; -fx-text-fill: #1877f2; -fx-font-weight: bold;");
        }

        textContainer.getChildren().addAll(textFlow, lblTime);

        row.getChildren().addAll(avatar, textContainer);

        // 3. Nếu chưa đọc -> Thêm chấm xanh thông báo ở góc phải
        if (isUnread) {
            Circle unreadDot = new Circle(6, javafx.scene.paint.Color.web("#1877f2"));
            row.getChildren().add(unreadDot);
        }

        return row;
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
                List<String> notifications = (List<String>) response.getData();
                Platform.runLater(() -> {
                    if (notifications != null) {
                        for (String note : notifications) {
                            String defaultAvatar = "https://www.w3schools.com/howto/img_avatar.png";

                            HBox notificationItem = createNotificationItem(
                                    defaultAvatar,
                                    "",
                                    note,
                                    "Vừa xong",
                                    true
                            );

                            vboxMainNotifications.getChildren().add(notificationItem);
                        }
                    }
                });
            }
        });

        new Thread(getMessageTask).start();
    }
}
