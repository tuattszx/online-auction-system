package auction.client.controllers;

import auction.common.model.users.User;
import auction.common.Message;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javafx.event.ActionEvent;

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
            showAlert(Alert.AlertType.ERROR,"Lỗi đăng ký", "Cần nhập lại mâtj khẩu");
            return;
        }
        if (!password.equals(repass)){
            showAlert(Alert.AlertType.ERROR,"Lỗi đăng ký","Mật khẩu không trùng khớp");
            return;
        }
        User newUser = new User(0, userName, password, "USER", email, "N/A", 0);
        Message regMsg = new Message("REGISTER",newUser);
       // ClientNetwork.getInstance().sendMessage(regMsg);
    }
    @FXML
    public void onBackToLoginClick(ActionEvent event){
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        MainAuctionController.switchScene(stage, "login-view.fxml", "Hệ thống Đấu giá - Đăng nhập");
    }
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}