package auction.client.controllers;

import auction.client.session.DataSession;
import auction.common.model.items.AuctionItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
    private TableColumn<AuctionItem, Double> colCurrentBid;
    @FXML
    private TableColumn<AuctionItem, Double> colYourBid;

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

        // 2. Gọi hàm cấu hình hiển thị cột Status
        setupStatusColumn();

        // 3. Khởi tạo dữ liệu mẫu
        ObservableList<AuctionItem> auctionData = FXCollections.observableArrayList(
                new AuctionItem("UFC 300: Pereira vs. Hill", "1:45:30", 2100.0, 2100.0, "Winning"),
                new AuctionItem("UFC 302: Makhachev vs. Poirier", "0:30:12", 4300.0, 3800.0, "Losing")
        );

        // 4. Đưa dữ liệu vào TableView
        auctionTable.setItems(auctionData);
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