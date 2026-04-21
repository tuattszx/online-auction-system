package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.common.message.Message;
import auction.common.model.users.Account;
import auction.common.model.users.User;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;


import java.io.IOException;

class AccountSession {
    public static Account loggedInAccount;
}
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
    private ProgressIndicator loadingIndicator;
    @FXML
    public void initialize() {
        changeImage("/auction/img/pxfuel.jpg");
        loadingIndicator.setVisible(false);
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
    public void onLoginButtonClick(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        loadingIndicator.setVisible(true);
        loginVBox.setDisable(true);

        Task<Message> loginTask = new Task<>() {
            @Override
            protected Message call() throws Exception {
                Account account = new Account(username, password);
                return network.sendRequest(new Message("LOGIN", account));
            }
        };

        loginTask.setOnSucceeded(e -> {
            Message response = loginTask.getValue();
            if (response != null && "SUCCESS".equals(response.getStatus()) && response.getData() != null) {
                // Lưu vào static field để các màn hình sau có thể dùng
                Account.loggedInAccount = (Account) response.getData();

                ViewManager.switchScene(event, "main-view.fxml", "Trang chủ");
            } else {
                resetUI();
                showAlert(Alert.AlertType.ERROR, "Thất bại", "Tài khoản/Mật khẩu không đúng!");
            }
        });
        loginTask.setOnFailed(e -> {
            resetUI();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối đến Server!");
        });

        new Thread(loginTask).start();
    }

    private void resetUI() {
        loadingIndicator.setVisible(false);
        loginVBox.setDisable(false);
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