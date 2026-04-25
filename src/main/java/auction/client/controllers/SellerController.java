package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.utils.ImageService;
import auction.common.message.Message;
import auction.common.model.items.Item;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SellerController {

    @FXML  TextField txtTitle;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextField txtHeight;
    @FXML private TextField txtLength;
    @FXML private TextField txtWidth;
    @FXML private TextField txtWeight;
    @FXML private TextField txtPrice;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextArea txtDescription;
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

    private List<File> selectedFiles;

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
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        List<File> files = fileChooser.showOpenMultipleDialog(stage);

        if (files != null && !files.isEmpty()) {
            List<File> validFiles = new ArrayList<>();
            StringBuilder errorLog = new StringBuilder();

            for (File file : files) {
                // 1. Kiểm tra định dạng qua ImageService (đã viết ở các bước trước)
                if (!ImageService.isValidImage(file)) {
                    errorLog.append("- ").append(file.getName()).append(": Sai định dạng hoặc quá 5MB\n");
                    continue;
                }
                validFiles.add(file);
            }

            if (!validFiles.isEmpty()) {
                this.selectedFiles = validFiles;
                lblFileName.setText("Đã chọn " + validFiles.size() + " ảnh hợp lệ");
            }

            // Nếu có file lỗi thì thông báo cho người dùng biết
            if (errorLog.length() > 0) {
                ViewManager.showAlert(Alert.AlertType.WARNING, "Cảnh báo file",
                        "Một số file bị bỏ qua do không hợp lệ:\n" + errorLog.toString());
            }
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

    @FXML
    private void handleSaveProduct(ActionEvent event) {
        try {
            // 1. Thu thập dữ liệu từ các TextField/ComboBox/DatePicker
            String name = txtTitle.getText().trim();
            String priceText = txtPrice.getText().trim();
            String description = txtDescription.getText();
            String categoryName = categoryComboBox.getValue(); // Lấy từ ComboBox

            // 2. Validation (Kiểm tra dữ liệu đầu vào)
            if (name.isEmpty() || priceText.isEmpty() || selectedFiles == null) {
                ViewManager.showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập Tên, Giá và chọn Ảnh sản phẩm!");
                return;
            }

            // 3. Khởi tạo đối tượng Item (Common Model)
            Item newItem = new Item();
            newItem.setName(name);
            newItem.setDescription(description);
            newItem.setStartingPrice(Long.parseLong(priceText));
            newItem.setCurrentPrice(Long.parseLong(priceText));

            // Set kích thước
            newItem.setLength(txtLength.getText().isEmpty() ? 0 : Double.parseDouble(txtLength.getText()));
            newItem.setWidth(txtWidth.getText().isEmpty() ? 0 : Double.parseDouble(txtWidth.getText()));
            newItem.setHeight(txtHeight.getText().isEmpty() ? 0 : Double.parseDouble(txtHeight.getText()));

            // Set thời gian (LocalDateTime)
            if (startDatePicker.getValue() != null) {
                newItem.setStartTime(startDatePicker.getValue().atStartOfDay());
            }
            if (endDatePicker.getValue() != null) {
                newItem.setEndTime(endDatePicker.getValue().atTime(23, 59, 59));
            }

            // 4. Xử lý Ảnh (Chuyển sang mảng byte để gửi qua Socket)
            // Lưu ý: Ta không tạo URL ở đây, Server sẽ tạo URL sau khi lưu file vật lý thành công
            List<byte[]> allImageData = new ArrayList<>();
            List<String> allFileNames = new ArrayList<>();
            for (File file : selectedFiles) {
                allImageData.add(ImageService.toBytes(file));
                allFileNames.add(file.getName());
            }
            // 5. Đóng gói payload gửi đi
            // Gửi mảng Object gồm: [Item, byte[], tên ảnh, tên Category]
            // Việc gửi categoryName riêng giúp Server dùng CategoryDAO tìm ID chính xác
            Object[] payload = new Object[]{ newItem, allImageData,allFileNames, categoryName };

            Message request = new Message("ADD_ITEM", payload);

            // 6. Gửi qua ClientNetwork
            System.out.println("Đang gửi yêu cầu thêm sản phẩm lên Server...");
            Message response = ClientNetwork.getInstance().sendRequest(request);

            // 7. Xử lý kết quả trả về từ Server
            if (response != null && "SUCCESS".equals(response.getStatus())) {
                ViewManager.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Sản phẩm đã được đăng đấu giá thành công!");

                // Xóa trắng form và quay về danh sách sản phẩm
                clearFields();
                showMyProducts();
            } else {
                String errorMsg = (response != null) ? response.getStatus() : "Server không phản hồi";
                ViewManager.showAlert(Alert.AlertType.ERROR, "Thất bại", "Lỗi: " + errorMsg);
            }

        } catch (NumberFormatException e) {
            ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Giá và kích thước phải là số hợp lệ!");
        } catch (IOException e) {
            ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi file", "Không thể đọc dữ liệu ảnh!");
            e.printStackTrace();
        } catch (Exception e) {
            ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Hàm hỗ trợ reset các trường nhập liệu sau khi lưu thành công
     */
    private void clearFields() {
        txtTitle.clear();
        txtPrice.clear();
        txtDescription.clear();
        txtLength.clear();
        txtWidth.clear();
        txtHeight.clear();
        txtWeight.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        categoryComboBox.setValue(null);
        lblFileName.setText("Chưa chọn file");
        selectedFiles = null;
    }
}