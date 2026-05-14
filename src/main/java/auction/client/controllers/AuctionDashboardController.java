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

        // 2.cấu hình hiển thị cột Status
        setupStatusColumn();

        // 3. Khởi tạo dữ liệu mẫu
        ObservableList<AuctionItem> auctionData = FXCollections.observableArrayList(
                // Nhóm Đang thắng (Winning)
                new AuctionItem("iPhone 15 Pro Max - 256GB", "0:05:12", 1200.0, 1200.0, "Winning"),
                new AuctionItem("MacBook Pro M3 Max", "1:20:45", 3500.0, 3500.0, "Winning"),
                new AuctionItem("Sony PS5 Slim Edition", "0:15:30", 550.0, 550.0, "Winning"),
                new AuctionItem("Mechanical Keyboard Custom", "2:10:00", 150.0, 150.0, "Winning"),
                new AuctionItem("Vintage Rolex Datejust", "0:02:15", 8500.0, 8500.0, "Winning"),
                new AuctionItem("AirPods Pro Gen 2", "4:30:12", 210.0, 210.0, "Winning"),

                // Nhóm Đang thua (Losing)
                new AuctionItem("Samsung Galaxy S24 Ultra", "0:10:20", 1150.0, 1000.0, "Losing"),
                new AuctionItem("RTX 4090 Rog Strix", "0:45:00", 2200.0, 1900.0, "Losing"),
                new AuctionItem("Dell XPS 15 9530", "1:05:15", 1800.0, 1650.0, "Losing"),
                new AuctionItem("Nintendo Switch OLED", "0:08:45", 320.0, 280.0, "Losing"),
                new AuctionItem("Canon EOS R5 Body", "3:15:40", 3100.0, 2900.0, "Losing"),
                new AuctionItem("Herman Miller Aeron Chair", "5:20:00", 1200.0, 950.0, "Losing"),
                new AuctionItem("LEGO Star Wars Millennium Falcon", "0:25:30", 650.0, 500.0, "Losing"),

                // Nhóm Tham gia (Participate)
                new AuctionItem("Jordan 1 Retro High OG", "12:45:00", 450.0, 400.0, "Participate"),
                new AuctionItem("Dyson V15 Detect Vacuum", "8:10:25", 700.0, 620.0, "Participate"),
                new AuctionItem("iPad Pro 12.9 M2", "6:50:10", 1100.0, 980.0, "Participate"),
                new AuctionItem("Marshall Emberton II", "2:30:45", 140.0, 120.0, "Participate"),
                new AuctionItem("Kindle Paperwhite 5", "15:00:00", 130.0, 100.0, "Participate"),
                new AuctionItem("Logitech MX Master 3S", "1:15:20", 95.0, 85.0, "Participate"),
                new AuctionItem("FujiFilm X100V", "0:01:45", 2300.0, 2100.0, "Participate")
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