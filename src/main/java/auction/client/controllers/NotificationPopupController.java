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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static auction.client.utils.ServerTimeSync.formatRelativeTime;

public class NotificationPopupController {

    // Giữ lại các thuộc tính FXML của bạn để điều khiển dữ liệu bên trong bảng
    @FXML
    private VBox notifPane;
    @FXML
    private VBox vboxNotifItems;
    @FXML
    private Button btnAll;          // Nút "Tất cả" vừa đặt fx:id
    @FXML
    private Button btnUnread;       // Nút "Chưa đọc" vừa đặt fx:id

    // Quản lý thực thể Popup duy nhất thông qua biến static thay vì Singleton Class
    private static Popup popupInstance;
    private boolean isFilteringUnread = false;
    private static NotificationPopupController currentController;

    @FXML
    public void initialize() {
        currentController=this;
        // Hàm này tự động chạy sau khi FXML load xong.
        // Bạn có thể viết code load dữ liệu thông báo ở đây nếu muốn.
        switchButtonStyle(btnAll, true);
        switchButtonStyle(btnUnread, false);
        loadNotifications(false);

        NotificationSubscriptionManager.getInstance().subscribe(newNotif -> {
            Platform.runLater(() -> {
                loadNotifications(isFilteringUnread);
            });
        });
    }

