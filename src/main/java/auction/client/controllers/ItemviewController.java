package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.services.AuctionManager;
import auction.client.services.AuctionSubscriptionManager;
import auction.client.services.AuctionTimerManager;
import auction.client.services.Cleanable;
import auction.client.session.DataSession;
import auction.client.utils.ToastManager;
import auction.common.message.BidUpdateNotification;
import auction.common.model.bid.Bid;
import auction.common.model.categories.Category;
import auction.common.model.items.*;
import auction.common.model.notifications.Notification;
import auction.common.model.users.User;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ItemviewController implements Cleanable {
    private AuctionTimerManager timer;
    @FXML private ImageView mainImage;
    @FXML private Label itemNameLabel;
    @FXML private Label lbCurrentBid;
    @FXML private Label lbShortDesc;
    @FXML private Label lbbalance;
    @FXML private VBox Vboxdetails;
    @FXML private AreaChart<String ,Number> priceChart;
    @FXML private TextField txtBid;
    @FXML private Label lbBidError;
    @FXML private Label lbstarttime;
    @FXML private Label lbendtime;
    @FXML private HBox statusContainer;
    @FXML private Label lblStatus;
    @FXML private Label lbShow;
    @FXML private Circle dot;
    @FXML
    private TableView<Bid> bidTable;
    @FXML
    private TableColumn<Bid, String> colBidder;
    @FXML
    private TableColumn<Bid, String> colTime;
    @FXML
    private TableColumn<Bid, String> colPrice;

    @FXML
    private Label lblCategory;

    @FXML
    private Label lblHeight;

    @FXML
    private Label lblLength;

    @FXML
    private Label lblWidth;

    @FXML
    private Label lblWeight;

    @FXML
    private HeaderMenuController headerMenuController;

    @FXML private HBox searchBar; // Liên kết với thanh tìm kiếm
    ClientNetwork network = ClientNetwork.getInstance();
    private Timeline timeline;
    private Item currentItem; // Object chứa endTime từ Database

    private PauseTransition errorTimeout = new PauseTransition(javafx.util.Duration.seconds(3));
    private Consumer<BidUpdateNotification> bidUpdateCallback;
    private Consumer<Notification> globalNotificationCallback;
    private int currentImageIndex = 0;
    List<ItemImage> currentImageList=DataSession.getInstance().getSelectedItem().getImages();
    Item selectedItem = DataSession.getInstance().getSelectedItem();
    @FXML
    public void initialize() {
        //Lấy dữ liệu "Tĩnh" từ Session

        if (selectedItem != null) {
            this.currentItem=selectedItem;
            itemNameLabel.setText(selectedItem.getName());
            lbShortDesc.setText(selectedItem.getDescription());
            lblCategory.setText(selectedItem.getCategories().stream()
                    .map(Category::getName)
                    .toList().toString().replace("[", "").replace("]", ""));
            lblHeight.setText(String.valueOf(selectedItem.getHeight()));
            lblWeight.setText(String.valueOf(selectedItem.getWeight()));
            lblLength.setText(String.valueOf(selectedItem.getLength()));
            lblWidth.setText(String.valueOf(selectedItem.getWidth()));



            if (selectedItem.getImages() != null && !selectedItem.getImages().isEmpty()) {
                String imageUrl = selectedItem.getImages().get(0).getUrlImage();

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    // Load ảnh trực tiếp từ link Cloudinary
                    Image img = new Image(imageUrl, true);
                    mainImage.setImage(img);
                }
            }

            lbCurrentBid.setText(String.format("$ %,d", selectedItem.getCurrentPrice(),"Updating...."));

            loadExtraImages(selectedItem.getId());
            updateAllData(selectedItem.getId());
            setupRealtimeUpdate(selectedItem.getId());
            setupGlobalNotificationListener();
        }


        // Hiện số dư người dùng
        headerMenuController.setBalance(DataSession.getInstance().getLoggedInUser() != null ? DataSession.getInstance().getLoggedInUser().getBalance() + " $" : "0 $");
        headerMenuController.hideSearchBar();

        // Initialize bidTable columns
        colBidder.setCellValueFactory(cellData -> {
            String bidderName = cellData.getValue().getBidderName();
            return new SimpleStringProperty(bidderName != null ? bidderName : "Unknown");
        });
        colTime.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getBidTime().toString()));
        colPrice.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%,d $", cellData.getValue().getBidAmount())));

        setItemData(selectedItem);
         lbstarttime.setText(toString(currentItem.getStartTime()));
         lbendtime.setText(toString(currentItem.getEndTime()));
    }

    @Override
    public void cleanup() {
        if (currentItem != null && bidUpdateCallback != null) {
            AuctionSubscriptionManager.getInstance().unsubscribe(currentItem.getId(), bidUpdateCallback);
            System.out.println("ItemviewController: Đã hủy subscribe real-time thành công cho item ID: " + currentItem.getId());
        }
        if (globalNotificationCallback != null) {
            auction.client.services.NotificationSubscriptionManager.getInstance().unsubscribe(globalNotificationCallback);
            System.out.println("ItemviewController: Đã gỡ lắng nghe thông báo hủy Auto Bid toàn cục.");
        }
        if (timeline != null) {
            timeline.stop();
        }
        Item sessionItem = DataSession.getInstance().getSelectedItem();
        if (sessionItem != null && currentItem != null && sessionItem.getId() == currentItem.getId()) {
            DataSession.getInstance().setSelectedItem(null);
        }
    }

    private String toString(LocalDateTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return time.format(formatter);
    }

    private void setupRealtimeUpdate(int itemId) {
        this.bidUpdateCallback = notification -> {
            Platform.runLater(() -> {
                // 1. Cập nhật giá cao nhất hiện tại
                lbCurrentBid.setText(String.format("$ %,d", notification.getNewPrice()));

                // 2. Thêm dòng mới vào bảng lịch sử (Bid Table)
                Bid newBid = new Bid();
                newBid.setBidderName(notification.getBidderName());
                newBid.setBidAmount(notification.getNewPrice());
                newBid.setBidTime(notification.getBidTime());
                bidTable.getItems().add(0, newBid); // Thêm lên đầu bảng

                // 3. Cập nhật biểu đồ
                updateChartDirectly(notification.getBidTime(), notification.getNewPrice());

                // 4. Nếu Server báo có gia hạn thời gian (newEndTime != null)
                if (notification.getNewEndTime() != null) {
                    this.currentItem.setEndTime(notification.getNewEndTime());
                    lbendtime.setText(toString(notification.getNewEndTime()));
                }
            });
        };

        // Đăng ký với Tổng đài
        AuctionSubscriptionManager.getInstance().subscribe(itemId, bidUpdateCallback);
    }

    private void setupGlobalNotificationListener() {
        this.globalNotificationCallback = notification -> {
            if (currentItem != null && notification != null) {
                String title = notification.getTitle();
                int notifItemId = 0;
                if (notification instanceof auction.common.model.notifications.BidNotification) {
                    notifItemId = ((auction.common.model.notifications.BidNotification) notification).getItemId();
                }

                if ("AUTOBID_CANCELLED".equals(title) && notifItemId == currentItem.getId()) {

                    Platform.runLater(() -> {
                        if (isAutoBidActive) {
                            isAutoBidActive = false;

                            btnSetAutoBid.setText("SET AUTO BID");
                            btnSetAutoBid.setStyle("-fx-background-color: #0052FF; -fx-cursor: hand; -fx-font-weight: bold; -fx-text-fill: white");

                            Stage currentStage = (Stage) btnSetAutoBid.getScene().getWindow();
                            ToastManager.showToast(currentStage, ToastManager.ToastType.WARNING, notification.getMessage());
                        }
                    });
                }
            }
        };

        // Đăng ký Callback này vào tổng đài thông báo đẩy toàn cục
        auction.client.services.NotificationSubscriptionManager.getInstance().subscribe(globalNotificationCallback);
    }

    private void updateAllData(int itemId) {
        // 1. lay item moi nhat
        AuctionManager.getInstance().getLatestItemAsync(itemId).thenAccept(item -> {
            if (item != null) {
                Platform.runLater(() -> {
                    lbCurrentBid.setText(String.format("$ %,d", item.getCurrentPrice()));
                    this.currentItem = item; // Cập nhật để countdown chạy đúng
                });
            }
        });

        User currentUser = DataSession.getInstance().getLoggedInUser();
        if (currentUser != null) {
            AuctionManager.getInstance().checkAutoBidStatusAsync(itemId, currentUser.getId()).thenAccept(hasAutoBid -> {
                Platform.runLater(() -> {
                    if (hasAutoBid) {
                        isAutoBidActive = true;
                        btnSetAutoBid.setText("CANCEL AUTO BID");
                        btnSetAutoBid.setStyle("-fx-background-color: #EF4444; -fx-cursor: hand; -fx-font-weight: bold; -fx-text-fill: white");
                    } else {
                        // Nếu chưa cài -> Trả về nút màu xanh mặc định
                        isAutoBidActive = false;
                        btnSetAutoBid.setText("SET AUTO BID");
                        btnSetAutoBid.setStyle("-fx-background-color: #0052FF; -fx-cursor: hand; -fx-font-weight: bold; -fx-text-fill: white");
                    }
                });
            });
        }

        // 2. Lấy lịch sử Bid
        AuctionManager.getInstance().getBidHistoryAsync(itemId).thenAccept(bids -> {
            if (bids != null) {
                Platform.runLater(() -> bidTable.getItems().setAll(bids));
            }
        });

        // 3. Lấy dữ liệu biểu đồ
        AuctionManager.getInstance().getPriceChartAsync(itemId).thenAccept(chartData -> {
            Platform.runLater(() -> {
                if (chartData == null || chartData.isEmpty()) {
                    loadPriceChart(new java.util.ArrayList<>()); // Hàm loadPriceChart sẽ tự xử lý vẽ đường ngang
                } else {
                    // Nếu đã có bid, truyền chartData vào bình thường
                    loadPriceChart(chartData);
                }
            });
        });
    }

    private void showBidError(String message,boolean isSuccess) {
        Platform.runLater(() -> {
            lbBidError.setText(message);
            lbBidError.setTextFill(isSuccess? Color.GREEN:Color.RED);
            lbBidError.setVisible(true);
            lbBidError.setManaged(true);

            errorTimeout.setOnFinished(e -> {
                lbBidError.setVisible(false);
                lbBidError.setManaged(false);
            });
            errorTimeout.playFromStart();
        });
    }


    @FXML
    public void handlePlaceBid(){
        lbBidError.setVisible(false); // Ẩn lỗi cũ trước khi check mới
        lbBidError.setManaged(false);
        Item selectedItem = DataSession.getInstance().getSelectedItem();
        User currentUser = DataSession.getInstance().getLoggedInUser();

        if ("PENDING".equals(selectedItem.getStatus())) {
            showBidError("Phiên đấu giá chưa bắt đầu!", false);
            return;
        } else if ("CLOSED".equals(selectedItem.getStatus())) {
            showBidError("Phiên đấu giá đã kết thúc!", false);
            return;
        }

        String bidValue = txtBid.getText().trim();
        try {
            validateBid(bidValue, selectedItem.getCurrentPrice(), currentUser.getId(), selectedItem.getSellerId(), currentUser.getRole());
            long amount = Long.parseLong(bidValue);

            AuctionManager.getInstance().placeBidAsync(selectedItem.getId(), currentUser.getId(), amount)
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            txtBid.setDisable(false); // Mở lại input

                            if ("SUCCESS".equals(response.getStatus())) {
                                txtBid.clear();
                                showBidError("Đặt giá thành công!", true);

                                    // Lấy Stage gốc thông qua linh kiện giao diện có sẵn
                                    Stage currentStage = (Stage) txtBid.getScene().getWindow();

                                    // Bắn thông báo nổi lên góc màn hình
                                // Hiện mẫu Thành công (Success)
                                ToastManager.showToast(currentStage, ToastManager.ToastType.SUCCESS, "Success: Updated your bid");


                            } else {
                                showBidError(response.getData().toString(), false);
                            }
                        });
                    });
        }
        catch (IllegalArgumentException e) {
            showBidError(e.getMessage(),false);
        }
    }

    public void validateBid(String bidText, long currentPrice, int userId, int sellerId, String userRole)
            throws IllegalArgumentException {

        if (bidText == null || bidText.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số tiền!");
        }

        long amount;
        try {
            amount = Long.parseLong(bidText.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Vui lòng chỉ nhập số!");
        }

        if ("ADMIN".equals(userRole)) {
            throw new IllegalArgumentException("Admin không thể đấu giá!");
        }

        if (userId == sellerId) {
            throw new IllegalArgumentException("Người bán không thể đặt giá cho chính sản phẩm của mình!");
        }

        if (amount <= currentPrice) {
            throw new IllegalArgumentException("Giá trả phải lớn hơn " + currentPrice + "$");
        }
    }
    @FXML
    public void handleShow(MouseEvent event){
        if(lbShow.getText().equals("Show more")){
        Vboxdetails.setVisible(true);
        priceChart.setVisible(false);
        lbShow.setText("Show less");}
        else{
            Vboxdetails.setVisible(false);
            priceChart.setVisible(false);
            lbShow.setText("Show more");
        }
    }
    @FXML
    public void handleLineChart(MouseEvent event){
        priceChart.setVisible(true);
        Vboxdetails.setVisible(false);
    }

    private void updateChartDirectly(LocalDateTime time, long price) {
        if (priceChart.getData().isEmpty()) return;
        XYChart.Series<String, Number> series = priceChart.getData().get(0);
        String timeStr = time.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        series.getData().add(new XYChart.Data<>(timeStr, price));
    }

    private void loadPriceChart(List<Object[]> priceChartData) {
        priceChart.getData().clear();
        CategoryAxis xAxis = (CategoryAxis) priceChart.getXAxis();
        xAxis.setGapStartAndEnd(true);
        xAxis.setAnimated(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Item currentItem=DataSession.getInstance().getSelectedItem();

        if (currentItem == null) return;
        String startTime = currentItem.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        series.getData().add(new XYChart.Data<>(startTime, currentItem.getStartingPrice()));

        if (priceChartData != null && !priceChartData.isEmpty()) {
            for (Object[] dataPoint : priceChartData) {
                String time = dataPoint[0].toString();
                Number price = (Number) dataPoint[1];
                series.getData().add(new XYChart.Data<>(time, price));
            }
        }

        priceChart.getData().add(series);

    }
    @FXML private Label lblDays, lblHours, lblMins, lblSecs;



    public void setItemData(Item item) {
        this.currentItem = item;
        startCountdown();
    }

    private void startCountdown() {
        if (timeline != null) timeline.stop();

        timer = new AuctionTimerManager(
                currentItem,
                lblStatus,
                statusContainer,
                dot,
                lblDays,
                lblHours,
                lblMins,
                lblSecs
        );

        timer.start();
    }

    @FXML
    private ScrollPane thumbnailScrollPane;
    @FXML private VBox thumbnailContainer;

    // Khoảng cách cuộn mỗi lần click (từ 0.0 đến 1.0)
    private final double scrollStep = 0.2;

    @FXML
    void handleScrollUp(ActionEvent event) {
        // Lấy giá trị hiện tại và trừ đi bước cuộn
        double currentValue = thumbnailScrollPane.getVvalue();
        thumbnailScrollPane.setVvalue(currentValue - scrollStep);
    }

    @FXML
    void handleScrollDown(ActionEvent event) {
        // Lấy giá trị hiện tại và cộng thêm bước cuộn
        double currentValue = thumbnailScrollPane.getVvalue();
        thumbnailScrollPane.setVvalue(currentValue + scrollStep);
    }

    private void displayThumbnails(Item item) {
        if (item == null || item.getImages() == null) return;

        Platform.runLater(() -> {
            thumbnailContainer.getChildren().clear(); // Xóa sạch các ảnh cũ (cái lizard mặc định)

            for (ItemImage imgModel : item.getImages()) {
                // 1.lấy URL từ Cloudinary
                String url = imgModel.getUrlImage();
                if (url == null || url.isEmpty()) continue;

                // 2. Tạo Image trực tiếp từ URL (tham số true giúp load ngầm không lag UI)
                Image img = new Image(url, true);
                ImageView thumb = new ImageView(img);

                thumb.setFitHeight(60.0);
                thumb.setFitWidth(60.0);
                thumb.setPickOnBounds(true);
                thumb.setPreserveRatio(true);
                thumb.setCursor(Cursor.HAND);
                thumb.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0);");

                //Bấm ảnh nhỏ thì ảnh to (mainImage) thay đổi
                thumb.setOnMouseClicked(event -> {
                    mainImage.setImage(img);
                    // Bạn có thể thêm hiệu ứng mờ các ảnh không được chọn ở đây
                    thumbnailContainer.getChildren().forEach(node -> node.setOpacity(0.5));
                    thumb.setOpacity(1.0);
                });

                // Thêm vào VBox
                thumbnailContainer.getChildren().add(thumb);
            }

            // Mặc định cho ảnh đầu tiên là ảnh chính nếu mainImage đang trống
            if (!thumbnailContainer.getChildren().isEmpty()) {
                ImageView firstThumb = (ImageView) thumbnailContainer.getChildren().get(0);
                mainImage.setImage(firstThumb.getImage());
                firstThumb.setOpacity(1.0);
            }
        });
    }

    private void loadExtraImages(int itemId) {
        AuctionManager.getInstance().getItemImagesAsync(itemId).thenAccept(response -> {
            if (response != null && "SUCCESS".equals(response.getStatus())) {
                // 1. Ép kiểu dữ liệu nhận về thành List các đối tượng ảnh
                List<ItemImage> allImages = (List<ItemImage>) response.getData();

                if (allImages != null && !allImages.isEmpty()) {
                    // 2. Chuyển các thao tác cập nhật giao diện vào luồng xử lý UI chính
                    Platform.runLater(() -> {
                        // Cập nhật danh sách ảnh toàn cục phục vụ cho việc bấm nút Trái/Phải
                        this.currentImageList = new ArrayList<>(allImages);

                        // Reset chỉ mục về tấm ảnh đầu tiên của mặt hàng này
                        this.currentImageIndex = 0;

                        // Kích hoạt hàm hiển thị tấm ảnh hiện tại lên màn hình ImageView chính
                        displayThumbnails(selectedItem);
                    });
                }
            }
        }).exceptionally(ex -> {
            System.err.println("Lỗi tải ảnh: " + ex.getMessage());
            return null;
        });
    }
    private Popup autoBidPopup;
    @FXML private Button btnSetAutoBid;
    private boolean isAutoBidActive = false; // Biến cờ theo dõi trạng thái

    @FXML
    void toggleAutoBidPopup(ActionEvent event) {
        if (isAutoBidActive) {
            User currentUser = DataSession.getInstance().getLoggedInUser();
            if (currentUser == null) return;

            AuctionManager.getInstance().cancelAutoBidAsync(currentItem.getId(), currentUser.getId())
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            if (response != null && "SUCCESS".equals(response.getStatus())) {
                                isAutoBidActive = false;
                                // Trở về trạng thái ban đầu (Màu xanh dương chủ đạo của bạn)
                                btnSetAutoBid.setText("SET AUTO BID");
                                btnSetAutoBid.setStyle("-fx-background-color: #0052FF; -fx-cursor: hand; -fx-font-weight: bold; -fx-text-fill: white");
                                ToastManager.showToast((Stage) btnSetAutoBid.getScene().getWindow(),
                                        ToastManager.ToastType.SUCCESS, response.getData().toString());
                            } else {
                                String errMsg = response != null ? response.getData().toString() : "Lỗi kết nối!";
                                ToastManager.showToast((Stage) btnSetAutoBid.getScene().getWindow(),
                                        ToastManager.ToastType.ERROR, errMsg);
                            }
                        });
                    });
            return;
        }

        Node sourceNode = (Node) event.getSource();
        Window window = sourceNode.getScene().getWindow();

        if (autoBidPopup != null && autoBidPopup.isShowing()) {
            autoBidPopup.hide();
            return;
        }
        if ("ADMIN".contains(DataSession.getInstance().getLoggedInUser().getRole())) {
            showBidError("Admin không thể cài đặt đấu giá tự động!", false);
            return;
        }

        if (currentItem.getSellerId() == DataSession.getInstance().getLoggedInUser().getId()) {
            showBidError("Người bán không thể cài đặt đấu giá tự động!", false);
            return;
        }

        try {
            // Khởi tạo popup nếu chưa có
            if (autoBidPopup == null) {
                autoBidPopup = new Popup();
                autoBidPopup.setAutoHide(true);
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction/view/autobidding-view.fxml"));

            Parent root = loader.load(); // Load để sinh ra giao diện và controller riêng của nó

            // 🔑 LẤY CONTROLLER CỦA POPUP RA ĐỂ TRUYỀN DỮ LIỆU
            AutoBidController popupController = loader.getController();
            popupController.setInitData(autoBidPopup, currentItem.getCurrentPrice(),currentItem.getId(),this);

            // Làm sạch content cũ và thêm content mới
            autoBidPopup.getContent().clear();
            autoBidPopup.getContent().add(root);
            autoBidPopup.centerOnScreen();
            autoBidPopup.show(window);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi mở popup AutoBid!");
        }
    }
    public void activateAutoBidStatus(String maxPrice) {
        isAutoBidActive = true;

        // Đổi chữ và chuyển sang tone màu xám/vàng hoặc đỏ dịu để báo hiệu "Hủy"
        btnSetAutoBid.setText("CANCEL AUTO BID (" + maxPrice + " $)");
        btnSetAutoBid.setStyle("-fx-background-color: #EF4444;-fx-cursor: hand; -fx-font-weight: bold; -fx-text-fill: white");
        // Bạn có thể đổi màu #64748B thành màu đỏ cam #EF4444 nếu muốn nhấn mạnh hành động bấm vào là HỦY.
    }
    private void displayImageAtIndex(int index) {
        // Kiểm tra an toàn xem danh sách ảnh có hợp lệ không
        if (currentImageList == null || currentImageList.isEmpty()) {
            mainImage.setImage(null); // Không có ảnh thì xóa trống
            return;
        }

        // Bảo vệ chặn index không vượt quá phạm vi mảng
        if (index < 0 || index >= currentImageList.size()) return;

        // 1. Lấy URL từ Cloudinary giống hệt logic cũ của bạn
        ItemImage imgModel = currentImageList.get(index);
        String url = imgModel.getUrlImage();

        if (url != null && !url.isEmpty()) {
            // 2. Tạo đối tượng Image chạy ngầm mượt mà
            Image img = new Image(url, true);
            mainImage.setImage(img);

            // Cập nhật lại vị trí index hiện tại của hệ thống
            currentImageIndex = index;
        }
    }
    @FXML
    public void handlePrevImage(ActionEvent event) {
        if (currentImageList.isEmpty()) return;

        int newIndex = currentImageIndex - 1;

        // Logic xoay vòng: Nếu đang ở ảnh đầu tiên mà bấm Trái -> tự động nhảy xuống ảnh cuối cùng
        if (newIndex < 0) {
            newIndex = currentImageList.size() - 1;
        };

        displayImageAtIndex(newIndex);
    }

    @FXML
    public void handleNextImage(ActionEvent event) {
        if (currentImageList.isEmpty()) return;
        System.out.println("1");
        int newIndex = currentImageIndex + 1;

        // Logic xoay vòng: Nếu đang ở ảnh cuối cùng mà bấm Phải -> tự động quay về ảnh đầu tiên (0)
        if (newIndex >= currentImageList.size()) {
            newIndex = 0;
        }

        displayImageAtIndex(newIndex);
    }
    @FXML
    public void handleMainImageDoubleClick(MouseEvent event) {
        // 1. Chỉ kích hoạt khi người dùng nhấn ĐÚP (2 lần click chuột trái)
        if (event.getClickCount() == 2 && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {

            // Chặn an toàn nếu sản phẩm hiện tại không có ảnh nào
            if (currentImageList == null || currentImageList.isEmpty()) {
                return;
            }

            try {
                // 2. Tận dụng hàm tiện ích getView của bạn để lấy Root Node (Parent)
                // Hãy sửa lại đường dẫn "gallery-overlay.fxml" cho đúng với cấu trúc thư mục của bạn
                Parent overlayView = ViewManager.getView("gallery-overlay.fxml");

                // 3. Rút Controller đã được ép sẵn vào UserData ở hàm getView của bạn
                Object storedController = overlayView.getUserData();

                if (storedController instanceof GalleryOverlayController) {
                    GalleryOverlayController overlayController = (GalleryOverlayController) storedController;
                    // Nạp danh sách ảnh + vị trí ảnh to hiện tại sang cho màn hình phóng to
                    overlayController.setData(currentImageList, currentImageIndex);
                }

                javafx.stage.Stage galleryStage = new javafx.stage.Stage();
                galleryStage.setTitle("Gallery - View Images");

                // Thiết lập Scene cho Stage phụ
                javafx.scene.Scene scene = new javafx.scene.Scene(overlayView);
                galleryStage.setScene(scene);

                // Cấu hình hiển thị: Cho phép phóng to tối đa hoặc mở rộng theo màn hình máy tính
                galleryStage.setMinWidth(1024);
                galleryStage.setMinHeight(700);

                // Đặt Stage cha làm chủ để khi tắt giao diện chính thì tắt luôn giao diện phóng to
                if (thumbnailScrollPane != null && thumbnailScrollPane.getScene() != null) {
                    galleryStage.initOwner(thumbnailScrollPane.getScene().getWindow());
                }

                // Hiển thị cửa sổ lên
                galleryStage.show();

            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Lỗi khi nạp giao diện phóng to ảnh: " + e.getMessage());
            }
        }
        }
}
