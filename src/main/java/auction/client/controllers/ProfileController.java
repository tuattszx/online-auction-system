package auction.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
public class ProfileController {
    @FXML
    public void onBackToMainClick(javafx.event.ActionEvent event) {
        ViewManager.switchScene(event, "main-view.fxml", "Hệ thống Đấu giá - Trang chủ");
    }
    @FXML
    public void OnMouseBacktoMain(MouseEvent event){
        ViewManager.switchScene(event,"main-view.fxml", "Trang chủ");

    }
    @FXML
    public void onLogoutClick(javafx.event.ActionEvent event) {
        UserSession.loggedInUser = null;
        ViewManager.clearCache();
        ViewManager.switchScene(event, "login-view.fxml", "Hệ thống Đấu giá - Đăng nhập");
    }

}
