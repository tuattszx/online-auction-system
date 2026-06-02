package auction.client.controllers; // Cập nhật lại đúng package của bạn

import auction.client.services.LanguageManager;
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
    @FXML private Button btnClose;

    /**
     * Hàm nhận thực thể User từ bảng bên ngoài truyền vào và map lên các nhãn hiển thị
     */
    public void setCustomerData(User user) {
        if (user == null) return;

        lblUsername.setText(user.getUsername());

        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                (user.getLastName() != null ? user.getLastName() : "");
        lblFullName.setText(fullName.trim().isEmpty() ? LanguageManager.getString("sellerdemo.label.notudated") : fullName.trim());

        lblPhone.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : LanguageManager.getString("sellerdemo.label.notudated"));
        lblEmail.setText(user.getEmail() != null ? user.getEmail() : LanguageManager.getString("sellerdemo.label.notudated"));
        lblAddress.setText(user.getAddress() != null ? user.getAddress() : LanguageManager.getString("sellerdemo.label.notudated"));

    }

    @FXML
    private void handleClose() {
        // Lấy Stage hiện tại của cửa sổ popup nhỏ này và đóng nó lại
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }
}