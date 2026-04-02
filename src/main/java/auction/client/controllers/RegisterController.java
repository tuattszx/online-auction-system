package auction.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javafx.event.ActionEvent;

import static auction.server.DatabaseManager.registerUser;

public class RegisterController {
    @FXML
    private  TextField txtUserName;

    @FXML
    private  TextField txtEmail;

    @FXML
    private  PasswordField txtPassword;

    @FXML
    private  PasswordField txtRePassword;

    @FXML
    public void onSignupButtonClick(){
        String userName=txtUserName.getText();
        String password=txtPassword.getText();
        String email=txtEmail.getText();
        String repass=txtRePassword.getText();
        if (userName == null || userName.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Tài khoản không được để trống!");
            return;
        }
        if (email == null || email.isEmpty()){
            showAlert(Alert.AlertType.ERROR,"Lỗi đăng ký", "Email cần được nhập");
            return;
        }

        if (password == null || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Mật khẩu không được để trống!");
            return;
        }
        if (repass ==null || repass.isEmpty()){
            showAlert(Alert.AlertType.ERROR,"Lỗi đăng ký", "Cần nhập lại mật khẩu");
            return;
        }
        if (!password.equals(repass)){
            showAlert(Alert.AlertType.ERROR,"Lỗi đăng ký","Mật khẩu không trùng khớp");
            return;
        }
        if (registerUser(userName, password, email)){
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký tài khoản thành công!");
            Stage stage = (Stage) txtUserName.getScene().getWindow();
            SwitchScene.switchScene(stage, "login-view.fxml", "Hệ thống Đấu giá - Đăng nhập");
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Tên tài khoản đã tồn tại hoặc có lỗi xảy ra.");
        }
    }
    @FXML
    public void onBackToLoginClick(ActionEvent event){
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SwitchScene.switchScene(stage, "login-view.fxml", "Hệ thống Đấu giá - Đăng nhập");
    }
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}