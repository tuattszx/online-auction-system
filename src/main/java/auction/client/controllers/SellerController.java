package auction.client.controllers;

import auction.client.utils.ImageService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
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

    @FXML
    private Button btnNavCustomers;

    @FXML
    private Button btnNavConfig;

    @FXML
    private Label lblFileName;

    @FXML
    public void initialize() {
        showMyProducts();
    }

    @FXML
    private void handleShowAddProduct(ActionEvent event) {
        showAddProduct();
    }

    @FXML
    private void handleShowMyProducts(ActionEvent event) {
        showMyProducts();
    }

    @FXML
    private void handleShowCustomers(ActionEvent event) {
        // TODO: Implement customers view
        System.out.println("Showing Customers view");
        setActiveButton(btnNavCustomers);
    }

    @FXML
    private void handleShowConfiguration(ActionEvent event) {
        // TODO: Implement configuration view
        System.out.println("Showing Configuration view");
        setActiveButton(btnNavConfig);
    }
    private void showAddProduct() {
        vboxAddProduct.setVisible(true);
        vboxAddProduct.setManaged(true);

        vboxMyProducts.setVisible(false);
        vboxMyProducts.setManaged(false);

        setActiveButton(btnNavAdd);
    }

    private void showMyProducts() {
        vboxMyProducts.setVisible(true);
        vboxMyProducts.setManaged(true);

        vboxAddProduct.setVisible(false);
        vboxAddProduct.setManaged(false);

        setActiveButton(btnNavMyProducts);
    }

    private void setActiveButton(Button activeBtn) {
        // Reset all buttons to inactive state
        btnNavAdd.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7;");
        btnNavMyProducts.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7;");
        btnNavCustomers.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7;");
        btnNavConfig.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7;");

        // Set active button style
        activeBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white;");
    }
    @FXML
    private void handleBrowseFiles(ActionEvent event) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file thiết kế");

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            System.out.println("Đã chọn file: " + selectedFile.getName());
            lblFileName.setText(selectedFile.getName());
        }
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