package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.services.LanguageManager;
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
import org.mindrot.jbcrypt.BCrypt;


import javax.swing.text.View;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class LoginController {
    @FXML
    private StackPane centerContainer;
    @FXML
    private VBox loginVBox;
    @FXML private VBox resetPasswordVBox;
    @FXML
    private TextField txtUsername;
    @FXML
    private TextField txtEmailReset;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Button btnLogin;
    @FXML
    private Button btnSignUp;
    @FXML
    private Button btnVerify;
    @FXML
    private Button btnForgotPassword;
    @FXML
    private Label lbError;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML private VBox verifyCodeVBox;
    @FXML private TextField txtOTP;
    private String otpCode;
    @FXML private VBox newPasswordVBox;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmNewPassword;
    @FXML private Button btnResetPassword;
    @FXML private Button btnCancelReset;
    private List<VBox> vBoxes;
    ClientNetwork network = ClientNetwork.getInstance();
    @FXML
    public void initialize() {
        lbError.setVisible(false);
        vBoxes= Arrays.asList(loginVBox,resetPasswordVBox,verifyCodeVBox,newPasswordVBox);
        btnVerify.setStyle("-fx-background-color: #0052ff");
        changeImage("/auction/img/pxfuel.jpg");
        loadingIndicator.setVisible(false);
        resetPasswordVBox.setVisible(false);
        resetPasswordVBox.setManaged(false);
        loginVBox.setManaged(true);
        loginVBox.setVisible(true);
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
    public void onForgotButtonClick(ActionEvent event){
        for (VBox x:vBoxes){
            x.setVisible(false);
            x.setManaged(false);
        }
        resetPasswordVBox.setVisible(true);
        resetPasswordVBox.setManaged(true);
    }
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
    public void onSendCodeClick(ActionEvent event) {
        String emailInput = txtEmailReset.getText().trim();

        // Kiểm tra xem người dùng đã nhập email chưa
        if (emailInput.isEmpty()) {
            lbError.setText("Lỗi: Vui lòng nhập email xác thực!");
        } else {
            new Thread(() -> {
                // Gửi lệnh "FORGOT_PASSWORD" kèm theo chuỗi Email
                Message request = new Message("FORGOT_PASSWORD", emailInput);
                Message response = ClientNetwork.getInstance().sendRequest(request);
                // (Bạn có thể đổi ClientNetwork.getInstance() thành đối tượng "network" cũ của bạn nếu muốn)

                // 3. Đồng bộ kết quả trả về để cập nhật giao diện JavaFX
                Platform.runLater(() -> {
                    // Mở lại form và ẩn loading ẩn đi
                    loadingIndicator.setVisible(false);
                    resetPasswordVBox.setDisable(false);

                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        // Lưu lại mã OTP cố định được Server trả về vào biến toàn cục
                        this.otpCode = (String) response.getData();

                        // Hiển thị thông báo thành công dạng Pop-up giống ảnh mẫu của bạn
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Mã xác thực OTP đã được gửi đến Email của bạn!");
                        alert.show();

                        // CHUYỂN GIAO DIỆN: Ẩn màn nhập Email, hiện màn nhập OTP
                        resetPasswordVBox.setVisible(false);
                        resetPasswordVBox.setManaged(false);

                        verifyCodeVBox.setVisible(true);
                        verifyCodeVBox.setManaged(true);

                        lbError.setText(""); // Xóa thông báo chữ đỏ cũ
                    } else {
                        // Thất bại (Do lỗi Server hoặc Email không tồn tại trong DB)
                        String errMsg = (response != null) ? (String) response.getData() : "Không thể kết nối đến máy chủ!";

                        Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi hệ thống: " + errMsg);
                        alert.show();

                        lbError.setText(errMsg);
                    }
                });
            }).start(); // Bắt đầu kích hoạt luồng chạy ngầm
        }
    }
    public void onBackToLoginClick(ActionEvent event){
        for (VBox x:vBoxes){
            x.setVisible(false);
            x.setManaged(false);
        }
        loginVBox.setVisible(true);
        loginVBox.setManaged(true);
    }
    @FXML
    public void onVerifyCodeClick(ActionEvent event) {
        String otpInput = txtOTP.getText().trim(); //

        // 1. Kiểm tra xem người dùng đã nhập mã chưa
        if (otpInput.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Vui lòng nhập mã xác thực OTP!");
            alert.show();
            return;
        }

        // Khóa giao diện màn hình nhập mã để tạo cảm giác hệ thống đang xử lý xử lý ngầm
        verifyCodeVBox.setDisable(true);

        // 2. Chạy Thread ngầm giả lập xử lý kiểm tra mã đúng theo phong cách ảnh 30a720
        new Thread(() -> {
            try {
                // Giả lập hệ thống delay 0.5 giây kiểm tra cho giống thật
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 3. Sử dụng Platform.runLater để cập nhật lại giao diện JavaFX
            Platform.runLater(() -> {
                verifyCodeVBox.setDisable(false); // Mở khóa giao diện

                // So sánh mã người dùng nhập với mã OTP cố định (this.serverOtpCode đã lưu là "123456")
                if (otpInput.equals(this.otpCode)) {

                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Xác thực thành công! Vui lòng đặt lại mật khẩu mới.");
                    alert.showAndWait(); // Chờ người dùng click OK

                    // CHUYỂN GIAO DIỆN: Ẩn màn nhập OTP, hiển thị màn đặt mật khẩu mới
                    verifyCodeVBox.setVisible(false);
                    verifyCodeVBox.setManaged(false);

                    newPasswordVBox.setVisible(true);
                    newPasswordVBox.setManaged(true);

                    txtOTP.clear(); // Xóa sạch ô nhập OTP cũ
                } else {
                    // Nếu nhập sai mã
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Mã xác thực OTP không chính xác. Vui lòng thử lại!");
                    alert.show();
                }
            });
        }).start();
    }
    @FXML
    public void onBackToResetClick(ActionEvent event) {
        for (VBox x:vBoxes){
            x.setVisible(false);
            x.setManaged(false);
        }
        resetPasswordVBox.setVisible(true);
        resetPasswordVBox.setManaged(true);
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
                if (DataSession.getInstance().getLoggedInUser().isBanned()) {
                    ViewManager.showAlert(Alert.AlertType.WARNING, "Thất bại", "Tài khoản cua ban da bi BAN!");
                    DataSession.getInstance().setLoggedInUser(null);
                    return "WRONG_AUTH";
                }

                LanguageManager.setLocale(("English".equals(DataSession.getInstance().getLoggedInUser().getLanguage())) ? "en" : "vi");
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
    @FXML
    public void onResetPasswordClick(ActionEvent event) {
        String newPass = txtNewPassword.getText().trim();
        String confirmPass = txtConfirmNewPassword.getText().trim();

        if (newPass.length() < 6) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Mật khẩu mới phải từ 6 ký tự trở lên!");
            alert.show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Mật khẩu xác nhận không trùng khớp!");
            alert.show();
            return;
        }

        // Khóa giao diện và bật loading trong lúc gửi qua mạng
        loadingIndicator.setVisible(true);
        newPasswordVBox.setDisable(true);

        // 2. Chạy luồng nền xử lý mã hóa và gửi Socket theo style ảnh 30a720
        new Thread(() -> {
            // Mã hóa mật khẩu mới bằng BCrypt trước khi gửi qua mạng để bảo mật đường truyền
            String hashedNewPassword = BCrypt.hashpw(newPass, BCrypt.gensalt());

            // Đóng gói: Mẹo nhỏ là gửi một mảng Object[] chứa {Email, Mật khẩu đã băm} giống ảnh mẫu của bạn
            Message request = new Message("UPDATE_PASSWORD", new Object[]{txtEmailReset.getText().trim(), hashedNewPassword});
            Message response = ClientNetwork.getInstance().sendRequest(request);

            // 3. Cập nhật lại giao diện FX chính
            Platform.runLater(() -> {
                loadingIndicator.setVisible(false);
                newPasswordVBox.setDisable(false);

                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Đổi mật khẩu thành công! Hãy đăng nhập lại bằng mật khẩu mới.");
                    alert.showAndWait(); // Chờ người dùng ấn OK mới chuyển form

                    // Làm sạch form
                    txtNewPassword.clear();
                    txtConfirmNewPassword.clear();

                    // CHUYỂN GIAO DIỆN: Quay về màn hình đăng nhập chính
                    newPasswordVBox.setVisible(false);
                    newPasswordVBox.setManaged(false);

                    loginVBox.setVisible(true);
                    loginVBox.setManaged(true);
                } else {
                    String errMsg = (response != null) ? (String) response.getData() : "Lỗi kết nối Server!";
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi: " + errMsg);
                    alert.show();
                }
            });
        }).start();
    }
}