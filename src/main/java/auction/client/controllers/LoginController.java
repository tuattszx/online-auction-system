package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.session.DataSession;
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


import javax.swing.text.View;
import java.io.IOException;

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
    ClientNetwork network = ClientNetwork.getInstance();
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

    @FXML
    public void onLoginButtonClick(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (!validateInput(username, password)) {
            ViewManager.showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng nhập đầy đủ thông tin!");
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
            resetUI(); // Luôn reset UI trước khi xử lý tiếp

            if ("GOTO_MAIN".equals(handleLoginResponse(response))) {
                ViewManager.switchScene(event, "main-view.fxml", "Trang chủ");
            }
        });
        loginTask.setOnFailed(e -> {
            resetUI();
            ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối đến Server!");
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

    // Test
    public boolean validateInput(String username, String password) {
        return username != null && !username.trim().isEmpty()
                && password != null && !password.trim().isEmpty();
    }

    public String handleLoginResponse(Message response) {
        if (response == null) return "ERROR";

        switch (response.getStatus()) {
            case "SUCCESS":
                DataSession.getInstance().setLoggedInUser((User) response.getData());
                return "GOTO_MAIN";
            case "FAILED":
                ViewManager.showAlert(Alert.AlertType.WARNING, "Thất bại", "Tài khoản hoặc mật khẩu không chính xác!");
                return "WRONG_AUTH";
            case "SERVER_OFFLINE":
                ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Máy chủ hiện không hoạt động. Vui lòng thử lại sau!");
                return "OFFLINE";
            default:
                ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi", "Đã xảy ra lỗi không xác định!");
                return "UNKNOWN";
        }
    }
}