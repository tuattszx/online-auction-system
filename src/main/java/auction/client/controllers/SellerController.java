package auction.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;

import java.io.IOException;

public class SellerController {

    // Khai báo các VBox nội dung (phần bên phải)
    @FXML
    private VBox vboxAddProduct;

    @FXML
    private VBox vboxMyProducts;

    // Khai báo các nút bấm menu bên trái để đổi style khi click
    @FXML
    private Button btnNavAdd;

    @FXML
    private Button btnNavMyProducts;

    /**
     * Hàm khởi tạo - Chạy khi giao diện vừa load xong
     */
    @FXML
    public void initialize() {
        // Mặc định khi mở lên sẽ hiện trang My Products
        showMyProducts();
    }

    /**
     * Xử lý khi nhấn vào "Add Product" ở menu trái
     */
    @FXML
    private void handleShowAddProduct(ActionEvent event) {
        showAddProduct();
    }

    /**
     * Xử lý khi nhấn vào "My Products" ở menu trái
     */
    @FXML
    private void handleShowMyProducts(ActionEvent event) {
        showMyProducts();
    }
    private void showAddProduct() {
        vboxAddProduct.setVisible(true);
        vboxAddProduct.setManaged(true);

        vboxMyProducts.setVisible(false);
        vboxMyProducts.setManaged(false);

        // Thay đổi style nút để người dùng biết đang ở trang nào
        btnNavAdd.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white;");
        btnNavMyProducts.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7;");
    }

    private void showMyProducts() {
        vboxMyProducts.setVisible(true);
        vboxMyProducts.setManaged(true);

        vboxAddProduct.setVisible(false);
        vboxAddProduct.setManaged(false);

        btnNavMyProducts.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white;");
        btnNavAdd.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7;");
    }
    @FXML
    public void OnMouseBacktoMain(MouseEvent event){
        ViewManager.switchScene(event,"main-view.fxml", "Trang chủ");

    }
    @FXML
    public void onBidderClick(MouseEvent event) throws IOException {
        ViewManager.switchScene(event, "profile-view.fxml", "Hồ sơ cá nhân");
    }
}