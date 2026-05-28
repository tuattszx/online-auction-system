package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.session.DataSession;
import auction.common.message.Message;
import auction.common.model.items.Item;
import auction.common.model.users.User;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminController {
    @FXML Label lblTotalRevenue;
    @FXML Label lblLiveAuctions;
    @FXML Label lblUsers;
    @FXML Label lblSuccessRate;
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

    @FXML private TableView<User> adminTable;
    @FXML private TableColumn<User, Number> colId; // Đổi sang Number để làm STT
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, Long> colBalance;
    @FXML private TableColumn<User, Long> colFrozen;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, Void> colActions;

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

        setupPieChart();

        colFullName.setCellValueFactory(cellData -> {
            User u = cellData.getValue();
            String fullName = u.getDisplayName() != null ? u.getDisplayName() : (u.getFirstName() + " " + u.getLastName());
            return new javafx.beans.property.SimpleStringProperty(fullName.trim());
        });

        colUsername.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUsername()));
        colEmail.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
        colRole.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRole()));
        colBalance.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getBalance()));
        colFrozen.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getFrozenBalance()));

        setupIdColumn();
        setupCurrencyColumns();
        setupActionsColumn();

        loadUsersData();
        loadDashboardStatistics();
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

    private void setupIdColumn() {
        colId.setCellFactory(column -> new TableCell<User, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });
    }

    // --- TÁCH HÀM 2: Cấu hình định dạng tiền tệ ($1,000) ---
    private void setupCurrencyColumns() {
        // Tạo 1 factory dùng chung cho cả 2 cột tiền để tránh lặp code
        Callback<TableColumn<User, Long>, TableCell<User, Long>> currencyCellFactory = column -> {
            return new TableCell<User, Long>() {
                @Override
                protected void updateItem(Long price, boolean empty) {
                    super.updateItem(price, empty);
                    if (empty || price == null) {
                        setText(null);
                    } else {
                        setText(String.format("$%,d", price));
                    }
                }
            };
        };

        colBalance.setCellFactory(currencyCellFactory);
        colFrozen.setCellFactory(currencyCellFactory);
    }

    // --- TÁCH HÀM 3: Cấu hình cột nút bấm hành động ---
    private void setupActionsColumn() {
        colActions.setCellFactory(column -> new TableCell<User, Void>() {
            private final Button btnWarn = new Button("⚠️");
            private final Button btnToggleBan = new Button();
            private final HBox pane = new HBox(btnWarn, btnToggleBan);

            {
                pane.setSpacing(10);
                pane.setAlignment(javafx.geometry.Pos.CENTER);
                btnWarn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4; -fx-padding: 4 8;");
                btnToggleBan.setOnAction(e -> {
                    User user = (User) getTableRow().getItem();
                    if (user != null) {
                        if (user.isBanned()) {
                            handleUnbanUser(user);
                        } else {
                            handleBanUser(user);
                        }
                    }
                });

                btnWarn.setOnAction(e -> {
                    User user = (User) getTableRow().getItem();
                    if (user != null) handleWarnUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = (User) getTableRow().getItem();
                    if (user != null) {
                        // Thay đổi trạng thái nút dựa trên trường isBanned của Model
                        if (user.isBanned()) {
                            btnToggleBan.setText("🔓"); // Icon Unban
                            btnToggleBan.setStyle("-fx-background-color: #2ec4b6; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4; -fx-padding: 4 8;");
                        } else {
                            btnToggleBan.setText("❌"); // Icon Ban
                            btnToggleBan.setStyle("-fx-background-color: #e63946; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4; -fx-padding: 4 8;");
                        }
                    }
                    setGraphic(pane);
                }
            }
        });
    }

    private void setupPieChart() {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Hàng điện tử", 35),
                new PieChart.Data("Đồ cổ", 25),
                new PieChart.Data("Thời trang", 20),
                new PieChart.Data("Sách", 10),
                new PieChart.Data("Trang sức", 10),
                new PieChart.Data("Khác", 6)
        );
        categoryChart.setData(pieChartData);

        for (PieChart.Data data : pieChartData) {
            data.nameProperty().bind(
                    javafx.beans.binding.Bindings.concat(
                            data.getName(), " ", String.format("%.1f%%", data.getPieValue())
                    )
            );
        }
    }

    private void loadUsersData() {
        Task<List<User>> loadTask = new Task<>() {
            @Override
            protected List<User> call() throws Exception {
                // Giả sử server của bạn trả về Message chứa danh sách toàn bộ User
                Message response = network.sendRequest(new Message("GET_ALL_USERS", null));
                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    return (List<User>) response.getData();
                }
                return new ArrayList<>();
            }
        };

        loadTask.setOnSucceeded(e -> {
            adminTable.setItems(FXCollections.observableArrayList(loadTask.getValue()));
        });

        new Thread(loadTask).start();
    }

    // Xử lý nút bấm phát thông báo Cảnh báo người dùng
    private void handleWarnUser(User user) {
        String reason = ViewManager.showInputDialog("Cảnh báo", "Nhập nội dung cảnh báo gửi tới " + user.getUsername() + ":");
        if (reason == null || reason.trim().isEmpty()) return;

        Task<Message> warnTask = new Task<>() {
            @Override
            protected Message call() throws Exception {
                Object[] payload = new Object[]{user.getId(), reason};
                return network.sendRequest(new Message("WARN_USER", payload));
            }
        };

        warnTask.setOnSucceeded(e -> {
            Message response = warnTask.getValue();
            // BẮT BUỘC: Check trạng thái phản hồi từ Server
            if (response != null && "SUCCESS".equals(response.getStatus())) {
                ViewManager.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã gửi tin nhắn cảnh báo tới người dùng!");
            } else {
                ViewManager.showAlert(Alert.AlertType.ERROR, "Thất bại", "Server không thể xử lý yêu cầu cảnh báo!");
            }
        });
        new Thread(warnTask).start();
    }

    private void handleBanUser(User user) {
        boolean confirm = ViewManager.confirmAlert("Xác nhận cấm",
                "Hành động này sẽ cấm [" + user.getUsername() + "] Bạn có chắc không?");
        if (!confirm) return;

        Task<Message> banTask = new Task<>() {
            @Override
            protected Message call() throws Exception {
                return network.sendRequest(new Message("DELETE_USER", user.getId()));
            }
        };

        banTask.setOnSucceeded(e -> {
            Message response = banTask.getValue();
            if (response != null && "SUCCESS".equals(response.getStatus())) {
                user.setBanned(true);
                adminTable.refresh();
                ViewManager.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cấm tài khoản thành công!");
            } else {
                ViewManager.showAlert(Alert.AlertType.ERROR, "Thất bại", "Không thể cấm người dùng. Vui lòng thử lại!");
            }
        });
        new Thread(banTask).start();
    }

    private void handleUnbanUser(User user) {
        boolean confirm = ViewManager.confirmAlert("Xác nhận mở khóa",
                "Bạn có chắc chắn muốn MỞ KHÓA lại cho tài khoản [" + user.getUsername() + "] không?");
        if (!confirm) return;

        Task<Message> unbanTask = new Task<>() {
            @Override
            protected Message call() throws Exception {
                return network.sendRequest(new Message("UNBAN_USER", user.getId()));
            }
        };

        unbanTask.setOnSucceeded(e -> {
            Message response = unbanTask.getValue();
            if (response != null && "SUCCESS".equals(response.getStatus())) {
                user.setBanned(false);
                adminTable.refresh();
                ViewManager.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã mở khóa tài khoản thành công!");
            } else {
                ViewManager.showAlert(Alert.AlertType.ERROR, "Thất bại", "Không thể mở khóa người dùng!");
            }
        });
        new Thread(unbanTask).start();
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
                            setGraphic(container);
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

                            Alert alert = new Alert(Alert.AlertType.INFORMATION, (String) response.getData());
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
            Message request = new Message("GET_UNAPPROVED_ITEMS", null); // Gọi lệnh lấy hết sản phẩm hệ thống
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

    private void loadDashboardStatistics() {
        Task<Map<String, Object>> statsTask = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                Message response = network.sendRequest(new Message("GET_DASHBOARD_STATS", null));
                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    return (Map<String, Object>) response.getData();
                }
                return null;
            }
        };

        statsTask.setOnSucceeded(e -> {
            Map<String, Object> stats = statsTask.getValue();
            if (stats != null) {
                // 1. Đổ dữ liệu Total Revenue (Định dạng kiểu tiền tệ: $125,430)
                long revenue = ((Number) stats.get("totalRevenue")).longValue();
                lblTotalRevenue.setText(String.format("$%,d", revenue));

                // 2. Đổ dữ liệu Live Auctions
                int live = ((Number) stats.get("liveAuctions")).intValue();
                lblLiveAuctions.setText(String.valueOf(live));

                // 3. Đổ dữ liệu Tổng số Users (Lưu ý sửa fx:id trong FXML từ lblUsers thành lblNewUsers hoặc ngược lại cho khớp)
                int users = ((Number) stats.get("totalUsers")).intValue();
                lblUsers.setText(String.format("%,d", users));

                // 4. Đổ dữ liệu Tỷ lệ thành công Success Rate (Định dạng 1 chữ số thập phân: 89.5%)
                double rate = ((Number) stats.get("successRate")).doubleValue();
                lblSuccessRate.setText(String.format("%.1f%%", rate));
            }
        });

        new Thread(statsTask).start();
    }
}