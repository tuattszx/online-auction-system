package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.services.AdminManager;
import auction.client.session.DataSession;
import auction.client.utils.ToastManager;
import auction.common.message.Message;
import auction.common.model.items.Item;
import auction.common.model.users.User;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminController {
    @FXML private LineChart revenueChart;
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
    private final ObservableList<User> userMasterList = FXCollections.observableArrayList();
    private FilteredList<User> filteredUserList;

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
//        handleRefreshProducts();
        adminProductTable.setOnMouseClicked(event -> {
            // Kiểm tra xem có click đúp (2 click) vào dòng hợp lệ không
            if (event.getClickCount() == 2 && adminProductTable.getSelectionModel().getSelectedItem() != null) {
                Item selectedProduct = adminProductTable.getSelectionModel().getSelectedItem();

                // Gọi hàm hiển thị popup truyền data vào
                showProductDetailPopup(selectedProduct);
            }
        });
        btnApproveItems.getStyleClass().add("admin-menu-btn");
        btnApproveSeller.getStyleClass().add("admin-menu-btn");
        btnSettings.getStyleClass().add("admin-menu-btn");
        btnManageAuctions.getStyleClass().add("admin-menu-btn");
        btnManageUsers.getStyleClass().add("admin-menu-btn");
        btnTransactionHistory.getStyleClass().add("admin-menu-btn");
        btnLockAccount.getStyleClass().add("admin-menu-btn");
        setActiveButton(btnDashboard);
        // Tạo dữ liệu PieChart mới bao gồm tất cả các thành phần bạn muốn

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

        search();
        setupIdColumn();
        setupCurrencyColumns();
        setupActionsColumn();

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

    private void search(){
        filteredUserList = new FilteredList<>(userMasterList, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredUserList.setPredicate(user -> {
                // Nếu thanh tìm kiếm trống -> Hiển thị toàn bộ bản ghi người dùng
                if (newValue == null || newValue.trim().isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase().trim();

                // Lấy các trường thông tin phục vụ việc so khớp chuỗi ký tự
                String username = user.getUsername() != null ? user.getUsername().toLowerCase() : "";
                String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                String role = user.getRole() != null ? user.getRole().toLowerCase() : "";
                String displayName = user.getDisplayName() != null ? user.getDisplayName().toLowerCase() : "";
                String fullName = (user.getFirstName() + " " + user.getLastName()).toLowerCase();

                // Kiểm tra ký tự trùng khớp đa tiêu chí: Username, Tên đầy đủ, Email hoặc Quyền hạn
                if (username.contains(lowerCaseFilter)) {
                    return true;
                } else if (displayName.contains(lowerCaseFilter)) {
                    return true;
                } else if (fullName.contains(lowerCaseFilter)) {
                    return true;
                } else if (email.contains(lowerCaseFilter)) {
                    return true;
                } else if (role.contains(lowerCaseFilter)) {
                    return true;
                }

                return false; // Không khớp trường nào -> Ẩn bản ghi này khỏi bảng
            });
        });

        adminTable.setItems(filteredUserList);
    }

    private void setupPieChart(Map<Integer,Integer> categoryDistribution) {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        categoryDistribution.forEach((catIdKey, countVal) -> {
            int catId = Integer.parseInt(catIdKey.toString());
            int count = countVal.intValue();
            String catLabel = getCategoryLabelById(catId);
            pieChartData.add(new PieChart.Data(catLabel, count));
        });
        categoryChart.setData(pieChartData);

        for (PieChart.Data data : pieChartData) {
            data.nameProperty().bind(
                    javafx.beans.binding.Bindings.concat(
                            data.getName(), " ", String.format("%.1f%%", data.getPieValue())
                    )
            );
        }
    }

    private String getCategoryLabelById(int catId) {
        switch (catId) {
            case 30003: return "Nghệ thuật (🖼)";
            case 30001: return "Nội thất (🛋)";
            case 30004: return "Trang sức (💎)";
            case 60001: return "Đồng hồ (⌚)";
            case 60002: return "Thời trang (👜)";
            case 60003: return "Tiền cổ (✪)";
            case 30002: return "Xe cộ (🚗)";
            case 60004: return "Rượu vang (🍷)";
            case 60005: return "Sách báo (📚)";
            default: return "Khác (📦)";
        }
    }

    private void loadUsersData() {
        AdminManager.getInstance().getAllUsersAsync()
                .thenAcceptAsync(users -> {
                    userMasterList.setAll(users);
                    handleRefreshProducts();
                }, Platform::runLater)
                .exceptionally(ex -> {
                    System.err.println("Lỗi nạp users: " + ex.getMessage());
                    Platform.runLater(this::handleRefreshProducts);
                    return null;
                });
    }

    // Xử lý nút bấm phát thông báo Cảnh báo người dùng
    private void handleWarnUser(User user) {
        String reason = ViewManager.showInputDialog("Cảnh báo", "Nhập nội dung cảnh báo gửi tới " + user.getUsername() + ":");
        if (reason == null || reason.trim().isEmpty()) return;

        AdminManager.getInstance().warnUserAsync(user.getId(), reason)
                .thenAcceptAsync(response -> {
                    Stage currentStage= (Stage) btnManageUsers.getScene().getWindow();
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        ToastManager.showToast(currentStage, ToastManager.ToastType.SUCCESS,"Đã gửi tin nhắn cảnh báo tới người dùng!");
                    } else {
                        ToastManager.showToast(currentStage, ToastManager.ToastType.WARNING,"Server không thể xử lý yêu cầu cảnh báo!");
                    }
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi kết nối mạng: " + ex.getMessage()));
                    return null;
                });
    }

    private void handleBanUser(User user) {
        boolean confirm = ViewManager.confirmAlert("Xác nhận cấm",
                "Hành động này sẽ cấm [" + user.getUsername() + "] Bạn có chắc không?");
        if (!confirm) return;

        AdminManager.getInstance().banUserAsync(user.getId())
                .thenAcceptAsync(response -> {
                    Stage currentStage= (Stage) btnManageUsers.getScene().getWindow();
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        user.setBanned(true);
                        adminTable.refresh();
                        ToastManager.showToast(currentStage, ToastManager.ToastType.SUCCESS,"Đã cấm tài khoản thành công!");
                    } else {
                        ToastManager.showToast(currentStage, ToastManager.ToastType.WARNING,"Không thể cấm người dùng. Vui lòng thử lại!");
                    }
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi hệ thống: " + ex.getMessage()));
                    return null;
                });
    }

    private void handleUnbanUser(User user) {
        boolean confirm = ViewManager.confirmAlert("Xác nhận mở khóa",
                "Bạn có chắc chắn muốn MỞ KHÓA lại cho tài khoản [" + user.getUsername() + "] không?");
        if (!confirm) return;

        AdminManager.getInstance().unbanUserAsync(user.getId())
                .thenAcceptAsync(response -> {
                    Stage currentStage= (Stage) btnManageUsers.getScene().getWindow();
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        user.setBanned(false);
                        adminTable.refresh();
                        ToastManager.showToast(currentStage, ToastManager.ToastType.SUCCESS,"Đã mở khóa tài khoản thành công!");
                    } else {
                        ToastManager.showToast(currentStage, ToastManager.ToastType.WARNING,"Không thể mở khóa người dùng!");
                    }
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi hệ thống: " + ex.getMessage()));
                    return null;
                });
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
        loadUsersData();
    }


    @FXML
    private void handleApproveItems(ActionEvent event) {
        setActiveButton(btnApproveItems);
        handleSwitchHbox(vboxAdminProducts);
        loadDashboardStatistics();
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
                AdminManager.getInstance().confirmItemAsync(item.getId(), isapproved)
                        .thenAcceptAsync(response -> {
                            Stage currentStage= (Stage) btnApproveItems.getScene().getWindow();
                            if (response != null && "SUCCESS".equals(response.getStatus())) {
                                item.setStatus(targetStatus);
                                adminProductTable.refresh();
                                ToastManager.showToast(currentStage, ToastManager.ToastType.SUCCESS,(String) response.getData());
                            } else {
                                ToastManager.showToast(currentStage, ToastManager.ToastType.WARNING,"Lỗi hệ thống: Không thể xử lý phê duyệt.");
                            }
                        }, Platform::runLater)
                        .exceptionally(ex -> {
                            Platform.runLater(() -> ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi kết nối: " + ex.getMessage()));
                            return null;
                        });
            }
        });
    }

    /**
     * Hàm lấy toàn bộ danh sách sản phẩm từ Server về đổ vào TableView
     */
    @FXML
    public void handleRefreshProducts() {
        AdminManager.getInstance().getUnapprovedItemsAsync()
                .thenAcceptAsync(items -> {
                    masterData.clear();
                    if (items != null) {
                        masterData.addAll(items);
                    }
                    adminProductTable.setItems(masterData);
                }, Platform::runLater)
                .exceptionally(ex -> {
                    System.err.println("Lỗi nạp sản phẩm chưa duyệt: " + ex.getMessage());
                    return null;
                });
    }

    private void loadDashboardStatistics() {
        AdminManager.getInstance().getDashboardStatsAsync()
                .thenAcceptAsync(stats -> {
                    if (stats != null) {
                        lblTotalRevenue.setText(String.format("$%,d", ((Number) stats.get("totalRevenue")).longValue()));
                        lblLiveAuctions.setText(String.valueOf(((Number) stats.get("liveAuctions")).intValue()));
                        lblUsers.setText(String.format("%,d", ((Number) stats.get("totalUsers")).intValue()));
                        lblSuccessRate.setText(String.format("%.1f%%", ((Number) stats.get("successRate")).doubleValue()));
                        setupPieChart((Map<Integer, Integer>) stats.get("categoryDistribution"));
                        if (stats.containsKey("revenueTrend")) {
                            revenueChart.getData().clear();

                            javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();

                            List<ArrayList<Object>> trendList = (List<ArrayList<Object>>) stats.get("revenueTrend");

                            for (Object itemObj : trendList) {
                                List<Object> dataPoint = (List<Object>) itemObj;
                                String dayLabel = (String) dataPoint.get(0);
                                Number dailyAmount = (Number) dataPoint.get(1);
                                series.getData().add(new javafx.scene.chart.XYChart.Data<>(dayLabel, dailyAmount));
                            }
                            revenueChart.setData(FXCollections.observableArrayList(series));
                        }
                    }
                    loadUsersData();
                }, Platform::runLater)
                .exceptionally(ex -> {
                    System.err.println("Lỗi nạp stats: " + ex.getMessage());
                    Platform.runLater(this::loadUsersData);
                    return null;
                });
    }

    public void showProductDetailPopup(Item product) { // Thay 'Object' bằng Model của bạn, ví dụ: 'ProductModel product'
        // 1. Tạo Stage cho Popup
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initStyle(StageStyle.TRANSPARENT); // Giúp bo góc không bị viền trắng Windows

        // --- CONTAINER CHÍNH ---
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        // Bo góc 12px, nền trắng, đổ bóng mờ hiện đại
        root.setStyle(
                "-fx-background-color: #ffffff; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-radius: 12; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 0);"
        );
        root.setPrefWidth(520);

        // --- 1. HEADER ---
        VBox header = new VBox(4);
        Label titleLabel = new Label("Thông Tin Chi Tiết Sản Phẩm");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.web("#1e293b"));

        Label subTitleLabel = new Label("Xem thông tin và kiểm duyệt sản phẩm trên hệ thống");
        subTitleLabel.setFont(Font.font("System", 13));
        subTitleLabel.setTextFill(Color.web("#64748b"));
        header.getChildren().addAll(titleLabel, subTitleLabel);

        // --- 2. THÔNG TIN CHÍNH (ẢNH & THÔNG SỐ) ---
        HBox bodyBox = new HBox(20);
        bodyBox.setAlignment(Pos.TOP_LEFT);

        // Khung chứa ảnh sản phẩm
        VBox imageContainer = new VBox();
        imageContainer.setPrefSize(140, 140);
        imageContainer.setMinSize(140, 140);
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8;");

        ImageView imgProduct = new ImageView();
        imgProduct.setFitWidth(130);
        imgProduct.setFitHeight(130);
        imgProduct.setPreserveRatio(true);
        if (product.getImages() != null && !product.getImages().isEmpty()) { //

            // 1. Dùng mảng 1 phần tử hoặc biến lớp để lưu vị trí ảnh hiện tại (bắt đầu từ 0)
            AtomicInteger count= new AtomicInteger();

            // 2. Tạo hàm cập nhật ảnh để tái sử dụng khi click
            Runnable updateImageDisplay = () -> {
                String imgUrl = product.getImages().get(count.get()).getUrlImage();
                if (imgUrl != null && !imgUrl.isEmpty()) { //
                    // Bật backgroundLoading để không làm đơ giao diện khi load ảnh mới từ mạng
                    Image img = new Image(imgUrl, true); //
                    imgProduct.setImage(img); //
                }
            };

            // Hiển thị ảnh đầu tiên khi vừa mở giao diện lên
            updateImageDisplay.run();

            // 3. Thiết lập hiệu ứng và sự kiện Click cho khung hiển thị ảnh
            imgProduct.setCursor(Cursor.HAND); // Đổi chuột thành hình bàn tay khi di chuyển vào ảnh để báo hiệu click được

            imgProduct.setOnMouseClicked(event -> {
                // Tăng index lên 1, nếu vượt quá số lượng ảnh thì quay về 0 (Vòng lặp ảnh)
                count.set((count.get() + 1) % product.getImages().size());

                // Gọi hàm cập nhật hiển thị ảnh mới
                updateImageDisplay.run();
            });
        } else {
            // Trường hợp sản phẩm không có ảnh nào, hiển thị ảnh lỗi/mặc định hệ thống
            Image defaultImg = new Image(getClass().getResourceAsStream("/images/default-bell.png"));
            imgProduct.setImage(defaultImg);
        }

        imageContainer.getChildren().add(imgProduct);

        // Grid chứa thông tin dạng Label: Value
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        HBox.setHgrow(grid, Priority.ALWAYS);

        // Định nghĩa độ rộng cột danh mục (cột 0)
        ColumnConstraints col1 = new ColumnConstraints(100);
        grid.getColumnConstraints().add(col1);

        // Tạo các nhãn thông tin (Dữ liệu cứng mẫu, bạn thay bằng product.get...() nhé)
        addInfoRow(grid, 0, "Tên sản phẩm:", "caa", true, "#0f172a");
        addInfoRow(grid, 1, "Người bán ID:", "420001", false, "#334155");
        addInfoRow(grid, 2, "Giá khởi điểm:", "1 USD", true, "#0284c7"); // Chữ đậm màu xanh dương
        addInfoRow(grid, 3, "Kích thước:", "10 x 10 x 10 cm", false, "#334155");

        bodyBox.getChildren().addAll(imageContainer, grid);

        // --- 3. KHUNG THỜI GIAN ĐẤU GIÁ (Nền xám nhạt) ---
        VBox timeBox = new VBox(8);
        timeBox.setPadding(new Insets(12));
        timeBox.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 8;");

        HBox startRow = new HBox(10);
        Label lblStartTitle = new Label("Thời gian bắt đầu:");
        lblStartTitle.setPrefWidth(110);
        lblStartTitle.setTextFill(Color.web("#64748b"));
        Label lblStartVal = new Label(); // product.getStartTime()
        lblStartVal.setText(String.valueOf(product.getStartTime()));
        lblStartVal.setTextFill(Color.web("#334155"));
        startRow.getChildren().addAll(lblStartTitle, lblStartVal);

        HBox endRow = new HBox(10);
        Label lblEndTitle = new Label("Thời gian kết thúc:");
        lblEndTitle.setPrefWidth(110);
        lblEndTitle.setTextFill(Color.web("#64748b"));
        Label lblEndVal = new Label(); // product.getEndTime()
        lblEndVal.setText(String.valueOf(product.getEndTime()));
        lblEndVal.setTextFill(Color.web("#334155"));
        endRow.getChildren().addAll(lblEndTitle, lblEndVal);

        timeBox.getChildren().addAll(startRow, endRow);

        // --- 4. PHẦN MÔ TẢ ---
        VBox descBox = new VBox(6);
        Label descTitle = new Label("Mô tả sản phẩm:");
        descTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
        descTitle.setTextFill(Color.web("#64748b"));

        Label descValue = new Label();
        descValue.setText(product.getDescription());
        descValue.setTextFill(Color.web("#475569"));
        descValue.setWrapText(true); // Tự động xuống dòng cực kỳ quan trọng
        descBox.getChildren().addAll(descTitle, descValue);

        // --- 5. HÀNG NÚT BẤM (FOOTER) ---
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(footer, new Insets(10, 0, 0, 0));

        Button btnClose = new Button("Đóng");
        btnClose.setPrefSize(90, 36);
        btnClose.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-background-radius: 6; -fx-cursor: hand;");
        btnClose.setOnAction(e -> popupStage.close()); // Đóng popup khi nhấn

        Button btnApprove = new Button("Duyệt");
        btnApprove.setPrefSize(90, 36);
        btnApprove.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        btnApprove.setOnAction(e -> {
            if (product != null) {
                processProductApproval(product, "CONFIRM_ITEM", "PENDING",true);
            }
        });

        footer.getChildren().addAll(btnClose, btnApprove);

        // --- THÊM TẤT CẢ VÀO ROOT VÀ HIỂN THỊ ---
        root.getChildren().addAll(header, bodyBox, timeBox, descBox, footer);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT); // Đảm bảo bo góc mượt mà
        popupStage.setScene(scene);
        popupStage.showAndWait();
    }

    /**
     * Hàm hỗ trợ tạo nhanh các dòng thông tin trong GridPane đỡ phải lặp lại code
     */
    private void addInfoRow(GridPane grid, int row, String labelText, String valueText, boolean isBold, String hexColor) {
        Label lblTitle = new Label(labelText);
        lblTitle.setTextFill(Color.web("#64748b"));

        Label lblValue = new Label(valueText);
        lblValue.setWrapText(true);
        lblValue.setTextFill(Color.web(hexColor));
        if (isBold) {
            lblValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        }

        grid.add(lblTitle, 0, row);
        grid.add(lblValue, 1, row);
    }
}