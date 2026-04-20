package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.common.message.Message;
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
    // Trong LoginController.java
    ClientNetwork network = new ClientNetwork();

    @FXML
    public void onLoginButtonClick() {
        try {
            network.connect();
            User user = new User(); // Tạo object user từ TextField
            user.setUsername(txtUsername.getText());
            user.setPassword(txtPassword.getText());

            Message response = network.sendRequest(new Message("LOGIN", user));

            if ("SUCCESS".equals(response.getStatus())) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng nhập qua Server thành công!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Thất bại", "Sai tài khoản hoặc mật khẩu!");
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối tới Server!");
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