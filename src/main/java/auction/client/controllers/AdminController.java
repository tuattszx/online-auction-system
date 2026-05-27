package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.session.DataSession;
import auction.common.message.Message;
import auction.common.model.items.Item;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.util.Callback;
import javafx.util.Duration;

public class AdminController {
    @FXML private PieChart categoryChart;
    @FXML
    private TableView<Item> adminProductTable;
    @FXML
    private TableColumn<Item, Void> colSn;
    @FXML
    private TableColumn<Item, String> colName;
    @FXML
    private TableColumn<Item, String> colSeller;
    @FXML
    private TableColumn<Item, Long> colStartingPrice;
    @FXML
    private TableColumn<Item, Long> colCurrentPrice;
    @FXML
    private TableColumn<Item, String> colCurrentBidder;
    @FXML
    private TableColumn<Item, String> colStatus;
    @FXML
    private TableColumn<Item, Void> colAction;
    @FXML
    private VBox vboxAdminProducts;
    private ObservableList<Item> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1. Cấu hình các cột hiển thị dữ liệu text/số cơ bản
        colName.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));
        colSeller.setCellValueFactory(cellData ->new javafx.beans.property.SimpleStringProperty( String.valueOf(cellData.getValue().getSellerId()))); // Hoặc sellerName nếu có
        colStartingPrice.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getStartingPrice()));
        colCurrentPrice.setCellValueFactory(cellData ->  new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getCurrentPrice()));
        colCurrentBidder.setCellValueFactory(cellData ->new javafx.beans.property.SimpleStringProperty( String.valueOf(cellData.getValue().getCurrentBidderId())));
        colStatus.setCellValueFactory(cellData ->new javafx.beans.property.SimpleStringProperty( String.valueOf(cellData.getValue().getStatus())));

        // 2. Tự động tăng số thứ tự cho cột colSn (#)
        setupSerialColumn();

        // 3. Khởi tạo 2 nút Approve và Reject sinh động cho cột Hành động
        setupActionColumn();

        // 4. Tải dữ liệu từ Server lên bảng khi mở màn hình
        handleRefreshProducts();
        btnApproveItems.getStyleClass().add("admin-menu-btn");
        btnApproveSeller.getStyleClass().add("admin-menu-btn");
        btnSettings.getStyleClass().add("admin-menu-btn");
        btnManageAuctions.getStyleClass().add("admin-menu-btn");
        btnManageUsers.getStyleClass().add("admin-menu-btn");
        btnTransactionHistory.getStyleClass().add("admin-menu-btn");
        btnLockAccount.getStyleClass().add("admin-menu-btn");
        setActiveButton(btnDashboard);
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
        vboxAdminProducts.setVisible(false);
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
        handleSwitchHbox(vboxAdminProducts);
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
    private void setupSerialColumn() {
        colSn.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });
    }

    private void setupActionColumn() {
        colAction.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Item, Void> call(TableColumn<Item, Void> param) {
                return new TableCell<>() {
                    private final Button btnApprove = new Button("Approve");
                    private final Button btnReject = new Button("Reject");
                    private final HBox container = new HBox(10, btnApprove, btnReject);

                    {
                        // Định dạng giao diện nút hiện đại cho Admin
                        btnApprove.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
                        btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
                        container.setAlignment(Pos.CENTER);

                        // Sự kiện khi bấm nút APPROVE (Duyệt cho sản phẩm lên sàn)
                        btnApprove.setOnAction(e -> {
                            Item selectedItem = getTableRow().getItem();
                            if (selectedItem != null) {
                                processProductApproval(selectedItem, "CONFIRM_ITEM", "PENDING",true);
                            }
                        });

                        // Sự kiện khi bấm nút REJECT (Từ chối phê duyệt)
                        btnReject.setOnAction(e -> {
                            Item selectedItem = getTableRow().getItem();
                            if (selectedItem != null) {
                                processProductApproval(selectedItem, "CONFIRM_ITEM", "DELETED",false);
                            }
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            Item currentItem = getTableRow().getItem();
                            // Chỉ hiển thị nút xử lý nếu sản phẩm đang ở trạng thái chờ duyệt (PENDING)
                            if (currentItem != null && "PENDING".equals(currentItem.getStatus())) {
                                setGraphic(container);
                            } else {
                                setGraphic(null); // Đã duyệt hoặc đóng sàn rồi thì ẩn nút đi
                            }
                        }
                    }
                };
            }
        });
    }

    /**
     * Hàm gửi yêu cầu Approve/Reject lên Server qua Socket mạng
     */
    private void processProductApproval(Item item, String command, String targetStatus, boolean isapproved) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc chắn muốn thực hiện hành động này?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận kiểm duyệt");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                new Thread(() -> {
                    // Gửi mã lệnh xử lý kèm theo ID của sản phẩm lên Server
                    Message request = new Message(command, new Object[]{item.getId(),isapproved});
                    Message response = ClientNetwork.getInstance().sendRequest(request);

                    Platform.runLater(() -> {
                        if (response != null && "SUCCESS".equals(response.getStatus())) {
                            // Cập nhật trạng thái nóng ngay trên giao diện mà không cần reload toàn bộ bảng
                            item.setStatus(targetStatus);
                            adminProductTable.refresh();

                            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Xử lý phê duyệt sản phẩm thành công!");
                            alert.show();
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi hệ thống: Không thể xử lý phê duyệt.");
                            alert.show();
                        }
                    });
                }).start();
            }
        });
    }

    /**
     * Hàm lấy toàn bộ danh sách sản phẩm từ Server về đổ vào TableView
     */
    @FXML
    public void handleRefreshProducts() {
        new Thread(() -> {
            Message request = new Message("GET_ALL_ITEMS", null); // Gọi lệnh lấy hết sản phẩm hệ thống
            Message response = ClientNetwork.getInstance().sendRequest(request);

            Platform.runLater(() -> {
                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    java.util.List<Item> items = (java.util.List<Item>) response.getData();
                    masterData.clear();
                    if (items != null) {
                        masterData.addAll(items);
                    }
                    adminProductTable.setItems(masterData);
                }
            });
        }).start();
    }
}