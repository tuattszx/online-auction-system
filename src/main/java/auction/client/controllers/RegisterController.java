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
    private TextField txtphonenumber;
    @FXML
    private StackPane centerContainer;
    @FXML
    public void onSignupButtonClick(ActionEvent event){
        String fullname = txtFullName.getText().trim();
        String userName = txtUserName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtphonenumber.getText().trim();
        String password = txtPassword.getText().trim();
        String repass = txtRePassword.getText().trim();
        if (fullname.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Họ và tên không được để trống!");
            return;
        }

        if (userName.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Tài khoản không được để trống!");
            return;
        }

        if (email.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Email không được để trống!");
            return;
        }

        if (phone.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Số điện thoại không được để trống!");
            return;
        }

        if (password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Mật khẩu không được để trống!");
            return;
        }

        if (repass.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Vui lòng nhập lại mật khẩu!");
            return;
        }

        // 3. Kiểm tra định dạng Email
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (!email.matches(emailRegex)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Định dạng email không hợp lệ (ví dụ: user@gmail.com)!");
            return;
        }

        // 4. Kiểm tra định dạng Số điện thoại (Phải là số và đủ 10-11 ký tự)
        if (!phone.matches("\\d{10,11}")) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Số điện thoại phải là chữ số và dài từ 10 đến 11 ký tự!");
            return;
        }

        // 5. Kiểm tra độ dài mật khẩu (Tối thiểu 6 ký tự để bảo mật)
        if (password.length() < 6) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Mật khẩu phải có ít nhất 6 ký tự!");
            return;
        }

        // 6. Kiểm tra mật khẩu trùng khớp
        if (!password.equals(repass)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Mật khẩu không trùng khớp!");
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