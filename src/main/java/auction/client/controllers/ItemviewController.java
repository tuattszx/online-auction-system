package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.services.AuctionManager;
import auction.client.session.DataSession;
import auction.client.utils.ServerTimeSync;
import auction.common.message.Message;
import auction.common.model.bid.Bid;
import auction.common.model.items.*;
import auction.common.model.users.User;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.Cursor;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ItemviewController {
    @FXML private ImageView mainImage;
    @FXML private Label itemNameLabel;
    @FXML private Label lbCurrentBid;
    @FXML private Label lbShortDesc;
    @FXML private Label lbbalance;
    @FXML private Hyperlink btnShowMore;
    @FXML private VBox Vboxdetails;
    @FXML private LineChart<String ,Number> priceChart;
    @FXML private TextField txtBid;
    @FXML private Label lbBidError;
    @FXML private Label lbstarttime;
    @FXML private Label lbendtime;

    @FXML
    private TableView<Bid> bidTable;
    @FXML
    private TableColumn<Bid, String> colBidder;
    @FXML
    private TableColumn<Bid, String> colTime;
    @FXML
    private TableColumn<Bid, String> colPrice;

    private Timeline autoUpdateTimeline;
    private volatile boolean isUpdatingLastestPrice = false;
    private PauseTransition errorTimeout = new PauseTransition(javafx.util.Duration.seconds(3));

    @FXML
    public void initialize() {
        //Lấy dữ liệu "Tĩnh" từ Session
        Item selectedItem = DataSession.getInstance().getSelectedItem();

        if (selectedItem != null) {
            itemNameLabel.setText(selectedItem.getName());
            lbShortDesc.setText(selectedItem.getDescription());

            if (selectedItem.getImages() != null && !selectedItem.getImages().isEmpty()) {
                String imageUrl = selectedItem.getImages().get(0).getUrlImage();

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    // Load ảnh trực tiếp từ link Cloudinary
                    Image img = new Image(imageUrl, true);
                    mainImage.setImage(img);
                }
            }

            lbCurrentBid.setText(String.format("€ %,d", selectedItem.getCurrentPrice(),"Updating...."));

            loadExtraImages(selectedItem.getId());
            updateAllData(selectedItem.getId());
            startAutoUpdate(selectedItem.getId());
        }


        // Hiện số dư người dùng
        if (DataSession.getInstance().getLoggedInUser() != null) {
            lbbalance.setText(String.format("%,d$", DataSession.getInstance().getLoggedInUser().getBalance()));
        }

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

    private String toString(LocalDateTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return time.format(formatter);
    }

    private void startAutoUpdate(int itemId) {
        if (autoUpdateTimeline != null) autoUpdateTimeline.stop();

        autoUpdateTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(3), event -> {
            // Chỉ chạy nếu luồng trước đó đã xong
            if (!isUpdatingLastestPrice) {
                updateAllData(itemId);
            }
        }));
        autoUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        autoUpdateTimeline.play();
    }

    private void updateAllData(int itemId) {
        isUpdatingLastestPrice = true;
        new Thread(() -> {
            try {
                Item item = AuctionManager.getInstance().getLatestItem(itemId);
                List<Bid> bids = AuctionManager.getInstance().getBidHistory(itemId);
                List<Object[]> chartData = AuctionManager.getInstance().getPriceChart(itemId);

                Platform.runLater(() -> {
                    if (item != null) lbCurrentBid.setText(String.format("%,d $", item.getCurrentPrice()));
                    if (bids != null) bidTable.getItems().setAll(bids);
                    if (chartData != null) loadPriceChart(chartData);
                });
            } finally {
                isUpdatingLastestPrice = false;
            }
        }).start();
    }
    private void stopTimeline() {
        if (autoUpdateTimeline != null) {
            autoUpdateTimeline.stop();
        }
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

        /*if ("PENDING".equals(selectedItem.getStatus())) {
            showBidError("Phiên đấu giá chưa bắt đầu!", false);
            return;
        } else if ("CLOSED".equals(selectedItem.getStatus())) {
            showBidError("Phiên đấu giá đã kết thúc!", false);
            return;
        }*/

        String bidValue = txtBid.getText().trim();
        try {
            validateBid(bidValue, selectedItem.getCurrentPrice(), currentUser.getId(), selectedItem.getSellerId(), currentUser.getRole());
            long amount = Long.parseLong(bidValue);

            new Thread(() -> {
                Message response = AuctionManager.getInstance().placeBid(selectedItem.getId(), currentUser.getId(), amount);
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        long newPrice = Long.parseLong(bidValue);
                        selectedItem.setCurrentPrice(newPrice);
                        updateAllData(selectedItem.getId());
                        txtBid.clear();
                        showBidError(response.getData().toString(),true);
                    }
                    else {
                        showBidError(response.getData().toString(),false);
                    }
                });
            }).start();
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

        if ("ADMIN".equals(userRole) || userId == sellerId) {
            throw new IllegalArgumentException("Admin hoặc người bán không thể đấu giá!");
        }

        if (amount <= currentPrice) {
            throw new IllegalArgumentException("Giá trả phải lớn hơn " + currentPrice + "$");
        }
    }
    @FXML
    public void onBackToMainClick(ActionEvent event){
        ViewManager.switchScene(event, "main-view.fxml", "Trang chủ");
    }
    @FXML
    public void OnMouseBacktoMain(MouseEvent event){
        stopTimeline();
        ViewManager.removeView("main-view.fxml");
        DataSession.getInstance().setSelectedItem(null);
        ViewManager.switchScene(event,"main-view.fxml", "Trang chủ");

    }
    @FXML
    public void handleShowMore(ActionEvent event){
        Vboxdetails.setVisible(true);
        priceChart.setVisible(false);
        btnShowMore.setText("Show less");
    }
    @FXML
    public void handleShowLess(ActionEvent event){
        Vboxdetails.setVisible(false);
        priceChart.setVisible(false);
        btnShowMore.setText("Show more");
    }
    @FXML
    public void handleLineChart(MouseEvent event){
        priceChart.setVisible(true);
        Vboxdetails.setVisible(false);
    }
    @FXML
    public void onSellerClick(MouseEvent event) throws IOException {
        ViewManager.switchScene(event, "seller_demo.fxml", "seller page");
    }
    @FXML
    public void onProfileClick(MouseEvent event) throws IOException {
        if (DataSession.getInstance().getLoggedInUser() == null) return;

        String view = DataSession.getInstance().getLoggedInUser().getRole().equals("ADMIN") ? "admin-view.fxml" : "profile-view.fxml";
        ViewManager.switchScene(event, view, "Hồ sơ cá nhân");
    }

    private void loadPriceChart(List<Object[]> priceChartData) {
        priceChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (Object[] dataPoint : priceChartData) {
            String time = dataPoint[0].toString();
            Number price = (Number) dataPoint[1];
            series.getData().add(new XYChart.Data<>(time, price));
        }

        priceChart.getData().add(series);

    }
    @FXML private Label lblDays, lblHours, lblMins, lblSecs;

    private Timeline timeline;
    private Item currentItem; // Object chứa endTime từ Database

    public void setItemData(Item item) {
        this.currentItem = item;
        startCountdown();
    }

    private void startCountdown() {
        if (timeline != null) timeline.stop();

        timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
            updateCountdownUI();
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void updateCountdownUI() {
        // 1. Lấy giờ chuẩn Server đã đồng bộ (Dùng class ServerTimeSync đã làm ở bước trước)
        LocalDateTime now = ServerTimeSync.getNow();
        LocalDateTime startTime = currentItem.getStartTime();
        LocalDateTime endTime = currentItem.getEndTime();

        // 2. Tính khoảng cách
        Duration duration;
        boolean isStarted = now.isAfter(startTime);
        boolean isEnded = now.isAfter(endTime);
        if (!isStarted) {
            duration = Duration.between(now, startTime);
            setCountdownColor(Color.BLACK); // Chưa bắt đầu
        }
        else if (isEnded) {
            timeline.stop();
            currentItem.setStatus("CLOSED");
            setCountdownColor(Color.GRAY); // Đã kết thúc - màu xám
            displayExpired();
            return;
        }
        else {
            duration = Duration.between(now, endTime);
            setCountdownColor(Color.GREEN); // Đang diễn ra - màu xanh lá
            if (!"OPEN".equals(currentItem.getStatus())) {
                currentItem.setStatus("OPEN");
            }
        }
        long seconds = duration.getSeconds();

        if (seconds <= 0) {
            timeline.stop();
            lblDays.setText("00");
            lblHours.setText("00");
            lblMins.setText("00");
            lblSecs.setText("00");
            return;
        }

        // 3. Phân tách thời gian
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        // 4. Hiển thị lên giao diện (Format %02d để luôn có 2 chữ số, ví dụ: 03)
        lblDays.setText(String.format("%02d", days));
        lblHours.setText(String.format("%02d", hours));
        lblMins.setText(String.format("%02d", minutes));
        lblSecs.setText(String.format("%02d", secs));
    }

    private void displayExpired() {
        lblDays.setText("00");
        lblHours.setText("00");
        lblMins.setText("00");
        lblSecs.setText("00");
        // Có thể thêm thông báo "Phiên đấu giá đã kết thúc"
    }

    private void setCountdownColor(Color color) {
        lblDays.setTextFill(color);
        lblHours.setTextFill(color);
        lblMins.setTextFill(color);
        lblSecs.setTextFill(color);
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

                thumb.setFitHeight(80.0);
                thumb.setFitWidth(80.0);
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
        new Thread(() -> {
            try {
                Message response = AuctionManager.getInstance().getItemImages(itemId);

                if ("SUCCESS".equals(response.getStatus())) {
                    List<ItemImage> allImages = (List<ItemImage>) response.getData();

                    Platform.runLater(() -> {
                        Item tempItem = new Item();
                        tempItem.setImages(allImages);
                        displayThumbnails(tempItem);
                    });
                }
            } catch (Exception e) {
                System.err.println("Không thể tải thêm ảnh: " + e.getMessage());
            }
        }).start();
    }
}
