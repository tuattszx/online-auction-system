package auction.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javafx.event.ActionEvent;

import static auction.server.dao.UserDao.CheckLogin;

public class LoginController {

    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Button btnLogin;
    @FXML
    private Button btnSignUp;
    @FXML
    private Button btnForgotPassword;
    @FXML
    public void onLoginButtonClick(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi!", "Vui lòng nhập đầy đủ thông tin");
            return;
        }
        if (CheckLogin(username, password)){
            showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Đăng nhập thành công");
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            MainAuctionController.switchScene(stage, "main-view.fxml", "Hệ thống Đấu giá - Trang chủ");
        }
        else{
            showAlert(Alert.AlertType.ERROR, "Lỗi!", "Sai tài khoản hoặc mật khẩu");
        }
    }
    @FXML
    public void onSignUpClick(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        MainAuctionController.switchScene(stage, "register-view.fxml", "Hệ thống Đấu giá - Đăng ký");
    }
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}