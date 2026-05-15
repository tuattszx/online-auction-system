package auction.client.controllers;

import auction.client.services.AuctionManager;
import auction.client.session.DataSession;
import auction.common.model.items.AuctionItem;
import auction.common.model.users.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.io.IOException;

public class AuctionDashboardController {

    @FXML
    private TableView<AuctionItem> auctionTable;
    @FXML
    private TableColumn<AuctionItem, String> colStatus;
    @FXML
    private TableColumn<AuctionItem, String> colProduct;
    @FXML
    private TableColumn<AuctionItem, String> colRemaining;
    @FXML
    private TableColumn<AuctionItem, Long> colCurrentBid;
    @FXML
    private TableColumn<AuctionItem, Long> colYourBid;

    // --- CÁC HÀM SỰ KIỆN GIỮ NGUYÊN ---
    @FXML
    public void OnMouseBacktoMain(MouseEvent event){
        ViewManager.switchScene(event,"main-view.fxml", "Trang chủ");
    }
    @FXML
    public void OnMouseCart(MouseEvent event){
        ViewManager.switchScene(event,"cart_view.fxml", "Giỏ hàng");
    }
    @FXML
    public void GoToFavoriteView(MouseEvent event){
        ViewManager.switchScene(event,"favourite_view.fxml", "Yêu thích");
    }
    @FXML
    public void onProfileClick(MouseEvent event) throws IOException {
        if (DataSession.getInstance().getLoggedInUser() == null) return;
        String view = DataSession.getInstance().getLoggedInUser().getRole().equals("ADMIN") ? "admin-view.fxml" : "profile-view.fxml";
        ViewManager.switchScene(event, view, "Hồ sơ cá nhân");
    }

    public void initialize() {
        // 1. Kết nối các cột (Sử dụng Lambda cho Property giúp cập nhật real-time)
        colProduct.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colRemaining.setCellValueFactory(cellData -> cellData.getValue().remainingTimeProperty());
        colCurrentBid.setCellValueFactory(cellData -> cellData.getValue().currentBidProperty().asObject());
        colYourBid.setCellValueFactory(cellData -> cellData.getValue().yourBidProperty().asObject());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // 2.cấu hình hiển thị cột Status
        setupStatusColumn();

        // 3. Khởi tạo dữ liệu mẫu
        User currentUser = DataSession.getInstance().getLoggedInUser();
        if (currentUser != null) {
            // Gọi dữ liệu từ Server
            loadMyAuctions(currentUser.getId());
        }

        auctionTable.setRowFactory(tv -> {
            TableRow<AuctionItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    handleAuctionItemSelection(row.getItem(), event);
                }
            });
            return row;
        });
    }

    private void loadMyAuctions(int userId) {
        AuctionManager.getInstance().getMyAuctionsAsync(userId).thenAccept(list -> {
            Platform.runLater(() -> {
                auctionTable.getItems().setAll(list);
            });
        });
    }private void handleAuctionItemSelection(AuctionItem selected, MouseEvent event) {
        if (selected == null) return;

        // Hiển thị trạng thái đang tải (nếu cần)
        auctionTable.setCursor(Cursor.WAIT);

        // 1. Gọi Async lấy thông tin chi tiết nhất từ Server
        AuctionManager.getInstance().getLatestItemAsync(selected.getId())
                .thenAccept(item -> {
                    Platform.runLater(() -> {
                        auctionTable.setCursor(Cursor.DEFAULT);
                        if (item != null) {
                            // 2. Lưu vào Session để trang ItemView có dữ liệu dùng ngay
                            DataSession.getInstance().setSelectedItem(item);

                            // 3. Chuyển sang màn hình chi tiết sản phẩm
                            ViewManager.switchScene(event, "item-view.fxml", "Chi tiết: " + item.getName());
                        } else {
                            // Xử lý khi sản phẩm không tồn tại (có thể bị xóa)
                            System.err.println("Không tìm thấy sản phẩm!");
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> auctionTable.setCursor(Cursor.DEFAULT));
                    return null;
                });
    }



    private void setupStatusColumn() {
        colStatus.setCellFactory(column -> {
            return new TableCell<AuctionItem, String>() {
                // Khởi tạo sẵn Label một lần để tái sử dụng, tránh lỗi mất nội dung khi click
                private final Label statusLabel = new Label();

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        statusLabel.setText(item);

                        // Xóa sạch các class cũ trước khi thêm mới để không bị chồng chéo màu
                        statusLabel.getStyleClass().removeAll("status-label", "status-winning", "status-losing", "status-participate");
                        statusLabel.getStyleClass().add("status-label");

                        if (item.equalsIgnoreCase("Winning")) {
                            statusLabel.getStyleClass().add("status-winning");
                        } else if (item.equalsIgnoreCase("Losing")) {
                            statusLabel.getStyleClass().add("status-losing");
                        } else {
                            statusLabel.getStyleClass().add("status-participate");
                        }

                        // Cấu hình hiển thị
                        setGraphic(statusLabel);
                        setText(null); // BẮT BUỘC: Xóa text mặc định để không bị đè khi hàng được chọn
                    }
                }
            };
        });
    }
}