package auction.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import javafx.event.ActionEvent;
import java.io.IOException;

public class MainViewController {
    @FXML
    private Label lbbalance;
    @FXML
    private Button btusername;
    @FXML
    private TextField txtsearch;
    @FXML
    private FlowPane flitems;
    @FXML
    private Button btntrangsuc;
    @FXML
    private Button btndongho;
    @FXML
    private Button btnthoitrang;
    @FXML
    private Button btnsach;

    @FXML
    public void initialize() {
        if (UserSession.loggedInUser != null){
            btusername.setText("Chào: " + UserSession.loggedInUser);
        }
    }
    @FXML
    public void onProfileClick(ActionEvent event) throws IOException {
        ViewManager.switchScene(event, "profile-view.fxml", "Hồ sơ cá nhân");
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
