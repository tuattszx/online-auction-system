package auction.client.controllers;

import auction.common.model.users.User;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import javafx.event.ActionEvent;

import static auction.server.dao.UserDao.CheckLogin;
import auction.client.controllers.MainViewController;

import java.io.IOException;

class UserSession{
    public static User loggedInUser;
}
public class LoginController {
    @FXML
    private StackPane centerContainer;
    @FXML
    private VBox loginVBox;
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
    public void initialize() {
        changeImage("/auction/img/pxfuel.jpg");
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
    @FXML
    public void onLoginButtonClick(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi!", "Vui lòng nhập đầy đủ thông tin");
            return;
        }
        User user= CheckLogin(username,password);
        if (user != null){
            showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Đăng nhập thành công");
            UserSession.loggedInUser = user;
            //Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            ViewManager.clearCache();
            ViewManager.switchScene(event, "main-view.fxml", "Hệ thống Đấu giá - Trang chủ");
        }
        else{
            showAlert(Alert.AlertType.ERROR, "Lỗi!", "Sai tài khoản hoặc mật khẩu");
        }
    }
    @FXML
    public void onSignUpClick(ActionEvent event) throws IOException {
        //Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        ViewManager.switchScene(event, "register-view.fxml", "Hệ thống Đấu giá - Đăng ký");
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}