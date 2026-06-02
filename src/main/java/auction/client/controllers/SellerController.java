package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.services.UploadItemTask;
import auction.client.session.DataSession;
import auction.client.utils.ImageService;
import auction.common.message.Message;
import auction.common.model.categories.Category;
import auction.common.model.items.Item;
import auction.common.model.users.User;
import auction.server.dao.ItemDao;
import auction.server.dao.UserDao;
import auction.server.utils.CloudinaryUtil;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.CheckComboBox;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SellerController {

    @FXML
    TextField txtTitle;
    @FXML
    private CheckComboBox<String> categoryComboBox;
    @FXML
    private TextField txtHeight;
    @FXML
    private TextField txtLength;
    @FXML
    private TextField txtWidth;
    @FXML
    private TextField txtWeight;
    @FXML
    private TextField txtPrice;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private TextArea txtDescription;

    @FXML
    private Spinner<Integer> startHour;
    @FXML
    private Spinner<Integer> startMin;
    @FXML
    private Spinner<Integer> startSec;

    @FXML
    private Spinner<Integer> endHour;
    @FXML
    private Spinner<Integer> endMin;
    @FXML
    private Spinner<Integer> endSec;

    @FXML
    private TableView<Item> productTable;
    @FXML
    private TableColumn<Item, Integer> colSn;
    @FXML
    private TableColumn<Item, String> colName;
    @FXML
    private TableColumn<Item, Long> colStartingPrice;
    @FXML
    private TableColumn<Item, Long> colCurrentPrice;
    @FXML
    private TableColumn<Item, String> colStatus;
    @FXML
    private TableColumn<Item, Object> colSold;
    @FXML
    private TableColumn<Item, Void> colAction;

    @FXML
    ProgressBar progressbar;

    @FXML
    private VBox vboxAddProduct;

    @FXML
    private VBox vboxMyProducts;

    @FXML
    private VBox vboxCustomers;

    @FXML
    private HBox btnNavMyProducts;
    @FXML
    private HBox btnNavAdd;
    @FXML
    private HBox btnNavCustomers;
    @FXML
    private HBox btnNavConfig;

    @FXML
    private Region myProductsIndicator;
    @FXML
    private Region addIndicator;
    @FXML
    private Region customersIndicator;
    @FXML
    private Region configIndicator;

    @FXML
    private Label lblMyProducts;
    @FXML
    private Label lblAddProduct;
    @FXML
    private Label lblCustomers;
    @FXML
    private Label lblConfig;
    @FXML
    private Label lblFileName;
    @FXML
    private Label lblCurrencyIcon;
    @FXML
    private Label lblTotalRevenue;

    @FXML
    private TableView<Object[]> customerTable;
    @FXML
    private TableColumn<Object[], Integer> colId;
    @FXML
    private TableColumn<Object[], String> collName;
    @FXML
    private TableColumn<Object[], String> colPhone;
    @FXML
    private TableColumn<Object[], Void> collAction;
    @FXML
    private TableColumn<Object[], String> colProductName;
    @FXML
    private TextField searchField;
    @FXML
    private Button addCustomerBtn;

    // 🌟 Inject VBox đóng vai trò làm lớp phủ mờ chặn tương tác
    @FXML
    private VBox loadingIndicator;

    private ObservableList<Object[]> masterData = FXCollections.observableArrayList();

    private List<File> selectedFiles;
    @FXML
    private HeaderMenuController headerMenuController;

    private final ObservableList<Item> sellerProductList = FXCollections.observableArrayList();
    private boolean isEditMode = false;
    private int editingItemId = -1;

    @FXML
    public void initialize() {
        lblCurrencyIcon.setText("$");
        categoryComboBox.getItems().addAll("Art", "Interiors", "Jewelry", "Watches", "Fashion", "Coins", "Cars", "Wine", "Books");
        headerMenuController.resetText();
        headerMenuController.hideSearchBar();
        setupTableColumns();
        setupRowFactory();
        loadSellerProducts();
        showMyProducts();
        progressbar.setVisible(false);

        startHour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        endHour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));

        startMin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        endMin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        startSec.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        endSec.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        colId.setCellFactory(column -> new TableCell<Object[], Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });

        collName.setCellValueFactory(cellData -> {
            Object[] user = cellData.getValue();
            String fullName = ((User) user[0]).getUsername();
            return new SimpleStringProperty(fullName.trim());
        });

        colPhone.setCellValueFactory(cellData -> new SimpleStringProperty(((User) cellData.getValue()[0]).getPhoneNumber()));
        colProductName.setCellValueFactory(cellData -> new SimpleStringProperty((String) cellData.getValue()[1]));

        setupActionColumn();
    }

    private void setupTableColumns() {
        colSn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        colStartingPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colStartingPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Long price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("%,d $", price));
            }
        });

        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colCurrentPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Long price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("%,d $", price));
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colSold.setCellFactory(column -> new TableCell<>() {
            private final Label lblCheck = new Label("✓");
            {
                lblCheck.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
            }

            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Item rowItem = getTableView().getItems().get(getIndex());
                    if (("CLOSED".equalsIgnoreCase(rowItem.getStatus()) || "DELETED".equalsIgnoreCase(rowItem.getStatus())) && rowItem.getCurrentBidderId() != null && rowItem.getCurrentBidderId() > 0) {
                        setGraphic(lblCheck);
                    } else {
                        setGraphic(new Label("—"));
                    }
                }
            }
        });

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnModify = new Button("MODIFY");
            private final Button btnDelete = new Button("DELETE");
            private final HBox container = new HBox(btnModify, btnDelete);

            {
                container.setSpacing(10);
                container.setAlignment(Pos.CENTER);
                btnModify.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");

                btnModify.setOnAction(e -> {
                    Item selected = getTableView().getItems().get(getIndex());
                    handleModifyAction(selected);
                });

                btnDelete.setOnAction(e -> {
                    Item selected = getTableView().getItems().get(getIndex());
                    handleDeleteAction(selected);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Item rowItem = getTableView().getItems().get(getIndex());
                    String status = rowItem.getStatus() != null ? rowItem.getStatus().toUpperCase() : "";

                    if (!"UNAPPROVED".equals(status)) {
                        btnModify.setDisable(true);
                        btnModify.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-cursor: not-allowed; -fx-background-radius: 5;");
                    } else {
                        btnModify.setDisable(false);
                        btnModify.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                    }

                    setGraphic(container);
                }
            }
        });
    }

    private void loadSellerProducts() {
        if (DataSession.getInstance().getLoggedInUser() == null) return;

        Task<List<Item>> task = new Task<>() {
            @Override
            protected List<Item> call() throws Exception {
                Message request = new Message("GET_SELLER_PRODUCTS", DataSession.getInstance().getLoggedInUser().getId());
                Message response = ClientNetwork.getInstance().sendRequest(request);

                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    return (List<Item>) response.getData();
                }
                return new ArrayList<>();
            }
        };

        task.setOnSucceeded(e -> {
            sellerProductList.setAll(task.getValue());
            productTable.setItems(sellerProductList);
            productTable.refresh();
            productTable.requestLayout();
        });

        new Thread(task).start();
    }

    private void handleModifyAction(Item item) {
        String currentStatus = item.getStatus();
        if (!"UNAPPROVED".equals(currentStatus)) {
            ViewManager.showAlert(Alert.AlertType.WARNING,
                    "Không thể chỉnh sửa",
                    "Sản phẩm '" + item.getName() + "' đang ở trạng thái [" + currentStatus + "]. Bạn chỉ được quyền sửa đổi những sản phẩm chưa được duyệt (UNAPPROVED)!");
            return;
        }
        isEditMode = true;
        editingItemId = item.getId();

        txtTitle.setText(item.getName());
        txtPrice.setText(String.valueOf(item.getStartingPrice()));
        txtDescription.setText(item.getDescription());
        txtLength.setText(String.valueOf(item.getLength()));
        txtWidth.setText(String.valueOf(item.getWidth()));
        txtHeight.setText(String.valueOf(item.getHeight()));

        categoryComboBox.getCheckModel().clearChecks();

        if (item.getCategories() != null && !item.getCategories().isEmpty()) {
            for (Category cat : item.getCategories()) {
                String catName = cat.getName();
                int index = categoryComboBox.getItems().indexOf(catName);
                if (index >= 0) {
                    categoryComboBox.getCheckModel().check(index);
                }
            }
        }

        if (item.getStartTime() != null) {
            startDatePicker.setValue(item.getStartTime().toLocalDate());
            startHour.getValueFactory().setValue(item.getStartTime().getHour());
            startMin.getValueFactory().setValue(item.getStartTime().getMinute());
            startSec.getValueFactory().setValue(item.getStartTime().getSecond());
        }

        if (item.getEndTime() != null) {
            endDatePicker.setValue(item.getEndTime().toLocalDate());
            endHour.getValueFactory().setValue(item.getEndTime().getHour());
            endMin.getValueFactory().setValue(item.getEndTime().getMinute());
            endSec.getValueFactory().setValue(item.getEndTime().getSecond());
        }

        lblFileName.setText("(Chế độ chỉnh sửa thông tin - Giữ nguyên ảnh cũ nếu không chọn lại)");
        showAddProduct();
    }

    private void handleDeleteAction(Item item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc chắn muốn xóa sản phẩm '" + item.getName() + "'?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận hành động xóa");
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                new Thread(() -> {
                    Message request = new Message("DELETE_ITEM", item.getId());
                    Message response = ClientNetwork.getInstance().sendRequest(request);

                    Platform.runLater(() -> {
                        if (response != null && "SUCCESS".equals(response.getStatus())) {
                            item.setStatus("DELETED");
                            productTable.refresh();
                            ViewManager.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa sản phẩm thành công!");
                        } else {
                            ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xóa sản phẩm vào lúc này!");
                        }
                    });
                }).start();
            }
        });
    }

    @FXML
    private void handleSidebarClick(MouseEvent event) {
        HBox clickedBox = (HBox) event.getSource();
        resetAllSidebarItems();

        if (clickedBox == btnNavMyProducts) {
            btnNavMyProducts.setStyle("-fx-background-color: #e8f0fe; -fx-background-radius: 8;");
            myProductsIndicator.setVisible(true);
            lblMyProducts.setStyle("-fx-text-fill: #1a73e8; -fx-font-weight: bold;");

            isEditMode = false;
            editingItemId = -1;
            showMyProducts();
            handleShowMyProducts(null);
        } else if (clickedBox == btnNavAdd) {
            btnNavAdd.setStyle("-fx-background-color: #e8f0fe; -fx-background-radius: 8;");
            addIndicator.setVisible(true);
            lblAddProduct.setStyle("-fx-text-fill: #1a73e8; -fx-font-weight: bold;");

            clearFields();
            isEditMode = false;
            editingItemId = -1;
            showAddProduct();
        } else if (clickedBox == btnNavCustomers) {
            btnNavCustomers.setStyle("-fx-background-color: #e8f0fe; -fx-background-radius: 8;");
            customersIndicator.setVisible(true);
            lblCustomers.setStyle("-fx-text-fill: #1a73e8; -fx-font-weight: bold;");
            isEditMode = false;
            editingItemId = -1;

            handleShowCustomers(null);
        } else if (clickedBox == btnNavConfig) {
            btnNavConfig.setStyle("-fx-background-color: #e8f0fe; -fx-background-radius: 8;");
            configIndicator.setVisible(true);
            lblConfig.setStyle("-fx-text-fill: #1a73e8; -fx-font-weight: bold;");

            handleShowConfiguration(null);
        }
    }

    private void setupRowFactory() {
        productTable.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    Item selected = row.getItem();
                    String currentStatus = selected.getStatus() != null ? selected.getStatus().toUpperCase() : "";

                    if (currentStatus.equals("PENDING") || currentStatus.equals("OPEN") || currentStatus.equals("CLOSED")) {
                        productTable.setCursor(Cursor.WAIT);
                        DataSession.getInstance().setSelectedItem(selected);

                        try {
                            ViewManager.switchScene(event, "item-view.fxml", "Chi tiết: " + selected.getName());
                        } finally {
                            productTable.setCursor(Cursor.DEFAULT);
                        }
                    }
                }
            });
            return row;
        });
    }

    private void resetAllSidebarItems() {
        btnNavMyProducts.setStyle("-fx-background-color: transparent;");
        btnNavAdd.setStyle("-fx-background-color: transparent;");
        btnNavCustomers.setStyle("-fx-background-color: transparent;");
        btnNavConfig.setStyle("-fx-background-color: transparent;");

        myProductsIndicator.setVisible(false);
        addIndicator.setVisible(false);
        customersIndicator.setVisible(false);
        configIndicator.setVisible(false);

        lblMyProducts.setStyle("-fx-text-fill: #495057; -fx-font-weight: normal;");
        lblAddProduct.setStyle("-fx-text-fill: #495057; -fx-font-weight: normal;");
        lblCustomers.setStyle("-fx-text-fill: #495057; -fx-font-weight: normal;");
        lblConfig.setStyle("-fx-text-fill: #495057; -fx-font-weight: normal;");
    }

    @FXML
    private void handleShowAddProduct(ActionEvent event) {
        showAddProduct();
    }

    @FXML
    private void handleShowMyProducts(ActionEvent event) {
        loadSellerRevenue();
        showMyProducts();
    }

    @FXML
    private void handleShowCustomers(ActionEvent event) {
        vboxAddProduct.setVisible(false);
        vboxMyProducts.setVisible(false);
        vboxCustomers.setVisible(true);
        setActiveButton(btnNavCustomers);
        onCustomersTabSelected();
    }

    @FXML
    private void handleShowConfiguration(ActionEvent event) {
        System.out.println("Showing Configuration view");
        setActiveButton(btnNavConfig);
    }

    private void showAddProduct() {
        vboxAddProduct.setVisible(true);
        vboxMyProducts.setVisible(false);
        vboxCustomers.setVisible(false);
        setActiveButton(btnNavAdd);
    }

    private void showMyProducts() {
        vboxMyProducts.setVisible(true);
        vboxAddProduct.setVisible(false);
        vboxCustomers.setVisible(false);
        setActiveButton(btnNavMyProducts);
    }

    private void setActiveButton(HBox activeBtn) {
        btnNavAdd.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7;");
        btnNavMyProducts.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7;");
        btnNavCustomers.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7;");
        btnNavConfig.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7;");

        activeBtn.setStyle("-fx-background-color:  #e8f0fe; -fx-text-fill:  #1a73e8; -fx-font: bold");
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

            if (errorLog.length() > 0) {
                ViewManager.showAlert(Alert.AlertType.WARNING, "Cảnh báo file",
                        "Một số file bị bỏ qua do không hợp lệ:\n" + errorLog.toString());
            }
        }
    }

    @FXML
    private void handleSaveProduct(ActionEvent event) {
        try {
            String name = txtTitle.getText().trim();
            String priceText = txtPrice.getText().trim();
            String description = txtDescription.getText();
            List<String> checkedCategories = new ArrayList<>(categoryComboBox.getCheckModel().getCheckedItems());

            if (name.isEmpty() || priceText.isEmpty() || checkedCategories.isEmpty()) {
                ViewManager.showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập Tên, Giá khởi điểm và chọn ít nhất một danh mục!");
                return;
            }
            if (!isEditMode && (selectedFiles == null || selectedFiles.isEmpty())) {
                ViewManager.showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng chọn ảnh cho sản phẩm mới!");
                return;
            }

            // 🌟 KÍCH HOẠT LỚP PHỦ MỜ (Chặn đứng hoàn toàn click chuột của người dùng)
            loadingIndicator.setVisible(true);

            Item newItem = new Item();
            newItem.setName(name);
            newItem.setDescription(description);
            newItem.setStartingPrice(Long.parseLong(priceText));
            newItem.setCurrentPrice(Long.parseLong(priceText));
            newItem.setSellerId(DataSession.getInstance().getLoggedInUser().getId());

            newItem.setLength(txtLength.getText().isEmpty() ? 0 : Double.parseDouble(txtLength.getText()));
            newItem.setWidth(txtWidth.getText().isEmpty() ? 0 : Double.parseDouble(txtWidth.getText()));
            newItem.setHeight(txtHeight.getText().isEmpty() ? 0 : Double.parseDouble(txtHeight.getText()));
            newItem.setWeight(txtWeight.getText().isEmpty() ? 0 : Double.parseDouble(txtWeight.getText()));

            if (startDatePicker.getValue() != null) {
                int h = startHour.getValue();
                int m = startMin.getValue();
                int s = startSec.getValue();
                newItem.setStartTime(startDatePicker.getValue().atTime(h, m, s));
            }
            if (endDatePicker.getValue() != null) {
                int h = endHour.getValue();
                int m = endMin.getValue();
                int s = endSec.getValue();
                newItem.setEndTime(endDatePicker.getValue().atTime(h, m, s));
            }

            if (newItem.getStartTime() != null && newItem.getEndTime() != null) {
                if (newItem.getEndTime().isBefore(newItem.getStartTime())) {
                    ViewManager.showAlert(Alert.AlertType.WARNING, "Lỗi thời gian", "Thời gian kết thúc phải sau thời gian bắt đầu!");
                    loadingIndicator.setVisible(false); // Ẩn lớp phủ nếu validation lỗi
                    return;
                }
            }

            UploadItemTask task;
            if (isEditMode) {
                newItem.setId(editingItemId);
                task = new UploadItemTask(newItem, selectedFiles, checkedCategories, true);
            } else {
                task = new UploadItemTask(newItem, selectedFiles, checkedCategories);
            }
            progressbar.setVisible(true);
            progressbar.progressProperty().bind(task.progressProperty());

            task.setOnSucceeded(e -> {
                progressbar.setVisible(false);
                progressbar.progressProperty().unbind();
                loadingIndicator.setVisible(false); // 🌟 ẨN LỚP PHỦ KHI HOÀN THÀNH THÀNH CÔNG

                showMyProducts();
                Message response = task.getValue();
                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    loadSellerProducts();
                    clearFields();
                } else {
                    ViewManager.showAlert(Alert.AlertType.ERROR, "Thất bại",
                            response != null ? (String) response.getData() : "Lỗi kết nối Server!");
                }
            });

            task.setOnFailed(e -> {
                progressbar.setVisible(false);
                progressbar.progressProperty().unbind();
                loadingIndicator.setVisible(false); // 🌟 ẨN LỚP PHỦ KHI HOÀN THÀNH THẤT BẠI
                ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi", "Gặp sự cố hệ thống khi xử lý!");
            });

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        } catch (NumberFormatException e) {
            loadingIndicator.setVisible(false); // Ẩn lớp phủ nếu bắt được lỗi định dạng
            ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Giá và kích thước phải là số hợp lệ!");
        } catch (Exception e) {
            loadingIndicator.setVisible(false); // Ẩn lớp phủ khi lỗi crash bất ngờ
            ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", e.getMessage());
            e.printStackTrace();
        } finally {
            ViewManager.clearCache();
        }
    }

    @FXML
    private void clearFields() {
        isEditMode = false;
        editingItemId = -1;
        txtTitle.clear();
        txtPrice.clear();
        txtDescription.clear();
        txtLength.clear();
        txtWidth.clear();
        txtHeight.clear();
        txtWeight.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        categoryComboBox.getCheckModel().clearChecks();
        lblFileName.setText("Chưa chọn file");
        selectedFiles = null;
        startHour.getValueFactory().setValue(0);
        startMin.getValueFactory().setValue(0);
        startSec.getValueFactory().setValue(0);

        endHour.getValueFactory().setValue(0);
        endMin.getValueFactory().setValue(0);
        endSec.getValueFactory().setValue(0);
    }

    private void setupActionColumn() {
        collAction.setCellFactory(param -> new TableCell<Object[], Void>() {
            private final Button btnMore = new Button("MO...");
            private final javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(btnMore);

            {
                btnMore.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
                btnMore.setPrefWidth(65);
                container.setAlignment(javafx.geometry.Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User currentUser = (User) getTableRow().getItem()[0];

                    btnMore.setOnAction(e -> {
                        if (currentUser != null) {
                            openCustomerDetailPopup(currentUser);
                        }
                    });

                    setGraphic(container);
                }
            }

            private void openCustomerDetailPopup(User selectedUser) {
                try {
                    ViewManager.removeView("customer-detail-popup.fxml");
                    Parent popupRoot = ViewManager.getView("customer-detail-popup.fxml");

                    CustomerDetailPopupController controller = (CustomerDetailPopupController) popupRoot.getUserData();
                    controller.setCustomerData(selectedUser);

                    Stage popupStage = new Stage();
                    popupStage.setTitle("Hồ Sơ Khách Hàng - " + selectedUser.getUsername());

                    popupStage.initModality(Modality.APPLICATION_MODAL);
                    popupStage.initOwner(btnMore.getScene().getWindow());

                    Scene scene = new Scene(popupRoot);
                    popupStage.setScene(scene);
                    popupStage.setResizable(false);

                    popupStage.showAndWait();

                } catch (Exception ex) {
                    System.err.println("Lỗi nạp tệp Custom Popup FXML: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
    }

    private void loadRealData() {
        User currentSeller = DataSession.getInstance().getLoggedInUser();

        if (currentSeller == null) {
            System.err.println("Lỗi: Không tìm thấy phiên đăng nhập của Seller.");
            return;
        }

        int sellerId = currentSeller.getId();

        new Thread(() -> {
            Message request = new Message("GET_CUSTOMERS", sellerId);
            Message response = ClientNetwork.getInstance().sendRequest(request);

            Platform.runLater(() -> {
                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    List<Object[]> realCustomers = (List<Object[]>) response.getData();

                    masterData.clear();
                    if (realCustomers != null && !realCustomers.isEmpty()) {
                        masterData.addAll(realCustomers);
                    } else {
                        System.out.println("Seller này chưa có khách hàng nào mua sản phẩm.");
                    }

                    customerTable.setItems(masterData);
                    customerTable.refresh();
                } else {
                    ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi",
                            response != null ? (String) response.getData() : "Không thể lấy danh sách khách hàng từ Server!");
                }
            });
        }).start();
    }

    public void onCustomersTabSelected() {
        masterData.clear();
        loadRealData();
    }

    public void loadSellerRevenue() {
        if (DataSession.getInstance().getLoggedInUser() == null) return;
        int sellerId = DataSession.getInstance().getLoggedInUser().getId();

        new Thread(() -> {
            Message request = new Message("GET_SELLER_REVENUE", String.valueOf(sellerId));
            Message response = ClientNetwork.getInstance().getInstance().sendRequest(request);

            Platform.runLater(() -> {
                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    long totalMoney = (long) response.getData();
                    lblTotalRevenue.setText(String.valueOf(totalMoney));
                } else {
                    lblTotalRevenue.setText("0");
                }
            });
        }).start();
    }
}