package auction.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
        }

        if (password == null || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Mật khẩu không được để trống!");
            return;
        }
        if (repass ==null || repass.isEmpty()){
            showAlert(Alert.AlertType.ERROR,"Lỗi đăng ký", "Cần nhập lại mâtj khẩu");
        }
        if (password != repass){
            showAlert(Alert.AlertType.ERROR,"Lỗi đăng ký","Mật khẩu không trùng khớp");
        }
    }
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}