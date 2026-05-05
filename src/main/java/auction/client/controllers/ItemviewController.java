package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.session.DataSession;
import auction.common.message.Message;
import auction.common.model.bid.Bid;
import auction.common.model.items.Item;
import auction.common.model.users.User;
import javafx.animation.KeyFrame;
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
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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

            handleGetLatestPrice(selectedItem.getId());
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

        loadHistoryBid();// Load bid history when initializing
    }

    private void handleGetLatestPrice(int id) {
        try {
            Item item;
            // Gửi request lấy Item mới nhất bằng ID
            Message response = ClientNetwork.getInstance().sendRequest(new Message("GET_ITEM_BY_ID", id));
            if (response != null && "SUCCESS".equals(response.getStatus())) {
                item=(Item) response.getData();
            } else {
                throw new RuntimeException("Server không phản hồi hoặc có lỗi xảy ra");
            }
            Platform.runLater(() -> {
                lbCurrentBid.setText(String.format("%,d $", item.getCurrentPrice()));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startAutoUpdate(int itemId) {
        if (autoUpdateTimeline != null) autoUpdateTimeline.stop();

        autoUpdateTimeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> {
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
                handleGetLatestPrice(itemId);
                loadHistoryBid();
                loadPriceChart();
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

    private void showBidError(String message) {
        Platform.runLater(() -> {
            lbBidError.setText(message);
            lbBidError.setVisible(true);
            lbBidError.setManaged(true);

            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(() -> {
                        lbBidError.setVisible(false);
                        lbBidError.setManaged(false);
                    });
                } catch (InterruptedException e) { e.printStackTrace(); }
            }).start();
        });
    }


    @FXML
    public void handlePlaceBid(){
        lbBidError.setVisible(false); // Ẩn lỗi cũ trước khi check mới
        lbBidError.setManaged(false);

        String bidValue = txtBid.getText().trim();
        if (bidValue.isEmpty()){
            showBidError("Vui lòng nhập số tiền!");
            return;
        }
        try {
            long amount = Long.parseLong(bidValue);
            Item selectedItem = DataSession.getInstance().getSelectedItem();
            User currentUser = DataSession.getInstance().getLoggedInUser();
            if ("ADMIN".equals(currentUser.getRole()) || currentUser.getId() == selectedItem.getSellerId()) {
                showBidError("Admin hoặc người bán không thể đấu giá!");
                return;
            }

            if (amount <= selectedItem.getCurrentPrice()) {
                showBidError("Giá trả phải lớn hơn" + selectedItem.getCurrentPrice());
                return;
            }

            Bid newBid = new Bid();
            newBid.setIdItem(selectedItem.getId());
            newBid.setIdUser(currentUser.getId());
            newBid.setBidAmount(amount);

            new Thread(() -> {
                Message response = ClientNetwork.getInstance().sendRequest(new Message("PLACE_BID", newBid));
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        long newPrice = Long.parseLong(bidValue);
                        selectedItem.setCurrentPrice(newPrice);
                        updateAllData(selectedItem.getId());
                        txtBid.clear();
                    }
                    showBidError(response.getData().toString());
                });
            }).start();
        }
        catch (NumberFormatException e) {
            showBidError("Vui lòng chỉ nhập số!");
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

    @FXML
    public void loadHistoryBid() {
        Item selectedItem = DataSession.getInstance().getSelectedItem();
        if (selectedItem == null) return;
        try {
            Message response = ClientNetwork.getInstance().sendRequest(new Message("GET_BID_BY_ITEM_ID", selectedItem.getId()));
            if (response != null && "SUCCESS".equals(response.getStatus())) {
                @SuppressWarnings("unchecked")
                List<Bid> bidHistory = (List<Bid>) response.getData();
                Platform.runLater(() -> {
                    bidTable.getItems().clear();
                    bidTable.getItems().setAll(bidHistory);
                });
            } else {
                throw new RuntimeException("Failed to fetch bid history.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPriceChart() {
        Item selectedItem = DataSession.getInstance().getSelectedItem();
        if (selectedItem == null) return;

        Message response = ClientNetwork.getInstance().sendRequest(new Message("GET_PRICE_CHART", selectedItem.getId()));
        if (response != null && "SUCCESS".equals(response.getStatus())) {
            @SuppressWarnings("unchecked")
            List<Object[]> priceChartData = (List<Object[]>) response.getData();

            Platform.runLater(() -> {
                priceChart.getData().clear();
                XYChart.Series<String, Number> series = new XYChart.Series<>();

                for (Object[] dataPoint : priceChartData) {
                    String time = dataPoint[0].toString();
                    Number price = (Number) dataPoint[1];
                    series.getData().add(new XYChart.Data<>(time, price));
                }

                priceChart.getData().add(series);
                priceChart.setCreateSymbols(true);
            });
        } else {
            throw new RuntimeException("Failed to fetch price chart data.");
        }
    }
}
