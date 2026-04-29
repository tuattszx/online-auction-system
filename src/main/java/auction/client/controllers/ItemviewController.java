package auction.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Hyperlink;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ItemviewController {
    @FXML private Hyperlink btnShowMore;
    @FXML private VBox Vboxdetails;
    @FXML private LineChart<CategoryAxis,CategoryAxis> priceChart;
    @FXML
    public void onBackToMainClick(ActionEvent event){
        ViewManager.switchScene(event, "main-view.fxml", "Trang chủ");
    }
    @FXML
    public void OnMouseBacktoMain(MouseEvent event){
        ViewManager.switchScene(event,"main-view.fxml", "Trang chủ");

    }
    @FXML
    public void handleShowMore(ActionEvent event){
        Vboxdetails.setVisible(true);
        priceChart.setVisible(false);
        btnShowMore.setText("Show less");
    }
    @FXML
    public void handleShowLess(ActionEvent event){
        Vboxdetails.setVisible(false);
        priceChart.setVisible(false);
        btnShowMore.setText("Show more");
    }
    @FXML
    public void handleLineChart(MouseEvent event){
        priceChart.setVisible(true);
        Vboxdetails.setVisible(false);
    }
    @FXML
    public void onSellerClick(MouseEvent event) throws IOException {
        ViewManager.switchScene(event, "seller_demo.fxml", "seller page");
    }
    @FXML
    public void onProfileClick(MouseEvent event) throws IOException {
        if (UserSession.loggedInUser == null) return;

        String view = UserSession.loggedInUser.getRole().equals("ADMIN") ? "admin-view.fxml" : "profile-view.fxml";
        ViewManager.switchScene(event, view, "Hồ sơ cá nhân");
    }
}
