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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
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
    @FXML
    private HBox searchBar; // Liên kết với thanh tìm kiếm
    @FXML
    private HeaderMenuController headerMenuController;
    @FXML
    private VBox vboxMainNotifications;
    private Consumer<Notification> realtimeCallback;
    @FXML
    private Button btnAllread;
    @FXML
    private Button btnUnread;
    @FXML
    private Label threedot; // Nút ba chấm trên giao diện

    @FXML
    private VBox menuOverlay; // Khung hộp menu chứa 2 lựa chọn

    @FXML
    private HBox btnMarkAllRead; // Dòng Đánh dấu tất cả là đã đọc

    @FXML
    private HBox btnClearAllNoti; // Dòng Xóa tất cả

    // Giả sử gốc ngoài cùng của file FXML là AnchorPane, bạn gán fx:id cho nó là rootPane để bắt sự kiện click ra ngoài
    @FXML
    private AnchorPane rootPane;
    private boolean isFilteringUnread = false;

    @FXML
    public void initialize() {
        menuOverlay.setVisible(false);

        // 2. Khi click vào bất kỳ vùng trống nào trên rootPane (ngoài menu), thực hiện ẩn menu
        rootPane.setOnMouseClicked(event -> {
            if (menuOverlay.isVisible()) {
                menuOverlay.setVisible(false);
            }
        });
        threedot.setOnMouseClicked(event -> {
            // Đảo ngược trạng thái Hiện/Ẩn của menu
            boolean isNowVisible = !menuOverlay.isVisible();
            menuOverlay.setVisible(isNowVisible);

            if (isNowVisible) {
                menuOverlay.toFront();
            }

            // Ngăn sự kiện click sủi bọt ra rootPane (tránh việc vừa mở xong lại bị đóng ngay lập tức)
            event.consume();
        });

        // 4. Bắt sự kiện click chuột cho 2 dòng lựa chọn bên trong Menu
        btnMarkAllRead.setOnMouseClicked(event -> {
            handleMarkAllAsRead();
            menuOverlay.setVisible(false); // Xử lý xong tự đóng menu
            event.consume();
        });

        btnClearAllNoti.setOnMouseClicked(event -> {
            handleDeleteAllNotify();
            menuOverlay.setVisible(false); // Xử lý xong tự đóng menu
            event.consume();
        });

        // Ngăn chặn việc click trực tiếp vào khoảng trắng bên trong hộp menu làm ẩn chính nó
        menuOverlay.setOnMouseClicked(MouseEvent::consume);
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
        if (isUnread) {
            lblTime.setStyle("-fx-font-size: 12px; -fx-text-fill: #1877f2; -fx-font-weight: bold;");
        }

        textContainer.getChildren().addAll(titleFlow, messageFlow, lblTime);

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
                Message response = ClientNetwork.getInstance().sendRequest(request);

                List<HBox> uiRows = new ArrayList<>();
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
        };
        ;

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

    /**
     * Hàm xử lý khi bấm "Đánh dấu tất cả là đã đọc"
     */
    private void handleMarkAllAsRead() {
        System.out.println("LOG: Người dùng bấm Đánh dấu tất cả là đã đọc");

        new Thread(() -> {
            try {
                // Lấy ID của người dùng hiện tại đang đăng nhập trong hệ thống của bạn

                // BƯỚC 1: Truyền mã lệnh "READ_ALL_NOTIF" kèm theo ID người dùng lên Server
                Message request = new Message("READ_ALL_NOTIF", DataSession.getInstance().getLoggedInUser().getId());
                Message response = ClientNetwork.getInstance().sendRequest(request);

                // BƯỚC 2: Khi nhận phản hồi thành công, cập nhật lại giao diện trên UI Thread
                Platform.runLater(() -> {
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        // Gọi hàm tải lại danh sách thông báo đã có sẵn trong controller của bạn
                        loadNotifications(isFilteringUnread);
                    } else {
                        System.err.println("LỖI: Server không thể cập nhật trạng thái đọc thông báo.");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Hàm xử lý khi bấm "Xóa tất cả"
     */
    private void handleDeleteAllNotify() {
        System.out.println("LOG: Người dùng bấm Xóa tất cả thông báo");

        new Thread(() -> {
            try {
                // Lấy ID của người dùng hiện tại đang đăng nhập trong hệ thống của bạn

                // BƯỚC 1: Truyền mã lệnh "CLEAR_ALL_NOTIF" kèm theo ID người dùng lên Server
                Message request = new Message("CLEAR_ALL_NOTIF", DataSession.getInstance().getLoggedInUser().getId());
                Message response = ClientNetwork.getInstance().sendRequest(request);

                // BƯỚC 2: Khi nhận phản hồi thành công, dọn dẹp sạch sẽ giao diện hiển thị
                Platform.runLater(() -> {
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        // Xóa toàn bộ các phần tử hiển thị trong danh sách VBox của bạn
                        vboxMainNotifications.getChildren().clear();

                        // Thêm lại tiêu đề chữ "Mới" để giữ đúng thiết kế ban đầu
                        Label lblMoi = new Label("Mới");
                        lblMoi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #050505;");
                        VBox.setMargin(lblMoi, new Insets(10, 0, 5, 16));
                        vboxMainNotifications.getChildren().add(lblMoi);
                    } else {
                        System.err.println("LỖI: Server không thể thực hiện xóa tất cả thông báo.");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}