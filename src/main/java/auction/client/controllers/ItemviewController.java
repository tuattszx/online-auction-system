package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.session.DataSession;
import auction.common.message.Message;
import auction.common.model.items.Item;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ItemviewController {
    @FXML private ImageView mainImage;
    @FXML private Label itemNameLabel;
    @FXML private Label lbCurrentBid;
    @FXML private Label lbShortDesc;
    @FXML private Label lbbalance;
    @FXML private Hyperlink btnShowMore;
    @FXML private VBox Vboxdetails;
    @FXML private LineChart<CategoryAxis,CategoryAxis> priceChart;

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
        }


        // Hiện số dư người dùng
        if (DataSession.getInstance().getLoggedInUser() != null) {
            lbbalance.setText(String.format("%,d$", DataSession.getInstance().getLoggedInUser().getBalance()));
        }
    }

    private void handleGetLatestPrice(int id) {
        // Tạo luồng chạy ngầm để không treo giao diện
        new Thread(() -> {
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
        }).start();
    }

    @FXML
    public void onBackToMainClick(ActionEvent event){
        ViewManager.switchScene(event, "main-view.fxml", "Trang chủ");
    }
    @FXML
    public void OnMouseBacktoMain(MouseEvent event){
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
}
