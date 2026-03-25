package auction.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javafx.event.ActionEvent;

public class LoginController {

    @FXML
    private TextField txtUsername;
    // giong ben fx:id
    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnLogin;

    @FXML
    public void onLoginButtonClick() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username == null || username.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", "Tài khoản không được để trống!");
            return;
        }

        if (password == null || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", "Mật khẩu không được để trống!");
            return;
        }
        // Logic xử lý đăng nhập (Ví dụ tạm thời)
        if (username.equals("admin") && password.equals("123456")) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng nhập thành công!");
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Sai tài khoản hoặc mật khẩu!");
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