package auction.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import javafx.event.ActionEvent;
import java.io.IOException;

public class MainViewController {
    @FXML
    private TextField txtsearch;
    @FXML
    private ComboBox sortPrice, sortTime;
    @FXML
    private Label lbusername;
    @FXML
    public void initialize() {
        if (UserSession.loggedInUser != null){
            lbusername.setText("Chào: " + UserSession.loggedInUser.getUsername());
        }
        sortPrice.getItems().addAll("Giá tăng dần", "Giá giảm dần");
        sortTime.getItems().addAll("Thời gian tăng dần", "Thời gian giảm dần");
    }
    @FXML
    public void onProfileClick(MouseEvent event) throws IOException {
        switch (UserSession.loggedInUser.getRole()){
            case "ADMIN":
                ViewManager.switchScene(event, "admin-view.fxml", "Hồ sơ cá nhân");
                break;

            case "USER":
                ViewManager.switchScene(event, "profile-view.fxml", "Hồ sơ cá nhân");
                break;

            default:
                break;
        }
    }

    @FXML
    public void onItemClick(MouseEvent event) {
        ViewManager.switchScene(event, "item-view.fxml", "Chi tiết sản phẩm");
    }

    @FXML
    private void onSearchAction() {
        String query = txtsearch.getText();
        // Implement search logic
    }
}
