package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.services.AuctionManager;
import auction.client.session.DataSession;
import auction.client.utils.ServerTimeSync;
import auction.common.message.Message;
import auction.common.model.bid.Bid;
import auction.common.model.items.Item;
import auction.common.model.users.User;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

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
                byte[] data = selectedItem.getImages().get(0).getImageData();
                mainImage.setImage(new Image(new ByteArrayInputStream(data)));
            }

            lbCurrentBid.setText(String.format("€ %,d", selectedItem.getCurrentPrice(),"Updating...."));

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
        if (bidValue.isEmpty()){
            showBidError("Vui lòng nhập số tiền!",false);
            return;
        }
        try {
            long amount = Long.parseLong(bidValue);
            if ("ADMIN".equals(currentUser.getRole()) || currentUser.getId() == selectedItem.getSellerId()) {
                showBidError("Admin hoặc người bán không thể đấu giá!",false);
                return;
            }

            if (amount <= selectedItem.getCurrentPrice()) {
                showBidError("Giá trả phải lớn hơn " + selectedItem.getCurrentPrice(),false);
                return;
            }

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
        catch (NumberFormatException e) {
            showBidError("Vui lòng chỉ nhập số!",false);
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
}
