package auction.client.controllers; // Cập nhật lại đúng package của bạn

import auction.common.model.users.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class CustomerDetailPopupController {

    @FXML private Label lblUsername;
    @FXML private Label lblFullName;
    @FXML private Label lblPhone;
    @FXML private Label lblEmail;
    @FXML private Label lblAddress;
    @FXML private Label lblBalance;
    @FXML private Button btnClose;

    /**
     * Hàm nhận thực thể User từ bảng bên ngoài truyền vào và map lên các nhãn hiển thị
     */
    public void setCustomerData(User user) {
        if (user == null) return;

        lblUsername.setText(user.getUsername());

        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                (user.getLastName() != null ? user.getLastName() : "");
        lblFullName.setText(fullName.trim().isEmpty() ? "Chưa cập nhật" : fullName.trim());

        lblPhone.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "Chưa cập nhật");
        lblEmail.setText(user.getEmail() != null ? user.getEmail() : "Không có");
        lblAddress.setText(user.getAddress() != null ? user.getAddress() : "Chưa cập nhật");

        // Định dạng hiển thị tiền tệ số dư ví khách hàng
        lblBalance.setText(String.format("%,d VND", user.getBalance()));
    }

    @FXML
    private void handleClose() {
        // Lấy Stage hiện tại của cửa sổ popup nhỏ này và đóng nó lại
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }
}