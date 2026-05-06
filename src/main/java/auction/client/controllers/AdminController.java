package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.session.DataSession;
import auction.common.message.Message;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.util.Duration;

public class AdminController {
    @FXML private PieChart categoryChart;

    @FXML
    public void initialize() {
        // Tạo dữ liệu PieChart mới bao gồm tất cả các thành phần bạn muốn
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Hàng điện tử", 35),
                new PieChart.Data("Đồ cổ", 25),
                new PieChart.Data("Thời trang", 20),
                new PieChart.Data("Sách", 10),
                new PieChart.Data("trang suc", 10),
                new PieChart.Data("Khác", 6) // Đã thêm "Khác"
        );

        categoryChart.setData(pieChartData);

        // (Tùy chọn) Thêm phần trăm vào nhãn để chuyên nghiệp hơn
        for (PieChart.Data data : pieChartData) {
            data.nameProperty().bind(
                    javafx.beans.binding.Bindings.concat(
                            data.getName(), " ", String.format("%.1f%%", data.getPieValue())
                    )
            );
        }
    }

    // --- CÁC THÀNH PHẦN MỚI CHO SIDEBAR ---
    @FXML private VBox sidebar;
    @FXML private Line sideLine;
    @FXML private Label lblAdminPanel;
    @FXML private Label txtBack, txtDashboard, txtManageUsers, txtApproveItems,
            txtManageAuctions, txtTransactionHistory, txtSettings, txtSignOut;

    private boolean isExpanded = true;

    // --- CÁC BUTTON  ---
    @FXML private Button btnDashboard;
    @FXML private Button btnManageUsers;
    @FXML private Button btnApproveItems;
    @FXML private Button btnManageAuctions;
    @FXML private Button btnTransactionHistory;
    @FXML private Button btnSettings;
    @FXML private Button btnApproveSeller;
    @FXML private Button btnLockAccount;
    @FXML private TextField txtSearch;
    @FXML private TableView<?> adminTable;

    // ___ CÁC VBOX ___
    @FXML private VBox VBoxUserManagement;
    @FXML private VBox VBoxOverview;

    ClientNetwork network = ClientNetwork.getInstance();

    // --- HÀM TOGGLE SIDEBAR MỚI ---
    @FXML
    private void toggleSidebar(Event event) {
        double targetWidth = isExpanded ? 80.0 : 260.0;
        double lineEnd = isExpanded ? 40.0 : 180.0;

        Timeline timeline = new Timeline();

        // Hiệu ứng co giãn width và đường kẻ
        KeyValue kvWidth = new KeyValue(sidebar.prefWidthProperty(), targetWidth);
        KeyValue kvLine = new KeyValue(sideLine.endXProperty(), lineEnd);

        KeyFrame kf = new KeyFrame(Duration.millis(300), kvWidth, kvLine);
        timeline.getKeyFrames().add(kf);

        if (isExpanded) {
            // Nếu đang mở -> Thu nhỏ: Ẩn chữ ngay lập tức
            setLabelsVisible(false);
        } else {
            // Nếu đang thu nhỏ -> Mở rộng: Chạy xong animation mới hiện chữ
            timeline.setOnFinished(e -> setLabelsVisible(true));
        }

        timeline.play();
        isExpanded = !isExpanded;
    }

    private void setLabelsVisible(boolean visible) {
        Label[] labels = {lblAdminPanel, txtBack, txtDashboard, txtManageUsers,
                txtApproveItems, txtManageAuctions, txtTransactionHistory,
                txtSettings, txtSignOut};
        for (Label l : labels) {
            if (l != null) {
                l.setVisible(visible);
                l.setManaged(visible); // Giúp giải phóng không gian để icon căn giữa
            }
        }
    }

    // --- CÁC HÀM CŨ GIỮ NGUYÊN ---
    @FXML
    private void handleBackToMain(ActionEvent event) {
        ViewManager.switchScene(event, "main-view.fxml", "Main");
    }

    @FXML
    private void handleSwitchHbox(VBox vBox){
        VBoxUserManagement.setVisible(false);
        VBoxOverview.setVisible(false);
        vBox.setVisible(true);
    }

    @FXML
    private void handleShowDashboard(ActionEvent event) {
        setActiveButton(btnDashboard);
        handleSwitchHbox(VBoxOverview);
    }

    @FXML
    private void handleManageUsers(ActionEvent event) {
        setActiveButton(btnManageUsers);
        handleSwitchHbox(VBoxUserManagement);
    }

    @FXML
    private void handleApproveItems(ActionEvent event) {
        setActiveButton(btnApproveItems);
    }

    @FXML
    private void handleManageAuctions(ActionEvent event) {
        setActiveButton(btnManageAuctions);
    }

    @FXML
    private void handleTransactionHistory(ActionEvent event) {
        setActiveButton(btnTransactionHistory);
    }

    @FXML
    private void handleSettings(ActionEvent event) {
        setActiveButton(btnSettings);
    }

    @FXML
    private void handleApproveSeller(ActionEvent event) {}

    @FXML
    private void onSignOutClick(ActionEvent event){
        if (!ViewManager.confirmAlert("Thông báo", "Bạn có chắc chắn muốn đăng xuất không?")) return;
        Task<Message> logoutTask = new Task<>() {
            @Override
            protected Message call() throws Exception {
                return network.sendRequest(new Message("SIGNOUT", null));
            }
        };

        logoutTask.setOnSucceeded(e -> {
            DataSession.getInstance().clear();
            ViewManager.showAlert(Alert.AlertType.INFORMATION,"Thông báo", "Đăng xuất thành công!");
            ViewManager.clearCache();
            network.close();
            ViewManager.switchScene(event, "login-view.fxml", "Đăng nhập");
        });

        new Thread(logoutTask).start();
    }

    @FXML
    private void handleLockAccount(ActionEvent event) {}

    private void setActiveButton(Button activeBtn) {
        Button[] allBtns = {btnDashboard, btnManageUsers, btnApproveItems,
                btnManageAuctions, btnTransactionHistory, btnSettings};

        for (Button btn : allBtns) {
            btn.getStyleClass().remove("admin-menu-btn-active");
            btn.getStyleClass().add("admin-menu-btn");
        }

        activeBtn.getStyleClass().remove("admin-menu-btn");
        activeBtn.getStyleClass().add("admin-menu-btn-active");
    }
}