package auction.client.controllers;
import javafx.geometry.Insets;
import auction.client.ClientNetwork;
import auction.common.message.Message;
import auction.common.model.items.Item;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class MainViewController extends ProfileController {
    @FXML private TextField txtsearch;
    @FXML private ComboBox<String> sortPrice, sortTime;
    @FXML private Label lbusername;
    @FXML private FlowPane flitems;
    @FXML private ScrollPane scrollCategories; // Phải trùng với fx:id="scrollCategories" trong FXML

    // SỬA LỖI: Khai báo biến network
    private ClientNetwork network = ClientNetwork.getInstance();

    @FXML
    public void initialize() {
//        if (UserSession.loggedInUser != null) {
//            lbusername.setText("Chào: " + UserSession.loggedInUser.getUsername());
//        }

        // Cần ép kiểu String cho ComboBox để tránh lỗi type-safety
        sortPrice.getItems().addAll("Giá tăng dần", "Giá giảm dần");
        sortTime.getItems().addAll("Thời gian tăng dần", "Thời gian giảm dần");

        // GỌI HÀM: Để tải dữ liệu ngay khi mở trang
        loadItems();
    }

    private void loadItems() {
        new Thread(() -> {
            try {
                // Gửi request lấy danh sách sản phẩm
                Message response = network.sendRequest(new Message("GET_ALL_ITEMS", null));

                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    List<Item> itemList = (List<Item>) response.getData();

                    // Cập nhật giao diện trên luồng JavaFX chính
                    Platform.runLater(() -> renderItems(itemList));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Trong MainViewController.java
    private void renderItems(List<Item> items) {
        // Rất quan trọng: Xóa sạch các thẻ sản phẩm cũ/mẫu trong FlowPane
        Platform.runLater(() -> {
            flitems.getChildren().clear();
            if (items != null) {
                for (Item item : items) {
                    flitems.getChildren().add(createItemCard(item));
                }
            }
        });
    }

    private VBox createItemCard(Item item) {
        VBox card = new VBox();

        // 1. Cấu hình Kích thước & Căn lề
        card.setPrefSize(200, 280); // Tăng kích thước một chút để cân đối
        card.setAlignment(Pos.TOP_CENTER); // Chỉnh lên trên để ảnh nằm trên cùng
        card.setSpacing(10);
        card.setPadding(new Insets(10)); // Tạo khoảng cách từ nội dung đến mép khung

        // 2. TẠO CÁI KHUNG (Style trực tiếp để thấy ngay kết quả)
        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 15; " + // Bo góc 15px
                        "-fx-border-radius: 15; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 4); " + // Đổ bóng nhẹ
                        "-fx-cursor: hand;"
        );

        // 3. Xử lý Ảnh
        ImageView imgView = new ImageView();
        try {
            // Lưu ý: Đảm bảo đường dẫn này chính xác với cấu trúc thư mục của bạn
            Image image = new Image(getClass().getResourceAsStream("/auction/img/images.jpg"));
            imgView.setImage(image);
        } catch (Exception e) {
            System.err.println("Không tìm thấy ảnh: " + e.getMessage());
        }
        imgView.setFitHeight(140);
        imgView.setFitWidth(180);
        imgView.setPreserveRatio(false); // Để false nếu muốn ảnh lấp đầy khung hình chữ nhật

        // 4. Tên sản phẩm
        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");
        nameLabel.setWrapText(true); // Tự xuống dòng nếu tên quá dài
        nameLabel.setAlignment(Pos.CENTER);

        // 5. Giá tiền
        Label priceLabel = new Label(String.format("%,d $", item.getCurrentPrice()));
        priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #0052ff;");

        // 6. Nút Đấu giá
        Button bidBtn = new Button("Đấu giá");
        bidBtn.setPrefWidth(120);
        bidBtn.setStyle(
                "-fx-background-color: #0052ff; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 20; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 5 15 5 15;"
        );

        // Hiệu ứng hover cho nút
        bidBtn.setOnMouseEntered(e -> bidBtn.setStyle(bidBtn.getStyle() + "-fx-background-color: #003db3;"));
        bidBtn.setOnMouseExited(e -> bidBtn.setStyle(bidBtn.getStyle() + "-fx-background-color: #0052ff;"));

        // 7. Gom tất cả vào Card
        card.getChildren().addAll(imgView, nameLabel, priceLabel, bidBtn);

        // 8. Sự kiện click vào Card
        card.setOnMouseClicked(event -> {
            // Chỉ chuyển cảnh nếu không bấm trúng nút "Đấu giá"
            if (event.getTarget() != bidBtn) {
                ViewManager.switchScene(event, "item-view.fxml", "Chi tiết");
            }
        });

        return card;
    }
    @FXML
    public void onProfileClick(MouseEvent event) throws IOException {
        if (UserSession.loggedInUser == null) return;

        String view = UserSession.loggedInUser.getRole().equals("ADMIN") ? "admin-view.fxml" : "profile-view.fxml";
        ViewManager.switchScene(event, view, "Hồ sơ cá nhân");
    }

    @FXML
    private void onSearchAction() {
        // Implement sau
    }

    @FXML
    public void onItemClick(MouseEvent event) {
        ViewManager.switchScene(event, "item-view.fxml", "Chi tiết sản phẩm");
    }
@FXML
public void scrollRight() {
    if (scrollCategories != null) {
        double currentValue = scrollCategories.getHvalue();
        // Tính toán vị trí mới
        double newValue = currentValue + 0.2;

        if (newValue > 1.0) newValue = 1.0;

        // Đặt giá trị mới cho thanh cuộn
        scrollCategories.setHvalue(newValue);

        // In ra console để kiểm tra xem hàm có chạy không
        System.out.println("Đã bấm nút cuộn phải. Vị trí hiện tại: " + newValue);
    } else {
        System.out.println("Lỗi: scrollCategories đang bị null!");
    }
}
}