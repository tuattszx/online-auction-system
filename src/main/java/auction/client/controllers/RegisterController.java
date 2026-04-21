package auction.client.controllers;

import auction.common.model.users.User;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import javafx.event.ActionEvent;

import static auction.server.dao.UserDao.registerUser;

public class RegisterController {
    @FXML
    private  TextField txtUserName;
    @FXML
    private  TextField txtFullName;
    @FXML
    private  TextField txtEmail;

    @FXML
    private  PasswordField txtPassword;

    @FXML
    private  PasswordField txtRePassword;

    @FXML
    private StackPane centerContainer;
    @FXML
    public void onSignupButtonClick(ActionEvent event){
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

        User newUser=new User();
        newUser.setUsername(userName);
        newUser.setPassword(password);
        newUser.setEmail(email);

        if (registerUser(newUser)){
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký tài khoản thành công!");
            ViewManager.switchScene(event, "login-view.fxml", "Hệ thống Đấu giá - Đăng nhập");
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Tên tài khoản đã tồn tại hoặc có lỗi xảy ra.");
        }
    }
    @FXML
    public void onBackToLoginClick(MouseEvent event){
        ViewManager.switchScene(event, "login-view.fxml", "Hệ thống Đấu giá - Đăng nhập");
    }
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    public void changeImage(String newPath) {
        var resource = getClass().getResource(newPath);
        if (resource != null) {
            String url = resource.toExternalForm();
            // Dùng CSS để ép ảnh phủ kín (cover) và luôn nằm giữa
            centerContainer.setStyle(
                    "-fx-background-image: url('" + url + "'); " +
                            "-fx-background-position: center center; " +
                            "-fx-background-repeat: no-repeat; " +
                            "-fx-background-size: cover;"
            );
        }
    }
    public void initialize() {
        changeImage("/auction/img/pxfuel.jpg");
    }

}