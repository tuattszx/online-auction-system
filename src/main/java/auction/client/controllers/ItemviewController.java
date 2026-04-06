package auction.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class ItemviewController {
    @FXML
    public void onBackToMainClick(ActionEvent event){
        ViewManager.switchScene(event, "main-view.fxml", "Trang chủ");
    }
}
