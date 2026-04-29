package auction.client.controllers;

import auction.client.ClientNetwork;
import auction.client.session.DataSession;
import auction.common.message.Message;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javax.swing.*;
import javax.swing.text.View;

public class AdminController {
    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnManageUsers;
    @FXML
    private Button btnApproveItems;
    @FXML
    private Button btnManageAuctions;
    @FXML
    private Button btnTransactionHistory;
    @FXML
    private Button btnSettings;
    @FXML
    private Button btnApproveSeller;
    @FXML
    private Button btnLockAccount;
    @FXML
    private TextField txtSearch;
    @FXML
    private TableView<?> adminTable;
    ClientNetwork network = ClientNetwork.getInstance();

    @FXML
    private void handleBackToMain(ActionEvent event) {
        ViewManager.switchScene(event, "main-view.fxml", "Main");
    }

    @FXML
    private void handleShowDashboard(ActionEvent event) {
        setActiveButton(btnDashboard);
    }

    @FXML
    private void handleManageUsers(ActionEvent event) {
        setActiveButton(btnManageUsers);
    }

    @FXML
    private void handleApproveItems(ActionEvent event) {
        setActiveButton(btnApproveItems);
    }

    @FXML
    private void handleManageAuctions(ActionEvent event) {
        setActiveButton(btnManageAuctions);
    }

    @FXML
    private void handleTransactionHistory(ActionEvent event) {
        setActiveButton(btnTransactionHistory);
    }

    @FXML
    private void handleSettings(ActionEvent event) {
        setActiveButton(btnSettings);
    }

    @FXML
    private void handleApproveSeller(ActionEvent event) {

    }
    @FXML
    private void onSignOutClick(ActionEvent event){
        if (!ViewManager.confirmAlert("Thông báo", "Bạn có chắc chắn muốn đăng xuất không?")) return;
        Task<Message> logoutTask = new Task<>() {
            @Override
            protected Message call() throws Exception {
                return network.sendRequest(new Message("SIGNOUT", null));
            }
        };

        logoutTask.setOnSucceeded(e -> {
            DataSession.getInstance().clear();
            ViewManager.showAlert(Alert.AlertType.INFORMATION,"Thông báo", "Đăng xuất thành công!");
            ViewManager.clearCache();
            network.close(); // Đóng socket ở phía Client
            ViewManager.switchScene(event, "login-view.fxml", "Đăng nhập");
        });

        new Thread(logoutTask).start();
    }
    @FXML
    private void handleLockAccount(ActionEvent event) {


    }

    private void setActiveButton(Button activeBtn) {
        // Remove active style from all buttons
        btnDashboard.getStyleClass().remove("admin-menu-btn-active");
        btnManageUsers.getStyleClass().remove("admin-menu-btn-active");
        btnApproveItems.getStyleClass().remove("admin-menu-btn-active");
        btnManageAuctions.getStyleClass().remove("admin-menu-btn-active");
        btnTransactionHistory.getStyleClass().remove("admin-menu-btn-active");
        btnSettings.getStyleClass().remove("admin-menu-btn-active");

        btnDashboard.getStyleClass().add("admin-menu-btn");
        btnManageUsers.getStyleClass().add("admin-menu-btn");
        btnApproveItems.getStyleClass().add("admin-menu-btn");
        btnManageAuctions.getStyleClass().add("admin-menu-btn");
        btnTransactionHistory.getStyleClass().add("admin-menu-btn");
        btnSettings.getStyleClass().add("admin-menu-btn");

        // Set active style to clicked button
        activeBtn.getStyleClass().remove("admin-menu-btn");
        activeBtn.getStyleClass().add("admin-menu-btn-active");
    }
}
