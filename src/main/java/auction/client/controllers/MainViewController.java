package auction.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

public class MainViewController {
    @FXML
    private Label lbbalance;
    @FXML
    private Label lbusername;
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
            lbusername.setText("Chào: " + UserSession.loggedInUser);
        }
    }

    @FXML
    private void onSearchAction() {
        String query = txtsearch.getText();
        // Implement search logic
    }
}
