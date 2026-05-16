package auction.client.controllers;

import auction.client.services.AuctionManager;
import auction.client.services.AuctionSubscriptionManager;
import auction.client.services.Cleanable;
import auction.client.session.DataSession;
import auction.common.message.BidUpdateNotification;
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
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AuctionDashboardController implements Cleanable {

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

    @FXML
    private HeaderMenuController headerMenuController;

    @FXML private HBox searchBar; // Liên kết với thanh tìm kiếm

    private Consumer<BidUpdateNotification> dashboardBidUpdateCallback;

    private final List<Integer> subscribedItemIds = new ArrayList<>();

    // Hàm dùng để ẩn thanh tìm kiếm và thu hồi lại diện tích trống
    public void hideSearchBar() {
        if (searchBar != null) {
            searchBar.setVisible(false);
            searchBar.setManaged(false); // Dòng này cực kỳ quan trọng: nó giúp các thành phần khác tự động tràn vào lấp chỗ trống, không để lại một khoảng trắng vô duyên.
        }
    }

    @Override
    public void cleanup() {
        if (dashboardBidUpdateCallback != null) {
            for (int itemId : subscribedItemIds) {
                AuctionSubscriptionManager.getInstance().unsubscribe(itemId, dashboardBidUpdateCallback);
            }
            System.out.println("✅ Đã giải phóng toàn bộ luồng lắng nghe trên Dashboard (" + subscribedItemIds.size() + " items).");
            subscribedItemIds.clear();
        }
    }

    public void initialize() {
        if (headerMenuController != null) {
            headerMenuController.hideSearchBar();
        }
        headerMenuController.setBalance(DataSession.getInstance().getLoggedInUser() != null ? DataSession.getInstance().getLoggedInUser().getBalance() + " $" : "0 $");
        // 1. Kết nối các cột (Sử dụng Lambda cho Property giúp cập nhật real-time)
        colProduct.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colRemaining.setCellValueFactory(cellData -> cellData.getValue().remainingTimeProperty());
        colCurrentBid.setCellValueFactory(cellData -> cellData.getValue().currentBidProperty().asObject());
        colYourBid.setCellValueFactory(cellData -> cellData.getValue().yourBidProperty().asObject());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // 2.cấu hình hiển thị cột Status
        setupStatusColumn();

        setupRealtimeCallback();
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

    private void setupRealtimeCallback() {
        User currentUser = DataSession.getInstance().getLoggedInUser();
        if (currentUser == null) return;

        this.dashboardBidUpdateCallback = notification -> {
            Platform.runLater(() -> {
                // Duyệt qua danh sách các dòng đang hiển thị trên bảng
                for (AuctionItem item : auctionTable.getItems()) {
                    if (item.getId() == notification.getItemId()) {

                        // 1. Cập nhật giá cao nhất hiện tại trên dòng đó
                        item.setCurrentBid(notification.getNewPrice());

                        // 2. Tự động tính toán lại Trạng thái Winning / Losing nóng tại chỗ
                        if (notification.getBidderName().equals(currentUser.getUsername())) {
                            item.setStatus("Winning");
                            item.setYourBid(notification.getNewPrice()); // Cập nhật luôn mức giá của bạn
                        } else {
                            item.setStatus("Losing");
                        }

                        // 3. Ép TableView vẽ lại để cập nhật màu sắc CSS ngay lập tức
                        auctionTable.refresh();
                        break;
                    }
                }
            });
        };
    }

    private void loadMyAuctions(int userId) {
        cleanup();
        auctionTable.getItems().clear();

        AuctionManager.getInstance().getMyAuctionsAsync(userId).thenAccept(list -> {
            Platform.runLater(() -> {
                auctionTable.getItems().setAll(list);

                for (AuctionItem item : list) {
                    AuctionSubscriptionManager.getInstance().subscribe(item.getId(), dashboardBidUpdateCallback);
                    subscribedItemIds.add(item.getId()); // Lưu lại ID
                }
                auctionTable.refresh();
            });
        });
    }

    private void handleAuctionItemSelection(AuctionItem selected, MouseEvent event) {
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