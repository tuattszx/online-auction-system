package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.services.AuctionManager;
import auction.client.services.Cleanable;
import auction.client.services.NotificationSubscriptionManager;
import auction.client.session.DataSession;
import auction.common.message.Message;
import auction.common.model.notifications.BidNotification;
import auction.common.model.notifications.ItemNotification;
import auction.common.model.notifications.Notification;
import auction.common.model.notifications.SystemNotification;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static auction.client.utils.ServerTimeSync.formatRelativeTime;

public class NotificationViewController implements Cleanable {
    @FXML private HBox searchBar; // Liên kết với thanh tìm kiếm
    @FXML
    private HeaderMenuController headerMenuController;
    @FXML
    private VBox vboxMainNotifications;
    private Consumer<Notification> realtimeCallback;
    @FXML private Button btnAllread;
    @FXML private Button btnUnread;
    private boolean isFilteringUnread = false;

    @FXML
    public void initialize(){
        setupRealtimeNotification();
        switchButtonStyle(btnAllread, true);
        switchButtonStyle(btnUnread, false);
        loadNotifications(false);
        if (headerMenuController != null) {
            headerMenuController.hideSearchBar();
        }
        //headerMenuController.setBalance(DataSession.getInstance().getLoggedInUser() != null ? DataSession.getInstance().getLoggedInUser().getBalance() + " $" : "0 $");
    }

    @Override
    public void cleanup() {
        if (realtimeCallback != null) {
            NotificationSubscriptionManager.getInstance().unsubscribe(realtimeCallback);
        }
    }
    private void switchButtonStyle(Button button, boolean isActive) {
        button.getStyleClass().remove("fb-button-active");
        button.getStyleClass().remove("fb-button-normal");

        if (isActive) {
            button.getStyleClass().add("fb-button-active");
        } else {
            button.getStyleClass().add("fb-button-normal");
        }
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
            defaultAvatar = getClass().getResource("/auction/img/345629.png").toExternalForm();

        } else if (note instanceof ItemNotification itemNote) {
            defaultAvatar = getClass().getResource("/auction/img/box.png").toExternalForm();
        } else if (note instanceof SystemNotification) {
            defaultAvatar = getClass().getResource("/auction/img/warn.jpg").toExternalForm();
        }

        String timeDisplay = formatRelativeTime(note.getCreatedAt());

        // Gọi hàm build giao diện thô với dữ liệu đã được bóc tách riêng biệt
        HBox row = createNotificationItem(defaultAvatar, titleText, messageText, timeDisplay, !note.isRead());

        // XỬ LÝ SỰ KIỆN CLICK VÀ ĐIỀU HƯỚNG CHÍNH XÁC THEO FILE FXML
        row.setOnMouseClicked(e -> {
            if (!note.isRead()) {
                note.setRead(true);
                row.getChildren().removeIf(node -> node instanceof Circle && ((Circle) node).getFill().toString().contains("1877f2"));
                new Thread(() -> {
                    try {
                        ClientNetwork.getInstance().sendRequest(new Message("MARK_AS_READ", note.getId()));
                    } catch (Exception ex) {
                        System.err.println("Không thể cập nhật trạng thái đã đọc: " + ex.getMessage());
                    }
                }).start();
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
                if (isFilteringUnread && newNotif.isRead()) {
                    return;
                }
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

    public void loadNotifications(boolean unreadOnly) {
        // 1. Xóa danh sách cũ đi để tải mới
        vboxMainNotifications.getChildren().clear();

        // 2. Giữ nguyên tiêu đề "Mới" như thiết kế ban đầu
        Label lblMoi = new Label("Mới");
        lblMoi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #050505;");
        VBox.setMargin(lblMoi, new Insets(10, 0, 5, 16));
        vboxMainNotifications.getChildren().add(lblMoi);

        // 3. Gọi background task lấy dữ liệu từ Server
        Task<List<HBox>> loadNotifTask = new Task<>() {
            @Override
            protected List<HBox> call() throws Exception {
                Message request = new Message("GET_MESSAGE", DataSession.getInstance().getLoggedInUser().getId());
                Message response=ClientNetwork.getInstance().sendRequest(request);

                List<HBox> uiRows=new ArrayList<>();
                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    List<Notification> notifications = (List<Notification>) response.getData();
                    if (notifications != null) {
                        for (Notification note : notifications) {
                            // 🔥 THÊM ĐÚNG DÒNG NÀY: Kiểm tra điều kiện lọc chưa đọc
                            if (unreadOnly && note.isRead()) {
                                continue;
                            }
                            HBox notificationItem = createNotificationItemRow(note);
                            uiRows.add(notificationItem);
                        }
                    }
                }
                return uiRows;
            }
        };;

        loadNotifTask.setOnSucceeded(event -> {
            List<HBox> readyRows = loadNotifTask.getValue();
            Platform.runLater(() -> {
                if (readyRows != null && !readyRows.isEmpty()) {
                    vboxMainNotifications.getChildren().addAll(readyRows);
                }
            });
        });

        new Thread(loadNotifTask).start();
    }
    @FXML
    public void handleAllClick(ActionEvent event) {
        // Thay đổi class CSS để chuyển đổi trạng thái hiển thị sáng/tối giống FB
        switchButtonStyle(btnAllread, true);
        switchButtonStyle(btnUnread, false);
        isFilteringUnread = false;
        // --- VIẾT CODE LOAD TOÀN BỘ THÔNG BÁO CỦA BẠN TẠI ĐÂY ---
        System.out.println("Đang lọc hiển thị tất cả thông báo...");
        loadNotifications(false); // Ví dụ hàm load dữ liệu
    }

    /**
     * Hành động khi click vào nút "Chưa đọc"
     */
    @FXML
    public void handleUnreadClick(ActionEvent event) {
        // Thay đổi class CSS sang active
        switchButtonStyle(btnUnread, true);
        switchButtonStyle(btnAllread, false);
        isFilteringUnread = true;
        loadNotifications(true);
        // --- VIẾT CODE LỌC THÔNG BÁO CHƯA ĐỌC CỦA BẠN TẠI ĐÂY ---
        System.out.println("Đang lọc hiển thị thông báo chưa đọc...");
        // loadNotifications(true); // Ví dụ hàm load dữ liệu lọc
    }
}
