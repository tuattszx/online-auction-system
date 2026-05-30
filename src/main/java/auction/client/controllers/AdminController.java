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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
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
        if(product.getImages()!=null && !product.getImages().isEmpty()){
            String imgUrl=product.getImages().get(0).getUrlImage();
            if (imgUrl!=null && !imgUrl.isEmpty()){
                Image img=new Image(imgUrl,true);
                imgProduct.setImage(img);
            }
        }
        imageContainer.getChildren().add(imgProduct);


//        if (selectedItem.getImages() != null && !selectedItem.getImages().isEmpty()) {
//            String imageUrl = selectedItem.getImages().get(0).getUrlImage();
//
//            if (imageUrl != null && !imageUrl.isEmpty()) {
//                // Load ảnh trực tiếp từ link Cloudinary
//                Image img = new Image(imageUrl, true);
//                mainImage.setImage(img);
//            }
//        }

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
        Label lblStartVal = new Label("2026-05-29 08:00"); // product.getStartTime()
        lblStartVal.setTextFill(Color.web("#334155"));
        startRow.getChildren().addAll(lblStartTitle, lblStartVal);

        HBox endRow = new HBox(10);
        Label lblEndTitle = new Label("Thời gian kết thúc:");
        lblEndTitle.setPrefWidth(110);
        lblEndTitle.setTextFill(Color.web("#64748b"));
        Label lblEndVal = new Label("2026-06-05 18:00"); // product.getEndTime()
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
            // Xử lý logic duyệt sản phẩm của bạn ở đây...
            System.out.println("Đã duyệt sản phẩm!");
            popupStage.close();
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