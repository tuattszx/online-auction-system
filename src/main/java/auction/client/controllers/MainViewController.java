package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.common.message.Message;
import auction.common.model.items.Item;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class MainViewController {
    @FXML private TextField txtsearch;
    @FXML private ComboBox<String> sortPrice, sortTime;
    @FXML private Label lbusername;
    @FXML private FlowPane flitems;

    // SỬA LỖI: Khai báo biến network
    private ClientNetwork network = ClientNetwork.getInstance();

    @FXML
    public void initialize() {
        if (UserSession.loggedInUser != null) {
            lbusername.setText("Chào: " + UserSession.loggedInUser.getUsername());
        }

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
        card.getStyleClass().add("auction-card");
        card.setPrefSize(180, 220);
        card.setAlignment(Pos.CENTER);
        card.setSpacing(10);

        // Hiển thị ảnh (tạm thời lấy ảnh mặc định trong project)
        ImageView imgView = new ImageView();
        try {
            Image image = new Image(getClass().getResourceAsStream("/auction/img/images.jpg"));
            imgView.setImage(image);
        } catch (Exception e) {
            System.err.println("Không tìm thấy ảnh mặc định");
        }
        imgView.setFitHeight(120);
        imgView.setFitWidth(160);
        imgView.setPreserveRatio(true);

        Label nameLabel = new Label(item.getName());
        nameLabel.getStyleClass().add("item-name");

        Label priceLabel = new Label(String.format("%,d $", item.getCurrentPrice()));
        priceLabel.getStyleClass().add("item-price");

        Button bidBtn = new Button("Đấu giá");
        bidBtn.getStyleClass().add("bid-button-small");
        bidBtn.setOnAction(e -> System.out.println("Đấu giá: " + item.getName()));

        card.getChildren().addAll(imgView, nameLabel, priceLabel, bidBtn);

        // Click vào card để xem chi tiết (nếu có)
        card.setOnMouseClicked(event -> {
             ViewManager.switchScene(event, "item-view.fxml", "Chi tiết");
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
}