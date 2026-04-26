package auction.client.controllers;
import auction.common.model.items.ItemImage;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import auction.client.ClientNetwork;
import auction.common.message.Message;
import auction.common.model.items.Item;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import javafx.scene.paint.Color;
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

        flitems.getChildren().clear();
        for (int i = 0; i < 8; i++) {
            flitems.getChildren().add(createSkeletonCard());
        }

        Task<List<Item>> loadTask = new Task<>() {
            @Override
            protected List<Item> call() throws Exception {
                // Gửi request thông qua kết nối Socket
                Message response = network.sendRequest(new Message("GET_ALL_ITEMS", null));

                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    return (List<Item>) response.getData();
                } else {
                    throw new RuntimeException("Server không phản hồi hoặc có lỗi xảy ra");
                }
            }
        };

        // 2. Khi Task chạy thành công (đã lấy được danh sách Item)
        loadTask.setOnSucceeded(event -> {
            List<Item> result = loadTask.getValue();
            flitems.getChildren().clear();
            System.out.println("Đã tải xong " + (result != null ? result.size() : 0) + " sản phẩm.");

            // Gọi hàm render của bạn để hiển thị lên UI
            renderItems(result);
        });

        // 3. Khi Task thất bại (Lỗi mạng, Server sập, hoặc lỗi Serialization)
        loadTask.setOnFailed(event -> {
            Throwable exception = loadTask.getException();
            exception.printStackTrace();
            System.err.println("Lỗi khi tải dữ liệu: " + exception.getMessage());
            // Bạn có thể hiển thị một thông báo Alert lỗi ở đây
        });

        // 4. Thực thi Task trên một Thread riêng để không làm đơ giao diện
        Thread thread = new Thread(loadTask);
        thread.setDaemon(true); // Đảm bảo thread này tắt khi bạn đóng ứng dụng
        thread.start();
    }

    private VBox createSkeletonCard() {
        VBox card = new VBox();

        // 1. Cấu hình Kích thước & Căn lề (Y hệt card thật)
        card.setPrefSize(200, 280);
        card.setAlignment(Pos.TOP_CENTER);
        card.setSpacing(10);
        card.setPadding(new Insets(10));

        // 2. Style khung (Y hệt card thật)
        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-radius: 15; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 4);"
        );

        // Màu xám nhẹ cho các phần tử đang load
        Color skeletonColor = Color.web("#eeeeee");

        // 3. Giả lập Ảnh (ImageView 140x180)
        Rectangle imgSkeleton = new Rectangle(180, 140);
        imgSkeleton.setFill(skeletonColor);
        imgSkeleton.setArcWidth(10);
        imgSkeleton.setArcHeight(10);

        // 4. Giả lập Tên sản phẩm (Label 14px)
        Rectangle nameSkeleton = new Rectangle(150, 16);
        nameSkeleton.setFill(skeletonColor);
        nameSkeleton.setArcWidth(5);
        nameSkeleton.setArcHeight(5);

        // 5. Giả lập Giá tiền (Label 15px)
        Rectangle priceSkeleton = new Rectangle(100, 18);
        priceSkeleton.setFill(skeletonColor);
        priceSkeleton.setArcWidth(5);
        priceSkeleton.setArcHeight(5);

        // 6. Giả lập Nút Đấu giá (Button radius 20)
        Rectangle btnSkeleton = new Rectangle(120, 30);
        btnSkeleton.setFill(skeletonColor);
        btnSkeleton.setArcWidth(20);
        btnSkeleton.setArcHeight(20);

        // 7. Gom tất cả vào Card
        card.getChildren().addAll(imgSkeleton, nameSkeleton, priceSkeleton, btnSkeleton);

        // 8. THÊM HIỆU ỨNG NHẤP NHÁY (Pulse effect)
        // Tạo cảm giác App đang "thở" để người dùng biết là đang load
        FadeTransition ft = new FadeTransition(Duration.millis(1000), card);
        ft.setFromValue(1.0);
        ft.setToValue(0.4);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();

        return card;
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
        imgView.setFitHeight(140);
        imgView.setFitWidth(180);
        imgView.setPreserveRatio(false); // Để false nếu muốn ảnh lấp đầy khung hình chữ nhật

        boolean imageLoaded = false;
        if (item.getImages() != null && !item.getImages().isEmpty()) {
            // Lấy ảnh mặc định hoặc ảnh đầu tiên trong danh sách
            ItemImage defaultImg = item.getImages().stream()
                    .filter(ItemImage::isDefault)
                    .findFirst()
                    .orElse(item.getImages().get(0));

            byte[] data = defaultImg.getImageData();

            if (data != null && data.length > 0) {
                try {
                    Image img = new Image(new java.io.ByteArrayInputStream(data));
                    imgView.setImage(img);
                    imageLoaded = true;
                } catch (Exception e) {
                    System.err.println("Lỗi khi chuyển đổi byte[] sang Image: " + e.getMessage());
                }
            }
        }
        if (!imageLoaded) {
            try {
                imgView.setImage(new Image(getClass().getResourceAsStream("/auction/img/images.jpg")));
            } catch (Exception e) {
                System.err.println("Không tìm thấy file ảnh mặc định trong resources");
            }
        }

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