    /**
     * Hàm dùng chung để bật/tắt bảng thông báo, gọi đúng 1 lần tại HeaderMenuController
     */
    public static void toggleNotification(Node bellNode) {
        if (popupInstance == null) {
            try {
                popupInstance = new Popup();
                popupInstance.setAutoHide(true); // Click ra ngoài tự động ẩn

                // SỬA LỖI ĐƯỜNG DẪN: Thêm dấu "/" ở đầu để JavaFX tìm từ gốc thư mục resources
                Parent popupContent = ViewManager.getView("NotificationPopup.fxml");


                popupInstance.getContent().add(popupContent);
            } catch (IOException e) {
                System.err.println("LỖI: Không tìm thấy file NotificationPopup.fxml tại thư mục resources!");
                e.printStackTrace();
                return;
            } catch (NullPointerException e) {
                System.err.println("LỖI: Đường dẫn file FXML trả về null. Hãy kiểm tra vị trí file.");
                e.printStackTrace();
                return;
            }
        }

        if (popupInstance.isShowing()) {
            popupInstance.hide();
        } else {
            if (currentController != null) {
                currentController.loadNotifications(currentController.isFilteringUnread);
            }
            // Lấy cửa sổ hiện tại
            Window window = bellNode.getScene().getWindow();

            // SỬA LỖI TỌA ĐỘ: Công thức tính chuẩn xác vị trí trên màn hình máy tính
            double x = window.getX() + bellNode.getScene().getX() + bellNode.localToScene(0, 0).getX();
            double y = window.getY() + bellNode.getScene().getY() + bellNode.localToScene(0, 0).getY();

            // Hiển thị ngay dưới chân cái chuông (dịch sang trái 230px để không tràn màn hình)
            popupInstance.show(window, x - 230, y + bellNode.getBoundsInLocal().getHeight() );
        }
    }
    @FXML
    private void handleAllClick(ActionEvent event) {
        // Thay đổi class CSS để chuyển đổi trạng thái hiển thị sáng/tối giống FB
        switchButtonStyle(btnAll, true);
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
    private void handleUnreadClick(ActionEvent event) {
        // Thay đổi class CSS sang active
        switchButtonStyle(btnUnread, true);
        switchButtonStyle(btnAll, false);
        isFilteringUnread = true;
        loadNotifications(true);
        // --- VIẾT CODE LỌC THÔNG BÁO CHƯA ĐỌC CỦA BẠN TẠI ĐÂY ---
        System.out.println("Đang lọc hiển thị thông báo chưa đọc...");
       // loadNotifications(true); // Ví dụ hàm load dữ liệu lọc
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
    // 1. Định nghĩa một giao diện nhận sự kiện click dấu 3 chấm
    public interface OnMoreOptionsClickListener {
        void onClick() throws IOException;
    }

    private static OnMoreOptionsClickListener listener;

    // 2. Hàm để HeaderMenuController đăng ký nhận sự kiện
    public static void setOnMoreOptionsClickListener(OnMoreOptionsClickListener clickListener) {
        listener = clickListener;
    }


    /**
     * Hàm xử lý khi bấm vào dấu 3 chấm
     */
    @FXML
    public void switchNotificationview(MouseEvent event) throws IOException {
        // 1. Ẩn popup đi trước
        if (popupInstance != null) {
            popupInstance.hide();
        }

        // 2. Bắn tín hiệu ra cho thằng HeaderMenu ở ngoài tự xử lý chuyển trang
        if (listener != null) {
            listener.onClick();
        }
    }

    private void loadNotifications(boolean unreadOnly) {
        if (DataSession.getInstance().getLoggedInUser() == null) return;
        vboxNotifItems.getChildren().clear();

        Task<List<Notification>> loadNotifTask = new Task<>() {
            @Override
            protected List<Notification> call() throws Exception {
                Message request = new Message("GET_MESSAGE", DataSession.getInstance().getLoggedInUser().getId());
                Message response = ClientNetwork.getInstance().sendRequest(request);

                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    return (List<Notification>) response.getData();
                }
                return new ArrayList<>();
            }
        };

        loadNotifTask.setOnSucceeded(event -> {
            List<Notification> notifications = loadNotifTask.getValue();
            vboxNotifItems.getChildren().clear();

            if (notifications == null || notifications.isEmpty()) {
                Label emptyLabel = new Label("Không có thông báo mới");
                emptyLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px; -fx-padding: 15;");
                vboxNotifItems.getChildren().add(emptyLabel);
                return;
            }

            for (Notification note : notifications) {
                if (unreadOnly && note.isRead()) {
                    continue;
                }
                // Vẽ row mini
                HBox rowItem = createMiniNotificationItemRow(note);
                vboxNotifItems.getChildren().add(rowItem);
            }
        });

        new Thread(loadNotifTask).start();
    }

    private HBox createMiniNotificationItemRow(Notification note) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 10, 6, 10)); // Thu nhỏ padding (6px thay vì 8px, 10px thay vì 16px)
        row.setSpacing(10); // Thu nhỏ khoảng cách giữa ảnh và chữ
        row.setPrefWidth(290);

        if (!note.isRead()) {
            row.setStyle("-fx-background-color: #f5f6f7; -fx-background-radius: 8; -fx-cursor: hand;");
        } else {
            row.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand;");
        }

        // Hover mượt mà
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #f2f2f2; -fx-background-radius: 8; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> {
            if (!note.isRead()) {
                row.setStyle("-fx-background-color: #f5f6f7; -fx-background-radius: 8;");
            } else {
                row.setStyle("-fx-background-color: transparent;");
            }
        });

        // 1. Thu nhỏ Avatar xuống kích thước Mini: 36x36 (Thay vì 56x56)
        String avatarUrl = "https://www.w3schools.com/howto/img_avatar.png";
        if (note instanceof BidNotification) {
            avatarUrl = getClass().getResource("/auction/img/hammernotif.jpg").toExternalForm();
        } else if (note instanceof ItemNotification) {
            avatarUrl = getClass().getResource("/auction/img/itemnotif.jpg").toExternalForm();
        }

        ImageView avatar = new ImageView(new Image(avatarUrl));
        avatar.setFitWidth(36);
        avatar.setFitHeight(36);
        Circle clip = new Circle(18, 18, 18); // Cắt tròn bán kính 18
        avatar.setClip(clip);

        // 2. Nội dung chữ thu nhỏ size (12px - 13px)
        VBox textContainer = new VBox();
        textContainer.setSpacing(2); // Giảm khoảng cách giãn dòng chữ
        HBox.setHgrow(textContainer, Priority.ALWAYS);

        Text textTitle = new Text(note.getTitle() + "\n");
        textTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-fill: #050505;"); // Tiêu đề 13px

        Text textAction = new Text(note.getMessage());
        textAction.setStyle("-fx-font-size: 12px; -fx-fill: #333333;"); // Nội dung 12px

        TextFlow contentFlow = new TextFlow(textTitle, textAction);
        contentFlow.setMaxWidth(200); // Khóa chiều rộng tránh đẩy tràn popup

        // Định dạng thời gian tương đối giống Main View của bạn
        Label lblTime = new Label(formatRelativeTime(note.getCreatedAt()));
        if (!note.isRead()) {
            lblTime.setStyle("-fx-font-size: 11px; -fx-text-fill: #1877f2; -fx-font-weight: bold;");
        } else {
            lblTime.setStyle("-fx-font-size: 11px; -fx-text-fill: #65676b;");
        }

        textContainer.getChildren().addAll(contentFlow, lblTime);
        row.getChildren().addAll(avatar, textContainer);

        // 3. Chấm tròn thông báo màu xanh dương nhỏ xinh (Bán kính 4px thay vì 6px)
        if (!note.isRead()) {
            Circle unreadDot = new Circle(4, javafx.scene.paint.Color.web("#1877f2"));
            row.getChildren().add(unreadDot);
        }

        // 4. KIỂU CLICK ĐIỀU HƯỚNG THÔNG MINH
        row.setOnMouseClicked(e -> {
            Stage primaryStage = null;
            if (popupInstance != null && popupInstance.getOwnerWindow() instanceof Stage) {
                primaryStage = (Stage) popupInstance.getOwnerWindow();
            } else {
                // Phương án dự phòng an toàn tuyệt đối nếu không lấy được từ Popup
                primaryStage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            }

            if (popupInstance != null) popupInstance.hide(); // Click phát ẩn popup luôn

            if (!note.isRead()) {
                note.setRead(true);
                new Thread(() -> {
                    try {
                        ClientNetwork.getInstance().sendRequest(new Message("MARK_AS_READ", note.getId()));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }

            final Stage finalStage = primaryStage;
            // Thực hiện chuyển View
            if (note instanceof BidNotification bidNote) {
                AuctionManager.getInstance().getLatestItemAsync(bidNote.getItemId())
                        .thenAccept(item -> Platform.runLater(() -> {
                            if (item != null) {
                                DataSession.getInstance().setSelectedItem(item);
                                ViewManager.switchScene(finalStage, "item-view.fxml", "Chi tiết: " + item.getName());
                            }
                        }));
            } else if (note instanceof ItemNotification) {
                ViewManager.switchScene(finalStage, "seller-view.fxml", "Quản lý bán hàng");
            }
        });

        return row;
    }
}