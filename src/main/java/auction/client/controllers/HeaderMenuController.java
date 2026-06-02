package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.services.NotificationSubscriptionManager;
import auction.client.session.DataSession;
import auction.client.utils.ToastManager;
import auction.common.message.Message;
import auction.common.model.notifications.BidNotification;
import auction.common.model.notifications.ItemNotification;
import auction.common.model.notifications.SystemNotification;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

public class HeaderMenuController {

    @FXML private TextField txtsearch;
    @FXML private Label lbsell;
    @FXML private Label lbbalance;
    @FXML private AnchorPane bellContainer;
    @FXML private VBox notifPane;
    @FXML private VBox vboxNotifItems;
    @FXML private StackPane badgeContainer;
    @FXML private Label lblBellBadge;
    @FXML private Region regionsearch;
    @FXML private Region regionsell;

    @FXML private HBox searchBar; // Liên kết với thanh tìm kiếm


    @FXML
    public void initialize() {
        // Khởi tạo trạng thái ban đầu: 0 thông báo ẩn chấm đỏ
        txtsearch.textProperty().addListener((observable, oldValue, newValue) -> {

            // Lấy tham chiếu của MainViewController từ DataSession ra
            MainViewController mainVC = DataSession.getInstance().getMainViewController();

            if (mainVC != null) {
                // Ra lệnh cho MainViewController thực hiện lọc dữ liệu với từ khóa mới (newValue)
                mainVC.handleSearch(newValue);
            }
        });
        if (DataSession.getInstance().getLoggedInUser() != null) {
            Task<Integer> countTask = new Task<>() {
                @Override
                protected Integer call() throws Exception {
                    // Gửi request siêu gọn với Action mới
                    Message request = new Message("GET_UNREAD_COUNT", DataSession.getInstance().getLoggedInUser().getId());
                    Message response = ClientNetwork.getInstance().sendRequest(request);

                    // Nếu Server trả về thành công, lấy trực tiếp số int ra
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        return (Integer) response.getData();
                    }
                    return 0;
                }
            };

            countTask.setOnSucceeded(e -> {
                int unreadServerCount = countTask.getValue();
                DataSession.getInstance().setUnreadNotificationCount(unreadServerCount);
                updateBadgeUI();
            });

            new Thread(countTask).start();
        }
        else {
            updateBadgeUI();
        }
        // DĂNG KÝ TỔNG ĐÀI REAL-TIME: Hễ mạng nhận được thông báo đè giá là nhảy số luôn
        NotificationSubscriptionManager.getInstance().subscribe(newNotif -> {
            Platform.runLater(() -> {
                // Tăng biến đếm thông báo chưa đọc
                DataSession.getInstance().incrementUnreadNotificationCount();
                updateBadgeUI();

                Stage activeStage = null;
                if (bellContainer != null && bellContainer.getScene() != null) {
                    activeStage = (Stage) bellContainer.getScene().getWindow();
                } else {
                    // Phương án dự phòng: Quét trong danh sách window đang active của hệ thống
                    activeStage = Window.getWindows().stream()
                            .filter(window -> window instanceof Stage)
                            .map(window -> (Stage) window)
                            .filter(Stage::isShowing)
                            .findFirst()
                            .orElse(null);
                }

                if (activeStage != null) {
                    // Tự động phân loại màu sắc Toast dựa vào loại Notification
                    ToastManager.ToastType toastType = ToastManager.ToastType.SUCCESS;
                    if (newNotif instanceof BidNotification) {
                        toastType = ToastManager.ToastType.INFO;
                    } else if (newNotif instanceof ItemNotification) {
                        toastType = ToastManager.ToastType.WARNING;
                    } else if (newNotif instanceof SystemNotification){
                        toastType = ToastManager.ToastType.ERROR;
                    }

                    // Gọi ToastManager của bạn
                    String fullMessage = newNotif.getTitle() + ": " + newNotif.getMessage();
                    ToastManager.showToast(activeStage, toastType, fullMessage);
                }
            });
        });
    }
    // Hàm dùng để ẩn thanh tìm kiếm và thu hồi lại diện tích trống
    public void hideSearchBar() {
        if (searchBar != null) {
            searchBar.setVisible(false);
            searchBar.setManaged(false); // Dòng này cực kỳ quan trọng: nó giúp các thành phần khác tự động tràn vào lấp chỗ trống, không để lại một khoảng trắng vô duyên.
        }
        regionsearch.setVisible(false);
        regionsearch.setManaged(false);
    }
    public void resetText(){
        lbsell.setVisible(false);lbsell.setManaged(false);
        regionsell.setVisible(false);
        regionsell.setManaged(false);
    }

    private void updateBadgeUI() {
        if (DataSession.getInstance().getUnreadNotificationCount() > 0) {
            lblBellBadge.setText(String.valueOf(DataSession.getInstance().getUnreadNotificationCount()));
            badgeContainer.setVisible(true);
        } else {
            lblBellBadge.setText("0");
            badgeContainer.setVisible(false);
        }
    }

    // Viết thêm hàm này để các Controller khác gọi tới
    public void setBalance(String amount) {
//        //lbbalance.setText(amount);
   }

    @FXML
    public void OnMouseBacktoMain(MouseEvent event){
        ViewManager.switchScene(event,"main-view.fxml", "Trang chủ");

    }

    @FXML
    public void OnMouseCart(MouseEvent event){
        ViewManager.switchScene(event,"cart-view.fxml", "rỏ hàng");

    }

    @FXML
    public void onSellerClick(MouseEvent event) throws IOException {
        ViewManager.switchScene(event, "seller-view.fxml", "seller page");
    }

    @FXML
    public void GoToFavoriteView(MouseEvent event){
        ViewManager.switchScene(event,"favourite-view.fxml", " yêu thích");

    }

    @FXML
    void handleShowNotifications(MouseEvent event) {
        // Ẩn/hiển thị cái pop-up notifPane nhỏ ở chuông
        notifPane.setVisible(!notifPane.isVisible());
    }

    @FXML
    public void onProfileClick(MouseEvent event) throws IOException {
        if (DataSession.getInstance().getLoggedInUser() == null) return;

        String view = DataSession.getInstance().getLoggedInUser().getRole().equals("ADMIN") ? "admin-view.fxml" : "profile-view.fxml";
        ViewManager.switchScene(event, view, "Hồ sơ cá nhân");
    }

    @FXML
    public void onBellClick(MouseEvent event) throws IOException {
        NotificationPopupController.toggleNotification(bellContainer);
        DataSession.getInstance().setUnreadNotificationCount(0);
        updateBadgeUI();
      //  ViewManager.switchScene(event, "notification-view.fxml", "thông báo");
        NotificationPopupController.setOnMoreOptionsClickListener(() -> {
            System.out.println("HeaderMenu đã nhận được tín hiệu! Đang chuyển trang an toàn...");

            // Gọi CHÍNH XÁC hàm switchScene của bạn bằng 'event' của chiếc chuông
            // Vì chiếc chuông nằm ở Stage gốc, ViewManager sẽ không bao giờ bị lỗi ép kiểu nữa!
            ViewManager.switchScene(event, "notification-view.fxml", "thông báo");
        });
    }

    @FXML
    private void onSearchAction() {
        // Implement sau
    }
}