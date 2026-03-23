package auction;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField txtUsername;
    // giong ben fx:id
    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnLogin;

    @FXML
    public void onLoginButtonClick() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        System.out.println("Tài khoản: " + username);
    }
}