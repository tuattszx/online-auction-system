package auction.client.controllers;
import auction.client.session.DataSession;
import auction.common.model.items.ItemImage;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.collections.ListChangeListener;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import javafx.scene.paint.Color;
import java.io.IOException;
import java.util.List;

public class MainViewController extends ProfileController {
    public Label lbbalance;
    @FXML
    private TextField txtsearch;
    @FXML
    private ComboBox<String> sortPrice, sortTime;
    @FXML
    private Label lbusername;
    @FXML
    private FlowPane flitems;
    @FXML
    private ScrollPane scrollCategories; // Phải trùng với fx:id="scrollCategories" trong FXML
    @FXML
    protected HeaderMenuController headerMenuController;

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
        headerMenuController.setBalance(DataSession.getInstance().getLoggedInUser() != null ? DataSession.getInstance().getLoggedInUser().getBalance() + " $" : "0 $");
        // GỌI HÀM: Để tải dữ liệu ngay khi mở trang
        loadItems();
    }

    private void loadItems() {

        flitems.getChildren().clear();
        for (int i = 0; i < 10; i++) {
            flitems.getChildren().add(createSkeletonCard());
        }

        Task<List<Item>> loadTask = new Task<>() {
            @Override
            protected List<Item> call() throws Exception {
                // Gửi request thông qua kết nối Socket
                Message response = network.sendRequest(new Message("GET_ALL_ITEMS", null));

                if (response == null || !"SUCCESS".equals(response.getStatus())) {
                    throw new RuntimeException("Server không phản hồi hoặc có lỗi xảy ra");
                }
                return  (List<Item>) response.getData();
            }
        };

        // 2. Khi Task chạy thành công (đã lấy được danh sách Item)
        loadTask.setOnSucceeded(event -> {
            List<Item> result = loadTask.getValue();
            flitems.getChildren().clear();
            System.out.println("Đã tải xong " + (result != null ? result.size() : 0) + " sản phẩm.");

            // Gọi hàm render của bạn để hiển thị lên UI
            renderItems(result);
            if (DataSession.getInstance().getLoggedInUser() != null && result != null) {
                loadUserFavoritesInBackground(result);
            }
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

    private void loadUserFavoritesInBackground(List<Item> allItems) {
        Task<Void> favTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int userId = DataSession.getInstance().getLoggedInUser().getId();

                Message favResponse = network.sendRequest(new Message("GET_FAVOURITES", userId));

                if (favResponse != null && "SUCCESS".equals(favResponse.getStatus())) {
                    List<Integer> favoriteIds = (List<Integer>) favResponse.getData();

                    // Đẩy dữ liệu vào DataSession
                    Platform.runLater(() -> {
                        DataSession.getInstance().getFavoriteItems().clear();

                        for (Item item : allItems) {
                            if (favoriteIds.contains(item.getId())) {
                                DataSession.getInstance().addFavorite(item);
                            }
                        }
                    });
                }
                return null;
            }
        };

        Thread favThread = new Thread(favTask);
        favThread.setDaemon(true); // Đảm bảo luồng ngầm tự tắt khi đóng App
        favThread.start();
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

    public VBox createItemCard(Item item) {
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

            // 2. Lấy URL thay vì lấy mảng byte
            String imageUrl = defaultImg.getUrlImage();
            String optimizedUrl = imageUrl.replace("/upload/", "/upload/w_200,c_fill,f_auto,q_auto/");

            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    // 3. Tạo Image trực tiếp từ URL (để true để load ngầm)
                    Image img = new Image(optimizedUrl, 140, 180, true, true, true);
                    imgView.setImage(img);
                    imageLoaded = true;
                } catch (Exception e) {
                    System.err.println("Lỗi khi tải ảnh từ URL: " + e.getMessage());
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
        HBox nameAndHeartBox = new HBox();
        nameAndHeartBox.setAlignment(Pos.CENTER_LEFT); // Căn lề trái để icon và tên thẳng hàng
        nameAndHeartBox.setSpacing(10);
        nameAndHeartBox.setPadding(new Insets(0, 5, 0, 5));

        Label nameLabel = new Label(item.getName());
// Cho nameLabel co giãn để đẩy icon về phía bên phải
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
        nameLabel.setMaxWidth(140); // Giới hạn chiều rộng để không đè vào icon
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");
        nameLabel.setWrapText(false); // Thường tên sản phẩm trong Card nên để 1 dòng
        nameLabel.setEllipsisString("..."); // Nếu dài quá thì hiện dấu ...

// Icon trái tim (Dùng Label kèm mã Unicode hoặc ImageView)
        Label heartIcon = new Label("❤");
        heartIcon.setStyle("-fx-text-fill: #ccc; -fx-font-size: 18px; -fx-cursor: hand;");
        if (DataSession.getInstance().getFavoriteItems().contains(item)) {
            heartIcon.setStyle("-fx-text-fill: #ff4d4d; -fx-font-size: 18px; -fx-cursor: hand;");
        } else {
            heartIcon.setStyle("-fx-text-fill: #ccc; -fx-font-size: 18px; -fx-cursor: hand;");
        }
        DataSession.getInstance().getFavoriteItems().addListener((ListChangeListener<Item>) change -> {
            if (DataSession.getInstance().getFavoriteItems().contains(item)) {
                heartIcon.setStyle("-fx-text-fill: #ff4d4d; -fx-font-size: 18px; -fx-cursor: hand;");
            } else {
                heartIcon.setStyle("-fx-text-fill: #ccc; -fx-font-size: 18px; -fx-cursor: hand;");
            }
        });
// 2. Lúc Click:
        heartIcon.setOnMouseClicked(e -> {
            int currentUserId = DataSession.getInstance().getLoggedInUser().getId();
            Object[] payload = new Object[]{ currentUserId, item.getId() };
            if (DataSession.getInstance().getFavoriteItems().contains(item)) {
                DataSession.getInstance().removeFavorite(item); // Xóa nếu đã có
                heartIcon.setStyle("-fx-text-fill: #ccc; -fx-font-size: 18px; -fx-cursor: hand;");
                ClientNetwork.getInstance().sendRequestAsync(new Message("REMOVE_FAVOURITE", payload));
            } else {
                DataSession.getInstance().addFavorite(item); // Thêm nếu chưa có
                heartIcon.setStyle("-fx-text-fill: #ff4d4d; -fx-font-size: 18px; -fx-cursor: hand;");
                ClientNetwork.getInstance().sendRequestAsync(new Message("ADD_FAVOURITE", payload));
            }
            e.consume();
        });

        nameAndHeartBox.getChildren().addAll(nameLabel, heartIcon);

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

        HBox statusContainer = new HBox();
        statusContainer.setAlignment(Pos.CENTER);
        statusContainer.setSpacing(6);
        statusContainer.setMaxHeight(30);
        statusContainer.setPrefWidth(120);
        statusContainer.getStylesheets().add(getClass().getResource("/auction/css/labelstatus.css").toExternalForm());

        Circle dot = new Circle(4);
        dot.getStyleClass().add("badge-dot");

        Label lblStatus = new Label();
        lblStatus.getStyleClass().add("badge-text");

        // Logic đổ màu và text dựa trên trạng thái thực tế của item
        String status = item.getStatus().toLowerCase(); // Giả sử item có thuộc tính status
        statusContainer.getStyleClass().add("badge-container");

        AuctionTimerManager cardTimer = new AuctionTimerManager(
                item, lblStatus, statusContainer, dot);
        cardTimer.tick();


        statusContainer.getChildren().addAll(dot, lblStatus);
        // 7. Gom tất cả vào Card
        card.getChildren().addAll(imgView, statusContainer, nameAndHeartBox, priceLabel, bidBtn);

        // 8. Sự kiện click vào Card
        card.setOnMouseClicked(event -> {
            DataSession.getInstance().setSelectedItem(item);
            // Chỉ chuyển cảnh nếu không bấm trúng nút "Đấu giá"
            if (event.getTarget() != bidBtn) {
                ViewManager.switchScene(event, "item-view.fxml", "Chi tiết");
            }
        });

        return card;
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