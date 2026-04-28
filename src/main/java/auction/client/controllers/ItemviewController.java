package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.session.DataSession;
import auction.common.message.Message;
import auction.common.model.items.Item;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.*;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import java.io.ByteArrayInputStream;
import java.util.List;

public class ItemviewController {

    @FXML private Label itemName;
    @FXML private Label lbprice;
    @FXML private ImageView itemImg;
    @FXML private Label lbDescription;

    @FXML
    public void initialize() {
        // 1. Lấy dữ liệu từ "túi chung" DataSession
        Item selectedItem = DataSession.getInstance().getSelectedItem();

        if (selectedItem != null) {
            // 2. HIỆN DỮ LIỆU TĨNH
            itemName.setText(selectedItem.getName());
            lbDescription.setText(selectedItem.getDescription());

            // Đổ ảnh từ mảng byte
            if (selectedItem.getImages() != null && !selectedItem.getImages().isEmpty()) {
                byte[] data = selectedItem.getImages().get(0).getImageData();
                itemImg.setImage(new Image(new ByteArrayInputStream(data)));
            }

            // Tạm thời hiện giá cũ trong lúc chờ Server
            lbprice.setText(String.format("%,d $", selectedItem.getCurrentPrice()) + " (Updating...)");

            handleGetLatestPrice(selectedItem.getId());
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
                    lbprice.setText(String.format("%,d $", item.getCurrentPrice()));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    @FXML
    public void OnMouseBacktoMain(MouseEvent event){
        ViewManager.switchScene(event,"main-view.fxml", "Trang chủ");
    }
}
