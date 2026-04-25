package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.common.message.Message;
import auction.common.model.users.User;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javafx.event.ActionEvent;

public class RegisterController {
    @FXML
    private Label lberror;
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
    private ProgressIndicator loadingIndicator;
    @FXML
    private VBox registerVbox;
    // Trong RegisterController.java
    ClientNetwork network = ClientNetwork.getInstance(); // Sử dụng hạ tầng mạng đã có

    @FXML
    public void onSignupButtonClick(ActionEvent event) {
        // 1. Thu thập dữ liệu
        User newUser = gatherUserData();

        // 2. Kiểm tra dữ liệu tại chỗ (Client-side validation)
        String errorMsg = validateInput(newUser, txtRePassword.getText().trim());
        if (errorMsg != null) {
           // ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", errorMsg);
            lberror.setText(errorMsg);
            return; // Thoát sớm nếu dữ liệu nhập sai
        }

        loadingIndicator.setVisible(true);
        registerVbox.setDisable(true);

        Task<Message> registerTask = new Task<>(){
            @Override
            protected Message call() throws Exception {
                return network.sendRequest(new Message("REGISTER", newUser));
            }
        };

        registerTask.setOnSucceeded(e -> {
            Message response = registerTask.getValue();
            resetUI();
            switch (response.getStatus()){
                case "SUCCESS":
                    ViewManager.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký thành công!");
                    ViewManager.switchScene(event, "login-view.fxml", "Hệ thống Đấu giá - Đăng nhập");
                    break;

                case "SERVER_OFFLINE":
                    ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Máy chủ hiện không hoạt động. Vui lòng thử lại sau!");
                    break;

                case "FAILED":
                    ViewManager.showAlert(Alert.AlertType.WARNING, "Thất bại", "Tài khoản hoặc email đã tồn tại!");
                    break;

                default:
                    ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi", "Đã xảy ra lỗi không xác định!");
                    break;
            }
        });
        registerTask.setOnFailed(e -> {
            resetUI();
            ViewManager.showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối đến Server!");
        });
        new Thread(registerTask).start();
    }

    // Hàm gom nhóm kiểm tra để main method sạch sẽ
    private String validateInput(User user, String repass) {
        if (user.getDisplayName().isEmpty()) return "Họ tên không được để trống!";
        if (user.getUsername().isEmpty()) return "Tài khoản không được để trống!";
        if (user.getEmail().isEmpty() || !user.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$"))
            return "Email không hợp lệ!";
        if (user.getPhoneNumber().length() < 10) return "Số điện thoại không hợp lệ!";
        if (user.getPassword().length() < 6) return "Mật khẩu tối thiểu 6 ký tự!";
        if (!user.getPassword().equals(repass)) return "Mật khẩu không trùng khớp!";
        return null; // Không có lỗi
    }

    private User gatherUserData() {
        User user = new User();
        user.setDisplayName(txtFullName.getText().trim());
        user.setUsername(txtUserName.getText().trim());
        user.setEmail(txtEmail.getText().trim());
        user.setPhoneNumber(txtphonenumber.getText().trim());
        user.setPassword(txtPassword.getText().trim());
        return user;
    }
    private void resetUI(){
        loadingIndicator.setVisible(false);
        registerVbox.setDisable(false);
    }
    @FXML
    public void onBackToLoginClick(ActionEvent event){
        ViewManager.switchScene(event, "login-view.fxml", "Hệ thống Đấu giá - Đăng nhập");
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